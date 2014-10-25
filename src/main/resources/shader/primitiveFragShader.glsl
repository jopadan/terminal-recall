/*******************************************************************************
 * This file is part of TERMINAL RECALL 
 * Copyright (c) 2012-2014 Chuck Ritola.
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
 
 ///////// INCOMPLETE ///////////
 
 #version 330

//INPUTS
flat in mat4 uvzwQuad;
flat in mat4 nXnYnZQuad;

//OUTPUTS
layout(location = 0) out vec4	uvzwBuffer;
layout(location = 1) out vec4	nXnYnZBuffer;

void main(){
 uint quadrant = uint(gl_FragCoord.x)%2u+(uint(gl_FragCoord.y)%2u)*2u;
 uvzwBuffer = uvzwQuad[quadrant];
 nXnYnZBuffer = nXnYnZQuad[quadrant];
 }
 