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
package org.dyn4j.game2d.collision.manifold;

import org.dyn4j.game2d.geometry.Vector;

/**
 * Represents a collision point.
 * @author William Bittle
 */
public class ManifoldPoint {
	/** The id for this manifold point */
	protected ManifoldPointId id;
	
	/** The point in world coordinates */
	protected Vector point;
	
	/** The penetration depth */
	protected double depth;
	
	/**
	 * Default constructor.
	 */
	public ManifoldPoint() {}
	
	/**
	 * Full constructor.
	 * @param id the id for this manifold point
	 * @param point the manifold point in world coordinates
	 * @param depth the penetration depth
	 */
	public ManifoldPoint(ManifoldPointId id, Vector point, double depth) {
		this.id = id;
		this.point = point;
		this.depth = depth;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MANIFOLD_POINT[")
		.append(this.id).append("|")
		.append(this.point).append("|")
		.append(this.depth).append("]");
		return sb.toString();
	}
	
	/**
	 * Returns the id for this manifold point.
	 * @return {@link ManifoldPointId}
	 */
	public ManifoldPointId getId() {
		return this.id;
	}
	
	/**
	 * Returns the point.
	 * @return {@link Vector} the point in world coordinates
	 */
	public Vector getPoint() {
		return this.point;
	}
	
	/**
	 * Returns the depth.
	 * @return double
	 */
	public double getDepth() {
		return this.depth;
	}
}
