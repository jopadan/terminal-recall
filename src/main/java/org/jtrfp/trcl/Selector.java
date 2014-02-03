/*******************************************************************************
 * This file is part of TERMINAL RECALL 
 * Copyright (c) 2012, 2013 Chuck Ritola.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the COPYING and CREDITS files for more details.
 * 
 * Contributors:
 *      chuck - initial API and implementation
 ******************************************************************************/
package org.jtrfp.trcl;

public class Selector implements Controller{
	double frame=0;
	private boolean stale=true;
	public boolean debug=false;
	public Selector()
		{}
	
	public Selector(double frame){
		this.frame=frame;
		}
	
	public void set(double frame){
	    this.frame=frame;stale=true;if(debug)System.out.println("Set to frame "+frame);}
	
	@Override
	public double getCurrentFrame(){
	    	if(debug)System.out.println("getCurrentFrame() "+frame);
		return frame;
		}
/*
	@Override
	public void unstale() {
	    stale=false;
	}

	@Override
	public boolean isStale() {
	    return stale;
	}*/

	@Override
	public void setDebugMode(boolean b) {
	   debug=b;
	}
}//end Selector
