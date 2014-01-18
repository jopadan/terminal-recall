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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class AttribAnimator
	{
	Settable attrib;
	Controller controller;
	double [] frames;
	private final boolean loopInterpolate;
	public AttribAnimator(Settable attrib, Controller sequencer, double [] frames)
		{
		this(attrib,sequencer,frames,true);
		}
	public AttribAnimator(Settable attrib, Controller sequencer, double [] frames,boolean loopInterpolate)
		{
		this.loopInterpolate=loopInterpolate;
		this.attrib=attrib;
		this.controller=sequencer;
		this.frames=frames;
		}
	
	public void updateAnimation()
		{//if(!controller.isStale())return;//Not yet ready for prime time.
		double frame=controller.getCurrentFrame();
		controller.unstale();
		int lowFrame = (int)frame;
		int hiFrame = (lowFrame+1)%frames.length;
		double interpolation = frame-(double)lowFrame;
		if(hiFrame==0 && !loopInterpolate){lowFrame=0;hiFrame=1;}
		double hI=interpolation;
		double lI=1.-interpolation;
		attrib.set(frames[lowFrame]*lI+frames[hiFrame]*hI);
		}
	/*
	public static void updateAllAnimators(Object objectWithAnimators)
		{//Includes private fields!
		Field [] fields = objectWithAnimators.getClass().getDeclaredFields();
		for(Field f:fields)
			{
			if(f.getType()==AttribAnimator.class)
				{
				f.setAccessible(true);
				try {
					AttribAnimator ani = (AttribAnimator)f.get(objectWithAnimators);
					ani.updateAnimation();
					}
				catch(IllegalAccessException e)
					{e.printStackTrace();}
				if(Modifier.isPrivate(f.getModifiers()))f.setAccessible(false);
				}
			}//end for(fields)
		}//end updateAllAnimators()
	*/
	}//end AttribAnimator
