/*
 * Copyright (c) 2011 William Bittle  http://www.dyn4j.org/
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
package org.dyn4j.testbed.test;

import org.dyn4j.collision.Bounds;
import org.dyn4j.collision.RectangularBounds;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.joint.RopeJoint;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.testbed.ContactCounter;
import org.dyn4j.testbed.Entity;
import org.dyn4j.testbed.Test;

/**
 * Tests the rope joint.
 * @author William Bittle
 * @version 3.0.1
 * @since 2.2.1
 */
public class Rope extends Test {
	/* (non-Javadoc)
	 * @see org.dyn4j.testbed.Test#getName()
	 */
	@Override
	public String getName() {
		return "Rope";
	}
	
	/* (non-Javadoc)
	 * @see test.Test#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Tests a rope joint.";
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#initialize()
	 */
	@Override
	public void initialize() {
		// call the super method
		super.initialize();
		
		// setup the camera
		this.home();
		
		// create the world
		Bounds bounds = new RectangularBounds(Geometry.createRectangle(16.0, 15.0));
		this.world = new World(bounds);
		
		// setup the contact counter
		ContactCounter cc = new ContactCounter();
		this.world.setContactListener(cc);
		this.world.setStepListener(cc);
		
		// setup the bodies
		this.setup();
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#setup()
	 */
	@Override
	protected void setup() {
		// create the floor
		Rectangle floorRect = new Rectangle(15.0, 1.0);
		Entity floor = new Entity();
		floor.addFixture(new BodyFixture(floorRect));
		floor.setMass(Mass.Type.INFINITE);
		// move the floor down a bit
		floor.translate(0.0, -4.0);
		this.world.add(floor);
		
		/*
		 * Make this configuration
		 * +-----+
		 * |     |
		 * |     |
		 * |  .  |
		 * +--|--+
		 *    |
		 * +--|--+
		 * |  .  |
		 * |     |
		 * |     |
		 * +-----+
		 */
		
		// create a reusable rectangle
		Rectangle r = new Rectangle(0.5, 1.0);
		
		Entity obj1 = new Entity();
		obj1.addFixture(new BodyFixture(r));
		obj1.setMass();
		obj1.translate(2.0, 3.6);
		
		Entity obj2 = new Entity();
		obj2.addFixture(new BodyFixture(r));
		obj2.setMass();
		obj2.translate(2.0, 2.4);
		
		this.world.add(obj1);
		this.world.add(obj2);
		
		// compute the joint points
		Vector2 p1 = obj1.getWorldCenter().copy();
		Vector2 p2 = obj2.getWorldCenter().copy();
		p1.add(0.0, -0.4);
		p2.add(0.0, 0.4);
		
		// join them
		RopeJoint j1 = new RopeJoint(obj1, obj2, p1, p2);
		// VERY IMPORTANT FOR THIS JOINT
		// set and enable the limits
		j1.setLimitsEnabled(0.5, 1.5);
		j1.setCollisionAllowed(true);
		this.world.add(j1);
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#home()
	 */
	@Override
	public void home() {
		// set the scale
		this.scale = 64.0;
		// set the camera offset
		this.offset.set(0.0, 2.0);
	}
}