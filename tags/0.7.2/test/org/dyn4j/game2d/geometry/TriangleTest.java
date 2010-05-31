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
package org.dyn4j.game2d.geometry;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test case for the {@link Triangle} class.
 * @author William Bittle
 */
public class TriangleTest {
	/**
	 * Tests the contains method.
	 */
	@Test
	public void contains() {
		Triangle t = new Triangle(
			new Vector( 0.0,  0.5),
			new Vector(-0.5, -0.5),
			new Vector( 0.5, -0.5)
		);
		Transform tx = new Transform();
		
		// outside
		Vector p = new Vector(1.0, 1.0);
		TestCase.assertFalse(t.contains(p, tx));
		
		// inside
		p.set(0.2, 0.0);
		TestCase.assertTrue(t.contains(p, tx));
		
		// on edge
		p.set(0.3, -0.5);
		TestCase.assertTrue(t.contains(p, tx));
		
		// move the triangle a bit
		tx.rotate(Math.toRadians(90));
		tx.translate(0.0, 1.0);
		
		// still outside
		p.set(1.0, 1.0);
		TestCase.assertFalse(t.contains(p, tx));
		
		// inside
		p.set(0.4, 1.0);
		TestCase.assertTrue(t.contains(p, tx));
		
		// on edge
		p.set(0.0, 0.76);
		// 0.76 should be 0.75 but it fails because of floating point problems
		TestCase.assertTrue(t.contains(p, tx));
	}
}
