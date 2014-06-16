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

import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.RenderableSpacePartitioningGrid;
import org.jtrfp.trcl.World;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.file.PUPFile;
import org.jtrfp.trcl.file.PUPFile.PowerupLocation;

public class PUPObjectPlacer implements ObjectPlacer {
    ArrayList<PowerupObject> objs = new ArrayList<PowerupObject>();

    public PUPObjectPlacer(PUPFile pupFile, World world) {
	for (PowerupLocation loc : pupFile.getPowerupLocations()) {
	    PowerupObject powerup = new PowerupObject(loc.getType(), world);
	    final double[] pupPos = powerup.getPosition();
	    pupPos[0] = TR.legacy2Modern(loc.getZ());
	    pupPos[1] = (TR.legacy2Modern(loc.getY()) / TR.mapWidth) * 16.
		    * world.sizeY;
	    pupPos[2] = TR.legacy2Modern(loc.getX());
	    powerup.notifyPositionChange();
	    objs.add(powerup);
	}// end for(locations)
    }// end PUPObjectPlacer

    @Override
    public void placeObjects(RenderableSpacePartitioningGrid target, Vector3D positionOffset) {
	for (PowerupObject obj : objs) {
	    obj.movePositionBy(positionOffset);
	    target.add(obj);
	}//end for(objs)
    }//end placeObjects()

}// end PUPObjectPlacer
