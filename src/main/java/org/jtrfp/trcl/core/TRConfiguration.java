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
package org.jtrfp.trcl.core;

import org.jtrfp.trcl.flow.GameVersion;

public class TRConfiguration{
    	private GameVersion gameVersion=GameVersion.F3;
    	private Boolean usingTextureBufferUnmap,
    			usingNewTexturing;
    	private int targetFPS =60;
	public TRConfiguration(){}

	public GameVersion getGameVersion() {
	    return GameVersion.F3;
	}

	public boolean isUsingNewTexturing() {
	    if(usingNewTexturing!=null)return usingNewTexturing;
	    boolean result=false;
	    if(System.getProperties().containsKey("org.jtrfp.trcl.core.useNewTexturing")){
		if(System.getProperty("org.jtrfp.trcl.core.useNewTexturing").toUpperCase().contains("TRUE"))
		    result=true;
	    }//end if(contains key)
	    usingNewTexturing=result;
	    return result;
	}//end isUsingTextureBufferUnmap()
	
	public boolean isUsingTextureBufferUnmap() {
	    if(usingTextureBufferUnmap!=null)return usingTextureBufferUnmap;
	    boolean result=true;
	    if(System.getProperties().containsKey("org.jtrfp.trcl.Renderer.unmapTextureBuffer")){
		if(System.getProperty("org.jtrfp.trcl.Renderer.unmapTextureBuffer").toUpperCase().contains("FALSE"))
		    result=false;
	    }//end if(contains key)
	    usingTextureBufferUnmap=result;
	    return result;
	}//end isUsingTextureBufferUnmap()

	public int getTargetFPS() {
	    return targetFPS;
	}
}//end TRConfiguration
