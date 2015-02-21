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

import java.awt.Dimension;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.jtrfp.FileLoadException;
import org.jtrfp.trcl.AnimatedTexture;
import org.jtrfp.trcl.Sequencer;
import org.jtrfp.trcl.beh.Behavior;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.core.Texture;
import org.jtrfp.trcl.math.Vect3D;

public class Smoke extends OneShotBillboardEvent {
	    private final Sequencer sequencer;
	    private static final int NUM_FRAMES=4;
	    private final SmokeType type;
	    public Smoke(TR tr, SmokeType type) {
		super(tr,type.getMillisPerFrame(),type.getAnimationFiles().length);
		this.type=type;
		setBillboardSize(type.getBillboardSize());
		if(type.isRandomRotate())setRotation(2*Math.PI*Math.random());
		String [] aniFiles = type.getAnimationFiles();
		Texture [] frames = new Texture[aniFiles.length];
		try{for(int i=0; i<aniFiles.length;i++){
		        frames[i]=frame(aniFiles[i]);
		    }
		}//end try{}
		catch(Exception e){e.printStackTrace();}
		setTexture(new AnimatedTexture(sequencer=new Sequencer(type.getMillisPerFrame(), frames.length, false,false),frames),true);
	    }//end constructor
	    
	    @Override
	    public Smoke setPosition(double [] pos){
		final double [] superPos = super.getPosition();
		Vect3D.subtract(pos,type.getOrigin().scalarMultiply(getBillboardSize().getHeight()/2.).toArray(),superPos);
		super.notifyPositionChange();
		//super.setPosition(pos.subtract(type.getOrigin().scalarMultiply(getBillboardSize().getHeight()/2.)));
		return this;
	    }
	    
	    public static enum SmokeType{
		Puff(new String[]{
			"PUFF1.RAW",
			"PUFF2.RAW",
			"PUFF3.RAW",
			"PUFF4.RAW",
		},new Vector3D(0,0,0),new Dimension(14000,14000),200,true);
		
		private final String [] animationFiles;
		private final Vector3D origin;
		private final Dimension billboardSize;
		private final int millisPerFrame;
		private final boolean randomRotate;
		SmokeType(String [] animationFiles,Vector3D origin, Dimension billboardSize, int millisPerFrame, boolean randomRotate){
		    this.animationFiles=animationFiles; 
		    this.origin=origin;
		    this.billboardSize=billboardSize;
		    this.millisPerFrame=millisPerFrame;
		    this.randomRotate=randomRotate;
		}
		public int getMillisPerFrame() {
		    return millisPerFrame;
		}
		public Dimension getBillboardSize() {
		    return billboardSize;
		}
		public String [] getAnimationFiles(){
		    return animationFiles;
		}
		public Vector3D getOrigin(){
		    return origin;}
		/**
		 * @return the randomRotate
		 */
		public boolean isRandomRotate() {
		    return randomRotate;
		}
	    }//end SmokeType
	    
	    private Texture frame(String name) throws IllegalAccessException, IOException, FileLoadException
		{return (Texture)getTr().getResourceManager().getRAWAsTexture(name, getTr().getDarkIsClearPaletteVL(), null, false);}
}//end Smoke
