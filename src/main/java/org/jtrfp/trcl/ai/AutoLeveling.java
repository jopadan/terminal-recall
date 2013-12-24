package org.jtrfp.trcl.ai;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class AutoLeveling extends Behavior {
    @Override
    public void _tick(long timeInMillis){
	final Vector3D old = getParent().getHeading();
	getParent().setHeading(new Vector3D(old.getX(),old.getY()*.98,old.getZ()).normalize());
	final Vector3D oTop = getParent().getTop();
	getParent().setTop(new Vector3D(oTop.getX(),oTop.getY()*1.02,oTop.getZ()).normalize());
    }
}
