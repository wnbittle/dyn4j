package org.dyn4j.geometry.simplify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dyn4j.Epsilon;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Interval;
import org.dyn4j.geometry.Vector2;

// TODO this is the simple Douglas-Peucker algorithm that doesn't handle self-intersection
public final class DouglasPeucker extends AbstractSimplifier implements Simplifier {
	// TODO this should be an input
	private final double clusterTolerance;
	private final double e;
	private final boolean avoidSelfIntersection;
	
	private final RTree tree;
	
	public DouglasPeucker(double clusterTolerance, double epsilon) {
		this.clusterTolerance = clusterTolerance;
		this.e = epsilon;
		this.avoidSelfIntersection = true;
		this.tree = new RTree();
	}
	
	public List<Vector2> simplify(List<Vector2> vertices) {
		if (vertices == null) {
			return vertices;
		}
		
		if (vertices.size() < 4) {
			return vertices;
		}
		
		List<Vector2> result = new ArrayList<Vector2>();
		
		// 0. first reduce any clustered vertices in the polygon
		vertices = this.simplifyClusteredVertices(vertices, this.clusterTolerance);
		
		int size = vertices.size();
		List<Vertex> verts = new ArrayList<Vertex>();
		
		Vector2 v1 = vertices.get(0);
		Vector2 v2 = vertices.get(1);
		
		Vertex vertex = new Vertex();
		vertex.point = v1;
		vertex.index = 0;
		vertex.next = null;
		vertex.prev = null;
		vertex.prevSegment = null;
		vertex.nextSegment = null;
		
		if (this.avoidSelfIntersection) {
			// create the segments and add to RTree
			RTreeLeaf nextSegment = new RTreeLeaf(v1, v2, 0, 1);
			vertex.nextSegment = nextSegment;
			tree.add(nextSegment);
		}
		
		// reference the segments on the vertices (so we can remove/add when we remove add vertices)
		
		Vertex first = vertex;
		Vertex prev = vertex;
		for (int i = 0; i < size; i++) {
			int i0 = i - 1;
			int i2 = i + 1 == size ? 0 : i + 1;
			
			v1 = vertices.get(i);
			v2 = vertices.get(i2);
			
			vertex = new Vertex();
			vertex.point = v1;
			vertex.index = i;
			vertex.next = null;
			vertex.prev = prev;

			if (this.avoidSelfIntersection) {
				vertex.prevSegment = prev.nextSegment;
				vertex.nextSegment = new RTreeLeaf(v1, v2, i, i2);
				tree.add(vertex.nextSegment);
			}
			
			prev.next = vertex;
			prev = vertex;
			verts.add(vertex);	
		}
		
		first.prev = prev;
		prev.next = first;
		
		if (this.avoidSelfIntersection) {
			first.prevSegment = prev.nextSegment;
		}
		
		
		// 1. find two points to split the poly into two halves
		// for example:
		// 0-------------0        A-------------0  |                0
		// |              \       |                |                 \
		// |               \  =>  |                &                  \
		// |                \     |                |                   \
		// 0-----------------0    0                |  0-----------------B
		int startIndex = 0;
		int endIndex = this.getFartherestVertexFromVertex(startIndex, vertices);
		
		// 2. split into two polylines to simplify
		List<Vector2> aReduced = this.douglasPeucker(verts.subList(startIndex, endIndex + 1));
		List<Vector2> bReduced = this.douglasPeucker(verts.subList(endIndex, vertices.size()));
		
		// 3. merge the two polylines back together
		result.addAll(aReduced.subList(0, aReduced.size() - 1));
		result.addAll(bReduced);
		
		this.tree.clear();
		
		return result;
	}
	
	/**
	 * Recursively sub-divide the given polyline performing the douglas Peucker algorithm.
	 * <p>
	 * O(mn) in worst case, O(n log m) in best case, where n is the number of vertices in the
	 * original polyline and m is the number of vertices in the reduced polyline.
	 * @param polyline
	 * @return
	 */
	private List<Vector2> douglasPeucker(List<Vertex> polyline) {
		int size = polyline.size();

		// get the start/end vertices of the polyline
		Vector2 start = polyline.get(0).point;
		Vector2 end = polyline.get(size - 1).point;
		
		// get the farthest vertex from the line created from the start to the end
		// vertex on the polyline
		FarthestVertex fv = this.getFarthestVertexFromLine(start, end, polyline);
		
		// check the farthest point's distance - if it's higher than the minimum
		// distance epsilon, then we need to subdivide the polyline since we can't
		// reduce here (we might be able to reduce elsewhere)
		List<Vector2> result = new ArrayList<Vector2>();
		if (fv.distance > e) {
			// sub-divide and run the algo on each half
			List<Vector2> aReduced = this.douglasPeucker(polyline.subList(0, fv.index + 1));
			List<Vector2> bReduced = this.douglasPeucker(polyline.subList(fv.index, size));
			
			// recombine the reduced polylines
			result.addAll(aReduced.subList(0, aReduced.size() - 1));
			result.addAll(bReduced);
		} else {
			if (this.avoidSelfIntersection && this.intersects(polyline.get(0), polyline.get(size - 1))) {
				for (int i = 0; i < size; i++) {
					result.add(polyline.get(i).point);
				}
				return result;
			}
			
			if (this.avoidSelfIntersection && size >= 3) {
				// remove all segments from the r-tree in between these vertices
				Vertex s = polyline.get(0);
				Vertex e = polyline.get(size - 1);
				
				Vertex b = s;
				while (b != e) {
					this.tree.remove(b.nextSegment);
					b = b.next;
				}
				
				s.next = e;
				e.prev = s;
				s.nextSegment = new RTreeLeaf(start, end, s.index, e.index);
				e.prevSegment = s.nextSegment;
				
				this.tree.add(s.nextSegment);
			}
			
			// just use the start/end vertices
			// as the new polyline
			result.add(start);
			result.add(end);
		}
		
		return result;
	}
	
	/**
	 * Returns the vertex farthest from the given vertex.
	 * <p>
	 * O(n)
	 * @param index
	 * @param polygon
	 * @return
	 */
	private int getFartherestVertexFromVertex(int index, List<Vector2> polygon) {
		double dist2 = 0.0;
		int max = -1;
		int size = polygon.size();
		Vector2 vertex = polygon.get(index);
		for (int i = 0; i < size; i++) {
			Vector2 vert = polygon.get(i);
			double test = vertex.distanceSquared(vert);
			if (test > dist2) {
				dist2 = test;
				max = i;
			}
		}
		
		return max;
	}
	
	/**
	 * Returns the farthest vertex in the polyline from the line created by lineVertex1 and lineVertex2.
	 * <p>
	 * O(n)
	 * @param lineVertex1
	 * @param lineVertex2
	 * @param polyline
	 * @return
	 */
	private FarthestVertex getFarthestVertexFromLine(Vector2 lineVertex1, Vector2 lineVertex2, List<Vertex> polyline) {
		FarthestVertex max = new FarthestVertex();
		int size = polyline.size();
		Vector2 line = lineVertex1.to(lineVertex2);
		Vector2 lineNormal = line.getLeftHandOrthogonalVector();
		lineNormal.normalize();
		for (int i = 0; i < size; i++) {
			Vector2 vert = polyline.get(i).point;
			double test = Math.abs(lineVertex1.to(vert).dot(lineNormal));
			if (test > max.distance) {
				max.distance = test;
				max.index = i;
			}
		}
		return max;
	}
	

	public boolean intersects(Vertex ve1, Vertex ve2) {
		Vector2 v1 = ve1.point;
		Vector2 v2 = ve2.point;

		int min = ve1.index < ve2.index ? ve1.index : ve2.index;
		int max = ve1.index > ve2.index ? ve1.index : ve2.index;
		
		AABB aabb = AABB.createFromPoints(v1, v2);
		Iterator<RTreeLeaf> bp = tree.getAABBDetectIterator(aabb);
		while (bp.hasNext()) {
			RTreeLeaf leaf = bp.next();

			if (leaf.index1 >= min && leaf.index2 <= max ||
				leaf.index1 <= max && leaf.index2 >= min) {
				continue;
			}
			
			// we need to verify the segments truly overlap
			if (intersects(v1, v2, leaf.point1, leaf.point2)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean intersects(Vector2 a1, Vector2 a2, Vector2 b1, Vector2 b2) {
		Vector2 A = a1.to(a2);
		Vector2 B = b1.to(b2);

		// compute the bottom
		double BxA = B.cross(A);
		// compute the top
		double ambxA = a1.difference(b1).cross(A);
		
		// if the bottom is zero, then the segments are either parallel or coincident
		if (Math.abs(BxA) <= Epsilon.E) {
			// if the top is zero, then the segments are coincident
			if (Math.abs(ambxA) <= Epsilon.E) {
				// project the segment points onto the segment vector (which
				// is the same for A and B since they are coincident)
				A.normalize();
				double ad1 = a1.dot(A);
				double ad2 = a2.dot(A);
				double bd1 = b1.dot(A);
				double bd2 = b2.dot(A);
				
				// then compare their location on the number line for intersection
				Interval ia = new Interval(ad1, ad2);
				Interval ib = new Interval(bd1 < bd2 ? bd1 : bd2, bd1 > bd2 ? bd1 : bd2);
				
				if (ia.overlaps(ib)) {
					return true;
				}
			}
			
			// otherwise they are parallel
			return false;
		}
		
		// if just the top is zero, then there's no intersection
		if (Math.abs(ambxA) <= Epsilon.E) {
			return false;
		}
		
		// compute tb
		double tb = ambxA / BxA;
		if (tb <= 0.0 || tb >= 1.0) {
			// no intersection
			return false;
		}
		
		// compute the intersection point
		Vector2 ip = B.product(tb).add(b1);
		
		// since both are segments we need to verify that
		// ta is also valid.
		// compute ta
		double ta = ip.difference(a1).dot(A) / A.dot(A);
		if (ta <= 0.0 || ta >= 1.0) {
			// no intersection
			return false;
		}
		
		return true;
		
//		// solve the problem algebraically
//		Vector2 p0 = a1;
//		Vector2 d0 = a1.to(a2);
//		
//		Vector2 p1 = b1;
//		Vector2 p2 = b2;
//		Vector2 d1 = b1.to(b2);
//		
//		// is the segment vertical or horizontal?
//		boolean isVertical = Math.abs(d1.x) <= Epsilon.E;
//		boolean isHorizontal = Math.abs(d1.y) <= Epsilon.E;
//		
//		// if it's both, then it's degenerate
//		if (isVertical && isHorizontal) {
//			// it's a degenerate line segment
//			return false;
//		}
//		
//		// any point on a ray can be found by the parametric equation:
//		// P = tD0 + P0
//		// any point on a segment can be found by:
//		// P = sD1 + P1
//		// substituting the first equation into the second yields:
//		// tD0 + P0 = sD1 + P1
//		// solve for s and t:
//		// tD0.x + P0.x = sD1.x + P1.x
//		// tD0.y + P0.y = sD1.y + P1.y
//		// solve the first equation for s
//		// s = (tD0.x + P0.x - P1.x) / D1.x
//		// substitute into the second equation
//		// tD0.y + P0.y = ((tD0.x + P0.x - P1.x) / D1.x) * D1.y + P1.y
//		// solve for t
//		// tD0.yD1.x + P0.yD1.x = tD0.xD1.y + P0.xD1.y - P1.xD1.y + P1.yD1.x
//		// t(D0.yD1.x - D0.xD1.y) = P0.xD1.y - P0.yD1.x + D1.xP1.y - D1.yP1.x
//		// t(D0.yD1.x - D0.xD1.y) = P0.cross(D1) + D1.cross(P1)
//		// since the cross product is anti-cummulative
//		// t(D0.yD1.x - D0.xD1.y) = -D1.cross(P0) + D1.cross(P1)
//		// t(D0.yD1.x - D0.xD1.y) = D1.cross(P1) - D1.cross(P0)
//		// t(D0.yD1.x - D0.xD1.y) = D1.cross(P1 - P0)
//		// tD1.cross(D0) = D1.cross(P1 - P0)
//		// t = D1.cross(P1 - P0) / D1.cross(D0)
//		Vector2 p0ToP1 = p1.difference(p0);
//		double num = d1.cross(p0ToP1);
//		double den = d1.cross(d0);
//		
//		// check for zero denominator
//		if (Math.abs(den) <= Epsilon.E) {
//			// they are parallel but could be overlapping
//			
//			// since they are parallel d0 is the direction for both the
//			// segment and the ray; ie d0 = d1
//			
//			// get the common direction's normal
//			Vector2 n = d0.getRightHandOrthogonalVector();
//			// project a point from each onto the normal
//			double nDotP0 = n.dot(p0);
//			double nDotP1 = n.dot(p1);
//			// project the segment and ray onto the common direction's normal
//			if (Math.abs(nDotP0 - nDotP1) < Epsilon.E) {
//				// if their projections are close enough then they are
//				// on the same line
//				
//				// project the ray start point onto the ray direction
//				double d0DotP0 = d0.dot(p0);
//				
//				// project the segment points onto the ray direction
//				// and subtract the ray start point to receive their
//				// location on the ray direction relative to the ray
//				// start
//				double d0DotP1 = d0.dot(p1) - d0DotP0;
//				double d0DotP2 = d0.dot(p2) - d0DotP0;
//				
//				// if one or both are behind the ray, then
//				// we consider this a non-intersection
//				if (d0DotP1 < 0.0 || d0DotP2 < 0.0) {
//					// if either point is behind the ray
//					return false;
//				}
//				
//				return true;
//			} else {
//				// parallel but not overlapping
//				return false;
//			}
//		}
//		
//		// compute t
//		double t = num / den;
//		
//		// t should be in the range t >= 0.0
//		if (t < 0.0) {
//			return false;
//		}
//		
//		double s = 0;
//		if (isVertical) {
//			// use the y values to compute s
//			s = (t * d0.y + p0.y - p1.y) / d1.y;
//		} else {
//			// use the x values to compute s
//			s = (t * d0.x + p0.x - p1.x) / d1.x;
//		}
//		
//		// s should be in the range 0.0 <= s <= 1.0
//		if (s < 0.0 || s > 1.0) {
//			return false;
//		}
//		
//		// return success
//		return true;
		
	}
	
	private class FarthestVertex {
		int index;
		double distance;
	}
	
	private class Vertex {
		/** The index of the vertex in the original simple polygon */
		public int index;
		
		/** The next vertex */
		public Vertex next;
		
		/** The prev vertex */
		public Vertex prev;
		
		/** The vertex point */
		public Vector2 point;
		
		// only used if avoiding self intersection
		
		public RTreeLeaf prevSegment;
		public RTreeLeaf nextSegment;

		@Override
		public String toString() {
			return this.point.toString();
		}
	}
}