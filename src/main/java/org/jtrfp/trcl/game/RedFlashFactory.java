/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2015 Chuck Ritola and contributors. See CREDITS for details.
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/
package org.jtrfp.trcl.game;

import java.awt.Color;

import org.jtrfp.trcl.SelectableTexture;
import org.jtrfp.trcl.beh.Behavior;
import org.jtrfp.trcl.core.Feature;
import org.jtrfp.trcl.core.FeatureFactory;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.core.Texture;
import org.jtrfp.trcl.core.TextureDescription;
import org.jtrfp.trcl.core.TextureManager;
import org.jtrfp.trcl.obj.Sprite2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedFlashFactory implements FeatureFactory<TVF3Game>{
    private final TR tr;
    private TextureDescription texture; 
    
    @Autowired
    public RedFlashFactory(TR tr){
	this.tr = tr;
	getRedTexture(tr);
    }//end constructor
    
    private TextureDescription getRedTexture(TR tr){
	if(texture==null){
	    final TextureManager tm = tr.gpu.get().textureManager.get();
	    return new SelectableTexture(
		    new Texture[]{
			    (Texture)tm.solidColor(new Color(255,0,0,255)),
			    (Texture)tm.solidColor(new Color(255,0,0,200)),
			    (Texture)tm.solidColor(new Color(255,0,0,155)),
			    (Texture)tm.solidColor(new Color(255,0,0,100)),
			    (Texture)tm.solidColor(new Color(255,0,0,55)),
			    (Texture)tm.solidColor(new Color(255,0,0,1))}
		    );
	}return texture;
    }//end genTexture(...)
    
public class RedFlash extends Sprite2D implements Feature<TVF3Game> {
    private volatile long endTimeOfLastFlash;
    private static final long FRAME_INTERVAL_MS=50L;
    private static final long NUM_FRAMES=5;
    
    private RedFlash(TR tr){
	super(tr, .000000001, 2, 2,getRedTexture(tr), true,"RedFlash");
	setVisible(false);
	addBehavior(new RedFlashBehavior());
	setImmuneToOpaqueDepthTest(true);
    }//end constructor
    
    public void flash(){
	endTimeOfLastFlash = System.currentTimeMillis()+FRAME_INTERVAL_MS*(NUM_FRAMES-1);
    }
    
    private class RedFlashBehavior extends Behavior{
	@Override
	public void tick(long tickTimeMillis){
	    if(tickTimeMillis<endTimeOfLastFlash){
		if(!isVisible())setVisible(true);
		final SelectableTexture st = (SelectableTexture)RedFlash.this.getTexture();
		st.setFrame((int)((NUM_FRAMES-1)-(endTimeOfLastFlash-tickTimeMillis)/FRAME_INTERVAL_MS));
	    }//end if(flashing)
	    else if(isVisible())setVisible(false);
	}//end _tick(...)
    }//end RedFlashBehavior

    @Override
    public void apply(TVF3Game target) {
	tr.getDefaultGrid().add(this);
    }//end apply(...)
    
    @Override
    public void destruct(TVF3Game target) {
	tr.getDefaultGrid().remove(this);
    }
}//end RedFlash

@Override
public Feature<TVF3Game> newInstance(TVF3Game target) {
    return new RedFlash(tr);
}

@Override
public Class<TVF3Game> getTargetClass() {
    return TVF3Game.class;
}

@Override
public Class<? extends Feature> getFeatureClass() {
    return RedFlash.class;
}
}//end RedFlashFactory
