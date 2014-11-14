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
import org.dyn4j.dynamics.joint.WheelJoint;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.testbed.ContactCounter;
import org.dyn4j.testbed.Entity;
import org.dyn4j.testbed.Test;

/**
 * Tests the wheel joint.
 * @author William Bittle
 * @version 3.0.1
 * @since 3.0.0
 */
public class Wheel extends Test {
	/* (non-Javadoc)
	 * @see org.dyn4j.testbed.Test#getName()
	 */
	@Override
	public String getName() {
		return "Wheel";
	}
	
	/* (non-Javadoc)
	 * @see test.Test#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Tests a couple of wheel joints in a car configuration.";
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
		
		// create a reusable rectangle
		Rectangle r = new Rectangle(3.0, 0.5);
		
		Entity frame = new Entity();
		frame.addFixture(new BodyFixture(r));
		frame.setMass();
		frame.translate(-3.0, 4.25);
		
		Entity wheelr = new Entity();
		wheelr.addFixture(Geometry.createCircle(0.25));
		wheelr.setMass();
		wheelr.translate(-4.0, 3.6);
		
		Entity wheelf = new Entity();
		wheelf.addFixture(Geometry.createCircle(0.25));
		wheelf.setMass();
		wheelf.translate(-2.0, 3.6);
		
		this.world.add(frame);
		this.world.add(wheelr);
		this.world.add(wheelf);
		
		WheelJoint j1 = new WheelJoint(frame, wheelr, wheelr.getWorldCenter(), new Vector2(0.0, 1.0));
		j1.setCollisionAllowed(true);
		// setup a spring
		j1.setFrequency(8.0);
		j1.setDampingRatio(0.4);
		// give the car rear-wheel-drive
		j1.setMotorEnabled(true);
		// set the speed to -180 degrees per second
		j1.setMotorSpeed(Math.PI);
		// don't forget to set the maximum torque
		j1.setMaximumMotorTorque(1000);
		
		WheelJoint j2 = new WheelJoint(frame, wheelf, wheelf.getWorldCenter(), new Vector2(0.0, 1.0));
		j2.setCollisionAllowed(true);
		// setup a spring
		j2.setFrequency(8.0);
		j2.setDampingRatio(0.4);
		
		// add the joints to the world
		this.world.add(j1);
		this.world.add(j2);
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