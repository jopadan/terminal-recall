/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2015 Chuck Ritola
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/

package org.jtrfp.trcl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.obj.Positionable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CameraIT {
    protected Camera subject;
    protected Positionable[] zeroPositionables;

    @Before
    public void setUp() throws Exception {
	subject = new Camera(null);
	zeroPositionables = new Positionable[2];
	for(int i=0; i<2; i++){
	    Positionable pos = mock(Positionable.class);
	    when(pos.getPositionV3D()).thenReturn(new Vector3D(0,0,0));
	    zeroPositionables[i]=pos;
	    }
    }

    @After
    public void tearDown() throws Exception {
    }
/*
    @Test
    public void testEmpty() {
	assertTrue(subject.getRelevancePairs()         .isEmpty());
	assertTrue(subject.getFlatRelevanceCollection().isEmpty());
	assertTrue(subject.getRelevanceCollections()   .isEmpty());
    }
    
    @Test
    public void testAddEmptyGrid(){
	SpacePartitioningGrid<Positionable> spg = new SpacePartitioningGrid<Positionable>(){};
	subject.addGrid(spg);
	assertTrue(subject.getRelevancePairs()         .isEmpty());
	assertTrue(subject.getFlatRelevanceCollection().isEmpty());
	assertTrue(subject.getRelevanceCollections()   .isEmpty());
    }
    */
    @Test
    public void testAddSingleCubeGridChangePosition(){
	subject.setPosition(new Vector3D(0,0,0));
	SpacePartitioningGrid<Positionable> spg = new SpacePartitioningGrid<Positionable>(){};
	spg.add(zeroPositionables[0]);spg.add(zeroPositionables[1]);
	subject.addGrid(spg);
	subject.setPosition(new Vector3D(0,0,0));
	subject.notifyPositionChange();
	assertFalse(subject.getRelevancePairs()         .isEmpty());
	assertFalse(subject.getFlatRelevanceCollection().isEmpty());
	assertFalse(subject.getRelevanceCollections()   .isEmpty());
	subject.setPosition(new Vector3D(TR.mapSquareSize*20*7,TR.mapSquareSize*20*7,TR.mapSquareSize*20*7));
	subject.notifyPositionChange();
	assertTrue(subject.getRelevancePairs()         .isEmpty());
	assertTrue(subject.getRelevanceCollections()   .isEmpty());
	assertTrue(subject.getFlatRelevanceCollection().isEmpty());
    }

}//end CameraTest