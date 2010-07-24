/*
 * Copyright (c) 2010, William Bittle
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted 
 * provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions 
 *     and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 *     and the following disclaimer in the documentation and/or other materials provided with the 
 *     distribution.
 *   * Neither the name of dyn4j nor the names of its contributors may be used to endorse or 
 *     promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dyn4j.game2d.collision.narrowphase;

import org.dyn4j.game2d.geometry.Circle;
import org.dyn4j.game2d.geometry.Transform;
import org.dyn4j.game2d.geometry.Vector2;

/**
 * Abstract implementation of a narrow-phase collision detection algorithm.
 * <p>
 * Contains fast, shape type specific methods for collision detection.
 * @author William Bittle
 * @version 1.0.3
 * @since 1.0.0
 */
public abstract class AbstractNarrowphaseDetector implements NarrowphaseDetector {
	/**
	 * Fast method for determining a collision between two {@link Circle}s.
	 * <p>
	 * Returns true if the given {@link Circle}s are intersecting and places the
	 * penetration vector and depth in the given {@link Penetration} object.
	 * <p>
	 * If the {@link Circle} centers are coincident then the penetration {@link Vector2}
	 * will be the zero {@link Vector2}, however, the penetration depth will be
	 * correct.  In this case its up to the caller to determine a reasonable penetration
	 * {@link Vector2}.
	 * @param circle1 the first {@link Circle}
	 * @param transform1 the first {@link Circle}'s {@link Transform}
	 * @param circle2 the second {@link Circle}
	 * @param transform2 the second {@link Circle}'s {@link Transform}
	 * @param penetration the {@link Penetration} object to fill
	 * @return boolean
	 */
	public boolean detect(Circle circle1, Transform transform1, Circle circle2, Transform transform2, Penetration penetration) {
		// get their world centers
		Vector2 ce1 = transform1.getTransformed(circle1.getCenter());
		Vector2 ce2 = transform2.getTransformed(circle2.getCenter());
		// create a vector from one center to the other
		Vector2 v = ce1.to(ce2);
		// check the magnitude against the sum of the radii
		double radii = circle1.getRadius() + circle2.getRadius();
		// get the magnitude squared
		double mag = v.getMagnitude();
		// check difference
		if (mag < radii) {
			// then we have a collision
			penetration.normal = v;
			penetration.depth = radii - v.normalize();
			return true;
		}
		return false;
	}
	
	/**
	 * Fast method for determining a collision between two {@link Circle}s.
	 * @param circle1 the first {@link Circle}
	 * @param transform1 the first {@link Circle}'s {@link Transform}
	 * @param circle2 the second {@link Circle}
	 * @param transform2 the second {@link Circle}'s {@link Transform}
	 * @return boolean true if the two circles intersect
	 */
	public boolean detect(Circle circle1, Transform transform1, Circle circle2, Transform transform2) {
		// get their world centers
		Vector2 ce1 = transform1.getTransformed(circle1.getCenter());
		Vector2 ce2 = transform2.getTransformed(circle2.getCenter());
		// create a vector from one center to the other
		Vector2 v = ce1.to(ce2);
		// check the magnitude against the sum of the radii
		double radii = circle1.getRadius() + circle2.getRadius();
		// get the magnitude squared
		double mag = v.getMagnitude();
		// check difference
		if (mag < radii) {
			// then we have a collision
			return true;
		}
		return false;
	}
	
	/**
	 * Fast method for determining the distance between two {@link Circle}s.
	 * <p>
	 * Returns true if the given {@link Circle}s are separated and places the
	 * separating vector and distance in the given {@link Separation} object.
	 * @param circle1 the first {@link Circle}
	 * @param transform1 the first {@link Circle}'s {@link Transform}
	 * @param circle2 the second {@link Circle}
	 * @param transform2 the second {@link Circle}'s {@link Transform}
	 * @param separation the {@link Separation} object to fill
	 * @return boolean
	 */
	public boolean distance(Circle circle1, Transform transform1, Circle circle2, Transform transform2, Separation separation) {
		// get their world centers
		Vector2 ce1 = transform1.getTransformed(circle1.getCenter());
		Vector2 ce2 = transform2.getTransformed(circle2.getCenter());
		// get the radii
		double r1 = circle1.getRadius();
		double r2 = circle2.getRadius();
		// create a vector from one center to the other
		Vector2 v = ce1.to(ce2);
		// check the magnitude against the sum of the radii
		double radii = r1 + r2;
		// get the magnitude squared
		double mag = v.getMagnitude();
		// check difference
		if (mag >= radii) {
			// then the circles are separated
			separation.normal = v;
			separation.distance = v.normalize() - radii;
			separation.point1 = ce1.add(v.x * r1, v.y * r1);
			separation.point2 = ce2.add(-v.x * r2, -v.y * r2);
			return true;
		}
		return false;
	}
}
