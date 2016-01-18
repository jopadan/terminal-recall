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
package org.jtrfp.trcl.core;

import java.awt.Color;
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.TextureBehavior;
import org.jtrfp.trcl.Triangle;
import org.jtrfp.trcl.TriangleList;

public class PortalTexture implements TextureDescription {
    private int                       portalFramebufferNumber;
    private final ArrayList<Integer>  relevantVertexIndices = new ArrayList<Integer>();
    private TriangleVertexWindow      triangleVertexWindow;
    
    public PortalTexture(){}
    
    public PortalTexture(TriangleVertexWindow triangleVertexWindow){
	this();
	setTriangleVertexWindow(triangleVertexWindow);
    }//end constructor

    @Override
    public Color getAverageColor() {
	return Color.black;
    }

    @Override
    public void apply(TriangleList triangleList, int gpuTVIndex, int numFrames,
	    Triangle thisTriangle, Vector3D pos, TriangleVertexWindow vw) {
	throw new UnsupportedOperationException();
    }

    @Override
    public void addBehavior(TextureBehavior beh) {
	throw new UnsupportedOperationException();
	
    }

    @Override
    public void removeBehavior(TextureBehavior beh) {
	throw new UnsupportedOperationException();
	
    }

    /**
     * @return the portalFramebufferNumber
     */
    public int getPortalFramebufferNumber() {
        return portalFramebufferNumber;
    }
    
    public void addRelevantVertexIndex(int idx){
	relevantVertexIndices.add(idx);
    }

    public void setPortalFramebufferNumber(int portalFramebufferNumber) {
        this.portalFramebufferNumber = portalFramebufferNumber;
        final int textureID = 65536-getPortalFramebufferNumber();
        final TriangleVertexWindow vw = getTriangleVertexWindow();
        for(int i:relevantVertexIndices){
            vw.textureIDLo .set(i, (byte)(textureID & 0xFF));
    	    vw.textureIDMid.set(i, (byte)((textureID >> 8) & 0xFF));
    	    vw.textureIDHi .set(i, (byte)((textureID >> 16) & 0xFF));
        }//end for(relevantVertexIndices)
    }//end setPortalFramebufferNumber(...)

    public TriangleVertexWindow getTriangleVertexWindow() {
        return triangleVertexWindow;
    }

    public void setTriangleVertexWindow(TriangleVertexWindow triangleVertexWindow) {
        this.triangleVertexWindow = triangleVertexWindow;
    }
 
}//end PortalTexture
