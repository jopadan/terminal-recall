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
package org.jtrfp.trcl.beh;

import org.jtrfp.trcl.beh.DamageableBehavior.SupplyNotNeededException;
import org.jtrfp.trcl.obj.WorldObject;


public class ResetsRandomlyAfterDeath extends Behavior implements DeathListener {
private double minWaitMillis=100,maxWaitMillis=1000;
private Runnable runOnReset;

@Override
public void notifyDeath() {
   //Reset state
    final WorldObject thisObject = getParent();
    final Runnable _runOnReset = runOnReset;
    final long waitTime = (long)(2*minWaitMillis+Math.random()*.5*(maxWaitMillis-minWaitMillis));
    new Thread(){
	@Override
	public void run(){
	    try{Thread.currentThread().sleep(waitTime);}
	    catch(InterruptedException e){e.printStackTrace();}
	    thisObject.getContainingGrid().add(thisObject);
	    thisObject.setActive(true);//Is this really needed?
	    thisObject.setVisible(true);
	    try{thisObject.getBehavior().probeForBehavior(DamageableBehavior.class).unDamage();}
	    catch(SupplyNotNeededException e){e.printStackTrace();}//?!?!    
	    _runOnReset.run();
	}//end run()
    }.start();
 }//end notifyDeath

/**
 * @return the minWaitMillis
 */
public double getMinWaitMillis() {
    return minWaitMillis;
}

/**
 * @param minWaitMillis the minWaitMillis to set
 */
public ResetsRandomlyAfterDeath setMinWaitMillis(double minWaitMillis) {
    this.minWaitMillis = minWaitMillis;
    return this;
}

/**
 * @return the maxWaitMillis
 */
public double getMaxWaitMillis() {
    return maxWaitMillis;
}

/**
 * @param maxWaitMillis the maxWaitMillis to set
 */
public ResetsRandomlyAfterDeath setMaxWaitMillis(double maxWaitMillis) {
    this.maxWaitMillis = maxWaitMillis;
    return this;
}

/**
 * @return the runOnReset
 */
public Runnable getRunOnReset() {
    return runOnReset;
}

/**
 * @param runOnReset the runOnReset to set
 */
public ResetsRandomlyAfterDeath setRunOnReset(Runnable runOnReset) {
    this.runOnReset = runOnReset;
    return this;
}

}//end ResetsRandomlyAfterDeath
