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

import java.util.concurrent.Callable;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import org.jtrfp.trcl.ObjectListWindow;
import org.jtrfp.trcl.World;
import org.jtrfp.trcl.gpu.GLFragmentShader;
import org.jtrfp.trcl.gpu.GLFrameBuffer;
import org.jtrfp.trcl.gpu.GLProgram;
import org.jtrfp.trcl.gpu.GLProgram.ValidationHandler;
import org.jtrfp.trcl.gpu.GLTexture;
import org.jtrfp.trcl.gpu.GLUniform;
import org.jtrfp.trcl.gpu.GLVertexShader;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.gpu.ObjectProcessingStage;
import org.jtrfp.trcl.gpu.VertexProcessingStage;
import org.jtrfp.trcl.gui.Reporter;

public class RendererFactory {
    
    public static final int			PRIMITIVE_BUFFER_WIDTH  = 512;
    public static final int			PRIMITIVE_BUFFER_HEIGHT = 512;
    public static final int			NUM_PORTALS = 4;
    public static final int			PRIMITIVE_BUFFER_OVERSAMPLING = 4;
    public static final int			OBJECT_BUFFER_WIDTH = 4*RenderList.NUM_BLOCKS_PER_PASS*RenderList.NUM_RENDER_PASSES;
    
    private final GPU gpu;
    private final World world;
    private final Reporter reporter;
    //private final CollisionManager              collisionManager;
    private final	boolean			backfaceCulling;
    private 	 	GLUniform	    	sunVector;
    private 		GLTexture 		opaqueDepthTexture,
    /*					*/	opaquePrimitiveIDTexture,
    /*					*/	depthQueueTexture,
    /*					*/	primitiveUVZWTexture,primitiveNormTexture,
    /*					*/	layerAccumulatorTexture,
    /*					*/	portalTexture;
    private 		GLFrameBuffer 		opaqueFrameBuffer,
    /*					*/	depthQueueFrameBuffer,
    /*					*/	objectFrameBuffer,
    /*					*/	vertexFrameBuffer,
    /*					*/	primitiveFrameBuffer;
    private final	GLFrameBuffer[]		portalFrameBuffers = new GLFrameBuffer[NUM_PORTALS];
    
    private            GLProgram 		
    /*					*/	opaqueProgram, 
    /*					*/	deferredProgram, 
    /*					*/	depthQueueProgram, 
    /*                                  */      primitiveProgram,
    /*					*/	skyCubeProgram;
    private final ThreadManager			threadManager;
    private  ObjectProcessingStage              objectProcessingStage;
    private VertexProcessingStage               vertexProcessingStage;
    
    public RendererFactory(final GPU gpu, final ThreadManager threadManager, 
	    final GLCanvas canvas, Reporter reporter, final World world, 
	    /*CollisionManager collisionManager, */ObjectListWindow objectListWindow){
	this.gpu=gpu;
	this.threadManager = threadManager;
	this.reporter=reporter;
	this.world=world;
	//this.collisionManager=collisionManager;
	final GL3 gl = gpu.getGl();
	
	threadManager.submitToGL(new Callable<Void>(){
	    @Override
	    public Void call() throws Exception {
		// Fixed pipeline behavior
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		gl.glDepthRange(0, 1);
		gl.glEnable(GL3.GL_DEPTH_CLAMP);
		
		final ValidationHandler vh = new RFValidationHandler();
		objectProcessingStage      = new ObjectProcessingStage(gpu,vh);
		vertexProcessingStage      = new VertexProcessingStage(gpu,objectProcessingStage,vh);
		
		// VERTEX SHADERS
		GLVertexShader		
					traditionalVertexShader		= gpu.newVertexShader(),
					fullScreenQuadVertexShader	= gpu.newVertexShader(),
					primitiveVertexShader		= gpu.newVertexShader(),
					skyCubeVertexShader		= gpu.newVertexShader(),
					fullScreenTriangleShader	= gpu.newVertexShader();
		GLFragmentShader	
					opaqueFragShader		= gpu.newFragmentShader(),
					deferredFragShader		= gpu.newFragmentShader(),
					depthQueueFragShader		= gpu.newFragmentShader(),
					vertexFragShader		= gpu.newFragmentShader(),
					primitiveFragShader		= gpu.newFragmentShader(),
					skyCubeFragShader		= gpu.newFragmentShader();
		fullScreenTriangleShader  .setSourceFromResource("/shader/fullScreenTriangleVertexShader.glsl");
		traditionalVertexShader	  .setSourceFromResource("/shader/traditionalVertexShader.glsl");
		fullScreenQuadVertexShader.setSourceFromResource("/shader/fullScreenQuadVertexShader.glsl");
		opaqueFragShader	  .setSourceFromResource("/shader/opaqueFragShader.glsl");
		deferredFragShader	  .setSourceFromResource("/shader/deferredFragShader.glsl");
		depthQueueFragShader	  .setSourceFromResource("/shader/depthQueueFragShader.glsl");
		vertexFragShader	  .setSourceFromResource("/shader/vertexFragShader.glsl");
		primitiveFragShader	  .setSourceFromResource("/shader/primitiveFragShader.glsl");
		primitiveVertexShader	  .setSourceFromResource("/shader/primitiveVertexShader.glsl");
		skyCubeFragShader	  .setSourceFromResource("/shader/skyCubeFragShader.glsl");
		skyCubeVertexShader	  .setSourceFromResource("/shader/skyCubeVertexShader.glsl");
		
		
		opaqueProgram		=gpu.newProgram().setValidationHandler(vh).attachShader(traditionalVertexShader)	  .attachShader(opaqueFragShader).link();
		deferredProgram		=gpu.newProgram().setValidationHandler(vh).attachShader(skyCubeVertexShader)  	  .attachShader(deferredFragShader).link();
		depthQueueProgram	=gpu.newProgram().setValidationHandler(vh).attachShader(traditionalVertexShader)	  .attachShader(depthQueueFragShader).link();
		primitiveProgram	=gpu.newProgram().setValidationHandler(vh).attachShader(primitiveVertexShader)     .attachShader(primitiveFragShader).link();
		skyCubeProgram		=gpu.newProgram().setValidationHandler(vh).attachShader(skyCubeVertexShader)       .attachShader(skyCubeFragShader).link();
		
		skyCubeProgram.use();
		skyCubeProgram.getUniform("cubeTexture").set((int)0);
		
		opaqueProgram.use();
		opaqueProgram.getUniform("xyBuffer").set((int)1);
		/// 2 UNUSED
		/// 3 UNUSED
		opaqueProgram.getUniform("zBuffer").set((int)4);
		opaqueProgram.getUniform("wBuffer").set((int)5);
		/// 6 UNUSED
		/// 7 UNUSED
		
		primitiveProgram.use();
		primitiveProgram.getUniform("xyVBuffer").set((int)0);
		primitiveProgram.getUniform("wVBuffer").set((int)1);
		primitiveProgram.getUniform("zVBuffer").set((int)2);
		primitiveProgram.getUniform("uvVBuffer").set((int)3);
		primitiveProgram.getUniform("nXnYnZVBuffer").set((int)4);
		primitiveProgram.getUniform("nZVBuffer").set((int)5);
		
		depthQueueProgram.use();
		/// ... zero?
		/// 1 UNUSED
		depthQueueProgram.getUniform("xyBuffer").set((int)2);
		/// 3 UNUSED
		/// 4 UNUSED
		depthQueueProgram.getUniform("zBuffer").set((int)5);
		depthQueueProgram.getUniform("wBuffer").set((int)6);
		deferredProgram.use();
		sunVector 	= deferredProgram	.getUniform("sunVector");
		deferredProgram.getUniform("rootBuffer").set((int) 0);
		deferredProgram.getUniform("cubeTexture").set((int)1);
		deferredProgram.getUniform("portalTexture").set((int)2);
		deferredProgram.getUniform("ESTuTvTiles").set((int) 3);
		deferredProgram.getUniform("rgbaTiles").set((int) 4);
		deferredProgram.getUniform("primitiveIDTexture").set((int) 5);
		deferredProgram.getUniform("layerAccumulator").set((int)6);
		deferredProgram.getUniform("vertexTextureIDTexture").set((int) 7);
		deferredProgram.getUniform("primitiveUVZWTexture").set((int) 8);
		deferredProgram.getUniform("primitivenXnYnZTexture").set((int) 9);
		deferredProgram.getUniform("ambientLight").set(.4f, .5f, .7f);
		sunVector.set(.5774f,-.5774f,.5774f);
		final int width  = canvas.getWidth();
		final int height = canvas.getHeight();
		gpu.defaultProgram();
		gpu.defaultTIU();
		
		/////// PRIMITIVE
		primitiveNormTexture = gpu  //Does not need to be in reshape() since it is off-screen.
			.newTexture()
			.bind()
			.setImage(GL3.GL_RGBA32F,// A is unused. Intel driver doesn't like layering RGB and RGBA together. 
				PRIMITIVE_BUFFER_WIDTH * PRIMITIVE_BUFFER_OVERSAMPLING, 
				PRIMITIVE_BUFFER_HEIGHT * PRIMITIVE_BUFFER_OVERSAMPLING, 
				GL3.GL_RGBA,
				GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_LINEAR)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE)
			.setDebugName("primitiveNormTexture");
		primitiveUVZWTexture = gpu  //Does not need to be in reshape() since it is off-screen.
			.newTexture()
			.bind()
			.setImage(GL3.GL_RGBA32F, 
				PRIMITIVE_BUFFER_WIDTH * PRIMITIVE_BUFFER_OVERSAMPLING, 
				PRIMITIVE_BUFFER_HEIGHT * PRIMITIVE_BUFFER_OVERSAMPLING, 
				GL3.GL_RGBA,
				GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_LINEAR)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE)
			.setDebugName("primitiveUVZWTexture");
		primitiveFrameBuffer = gpu
			.newFrameBuffer()
			.bindToDraw()
			.attachDrawTexture(primitiveUVZWTexture,
				GL3.GL_COLOR_ATTACHMENT0)
			.attachDrawTexture(primitiveNormTexture,
				GL3.GL_COLOR_ATTACHMENT1)
			.setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0,GL3.GL_COLOR_ATTACHMENT1);
		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE){
		    throw new RuntimeException("Primitive framebuffer setup failure. OpenGL code "+gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
		}
		/////// PORTALS
		allocatePortals(width,height);
		/////// INTERMEDIATE
		opaqueDepthTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_DEPTH_COMPONENT16, width, height, 
				GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE)
			.setDebugName("opaqueDepthTexture");
		opaquePrimitiveIDTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_R32F, width, height, 
				GL3.GL_RED, GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE)
			.setExpectedMaxValue(.1, .1, .1, .1)
			.setDebugName("opaquePrimitiveIDTexture")
			.unbind();
		opaqueFrameBuffer = gpu
			.newFrameBuffer()
			.bindToDraw()
			.attachDepthTexture(opaqueDepthTexture)
			.attachDrawTexture(opaquePrimitiveIDTexture, 
				GL3.GL_COLOR_ATTACHMENT0)
			.attachDepthTexture(opaqueDepthTexture)
			.setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0)
			.unbindFromDraw();
		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE){
		    throw new RuntimeException("Intermediate framebuffer setup failure. OpenGL code "+gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
		}
		/////// DEPTH QUEUE
		layerAccumulatorTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_RGBA32F, width, height, GL3.GL_RGBA, GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE)
			.setExpectedMaxValue(65536, 65536, 65536, 65536)
			.setDebugName("floatShiftQueueTexture")
			.unbind();
		depthQueueFrameBuffer = gpu
			.newFrameBuffer()
			.bindToDraw()
			.attachDrawTexture(layerAccumulatorTexture, GL3.GL_COLOR_ATTACHMENT0)
			.attachDepthTexture(opaqueDepthTexture)
			/*
			.attachDrawTexture2D(depthQueueTexture, 
				GL3.GL_COLOR_ATTACHMENT0,GL3.GL_TEXTURE_2D_MULTISAMPLE)
			.attachDepthTexture2D(depthQueueStencil)
			.attachStencilTexture2D(depthQueueStencil)
			*/
			.setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0);
		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE){
		    throw new RuntimeException("Depth queue framebuffer setup failure. OpenGL code "+gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
		}
		gpu.defaultProgram();
		gpu.defaultTIU();
		gpu.defaultTexture();
		gpu.defaultFrameBuffers();
		return null;
	    }
	}).get();
	
	canvas.addGLEventListener(new GLEventListener() {
	    @Override
	    public void init(GLAutoDrawable drawable) {
		drawable.getGL().setSwapInterval(0);
	    }

	    @Override
	    public void dispose(GLAutoDrawable drawable) {
	    }

	    @Override
	    public void display(GLAutoDrawable drawable) {
	    }

	    @Override
	    public void reshape(GLAutoDrawable drawable, int x, int y,
		    int width, int height) {
		// SHAPE-DEPENDENT UNIFORMS
		deferredProgram.use();
		deferredProgram.getUniform("screenDims").set(drawable.getWidth(), drawable.getHeight());
		gpu.defaultProgram();
		gpu.defaultFrameBuffers();
		gpu.defaultTIU();
		opaqueDepthTexture.bind().setImage(GL3.GL_DEPTH_COMPONENT16, width, height, 
			GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);
		opaquePrimitiveIDTexture.bind().setImage(GL3.GL_R32F, width, height, GL3.GL_RED, GL3.GL_FLOAT, null);
		layerAccumulatorTexture.bind().setImage(GL3.GL_RGBA32F, width, height, GL3.GL_RGBA, GL3.GL_FLOAT, null);
		portalTexture.delete();
		for(int i=0; i<NUM_PORTALS; i++)
		    portalFrameBuffers[i].destroy();
		allocatePortals(width,height);
		gpu.defaultTexture();
	    }//end reshape(...)
	});
	
	if(System.getProperties().containsKey("org.jtrfp.trcl.core.RenderList.backfaceCulling")){
	    backfaceCulling = System.getProperty("org.jtrfp.trcl.core.RenderList.backfaceCulling").toUpperCase().contains("TRUE");
	}else backfaceCulling = true;
    }//end constructor
    
    private void allocatePortals(int width, int height){
	portalTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGB565).
		configure(new int[]{width,height,NUM_PORTALS}, 1).
		setMagFilter(GL3.GL_NEAREST).
		setMinFilter(GL3.GL_NEAREST).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE).
		unbind();
	for(int i=0; i<NUM_PORTALS; i++)
	    portalFrameBuffers[i]=gpu.
	     newFrameBuffer().
	     bindToDraw().
	     attachDrawTexture(portalTexture, i, GL3.GL_COLOR_ATTACHMENT0).
	     setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0).
	     unbindFromDraw();
    }//end allocatePortals()
    
    public Renderer newRenderer(String debugName){
	return new Renderer(this,world,threadManager,reporter,gpu.objectListWindow.get(),debugName);
    }

    /**
     * @return the gpu
     */
    public GPU getGPU() {
        return gpu;
    }
    

    /**
     * @return the depthQueueTexture
     */
    public GLTexture getDepthQueueTexture() {
        return depthQueueTexture;
    }
    
    /**
     * @return the depthQueueFrameBuffer
     */
    public GLFrameBuffer getDepthQueueFrameBuffer() {
        return depthQueueFrameBuffer;
    }

    /**
     * @return the objectFrameBuffer
     */
    public GLFrameBuffer getObjectFrameBuffer() {
        return objectFrameBuffer;
    }
    
    public GLFrameBuffer getOpaqueFrameBuffer() {
        return opaqueFrameBuffer;
    }
    
    public GLTexture getOpaqueDepthTexture() {
        return opaqueDepthTexture;
    }
    
    public GLTexture getOpaquePrimitiveIDTexture() {
        return opaquePrimitiveIDTexture;
    }
    
    public GLFrameBuffer getVertexFrameBuffer() {
        return vertexFrameBuffer;
    }

    /**
     * @return the primitiveUVZWTexture
     */
    public GLTexture getPrimitiveUVZWTexture() {
        return primitiveUVZWTexture;
    }

    /**
     * @return the primitiveNormTexture
     */
    public GLTexture getPrimitiveNormTexture() {
        return primitiveNormTexture;
    }

    /**
     * @return the primitiveFrameBuffer
     */
    public GLFrameBuffer getPrimitiveFrameBuffer() {
        return primitiveFrameBuffer;
    }

    public GLUniform getSunVectorUniform() {
	return sunVector;
    }

    /**
     * @return the primaryProgram
     */
    GLProgram getOpaqueProgram() {
        return opaqueProgram;
    }

    /**
     * @return the deferredProgram
     */
    GLProgram getDeferredProgram() {
        return deferredProgram;
    }

    /**
     * @return the backfaceCulling
     */
    protected boolean isBackfaceCulling() {
        return backfaceCulling;
    }

    /**
     * @return the depthQueueProgram
     */
    public GLProgram getDepthQueueProgram() {
        return depthQueueProgram;
    }


    /**
     * @return the primitiveProgram
     */
    public GLProgram getPrimitiveProgram() {
        return primitiveProgram;
    }

    /**
     * @return the floatShiftQueueTexture
     */
    public GLTexture getLayerAccumulatorTexture() {
        return layerAccumulatorTexture;
    }

    /**
     * @return the skyCubeProgram
     */
    public GLProgram getSkyCubeProgram() {
        return skyCubeProgram;
    }
    
    private class RFValidationHandler implements ValidationHandler{
	@Override
	public void invalidProgram(GLProgram p) {
	    //Ignore.
	}
    }//end RFValidationHandler

    /**
     * @return the portalTexture
     */
    public GLTexture getPortalTexture() {
        return portalTexture;
    }

    public ObjectProcessingStage getObjectProcessingStage() {
	return objectProcessingStage;
    }

    public VertexProcessingStage getVertexProcessingStage() {
	return vertexProcessingStage;
    }

    /**
     * @return the portalFrameBuffers
     */
    public GLFrameBuffer[] getPortalFrameBuffers() {
        return portalFrameBuffers;
    }
}//end RendererFactory
