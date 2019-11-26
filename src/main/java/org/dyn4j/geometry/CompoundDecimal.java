package org.dyn4j.geometry;

import java.util.Arrays;

/**
 * This is an implementation of multi-precision decimals based on the original work by Jonathan Richard Shewchuk,
 * "Routines for Arbitrary Precision Floating-point Arithmetic and Fast Robust Geometric Predicates".
 * <p>
 * More information about the algorithms, the original code in C and proofs of correctness can all
 * be found at <a href="http://www.cs.cmu.edu/~quake/robust.html">http://www.cs.cmu.edu/~quake/robust.html</a>
 * <p>
 * Short description:
 * The value of this {@link CompoundDecimal} is represented as the sum of some components,
 * where each component is a double value.
 * The components must be stored in increasing magnitude order, but there can be any amount of
 * zeros between components. The components must also satisfy the non-overlapping property, that is
 * the corresponding bit representation of adjacent components must not overlap. See {@link #checkInvariants()}
 * and the corresponding paper for more info.
 * <p>
 * This code <b>requires</b> that the floating point model is IEEE-754 with round-to-even in order
 * to work properly in all cases and fulfill the above properties. This is not a problem because this
 * is the default and only model the Java specification describes.
 * 
 * @author Manolis Tsamis
 * @version 3.4.0
 * @since 3.4.0
 */
/* strictfp */ class CompoundDecimal {
	/** The array storing this {@link CompoundDecimal}'s component values */
	private final double[] components;
	
	/** The number of components this {@link CompoundDecimal} currently contains */
	private int size;
	
	/** An index that is used to sequentially iterate the components of this {@link CompoundDecimal} */
	private int mark;
	
	/**
	 * Advances the iteration mark to the next component
	 */
	public void advanceMark() {
		this.mark++;
	}
	
	/**
	 * Resets the iteration mark to point to the first element.
	 * Should be called to initiate the iteration.
	 */
	public void resetMark() {
		this.mark = 0;
	}
	
	/**
	 * @return The component currently pointed by the iteration mark.
	 */
	public double getCurrent() {
		return this.get(mark);
	}
	
	/**
	 * @return boolean true iff the iteration mark currently points
	 * to a valid component within the logical bounds of this {@link CompoundDecimal}
	 */
	public boolean hasMore() {
		return this.mark < this.size;
	}
	
	/**
	 * Creates a new {@link CompoundDecimal} with the specified length.
	 * The initial {@link CompoundDecimal} created does not contains any components.
	 * 
	 * @param length The maximum number of components this {@link CompoundDecimal} can store
	 */
	public CompoundDecimal(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.components = new double[length];
		this.size = 0;
	}
	
	/**
	 * Deep copy constructor.
	 * @param other the {@link CompoundDecimal} to copy from
	 */
	public CompoundDecimal(CompoundDecimal other) {
		this.components = Arrays.copyOf(other.components, other.capacity());
		this.size = other.size;
	}
	
	/**
	 * Internal helper constructor to create a {@link CompoundDecimal} with two components
	 * @param a0 the component with the smallest magnitude
	 * @param a1 the component with the largest magnitude
	 */
	protected CompoundDecimal(double a0, double a1) {
		this.components = new double[] {a0, a1};
		this.size = 2;
	}
	
	/**
	 * @return The number of components this {@link CompoundDecimal} currently has
	 */
	public int size() {
		return this.size;
	}
	
	/**
	 * @return The maximum number of components this {@link CompoundDecimal} can hold
	 */
	public int capacity() {
		return this.components.length;
	}
	
	/**
	 * @return A deep copy of this {@link CompoundDecimal}
	 */
	public CompoundDecimal copy() {
		return new CompoundDecimal(this);
	}
	
	/**
	 * Copies the components of another {@link CompoundDecimal} into this.
	 * The capacity of the this {@link CompoundDecimal} is not modified and it should
	 * be enough to hold all the components.
	 * 
	 * @param other The {@link CompoundDecimal} to copy from
	 */
	public void copyFrom(CompoundDecimal other) {
		System.arraycopy(other.components, 0, this.components, 0, other.size());
		this.size = other.size;
	}
	
	/**
	 * @param index index of the component to return
	 * @return the component at the specified position
	 * @throws IndexOutOfBoundsException if the index is not in the range [0, size)
	 */
	public double get(int index) {
		if (index < 0 || index >= this.size()) {
			throw new IndexOutOfBoundsException();
		}
		
		return this.components[index];
	}
	
	/**
	 * Appends a new component after all the existing components.
	 * 
	 * @param value The component
	 * @return this {@link CompoundDecimal}
	 * @throws IndexOutOfBoundsException if this {@link CompoundDecimal} has no capacity for more components
	 */
	public CompoundDecimal append(double value) {
		if (this.size >= this.capacity()) {
			throw new IndexOutOfBoundsException();
		}
		
		this.components[this.size++] = value;
		return this;
	}
	
	/**
	 * Appends a new component after all the existing components, but only
	 * if it has a non zero value.
	 * 
	 * @param value The component
	 * @return this {@link CompoundDecimal}
	 */
	public CompoundDecimal appendNonZero(double value) {
		if (value != 0.0) {
			this.append(value);
		}
		
		return this;
	}
	
    private static final long SIGNIF_BIT_MASK = 0x000FFFFFFFFFFFFFL;
    private static final long IMPLICIT_MANTISSA_BIT = 0x0010000000000000L;
    
    /**
     * Returns a boolean value describing if this {@link CompoundDecimal} is a valid
     * representation as described in the header of this class.
     * Checks for the magnitude and non-overlapping property.
     * The invariants can be violated if bad input components are appended to this {@link CompoundDecimal}.
     * The append methods do not check for those conditions because there is a big overhead for the check.
     * The output of the exposed operations must satisfy the invariants, given that their input also does so.
     * 
     * @return true iff this {@link CompoundDecimal} satisfies the described invariants
     */
	public boolean checkInvariants() {
		if (this.size == 0) {
			return true;
		}
		
		// Holds the last value that needs to be checked
		// This skips all 0 except for mabe the first component (which is ok to be 0)
		double lastValue = this.get(0);
		
		for (int i = 1; i < this.size; i++) {
			double currentValue = this.get(i);
			if (currentValue != 0) {
				// the magnitude of previous non-zero elements must be smaller
				if (Math.abs(currentValue) < Math.abs(lastValue)) {
					return false;
				}
				
				// get the exponents
				int exp1 = Math.getExponent(lastValue);
				int exp2 = Math.getExponent(currentValue);
				
				// get the significants
				long mantissa1 = (Double.doubleToLongBits(lastValue) & SIGNIF_BIT_MASK) | IMPLICIT_MANTISSA_BIT;
				long mantissa2 = (Double.doubleToLongBits(currentValue) & SIGNIF_BIT_MASK) | IMPLICIT_MANTISSA_BIT;
				
				// We want to find the logical location of the most significant bit in the smallest component
				// and of the least significant bit in the largest component, accounting for the exponents as well
				// In the following convention bit numbering is done from msd to lsd
				int msd1 = Long.numberOfLeadingZeros(mantissa1);
				int lsd2 = Long.SIZE - Long.numberOfTrailingZeros(mantissa2) - 1;
				
				// The exponents are essentially shifts in the bit positions
				msd1 -= exp1;
				lsd2 -= exp2;
				
				// The non-overlapping property
				if (!(lsd2 < msd1)) {
					return false;
				}
				
				// Update the last non-zero value
				lastValue = currentValue;
			}
		}
		
		return true;
	}
	
	/**
	 * @throws IllegalStateException iff {@link #checkInvariants()} returns false
	 */
	public void ensureInvariants() {
		if (!this.checkInvariants()) {
			throw new IllegalStateException();
		}
	}
	
	/**
	 * Removes the components of this {@link CompoundDecimal}.
	 * 
	 * @return this {@link CompoundDecimal}
	 */
	public CompoundDecimal clear() {
		this.size = 0;
		return this;
	}
	
	/**
	 * Removes all the components with zero value from this {@link CompoundDecimal}.
	 * 
	 * @return this {@link CompoundDecimal}
	 */
	public CompoundDecimal removeZeros() {
		int oldSize = this.size;
		this.clear();
		
		for (int i = 0; i < oldSize; i++) {
			this.appendNonZero(this.components[i]);
		}
		
		return this;
	}
	
	/**
	 * Ensures this {@link CompoundDecimal} has at least one component.
	 * That is, appends the zero value if there are currently zero components.
	 * 
	 * @return this {@link CompoundDecimal}
	 */
	public CompoundDecimal normalize() {
		if (this.size == 0) {
			append(0.0);
		}
		
		return this;
	}
	
	/**
	 * Negates the logical value of this {@link CompoundDecimal}.
	 * This can be used with sum to perform subtraction .
	 * 
	 * @return this {@link CompoundDecimal}
	 */
	public CompoundDecimal negate() {
		for (int i = 0; i < this.size; i++) {
			this.components[i] = -this.components[i];
		}
		
		return this;
	}
	
	/**
	 * Computes an approximation for the value of this {@link CompoundDecimal} that fits in a double.
	 * 
	 * @return The approximation
	 */
	public double getEstimation() {
		double value = 0.0;
		
		for (int i = 0; i < this.size; i++) {
			value += this.components[i];
		}
		
		return value;
	}
	
	/**
	 * Performs addition and also allocates a new {@link CompoundDecimal} with the 
	 * appropriate capacity to store the result.
	 * 
	 * @param f The {@link CompoundDecimal} to sum with this {@link CompoundDecimal}
	 * @return A new {@link CompoundDecimal} that holds the result of the addition
	 * @see #sum(CompoundDecimal, CompoundDecimal)
	 */
	public CompoundDecimal sum(CompoundDecimal f) {
		return this.sum(f, new CompoundDecimal(this.size() + f.size()));
	}
	
	/**
	 * Helper method to implement the sum procedure.
	 * Sums the remaining components of a single {@link CompoundDecimal} to the result
	 * and the initial carry value from previous computations
	 * 
	 * @param carry The carry from previous computations
	 * @param e The {@link CompoundDecimal} that probably has more components
	 * @param result The {@link CompoundDecimal} in which the result is stored
	 * @return The result
	 */
	CompoundDecimal sumEpilogue(double carry, CompoundDecimal e, CompoundDecimal result) {
		double error, sum;
		
		while (e.hasMore()) {
			double enow = e.getCurrent();
			error = fromSum(carry, enow, sum = carry + enow);
			carry = sum;
			result.appendNonZero(error);
			e.advanceMark();
		}
		
		result.appendNonZero(carry);
		result.normalize();
		
		return result;
	}
	
	/**
	 * Performs the addition of this {@link CompoundDecimal} with the given {@link CompoundDecimal} f
	 * and stores the result in the provided {@link CompoundDecimal} result.
	 * Calling this method will erase all existing components of result.
	 * 
	 * Be careful that it must be {@code f} &ne; {@code result} &ne; {@code this}.
	 * 
	 * @param f The {@link CompoundDecimal} to sum with this {@link CompoundDecimal}
	 * @param result The {@link CompoundDecimal} in which the sum is stored
	 * @return The result
	 */
	public CompoundDecimal sum(CompoundDecimal f, CompoundDecimal result) {
		CompoundDecimal e = this;
		
		// The following algorithm performs addition of two CompoundDecimals
		// It is based on the original fast_expansion_sum_zeroelim function written
		// by the author of the said paper
		
		result.clear();
		e.resetMark();
		f.resetMark();
		
		// enow and fnow is the current component examined from each of e and f
		double enow = e.getCurrent();
		double fnow = f.getCurrent();
		
		// sum will be used to store the sum needed for the fromSum method
		// error will store the error as returned from fromSum method
		// carry will store the value that will be summed in the next sum
		double carry, sum, error;
		
		// (fnow > enow) == (fnow > -enow)
		// each time we need the next component in increasing magnitude
		if (Math.abs(enow) <= Math.abs(fnow)) {
			carry = enow;
			e.advanceMark();
			
			if (!e.hasMore()) {
				return sumEpilogue(carry, f, result);
			}
			
			enow = e.getCurrent();
		} else {
			carry = fnow;
			f.advanceMark();
			
			if (!f.hasMore()) {
				return sumEpilogue(carry, e, result);
			}
			
			fnow = f.getCurrent();
		}
		
		while (true) {
			if (Math.abs(enow) <= Math.abs(fnow)) {
				// perform the addition with the carry from the previous iterarion
				error = fromSum(carry, enow, sum = carry + enow);
				e.advanceMark();
				carry = sum;
				
				// append + zero elimination
				result.appendNonZero(error);
				
				// if this CompoundDecimal has no more components then move to the epilogue
				if (!e.hasMore()) {
					return sumEpilogue(carry, f, result);
				}
				
				enow = e.getCurrent();
			} else {
				// perform the addition with the carry from the previous iterarion
				error = fromSum(carry, fnow, sum = carry + fnow);
				f.advanceMark();
				carry = sum;
				
				// append + zero elimination
				result.appendNonZero(error);
				
				// if this CompoundDecimal has no more components then move to the epilogue
				if (!f.hasMore()) {
					return sumEpilogue(carry, e, result);
				}
				
				fnow = f.getCurrent();
			}				
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.size * 10);
		sb.append('[');
		
		for (int i = 0; i < this.size; i++) {
			sb.append(this.components[i]);
			
			if (i < this.size - 1) {
				sb.append(", ");
			}
		}
		
		sb.append("] ~= ");
		sb.append(this.getEstimation());
		
		return sb.toString();
	}
	
	/**
	 * Creates a {@link CompoundDecimal} with only a single component.
	 * 
	 * @param value The component
	 * @return {@link CompoundDecimal}
	 */
	public static CompoundDecimal valueOf(double value) {
		return new CompoundDecimal(1).append(value);
	}
	
	/**
	 * Creates a {@link CompoundDecimal} that holds the result of the
	 * addition of two double values.
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @return A new {@link CompoundDecimal} that holds the resulting sum
	 */
	public static CompoundDecimal fromSum(double a, double b) {
		double sum = a + b;
		return new CompoundDecimal(fromSum(a, b, sum), sum);
	}
	
	/**
	 * Creates a {@link CompoundDecimal} that holds the result of the
	 * difference of two double values.
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @return A new {@link CompoundDecimal} that holds the resulting difference
	 */
	public static CompoundDecimal fromDiff(double a, double b) {
		double diff = a - b;
		return new CompoundDecimal(fromDiff(a, b, diff), diff);
	}

	/**
	 * Creates a {@link CompoundDecimal} that holds the result of the
	 * product of two double values.
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @return A new {@link CompoundDecimal} that holds the resulting product
	 */
	public static CompoundDecimal fromProduct(double a, double b) {
		double product = a * b;
		return new CompoundDecimal(fromProduct(a, b, product), a * b);
	}
	
	/**
	 * Given two values a, b and their sum = fl(a + b) calculates the value error for which
	 * fl(a) + fl(b) = fl(a + b) + fl(error).
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @param sum Their sum, must always be sum = fl(a + b)
	 * @return The error described above
	 */
	static double fromSum(double a, double b, double sum) {
		// the exact order of those operations is necessary for correct functionality 
		double bvirt = sum - a;
		double avirt = sum - bvirt;
		double bround = b - bvirt;
		double around = a - avirt;
		double error = around + bround;
		
		return error;
	}
	
	/**
	 * Given two values a, b and their difference = fl(a - b) calculates the value error for which
	 * fl(a) - fl(b) = fl(a - b) + fl(error).
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @param diff Their difference, must always be diff = fl(a - b)
	 * @return The error described above
	 */
	static double fromDiff(double a, double b, double diff) {
		// the exact order of those operations is necessary for correct functionality 
		double bvirt = a - diff;
		double avirt = diff + bvirt;
		double bround = bvirt - b;
		double around = a - avirt;
		double error = around + bround;
		
		return error;
	}
	
	/**
	 * Given two unrolled expansions (a0, a1) and (b0, b1) performs the difference
	 * (a0, a1) - (b0, b1) and stores the 4 component result in the given {@link CompoundDecimal} result.
	 * Does not perform zero elimination.
	 * This is also a helper method to allow fast computation of the cross product
	 * without the overhead of creating new {@link CompoundDecimal} and performing
	 * the generalized sum procedure.
	 * 
	 * @param a0 The first component of a
	 * @param a1 The second component of a
	 * @param b0 The first component of b
	 * @param b1 The second component of b
	 * @param result The {@link CompoundDecimal} in which the difference is stored
	 * @return The result
	 */
	static CompoundDecimal fromDiff2x2(double a0, double a1, double b0, double b1, CompoundDecimal result) {
		// the exact order of those operations is necessary for correct functionality 
		// This is a rewrite of the corresponding Two_Two_Diff macro in the original code
		
		// x0-x1-x2-x3 store the resulting components with increasing magnitude
		double x0, x1, x2, x3;
		
		// variable to store immediate results for each pair of Diff/Sum
		double imm;
		
		// variables to store immediate results across the two pairs 
		double imm1, imm2;
		
		// Diff (a0, a1) - b0, result = (x0, imm1, imm2)
		x0 = CompoundDecimal.fromDiff(a0, b0, imm = a0 - b0);
		imm1 = CompoundDecimal.fromSum(a1, imm, imm2 = a1 + imm);
		
		// Diff (imm1, imm2) - b1, result = (x1, x2, x3)
		x1 = CompoundDecimal.fromDiff(imm1, b1, imm = imm1 - b1);
		x2 = CompoundDecimal.fromSum(imm2, imm, x3 = imm2 + imm);
		
		result.clear();
		result.append(x0);
		result.append(x1);
		result.append(x2);
		result.append(x3);
		
		return result;
	}
	
	/**
	 * Given two values a, b and their product = fl(a * b) calculates the value error for which
	 * fl(a) * fl(b) = fl(a * b) + fl(error).
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @param product Their product, must always be product = fl(a * b)
	 * @return The error described above
	 */
	public static double fromProduct(double a, double b, double product) {
		// the exact order of those operations is necessary for correct functionality 
		
		// split a in two parts
		double ac = RobustGeometry.splitter * a;
		double abig = ac - a;
		double ahi = ac - abig;
		double alo = a - ahi;
		
		// split b in two parts
		double bc = RobustGeometry.splitter * b;
		double bbig = bc - b;
		double bhi = bc - bbig;
		double blo = b - bhi;
		
		double error1 = product - (ahi * bhi);
		double error2 = error1 - (alo * bhi);
		double error3 = error2 - (ahi * blo);
		double error = alo * blo - error3;
		
		return error;
	}
	
	/**
	 * Performs cross product on four primitives and also allocates a new {@link CompoundDecimal}
	 * with the appropriate capacity to store the result.
	 * 
	 * @param ax The x value of the vector a
	 * @param ay The y value of the vector a
	 * @param bx The x value of the vector b
	 * @param by The y value of the vector b
	 * @return The result
	 * @see #Cross_Product(double, double, double, double, CompoundDecimal)
	 */
	public static CompoundDecimal Cross_Product(double ax, double ay, double bx, double by) {
		return CompoundDecimal.Cross_Product(ax, ay, bx, by, new CompoundDecimal(4));
	}
	
	/**
	 * Performs the cross product of two vectors a, b, that is ax * by - ay * bx but with extended precision
	 * and stores the 4 component result in the given {@link CompoundDecimal} result.
	 * 
	 * @param ax The x value of the vector a
	 * @param ay The y value of the vector a
	 * @param bx The x value of the vector b
	 * @param by The y value of the vector b
	 * @param result The {@link CompoundDecimal} in which the cross product is stored
	 * @return The result
	 */
	public static CompoundDecimal Cross_Product(double ax, double ay, double bx, double by, CompoundDecimal result) {
		double axby = ax * by;
		double aybx = bx * ay;
		double axbyTail = CompoundDecimal.fromProduct(ax, by, axby);
		double aybxTail = CompoundDecimal.fromProduct(bx, ay, aybx);
		
		CompoundDecimal.fromDiff2x2(axbyTail, axby, aybxTail, aybx, result);
		
		return result;
	}
}