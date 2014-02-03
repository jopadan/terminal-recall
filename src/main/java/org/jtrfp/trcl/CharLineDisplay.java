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

import java.util.Arrays;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.core.TR;

public class CharLineDisplay
	{
	private char [] content;
	private final CharDisplay [] displays;
	double [] position = new double []{0,0,.0001};
	private GLFont font;
	private final double glSize;
	
	public CharLineDisplay(TR tr,RenderableSpacePartitioningGrid grid, double glSize, int lengthInChars, GLFont font)
		{content = new char[lengthInChars];
		displays = new CharDisplay[lengthInChars];
		this.font=font;
		for(int i=0; i<lengthInChars; i++)
			{content[i]='X';
			displays[i]=new CharDisplay(tr,grid,glSize,font);
			displays[i].setChar('X');
			grid.add(displays[i]);
			}//end for(lengthInChars)
		
		this.glSize=glSize;
		updatePositions();
		}//end LineDisplay(...)
	
	public void setContent(String content)
		{for(int i=0; i<this.content.length; i++)
			{char newContent;
			if(i<content.length())
				{newContent=content.charAt(i);}
			else{newContent=' ';}
			this.content[i]=newContent;
			displays[i].setChar(newContent);
			}//end for(length)
		updatePositions();
		}//end setContent(...)
	
	private void updatePositions()
		{final double[] charPosition=Arrays.copyOf(position,3);
		for(int i=0; i<displays.length; i++){
		    	final double [] dispPos = displays[i].getPosition();
		    	dispPos[0]=charPosition[0];
		    	dispPos[1]=charPosition[1];
		    	dispPos[2]=charPosition[2];
		    	displays[i].notifyPositionChange();
			char _content = content[i];
			final double progress=((double)glSize)*font.glWidthOf(_content)*1.1;//1.1 fudge factor for space between letters
			charPosition[0]+=progress;
			//charPosition=charPosition.add(new Vector3D(progress,0,0));
			}//end for(length)
		}//end updatePositions
	
	public void setPosition(double [] location)
		{this.position=location;
		updatePositions();
		}//end setPosition(...)

	public void setPosition(double x, double y, double z) {
	    position[0]=x;
	    position[1]=y;
	    position[2]=z;
	    updatePositions();
	}

	/**
	 * @return the displays
	 */
	public CharDisplay[] getDisplays() {
	    return displays;
	}

	public GLFont getFont() {
	    // TODO Auto-generated method stub
	    return font;
	}
}//end LineDisplay
