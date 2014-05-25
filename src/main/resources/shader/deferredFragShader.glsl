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

// Deferred fragment rendering.

#version 330

// INPUTS
uniform sampler2D 		primaryRendering;
uniform sampler2D 		depthTexture;
uniform sampler2D 		texturePalette;
uniform sampler2D 		normTexture;
uniform usampler2D 		textureIDTexture;
uniform sampler2DArray 	rgbaTiles;
uniform usamplerBuffer 	rootBuffer; 	//Global memory, as a set of uint vec4s.
uniform vec3 			fogColor;
uniform uint 			screenWidth;
uniform uint 			screenHeight;
uniform vec3 			sunVector;

// OUTPUTS
layout(location = 0) out vec4 fragColor;

// CONSTANTS
const uint TOC_OFFSET_VEC4_HEADER				=91u;//1456/16
const uint TOC_HEADER_OFFSET_QUADS_WIDTH		=0u;
const uint TOC_HEADER_OFFSET_QUADS_HEIGHT		=1u;
const uint TOC_HEADER_OFFSET_QUADS_START_CODE	=2u;

const float TILE_PAGE_SIDE_WIDTH_TEXELS = 128;
const uint CODE_SIDE_WIDTH_TEXELS 		= 4u;
const uint CODE_PAGE_SIDE_WIDTH_CODES	= uint(TILE_PAGE_SIDE_WIDTH_TEXELS) / CODE_SIDE_WIDTH_TEXELS;
const uint CODES_PER_CODE_PAGE 			= CODE_PAGE_SIDE_WIDTH_CODES * CODE_PAGE_SIDE_WIDTH_CODES;
const uint CODE_PAGE_SIDE_WIDTH_TEXELS	= CODE_PAGE_SIDE_WIDTH_CODES * CODE_SIDE_WIDTH_TEXELS;
const float CODE_PAGE_TEXEL_SIZE_UV	= 1/float(CODE_PAGE_SIDE_WIDTH_TEXELS);

const uint SUBTEXTURE_SIDE_WIDTH_CODES = 39u;
const uint SUBTEXTURE_SIDE_WIDTH_TEXELS = SUBTEXTURE_SIDE_WIDTH_CODES * CODE_SIDE_WIDTH_TEXELS;

const vec3 sunColor 					= vec3(1.4,1.4,1.2);

//Adapted from http://www.geeks3d.com/20091216/geexlab-how-to-visualize-the-depth-buffer-in-glsl/
float linearizeDepth(float z){
float zNear = 6554;
float zFar = 1114112;
z = (2*zNear) / (zFar + zNear - z * (zFar - zNear));
return z;
}

uint UByte(uint _input, uint index)
	{return (_input >> 8u*index) & 0x000000FFu;}

vec4 codeTexel(vec2 codeXY, vec2 texelXY, uint textureID, uint startCode){

 vec2	subTexXY	= mod(texelXY,SUBTEXTURE_SIDE_WIDTH_TEXELS);
 uint	tTOCIdx		= uint(texelXY.x)/SUBTEXTURE_SIDE_WIDTH_TEXELS + (uint(texelXY.y)/SUBTEXTURE_SIDE_WIDTH_TEXELS) * 19u;
 uint	tTOCvec4Idx	= tTOCIdx / 4u;
 uint	tTOCsubIdx	= tTOCIdx % 4u;
 // Sub-Texture
 uint	subTexV4Addr= texelFetch(rootBuffer,int(textureID+tTOCvec4Idx))[tTOCsubIdx];

 vec2	subTexUVblnd= mod(texelXY,CODE_PAGE_TEXEL_SIZE_UV);//Subtexel to blend between texels
 uint	subTexByIdx = (uint(subTexXY.x)/CODE_SIDE_WIDTH_TEXELS + (uint(subTexXY.y)/CODE_SIDE_WIDTH_TEXELS) * 39u);
 uint	subTexV4Idx	= subTexByIdx / 16u;
 uint	subTexV4Sub = subTexByIdx % 16u;
 // Codebook
 uint	codeIdx		= UByte((texelFetch(rootBuffer,int(subTexV4Idx+subTexV4Addr))[subTexV4Sub/4u]),subTexV4Sub%4u)+startCode;
 uint	codeBkPgNum	= codeIdx / CODES_PER_CODE_PAGE;
 vec2	subTexUVsub	= codeXY*CODE_PAGE_TEXEL_SIZE_UV;
 vec2	codePgUV	= (vec2(float(codeIdx % CODE_PAGE_SIDE_WIDTH_CODES),float((codeIdx / CODE_PAGE_SIDE_WIDTH_CODES)%CODE_PAGE_SIDE_WIDTH_CODES))/float(CODE_PAGE_SIDE_WIDTH_CODES))+subTexUVsub;
 return				  texture(rgbaTiles,vec3(codePgUV,codeBkPgNum));
 }

////////// STRUCT LAYOUT DOCUMENTATION ///////////////
/**
textureTOC{
	19^2 x 4B
	offset 1456B or 91VEC4:
	TOCHeader {4B width, 4B height, 4B startCode, 4B ???}
	Unused 54B
	}
**/

void main(){
vec2	screenLoc 	= vec2(gl_FragCoord.x/screenWidth,gl_FragCoord.y/screenHeight);
float 	depth 		= texture(depthTexture,screenLoc)[0];
gl_FragDepth 		= depth;
float 	linearDepth = linearizeDepth(depth);
uint 	textureID 	= texture(textureIDTexture,screenLoc)[0u];
		fragColor 	= texture(primaryRendering,screenLoc);//GET UV
vec3 	norm 		= texture(normTexture,screenLoc).xyz*2-vec3(1,1,1);//UNPACK NORM
// TOC
uvec4 	tocHeader 	= texelFetch(rootBuffer,int(textureID+TOC_OFFSET_VEC4_HEADER));
vec2	tDims		= vec2(float(tocHeader[TOC_HEADER_OFFSET_QUADS_WIDTH]),float(tocHeader[TOC_HEADER_OFFSET_QUADS_HEIGHT]));
uint	startCode	= tocHeader[TOC_HEADER_OFFSET_QUADS_START_CODE];
vec2	texelXY		= tDims*fragColor.xy;
vec2	codeXY		= mod(texelXY,float(CODE_SIDE_WIDTH_TEXELS));
//Clamp sub-pixels within vector.
vec2	dH		= vec2(codeXY.x - 3.5,codeXY.y - 3.5);
vec2	dL		= vec2(.5 - codeXY.x,.5 - codeXY.y);

vec4	cTexel;
 //codeXY				= vec2(dH.x>0?3.5:codeXY.x,dH.y>0?3.5:codeXY.y);//Max
 //codeXY				= vec2(dL.x>0?.5:codeXY.x,dL.y>0?.5:codeXY.y);//Min
 codeXY					= clamp(codeXY,.5,3.5);
 
/*if(dH.x<0 && dH.y<0 && dL.x<0 && dL.y<0)*/cTexel = codeTexel(codeXY,texelXY,textureID,startCode); // Not near edge
//else cTexel = vec4(0,0,0,0);
//else if(dH.x>0 && dH.y<0 && dL.x<0 && dL.y<0)cTexel = codeTexel(codeXY,codeIdx);//Far right
//else if(dH.x<0 && dH.y<0 && dL.x>0 && dL.y<0)cTexel = codeTexel(codeXY,codeIdx);//Far left
//else if();

vec3 	origColor 	= textureID==960u?texture(texturePalette,fragColor.xy).rgb:
	cTexel.rgb;//GET COLOR
	
//TODO: code-tile edge blending compensation (up to 4 samplings of overhead)

// Illumination. Near-zero norm means assume full lighting
float sunIllumination	= length(norm)>.1?clamp(dot(sunVector,normalize(norm)),0,1):.5;
fragColor.rgb 			= origColor*fogColor+origColor*sunIllumination*sunColor;
fragColor 				= mix(fragColor,vec4(fogColor*sunColor,1),clamp(pow(linearDepth,3)*1.5,0,1));//FOG
}//end main()