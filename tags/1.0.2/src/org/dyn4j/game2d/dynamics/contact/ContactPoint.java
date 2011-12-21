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
package org.dyn4j.game2d.dynamics.contact;

import org.dyn4j.game2d.dynamics.Body;
import org.dyn4j.game2d.dynamics.Fixture;
import org.dyn4j.game2d.geometry.Vector2;

/**
 * Represents a contact point and used to report events via the {@link ContactListener}.
 * @author William Bittle
 */
public class ContactPoint {
	/** The first {@link Body} in contact */
	protected Body body1;
	
	/** The second {@link Body} in contact */
	protected Body body2;
	
	/** The first {@link Body}'s {@link Fixture} */
	protected Fixture fixture1;
	
	/** The second {@link Body}'s {@link Fixture} */
	protected Fixture fixture2;
	
	/** Whether this contact point is enabled or not */
	protected boolean enabled;
	
	/** The world space contact point */
	protected Vector2 point;
	
	/** The world space contact normal */
	protected Vector2 normal;
	
	/** The penetration depth */
	protected double depth;
	
	/** Default constructor */
	public ContactPoint() {}
	
	/**
	 * Full constructor.
	 * @param body1 the first {@link Body} in contact
	 * @param fixture1 the first {@link Body}'s {@link Fixture}
	 * @param body2 the second {@link Body} in contact
	 * @param fixture2 the second {@link Body}'s {@link Fixture}
	 * @param enabled true if this contact point is enabled
	 * @param point the world space contact point
	 * @param normal the world space contact normal
	 * @param depth the penetration depth
	 */
	public ContactPoint(Body body1, Fixture fixture1, Body body2, Fixture fixture2,
			boolean enabled, Vector2 point, Vector2 normal, double depth) {
		this.body1 = body1;
		this.fixture1 = fixture1;
		this.body2 = body2;
		this.fixture2 = fixture2;
		this.enabled = enabled;
		this.point = point;
		this.normal = normal;
		this.depth = depth;
	}
	
	/**
	 * Copy constructor (shallow).
	 * @param contactPoint the {@link ContactPoint} to copy
	 */
	public ContactPoint(ContactPoint contactPoint) {
		if (contactPoint == null) throw new NullPointerException("Cannot copy a null contact point.");
		// shallow copy all the fields
		this.body1 = contactPoint.body1;
		this.fixture1 = contactPoint.fixture1;
		this.body2 = contactPoint.body2;
		this.fixture2 = contactPoint.fixture2;
		this.enabled = contactPoint.enabled;
		this.point = contactPoint.point;
		this.normal = contactPoint.normal;
		this.depth = contactPoint.depth;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CONTACT_POINT[")
		.append(this.body1).append("|")
		.append(this.fixture1).append("|")
		.append(this.body2).append("|")
		.append(this.fixture2).append("|")
		.append(this.enabled).append("|")
		.append(this.point).append("|")
		.append(this.normal).append("|")
		.append(this.depth).append("]");
		return sb.toString();
	}
	
	/**
	 * Returns true if this contact point is enabled.
	 * @return boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Returns the contact point.
	 * @return {@link Vector2}
	 */
	public Vector2 getPoint() {
		return point;
	}
	
	/**
	 * Returns the normal.
	 * @return {@link Vector2}
	 */
	public Vector2 getNormal() {
		return normal;
	}
	
	/**
	 * Returns the depth.
	 * @return double
	 */
	public double getDepth() {
		return depth;
	}
	
	/**
	 * Returns the first {@link Body}.
	 * @return {@link Body}
	 */
	public Body getBody1() {
		return body1;
	}
	
	/**
	 * Returns the second {@link Body}.
	 * @return {@link Body}
	 */
	public Body getBody2() {
		return body2;
	}
	
	/**
	 * Returns the first {@link Body}'s {@link Fixture}.
	 * @return {@link Fixture}
	 */
	public Fixture getFixture1() {
		return fixture1;
	}
	
	/**
	 * Returns the second {@link Body}'s {@link Fixture}.
	 * @return {@link Fixture}
	 */
	public Fixture getFixture2() {
		return fixture2;
	}
}