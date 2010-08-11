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
package org.dyn4j.game2d.dynamics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.dyn4j.game2d.collision.Bounds;
import org.dyn4j.game2d.collision.BoundsAdapter;
import org.dyn4j.game2d.collision.BoundsListener;
import org.dyn4j.game2d.collision.Filter;
import org.dyn4j.game2d.collision.broadphase.BroadphaseDetector;
import org.dyn4j.game2d.collision.broadphase.BroadphasePair;
import org.dyn4j.game2d.collision.broadphase.Sap;
import org.dyn4j.game2d.collision.continuous.ConservativeAdvancement;
import org.dyn4j.game2d.collision.continuous.TimeOfImpact;
import org.dyn4j.game2d.collision.continuous.TimeOfImpactDetector;
import org.dyn4j.game2d.collision.manifold.ClippingManifoldSolver;
import org.dyn4j.game2d.collision.manifold.Manifold;
import org.dyn4j.game2d.collision.manifold.ManifoldSolver;
import org.dyn4j.game2d.collision.narrowphase.Gjk;
import org.dyn4j.game2d.collision.narrowphase.NarrowphaseDetector;
import org.dyn4j.game2d.collision.narrowphase.Penetration;
import org.dyn4j.game2d.collision.narrowphase.Separation;
import org.dyn4j.game2d.dynamics.contact.Contact;
import org.dyn4j.game2d.dynamics.contact.ContactAdapter;
import org.dyn4j.game2d.dynamics.contact.ContactConstraint;
import org.dyn4j.game2d.dynamics.contact.ContactEdge;
import org.dyn4j.game2d.dynamics.contact.ContactListener;
import org.dyn4j.game2d.dynamics.contact.ContactManager;
import org.dyn4j.game2d.dynamics.contact.ContactPoint;
import org.dyn4j.game2d.dynamics.joint.Joint;
import org.dyn4j.game2d.dynamics.joint.JointEdge;
import org.dyn4j.game2d.geometry.Convex;
import org.dyn4j.game2d.geometry.Transform;
import org.dyn4j.game2d.geometry.Vector2;

/**
 * Manages the logic of collision detection, resolution, and reporting.
 * <p>
 * Employs the same {@link Island} solving technique as <a href="http://www.box2d.org">Box2d</a>'s equivalent class.
 * @see <a href="http://www.box2d.org">Box2d</a>
 * @author William Bittle
 * @version 1.2.0
 * @since 1.0.0
 */
public class World {
	/** Earths gravity constant */
	public static final Vector2 EARTH_GRAVITY = new Vector2(0.0, -9.8);
	
	/** Zero gravity constant */
	public static final Vector2 ZERO_GRAVITY = new Vector2(0.0, 0.0);
		
	/** The {@link Step} used by the dynamics calculations */
	protected Step step;
	
	/** The world gravity vector */
	protected Vector2 gravity;
	
	/** The world {@link Bounds} */
	protected Bounds bounds;
	
	/** The {@link BroadphaseDetector} */
	protected BroadphaseDetector broadphaseDetector;
	
	/** The {@link NarrowphaseDetector} */
	protected NarrowphaseDetector narrowphaseDetector;
	
	/** The {@link ManifoldSolver} */
	protected ManifoldSolver manifoldSolver;
	
	/** The {@link TimeOfImpactDetector} */
	protected TimeOfImpactDetector timeOfImpactDetector;
	
	/** The {@link CollisionListener} */
	protected CollisionListener collisionListener;
	
	/** The {@link ContactManager} */
	protected ContactManager contactManager;

	/** The {@link ContactListener} */
	protected ContactListener contactListener;
	
	/** The {@link TimeOfImpactListener} */
	protected TimeOfImpactListener timeOfImpactListener;
	
	/** The {@link BoundsListener} */
	protected BoundsListener boundsListener;
	
	/** The {@link DestructionListener} */
	protected DestructionListener destructionListener;
	
	/** The {@link StepListener} */
	protected StepListener stepListener;
	
	/** The {@link CoefficientMixer} */
	protected CoefficientMixer coefficientMixer;
	
	/** The {@link Body} list */
	protected List<Body> bodies;
	
	/** The {@link Joint} list */
	protected List<Joint> joints;
	
	/** The reusable island */
	protected Island island;
	
	/** The accumulated time */
	protected double time;
	
	/**
	 * Default constructor.
	 * <p>
	 * Builds a simulation {@link World} without bounds.
	 */
	public World() {
		this(Bounds.UNBOUNDED);
	}
	
	/**
	 * Full constructor.
	 * <p>
	 * Defaults to using {@link #EARTH_GRAVITY}, {@link Sap} broad-phase,
	 * {@link Gjk} narrow-phase, and {@link ClippingManifoldSolver}.
	 * @param bounds the bounds of the {@link World}
	 */
	public World(Bounds bounds) {
		// check for null bounds
		if (bounds == null) throw new NullPointerException("The bounds cannot be null.  Use the Bounds.UNBOUNDED object to have no bounds.");
		// initialize all the classes with default values
		this.step = new Step();
		this.gravity = World.EARTH_GRAVITY;
		this.bounds = bounds;
		this.broadphaseDetector = new Sap();
		this.narrowphaseDetector = new Gjk();
		this.manifoldSolver = new ClippingManifoldSolver();
		this.timeOfImpactDetector = new ConservativeAdvancement();
		// create empty listeners
		this.collisionListener = new CollisionAdapter();
		this.contactManager = new ContactManager();
		this.contactListener = new ContactAdapter();
		this.timeOfImpactListener = new TimeOfImpactAdapter();
		this.boundsListener = new BoundsAdapter();
		this.destructionListener = new DestructionAdapter();
		this.stepListener = new StepAdapter();
		this.coefficientMixer = CoefficientMixer.DEFAULT_MIXER;
		this.bodies = new ArrayList<Body>();
		this.joints = new ArrayList<Joint>();
		this.island = new Island();
		this.time = 0.0;
	}
	
	/**
	 * Updates the {@link World}.
	 * <p>
	 * This method will only update the world given the step frequency contained
	 * in the {@link Settings} object.  You can use the {@link StepListener} interface
	 * to listen for when a step is actually performed.  In addition, this method will
	 * return true if a step was performed.
	 * <p>
	 * Alternatively you can call the {@link #update(double)} method to use a variable
	 * time step.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @see #update(double)
	 * @param elapsedTime the elapsed time in seconds
	 * @return boolean true if the {@link World} performed a simulation step
	 */
	public boolean update(double elapsedTime) {
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return false;
		// update the time
		this.time += elapsedTime;
		// check the frequency in settings
		double invhz = Settings.getInstance().getStepFrequency();
		// see if we should update or not
		if (this.time >= invhz) {
			// update the step
			this.step.update(invhz);
			// reset the time
			this.time = this.time - invhz;
			// step the world
			this.step();
			// return true indicating we performed a simulation step
			return true;
		}
		return false;
	}
	
	/**
	 * Updates the {@link World}.
	 * <p>
	 * This method will update the world on every call.  Unlike the {@link #update(double)}
	 * method, this method uses the given elapsed time and does not attempt to update the world
	 * in a set interval.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @see #update(double)
	 * @param elapsedTime the elapsed time in seconds
	 */
	public void updatev(double elapsedTime) {
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return;
		// update the step
		this.step.update(elapsedTime);
		// step the world
		this.step();
	}
	
	/**
	 * Performs the given number of simulation steps using the step frequency in {@link Settings}.
	 * @param steps the number of simulation steps to perform
	 */
	public void step(int steps) {
		// get the frequency from settings
		double invhz = Settings.getInstance().getStepFrequency();
		// perform the steps
		this.step(steps, invhz);
	}
	
	/**
	 * Performs the given number of simulation steps using the given step frequency.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @param steps the number of simulation steps to perform
	 * @param elapsedTime the elapsed time for each step
	 */
	public void step(int steps, double elapsedTime) {
		// make sure the number of steps is greather than zero
		if (steps <= 0) return;
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return;
		// perform the steps
		for (int i = 0; i < steps; i++) {
			// update the step object
			this.step.update(elapsedTime);
			// step the world
			this.step();
		}
	}
	
	/**
	 * Steps the {@link World} using the current {@link Step}.
	 * <p>
	 * Performs collision detection and resolution.
	 * <p>
	 * Use the various listeners to listen for events during the execution of
	 * this method.
	 * <p>
	 * Take care when performing methods on the {@link World} object in any
	 * event listeners tied to this method.
	 */
	protected void step() {
		// notify the step listener
		this.stepListener.begin(this.step, this);
		
		// clear the old contact list (does NOT clear the contact map
		// which is used to warm start)
		this.contactManager.clear();
		
		// get the number of bodies
		int size = this.bodies.size();
		
		// test for out of bounds objects
		// clear the body contacts
		// clear the island flag
		// save the current transform for CCD
		for (int i = 0; i < size; i++) {
			Body body = this.bodies.get(i);
			// skip if already not active
			if (!body.isActive()) continue;
			// check if the body is out of bounds
			if (this.bounds.isOutside(body)) {
				// set the body to inactive
				body.setActive(false);
				// if so, notify via the listener
				this.boundsListener.outside(body);
			}
			// clear all the old contacts
			body.contacts.clear();
			// remove the island flag
			body.setOnIsland(false);
			// save the current transform into the previous transform
			body.transform0.set(body.transform);
		}
		
		// clear the joint island flags
		int jSize = this.joints.size();
		for (int i = 0; i < jSize; i++) {
			// get the joint
			Joint joint = this.joints.get(i);
			// set the island flag to false
			joint.setOnIsland(false);
		}
		
		// make sure there are some bodies
		if (size > 0) {
			// test for collisions via the broad-phase
			List<BroadphasePair<Body>> pairs = this.broadphaseDetector.detect(this.bodies);
			int pSize = pairs.size();		
			
			// using the broad-phase results, test for narrow-phase
			for (int i = 0; i < pSize; i++) {
				BroadphasePair<Body> pair = pairs.get(i);
				
				// get the bodies
				Body body1 = pair.getObject1();
				Body body2 = pair.getObject2();
				
				// inactive objects don't have collision detection/response
				if (!body1.isActive() || !body2.isActive()) continue;
				// one body must be dynamic
				if (!body1.isDynamic() && !body2.isDynamic()) continue;
				// check for connected pairs who's collision is not allowed
				if (body1.isConnected(body2, false)) continue;
				
				// notify of the broadphase collision
				if (!this.collisionListener.collision(body1, body2)) {
					// if the collision listener returned false then skip this collision
					continue;
				}
	
				// get their transforms
				Transform transform1 = body1.transform;
				Transform transform2 = body2.transform;
				
				// create a reusable penetration object
				Penetration penetration = new Penetration();
				// create a reusable manifold object
				Manifold manifold = new Manifold();
				
				// loop through the fixtures of body 1
				int b1Size = body1.getFixtureCount();
				int b2Size = body2.getFixtureCount();
				for (int j = 0; j < b1Size; j++) {
					Fixture fixture1 = body1.getFixture(j);
					Filter filter1 = fixture1.getFilter();
					Convex convex1 = fixture1.getShape();
					// test against each fixture of body 2
					for (int k = 0; k < b2Size; k++) {
						Fixture fixture2 = body2.getFixture(k);
						Filter filter2 = fixture2.getFilter();
						Convex convex2 = fixture2.getShape();
						// test the filter
						if (!filter1.isAllowed(filter2)) {
							// if the collision is not allowed then continue
							continue;
						}
						// test the two convex shapes
						if (this.narrowphaseDetector.detect(convex1, transform1, convex2, transform2, penetration)) {
							// check for zero penetration
							if (penetration.getDepth() == 0.0) {
								// this should only happen if numerical error occurs
								continue;
							}
							// notify of the narrow-phase collision
							if (!this.collisionListener.collision(body1, fixture1, body2, fixture2, penetration)) {
								// if the collision listener returned false then skip this collision
								continue;
							}
							// if there is penetration then find a contact manifold
							// using the filled in penetration object
							if (this.manifoldSolver.getManifold(penetration, convex1, transform1, convex2, transform2, manifold)) {
								// check for zero points
								if (manifold.getPoints().size() == 0) {
									// this should only happen if numerical error occurs
									continue;
								}
								// notify of the manifold solving result
								if (!this.collisionListener.collision(body1, fixture1, body2, fixture2, manifold)) {
									// if the collision listener returned false then skip this collision
									continue;
								}
								// compute the friction and restitution
								double friction = this.coefficientMixer.mixFriction(fixture1.getFriction(), fixture2.getFriction());
								double restitution = this.coefficientMixer.mixRestitution(fixture1.getRestitution(), fixture2.getRestitution());
								// create a contact constraint
								ContactConstraint contactConstraint = new ContactConstraint(body1, fixture1, 
										                                                    body2, fixture2, 
										                                                    manifold, 
										                                                    friction, restitution);
								// add a contact edge to both bodies
								ContactEdge contactEdge1 = new ContactEdge(body2, contactConstraint);
								ContactEdge contactEdge2 = new ContactEdge(body1, contactConstraint);
								body1.contacts.add(contactEdge1);
								body2.contacts.add(contactEdge2);
								// add the contact constraint to the contact manager
								this.contactManager.add(contactConstraint);
							}
						}
					}
				}
			}
		}
		
		// warm start the contact constraints
		this.contactManager.updateContacts(this.contactListener);
		
		// notify of all the contacts that will be solved and all the sensed contacts
		this.contactManager.preSolveNotify(this.contactListener);
		
		// perform a depth first search of the contact graph
		// to create islands for constraint solving
		Stack<Body> stack = new Stack<Body>();
		stack.ensureCapacity(size);
		// loop over the bodies and their contact edges to create the islands
		for (int i = 0; i < size; i++) {
			Body seed = this.bodies.get(i);
			// skip if asleep, in active, static, or already on an island
			if (seed.isOnIsland() || seed.isAsleep() || !seed.isActive() || seed.isStatic()) continue;
			
			island.clear();
			stack.clear();
			stack.push(seed);
			while (stack.size() > 0) {
				// get the next body
				Body body = stack.pop();
				// add it to the island
				island.add(body);
				// flag that it has been added
				body.setOnIsland(true);
				// make sure the body is awake
				body.setAsleep(false);
				// if its static then continue since we dont want the
				// island to span more than one static object
				// this keeps the size of the islands small
				if (body.isStatic()) continue;
				// loop over the contact edges of this body
				int ceSize = body.contacts.size();
				for (int j = 0; j < ceSize; j++) {
					ContactEdge contactEdge = body.contacts.get(j);
					// get the contact constraint
					ContactConstraint contactConstraint = contactEdge.getContactConstraint();
					// skip sensor contacts
					if (contactConstraint.isSensor()) continue;
					// get the other body
					Body other = contactEdge.getOther();
					// check if the contact constraint has already been added to an island
					if (contactConstraint.isOnIsland()) continue;
					// add the contact constraint to the island list
					island.add(contactConstraint);
					// set the island flag on the contact constraint
					contactConstraint.setOnIsland(true);
					// has the other body been added to an island yet?
					if (!other.isOnIsland()) {
						// if not then add this body to the stack
						stack.push(other);
						other.setOnIsland(true);
					}
				}
				// loop over the joint edges of this body
				int jeSize = body.joints.size();
				for (int j = 0; j < jeSize; j++) {
					// get the joint edge
					JointEdge jointEdge = body.joints.get(j);
					// get the joint
					Joint joint = jointEdge.getJoint();
					// check if the joint is inactive
					if (!joint.isActive()) continue;
					// get the other body
					Body other = jointEdge.getOther();
					// check if the joint has already been added to an island
					// or if the other body is not active
					if (joint.isOnIsland() || !other.isActive()) continue;
					// add the joint to the island
					island.add(joint);
					// set the island flag on the joint
					joint.setOnIsland(true);
					// check if the other body has been added to an island
					if (!other.isOnIsland()) {
						// if not then add the body to the stack
						stack.push(other);
						other.setOnIsland(true);
					}
				}
			}
			
			// solve the island
			island.solve(this.gravity, this.step);
			
			// allow static bodies to participate in other islands
			for (int j = 0; j < size; j++) {
				Body body = this.bodies.get(j);
				if (body.isStatic()) {
					body.setOnIsland(false);
				}
			}
		}
		
		// notify of the all solved contacts
		this.contactManager.postSolveNotify(this.contactListener);
		
		// solve time of impact
		this.solveTOI();
		
		// notify the step listener
		this.stepListener.end(this.step, this);
	}
	
	/**
	 * Solves the time of impact for all bodies.
	 * @since 1.2.0
	 */
	protected void solveTOI() {
		// get the settings
		Settings settings = Settings.getInstance();
		// make sure time of impact solving is enabled
		if (!settings.isContinuousCollisionDetectionEnabled()) return;
		
		// get the linear tolerance (allowed penetration)
		double tol = settings.getLinearTolerance();
		
		// TODO i think we need to advance all bodies by the smallest
		// toi and continue doing this until all body toi's are solved
		
		int size = this.bodies.size();
		// loop over all the bodies and find the minimum TOI for each
		// dynamic body
		for (int i = 0; i < size; i++) {
			// get the body
			Body b1 = this.bodies.get(i);
			
			// we don't process kinematic or static bodies except with
			// dynamic bodies (in other words b1 must always be a dynamic
			// body)
			if (b1.isKinematic() || b1.isStatic()) continue;
			
			// don't bother with bodies that did not have their
			// positions integrated, if they were not added to an island then
			// that means they didn't move
			
			// we can also check for sleeping bodies and skip those since
			// they will only be asleep after being stationary for a set
			// time period
			if (!b1.isOnIsland() || b1.isAsleep()) continue;

			// setup the initial time bounds [0, 1]
			double t1 = 0.0;
			double t2 = 1.0;
			TimeOfImpact minToi = null;
			
			// loop over all the other bodies to find the minimum TOI
			for (int j = 0; j < size; j++) {
				// get the other body
				Body b2 = this.bodies.get(j);
				
				// skip this test if they are the same body
				if (b1 == b2) continue;
				
				// make sure the other body is active
				if (!b2.isActive()) continue;
				
				// skip other dynamic bodies; we only do TOI for
				// dynamic vs. static/kinematic unless its a bullet
				if (b2.isDynamic() && !b1.isBullet()) continue;
				
				// check for connected pairs who's collision is not allowed
				if (b1.isConnected(b2, false)) continue;
				
				// compute the time of impact between the two bodies
				TimeOfImpact toi = new TimeOfImpact();
				if (this.timeOfImpactDetector.getTimeOfImpact(b1, b2, t1, t2, toi)) {
					// get the time of impact
					double t = toi.getToi();
					// check if the time of impact is less than
					// the current time of impact
					if (t < t2) {
						// TODO see if these fixtures can collide according to the filter
						// TODO test for sensor fixtures
						if (this.timeOfImpactListener.dynamic(b1, b2, t)) {
							// set the new upper bound
							t2 = t;
							minToi = toi;
						}
					}
				} else {
					// if the bodies are intersecting or do not intersect
					// within the range of motion then skip this body
					// and move to the next
					continue;
				}
			}
			
			// make sure the time of impact is not null
			// we also can get some performance benefit if we don't do anything
			// for times of impact that are 1.0 which means they are already
			// colliding
			if (minToi != null && minToi.getToi() != 1.0) {
				// get the time of impact info
				double t = minToi.getToi();
				Separation s = minToi.getSeparation();
				// compute the distance to translate
				double d = s.getDistance() + tol;
				Vector2 n = s.getNormal();
				
				// move the dynamic body to the time of impact
				b1.transform0.lerp(b1.transform, t, b1.transform);
				
				// move the body along the separating axis the given distance plus
				// the allowed penetration to place the objects in collision
				// this will allow the discrete collision detection to find the
				// collision and solve it normally
				b1.translate(n.x * d, n.y * d);
				
				// this method does not conserve time
			}
		}
	}
	
	/**
	 * Adds a {@link Body} to the {@link World}.
	 * @param body the {@link Body} to add
	 */
	public void add(Body body) {
		// check for null body
		if (body == null) throw new NullPointerException("Cannot add a null body to the world.");
		// dont allow adding it twice
		if (this.bodies.contains(body)) throw new IllegalArgumentException("Cannot add the same body more than once.");
		// add it to the world
		this.bodies.add(body);
	}
	
	/**
	 * Adds a {@link Joint} to the {@link World}.
	 * @param joint the {@link Joint} to add
	 */
	public void add(Joint joint) {
		// check for null joint
		if (joint == null) throw new NullPointerException("Cannot add a null joint to the world.");
		// dont allow adding it twice
		if (this.joints.contains(joint)) throw new IllegalArgumentException("Cannot add the same joint more than once.");
		// add the joint to the joint list
		this.joints.add(joint);
		// get the associated bodies
		Body body1 = joint.getBody1();
		Body body2 = joint.getBody2();
		// create a joint edge from the first body to the second
		JointEdge jointEdge1 = new JointEdge(body2, joint);
		// add the edge to the body
		body1.joints.add(jointEdge1);
		// create a joint edge from the second body to the first
		JointEdge jointEdge2 = new JointEdge(body1, joint);
		// add the edge to the body
		body2.joints.add(jointEdge2);
	}
	
	/**
	 * Removes the given {@link Body} from the {@link World}.
	 * @param body the {@link Body} to remove
	 * @return boolean true if the body was removed
	 */
	public boolean remove(Body body) {
		// check for null body
		if (body == null) return false;
		// remove the body from the list
		boolean removed = this.bodies.remove(body);
		
		// only remove joints and contacts if the body was removed
		if (removed) {
			// wake up any bodies connected to this body by a joint
			// and destroy the joints and remove the edges
			Iterator<JointEdge> aIterator = body.joints.iterator();
			while (aIterator.hasNext()) {
				// get the joint edge
				JointEdge jointEdge = aIterator.next();
				// remove the joint edge from the given body
				aIterator.remove();
				// get the joint
				Joint joint = jointEdge.getJoint();
				// get the other body
				Body other = jointEdge.getOther();
				// wake up the other body
				other.setAsleep(false);
				// remove the joint edge from the other body
				Iterator<JointEdge> bIterator = other.joints.iterator();
				while (bIterator.hasNext()) {
					// get the joint edge
					JointEdge otherJointEdge = bIterator.next();
					// get the joint
					Joint otherJoint = otherJointEdge.getJoint();
					// are the joints the same object reference
					if (otherJoint == joint) {
						// remove the joint edge
						bIterator.remove();
						// we can break from the loop since there should
						// not be more than one contact edge per joint per body
						break;
					}
				}
				// notify of the destroyed joint
				this.destructionListener.destroyed(joint);
				// remove the joint from the world
				this.joints.remove(joint);
			}
			
			// remove any contacts this body had with any other body
			Iterator<ContactEdge> acIterator = body.contacts.iterator();
			while (acIterator.hasNext()) {
				// get the contact edge
				ContactEdge contactEdge = acIterator.next();
				// remove the contact edge from the given body
				acIterator.remove();
				// get the contact constraint
				ContactConstraint contactConstraint = contactEdge.getContactConstraint();
				// get the other body
				Body other = contactEdge.getOther();
				// wake up the other body
				other.setAsleep(false);
				// remove the contact edge connected from the other body
				// to this body
				Iterator<ContactEdge> iterator = other.contacts.iterator();
				while (iterator.hasNext()) {
					ContactEdge otherContactEdge = iterator.next();
					// get the contact constraint
					ContactConstraint otherContactConstraint = otherContactEdge.getContactConstraint();
					// check if the contact constraint is the same reference
					if (otherContactConstraint == contactConstraint) {
						// remove the contact edge
						iterator.remove();
						// break from the loop since there should only be
						// one contact edge per body pair
						break;
					}
				}
				// remove the contact constraint from the contact manager
				this.contactManager.remove(contactConstraint);
				// loop over the contact points
				Contact[] contacts = contactConstraint.getContacts();
				int size = contacts.length;
				for (int j = 0; j < size; j++) {
					// get the contact
					Contact contact = contacts[j];
					// create a contact point for notification
					ContactPoint contactPoint = new ContactPoint(
													contactConstraint.getBody1(), 
													contactConstraint.getFixture1(), 
													contactConstraint.getBody2(), 
													contactConstraint.getFixture2(),
													contact.isEnabled(),
													contact.getPoint(), 
													contactConstraint.getNormal(), 
													contact.getDepth());
					// call the destruction listener
					this.destructionListener.destroyed(contactPoint);
				}
			}
		}
		
		return removed;
	}
	
	/**
	 * Removes the given {@link Joint} from the {@link World}.
	 * @param joint the {@link Joint} to remove
	 * @return boolean true if the {@link Joint} was removed
	 */
	public boolean remove(Joint joint) {
		// check for null joint
		if (joint == null) return false;
		// remove the joint from the joint list
		boolean removed = this.joints.remove(joint);
		
		// get the involved bodies
		Body body1 = joint.getBody1();
		Body body2 = joint.getBody2();
		
		// see if the given joint was removed
		if (removed) {
			// remove the joint edges from body1
			Iterator<JointEdge> iterator = body1.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			// remove the joint edges from body2
			iterator = body2.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			
			// finally wake both bodies
			body1.setAsleep(false);
			body2.setAsleep(false);
		}
		
		return removed;
	}
	
	/**
	 * Sets the gravity.
	 * <p>
	 * Setting the gravity vector to the zero vector eliminates gravity.
	 * <p>
	 * A NullPointerException is thrown if the given gravity vector is null.
	 * @param gravity the gravity in meters/second<sup>2</sup>
	 */
	public void setGravity(Vector2 gravity) {
		if (gravity == null) throw new NullPointerException("The gravity vector cannot be null.");
		this.gravity = gravity;
	}
	
	/**
	 * Returns the gravity.
	 * @return {@link Vector2} the gravity in meters/second<sup>2</sup>
	 */
	public Vector2 getGravity() {
		return this.gravity;
	}

	/**
	 * Sets the bounds of the {@link World}.
	 * @param bounds the bounds
	 */
	public void setBounds(Bounds bounds) {
		// check for null bounds
		if (bounds == null) throw new NullPointerException("The bounds cannot be null.  To creat an unbounded world use the Bounds.UNBOUNDED static member.");
		this.bounds = bounds;
	}
	
	/**
	 * Returns the bounds of the world.
	 * @return {@link Bounds} the bounds
	 */
	public Bounds getBounds() {
		return this.bounds;
	}
	
	/**
	 * Sets the bounds listener.
	 * @param boundsListener the bounds listener
	 */
	public void setBoundsListener(BoundsListener boundsListener) {
		if (boundsListener == null) throw new NullPointerException("The bounds listener cannot be null.  Create an instance of the BoundsAdapter class to set it to the default.");
		this.boundsListener = boundsListener;
	}
	
	/**
	 * Returns the bounds listener.
	 * @return {@link BoundsListener} the bounds listener
	 */
	public BoundsListener getBoundsListener() {
		return this.boundsListener;
	}
	
	/**
	 * Sets the {@link ContactListener}.
	 * @param contactListener the contact listener
	 */
	public void setContactListener(ContactListener contactListener) {
		if (contactListener == null) throw new NullPointerException("The contact listener cannot be null.  Create an instance of the ContactAdapter class to set it to the default.");
		this.contactListener = contactListener;
	}
	
	/**
	 * Returns the contact listener.
	 * @return {@link ContactListener} the contact listener
	 */
	public ContactListener getContactListener() {
		return this.contactListener;
	}
	
	/**
	 * Returns the time of impact listener.
	 * @return {@link TimeOfImpactListener} the time of impact listener
	 */
	public TimeOfImpactListener getTimeOfImpactListener() {
		return this.timeOfImpactListener;
	}
	
	/**
	 * Sets the {@link TimeOfImpactListener}.
	 * @param timeOfImpactListener the time of impact listener
	 */
	public void setTimeOfImpactListener(TimeOfImpactListener timeOfImpactListener) {
		if (timeOfImpactListener == null) throw new NullPointerException("The time of impact listener cannot be null.");
		this.timeOfImpactListener = timeOfImpactListener;
	}
	
	/**
	 * Sets the {@link DestructionListener}.
	 * @param destructionListener the {@link DestructionListener}
	 */
	public void setDestructionListener(DestructionListener destructionListener) {
		if (destructionListener == null) throw new NullPointerException("The destruction listener cannot be null.  Create an instance of the DestructionAdapter class to set it to the default.");
		this.destructionListener = destructionListener;
	}
	
	/**
	 * Returns the {@link DestructionListener}.
	 * @return {@link DestructionListener} the destruction listener
	 */
	public DestructionListener getDestructionListener() {
		return this.destructionListener;
	}
	
	/**
	 * Sets the {@link StepListener}.
	 * @param stepListener the {@link StepListener}
	 */
	public void setStepListener(StepListener stepListener) {
		if (stepListener == null) throw new NullPointerException("The step listener cannot be null.  Create an instance of the StepAdapter class to set it to the default.");
		this.stepListener = stepListener;
	}
	
	/**
	 * Returns the {@link StepListener}
	 * @return {@link StepListener}
	 */
	public StepListener getStepListener() {
		return this.stepListener;
	}
	
	/**
	 * Sets the broad-phase collision detection algorithm.
	 * <p>
	 * If the given detector is null then the default {@link Sap}
	 * {@link BroadphaseDetector} is set as the current broad phase.
	 * @param broadphaseDetector the broad-phase collision detection algorithm
	 */
	public void setBroadphaseDetector(BroadphaseDetector broadphaseDetector) {
		if (broadphaseDetector == null) throw new NullPointerException("The broadphase detector cannot be null.");
		this.broadphaseDetector = broadphaseDetector;
	}
	
	/**
	 * Returns the broad-phase collision detection algorithm.
	 * @return {@link BroadphaseDetector} the broad-phase collision detection algorithm
	 */
	public BroadphaseDetector getBroadphaseDetector() {
		return this.broadphaseDetector;
	}
	
	/**
	 * Sets the narrow-phase collision detection algorithm.
	 * <p>
	 * If the given detector is null then the default {@link Gjk}
	 * {@link NarrowphaseDetector} is set as the current narrow phase.
	 * @param narrowphaseDetector the narrow-phase collision detection algorithm
	 */
	public void setNarrowphaseDetector(NarrowphaseDetector narrowphaseDetector) {
		if (narrowphaseDetector == null) throw new NullPointerException("The narrowphase detector cannot be null.");
		this.narrowphaseDetector = narrowphaseDetector;
	}
	
	/**
	 * Returns the narrow-phase collision detection algorithm.
	 * @return {@link NarrowphaseDetector} the narrow-phase collision detection algorithm
	 */
	public NarrowphaseDetector getNarrowphaseDetector() {
		return this.narrowphaseDetector;
	}
	
	/**
	 * Sets the manifold solver.
	 * @param manifoldSolver the manifold solver
	 */
	public void setManifoldSolver(ManifoldSolver manifoldSolver) {
		if (manifoldSolver == null) throw new NullPointerException("The manifold solver cannot be null.");
		this.manifoldSolver = manifoldSolver;
	}
	
	/**
	 * Returns the manifold solver.
	 * @return {@link ManifoldSolver} the manifold solver
	 */
	public ManifoldSolver getManifoldSolver() {
		return this.manifoldSolver;
	}
	
	/**
	 * Sets the time of impact detector.
	 * @param timeOfImpactDetector the time of impact detector
	 */
	public void setTimeOfImpactDetector(TimeOfImpactDetector timeOfImpactDetector) {
		if (timeOfImpactDetector == null) throw new NullPointerException("The time of impact solver cannot be null.");
		this.timeOfImpactDetector = timeOfImpactDetector;
	}
	
	/**
	 * Returns the time of impact detector.
	 * @return {@link TimeOfImpactDetector} the time of impact detector
	 */
	public TimeOfImpactDetector getTimeOfImpactDetector() {
		return this.timeOfImpactDetector;
	}
	
	/**
	 * Sets the collision listener.
	 * @param collisionListener the collision listener
	 */
	public void setCollisionListener(CollisionListener collisionListener) {
		if (collisionListener == null) throw new NullPointerException("The collision listener cannot be null.  Create an instance of the CollisionAdapter class to set it to the default.");
		this.collisionListener = collisionListener;
	}
	
	/**
	 * Returns the collision listener.
	 * @return {@link CollisionListener} the collision listener
	 */
	public CollisionListener getCollisionListener() {
		return this.collisionListener;
	}
	
	/**
	 * Returns the {@link CoefficientMixer}.
	 * @return {@link CoefficientMixer}
	 */
	public CoefficientMixer getCoefficientMixer() {
		return coefficientMixer;
	}
	
	/**
	 * Sets the {@link CoefficientMixer}.
	 * @param coefficientMixer the coefficient mixer
	 */
	public void setCoefficientMixer(CoefficientMixer coefficientMixer) {
		if (coefficientMixer == null) throw new NullPointerException("The coefficient mixer cannot be null.");
		this.coefficientMixer = coefficientMixer;
	}
	
	/**
	 * Returns the {@link ContactManager}.
	 * @return {@link ContactManager}
	 * @since 1.0.2
	 */
	public ContactManager getContactManager() {
		return contactManager;
	}
	
	/**
	 * Sets the {@link ContactManager}.
	 * @param contactManager the contact manager
	 * @since 1.0.2
	 */
	public void setContactManager(ContactManager contactManager) {
		// make sure the contact manager is not null
		if (contactManager == null) throw new NullPointerException("The contact manager cannot be null.");
		this.contactManager = contactManager;
	}
	
	/**
	 * Clears the joints and bodies from the world.
	 * <p>
	 * This method does not notify of destroyed objects.
	 * @see #clear(boolean)
	 */
	public void clear() {
		this.clear(false);
	}
	
	/**
	 * Clears the joints and bodies from the world.
	 * <p>
	 * This method will clear the joints and contacts from all {@link Body}s.
	 * @param notify true if destruction of joints and contacts should be notified of by the {@link DestructionListener}
	 * @since 1.0.2
	 */
	public void clear(boolean notify) {
		// loop over the bodies and clear the
		// joints and contacts
		int bsize = this.bodies.size();
		for (int i = 0; i < bsize; i++) {
			// get the body
			Body body = this.bodies.get(i);
			// clear the joint edges
			body.joints.clear();
			// do we need to notify?
			if (notify) {
				// notify of all the destroyed contacts
				Iterator<ContactEdge> aIterator = body.contacts.iterator();
				while (aIterator.hasNext()) {
					// get the contact edge
					ContactEdge contactEdge = aIterator.next();
					// get the other body involved
					Body other = contactEdge.getOther();
					// get the contact constraint
					ContactConstraint contactConstraint = contactEdge.getContactConstraint();
					// find the other contact edge
					Iterator<ContactEdge> bIterator = other.contacts.iterator();
					while (bIterator.hasNext()) {
						// get the contact edge
						ContactEdge otherContactEdge = bIterator.next();
						// get the contact constraint on the edge
						ContactConstraint otherContactConstraint = otherContactEdge.getContactConstraint();
						// are the constraints the same object reference
						if (otherContactConstraint == contactConstraint) {
							// if so then remove it
							bIterator.remove();
							// there should only be one contact edge
							// for each body-body pair
							break;
						}
					}
					// notify of all the contacts on the contact constraint
					Contact[] contacts = contactConstraint.getContacts();
					int csize = contacts.length;
					for (int j = 0; j < csize; j++) {
						Contact contact = contacts[j];
						// create a contact point for notification
						ContactPoint contactPoint = new ContactPoint(
														contactConstraint.getBody1(), 
														contactConstraint.getFixture1(), 
														contactConstraint.getBody2(), 
														contactConstraint.getFixture2(),
														contact.isEnabled(),
														contact.getPoint(), 
														contactConstraint.getNormal(), 
														contact.getDepth());
						// call the destruction listener
						this.destructionListener.destroyed(contactPoint);
					}
				}
			}
			// clear all the contacts
			body.contacts.clear();
			// notify of the destroyed body
			this.destructionListener.destroyed(body);
		}
		// do we need to notify?
		if (notify) {
			// notify of all the destroyed joints
			int jsize = this.joints.size();
			for (int i = 0; i < jsize; i++) {
				// get the joint
				Joint joint = this.joints.get(i);
				// call the destruction listener
				this.destructionListener.destroyed(joint);
			}
		}
		// clear all the joints
		this.joints.clear();
		// clear all the bodies
		this.bodies.clear();
		// clear the contact manager of cached contacts
		this.contactManager.reset();
	}
	
	/**
	 * Returns the number of {@link Body} objects.
	 * @return int the number of bodies
	 */
	public int getBodyCount() {
		return this.bodies.size();
	}
	
	/**
	 * Returns the {@link Body} at the given index.
	 * @param index the index
	 * @return {@link Body}
	 */
	public Body getBody(int index) {
		return this.bodies.get(index);
	}
	
	/**
	 * Returns the number of {@link Joint} objects.
	 * @return int the number of joints
	 */
	public int getJointCount() {
		return this.joints.size();
	}
	
	/**
	 * Returns the {@link Joint} at the given index.
	 * @param index the index
	 * @return {@link Joint}
	 */
	public Joint getJoint(int index) {
		return this.joints.get(index);
	}
	
	/**
	 * Returns the {@link Step} object used to advance
	 * the simulation.
	 * @return {@link Step} the current step object
	 */
	public Step getStep() {
		return this.step;
	}
}
