/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2014 Chuck Ritola
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/
package org.jtrfp.trcl.obj;

import java.lang.ref.WeakReference;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.beh.ProjectileBehavior;
import org.jtrfp.trcl.beh.ReportsCollisionsToStdout;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.file.Weapon;
import org.jtrfp.trcl.gpu.Model;
import org.jtrfp.trcl.obj.Explosion.ExplosionType;

public class ProjectileObject3D extends WorldObject implements Projectile {
    public static final long LIFESPAN_MILLIS=4500;
    private WeakReference<WorldObject> objectOfOrigin = new WeakReference<WorldObject>(null);
    public ProjectileObject3D(TR tr,Model m, Weapon w, ExplosionType explosionType){
	super(tr,m);
	addBehavior(new ProjectileBehavior(this,w.getDamage(),explosionType,w.isHoning()));
	addBehavior(new ReportsCollisionsToStdout().setEnable(false));
    }

    @Override
    public void reset(double [] newPos, Vector3D newVelocity, WorldObject objectOfOrigin){
	this.objectOfOrigin= new WeakReference<WorldObject>(objectOfOrigin);
	if(newVelocity.getNorm()!=0)setHeading(newVelocity.normalize());
	else {setHeading(Vector3D.PLUS_I);newVelocity=Vector3D.PLUS_I;}//meh.
	setPosition(newPos[0],newPos[1],newPos[2]);
	setActive(true);
	setVisible(true);
	getBehavior().probeForBehavior(Velocible.class).setVelocity(newVelocity);
	getBehavior().probeForBehavior(ProjectileBehavior.class).reset(newVelocity.normalize(),newVelocity.getNorm());
    }//end reset()

    @Override
    public WorldObject getObjectOfOrigin() {
	return objectOfOrigin.get();
    }
}//end ProjectilObject3D
