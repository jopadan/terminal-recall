package org.jtrfp.trcl.core;

import java.awt.Color;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.GridCubeProximitySorter;
import org.jtrfp.trcl.RenderableSpacePartitioningGrid;
import org.jtrfp.trcl.gpu.GLFragmentShader;
import org.jtrfp.trcl.gpu.GLFrameBuffer;
import org.jtrfp.trcl.gpu.GLProgram;
import org.jtrfp.trcl.gpu.GLTexture;
import org.jtrfp.trcl.gpu.GLUniform;
import org.jtrfp.trcl.gpu.GLVertexShader;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.obj.PositionedRenderable;
import org.jtrfp.trcl.obj.WorldObject;

public final class Renderer {

    public static final int			DEPTH_QUEUE_SIZE = 3;
    private 		RenderableSpacePartitioningGrid rootGrid;
    private final	GridCubeProximitySorter proximitySorter = new GridCubeProximitySorter();
    private final 	Camera			camera;
    			GLProgram 		primaryProgram, deferredProgram, depthQueueProgram, depthErasureProgram;
    private 		boolean 		initialized = false;
    private		boolean 		active = false;// TODO: Remove when conversion is complete
    private 		boolean 		renderListToggle = false;
    private final 	GPU 			gpu;
    public final 	TRFutureTask<RenderList>[]renderList = new TRFutureTask[2];
    private 	 	GLUniform	    	screenWidth, 
    /*    */	    				screenHeight,
    /*    */					fogColor,
    /*    */					sunVector;
    private 		GLTexture 		intermediateColorTexture,
    /*		*/				intermediateDepthTexture,
    /*		*/				intermediateNormTexture,
    /*		*/				intermediateTextureIDTexture,
    /*		*/				depthQueueTexture,
    /*		*/				depthQueueStencil;
    private 		GLFrameBuffer 		intermediateFrameBuffer,
    /*			*/			depthQueueFrameBuffer;
    private 		int			frameNumber;
    private 		long			lastTimeMillis;
    private final	boolean			backfaceCulling;
    private		double			meanFPS;
    private		Future			visibilityUpdateFuture;

    public Renderer(final GPU gpu) {
	final TR tr = gpu.getTr();
	this.gpu = gpu;
	this.camera = new Camera(gpu);
	final GL3 gl = gpu.getGl();
	tr.getThreadManager().submitToGL(new Callable<Void>(){
	    @Override
	    public Void call() throws Exception {
		// Fixed pipeline behavior
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glClearColor(0f, 0f, 0f, 0f);
		
		// VERTEX SHADERS
		GLVertexShader		primaryVertexShader		= gpu.newVertexShader(),
					fullScreenQuadVertexShader	= gpu.newVertexShader();
		GLFragmentShader	primaryFragShader		= gpu.newFragmentShader(),
					deferredFragShader		= gpu.newFragmentShader(),
					depthQueueFragShader		= gpu.newFragmentShader(),
					erasureFragShader		= gpu.newFragmentShader();
		primaryVertexShader	  .setSourceFromResource("/shader/vertexShader.glsl");
		fullScreenQuadVertexShader.setSourceFromResource("/shader/fullScreenQuadVertexShader.glsl");
		primaryFragShader	  .setSourceFromResource("/shader/fragShader.glsl");
		deferredFragShader	  .setSourceFromResource("/shader/deferredFragShader.glsl");
		erasureFragShader	  .setSourceFromResource("/shader/erasureFragShader.glsl");
		depthQueueFragShader	  .setSourceFromResource("/shader/depthQueueFragShader.glsl");
		
		primaryProgram		=gpu.newProgram().attachShader(primaryVertexShader)	  .attachShader(primaryFragShader).link();
		deferredProgram		=gpu.newProgram().attachShader(fullScreenQuadVertexShader).attachShader(deferredFragShader).link();
		depthQueueProgram	=gpu.newProgram().attachShader(primaryVertexShader)	  .attachShader(depthQueueFragShader).link();
		depthErasureProgram	=gpu.newProgram().attachShader(fullScreenQuadVertexShader).attachShader(erasureFragShader).link();
		deferredProgram.use();
		screenWidth 	= deferredProgram	.getUniform("screenWidth");
		screenHeight 	= deferredProgram	.getUniform("screenHeight");
		fogColor 	= deferredProgram	.getUniform("fogColor");
		sunVector 	= deferredProgram	.getUniform("sunVector");
		deferredProgram.getUniform("texturePalette").set((int) 0);
		deferredProgram.getUniform("primaryRendering").set((int) 1);
		deferredProgram.getUniform("depthTexture").set((int) 2);
		deferredProgram.getUniform("normTexture").set((int) 3);
		deferredProgram.getUniform("rootBuffer").set((int) 4);
		deferredProgram.getUniform("rgbaTiles").set((int) 5);
		deferredProgram.getUniform("textureIDTexture").set((int) 6);
		deferredProgram.getUniform("depthQueueTexture").set((int) 7);
		sunVector.set(.5774f,.5774f,.5774f);
		final int width = tr.getRootWindow().getWidth();
		final int height = tr.getRootWindow().getHeight();
		/////// INTERMEDIATE
		intermediateColorTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_RG16, width, height, GL3.GL_RGB,
				GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST);
		intermediateNormTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_RGB8, width, height, GL3.GL_RGB, GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST);
		intermediateDepthTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_DEPTH_COMPONENT24, width, height, 
				GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE);
		intermediateTextureIDTexture = gpu
			.newTexture()
			.bind()
			.setImage(GL3.GL_R32UI, width, height, 
				GL3.GL_RED_INTEGER, GL3.GL_UNSIGNED_INT, null)
			.setMagFilter(GL3.GL_NEAREST)
			.setMinFilter(GL3.GL_NEAREST)
			.setWrapS(GL3.GL_CLAMP_TO_EDGE)
			.setWrapT(GL3.GL_CLAMP_TO_EDGE);
		intermediateFrameBuffer = gpu
			.newFrameBuffer()
			.bindToDraw()
			.attachDrawTexture(intermediateColorTexture,
				GL3.GL_COLOR_ATTACHMENT0)
			.attachDrawTexture(intermediateNormTexture, 
				GL3.GL_COLOR_ATTACHMENT1)
			.attachDrawTexture(intermediateTextureIDTexture, 
				GL3.GL_COLOR_ATTACHMENT2)
			.attachDepthTexture(intermediateDepthTexture)
			.setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0,GL3.GL_COLOR_ATTACHMENT1,GL3.GL_COLOR_ATTACHMENT2);
		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE){
		    throw new RuntimeException("Intermediate framebuffer setup failure. OpenGL code "+gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
		}
		/////// DEPTH QUEUE
		depthQueueTexture = gpu
			.newTexture()
			.setBindingTarget(GL3.GL_TEXTURE_2D_MULTISAMPLE)
			.bind()
			.setImage2DMultisample(DEPTH_QUEUE_SIZE, GL3.GL_RGBA32F,width,height,false);//TODO: width,height
		depthQueueStencil = gpu
			.newTexture()
			.setBindingTarget(GL3.GL_TEXTURE_2D_MULTISAMPLE)
			.bind()
			.setImage2DMultisample(DEPTH_QUEUE_SIZE, GL3.GL_DEPTH24_STENCIL8,width,height,false);//TODO: width,height
		depthQueueFrameBuffer = gpu
			.newFrameBuffer()
			.bindToDraw()
			.attachDrawTexture2D(depthQueueTexture, 
				GL3.GL_COLOR_ATTACHMENT0,GL3.GL_TEXTURE_2D_MULTISAMPLE)
			.attachDepthTexture2D(depthQueueStencil)
			.setDrawBufferList(GL3.GL_COLOR_ATTACHMENT0);
		if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE){
		    throw new RuntimeException("Depth queue framebuffer setup failure. OpenGL code "+gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
		}
		primaryProgram.use();
		return null;
	    }
	}).get();
	
	System.out.println("Renderer adding GLEventListener");
	tr.getRootWindow().getCanvas().addGLEventListener(new GLEventListener() {
	    @Override
	    public void init(GLAutoDrawable drawable) {
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
		Renderer.this.getDeferredProgram().use();
		intermediateColorTexture.bind().setImage(GL3.GL_RG16, width,
			height, GL3.GL_RGB, GL3.GL_FLOAT, null);
		intermediateDepthTexture.bind().setImage(GL3.GL_DEPTH_COMPONENT24, width, height, 
			GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);
		intermediateNormTexture.bind().setImage(GL3.GL_RGB8, width, height, GL3.GL_RGB, GL3.GL_FLOAT, null);
		intermediateTextureIDTexture.bind().setImage(GL3.GL_R32UI, width, height, GL3.GL_RED_INTEGER, GL3.GL_UNSIGNED_INT, null);
		depthQueueStencil.bind().setImage2DMultisample(DEPTH_QUEUE_SIZE, GL3.GL_DEPTH24_STENCIL8,width,height,false);
		depthQueueTexture.bind().setImage2DMultisample(DEPTH_QUEUE_SIZE, GL3.GL_RGBA32F,width,height,false);//TODO: Change to RGBA32
		screenWidth.setui(width);
		screenHeight.setui(height);
		depthQueueProgram.use();
		//depthQueueScreenWidth.setui(width);
		//depthQueueScreenHeight.setui(height);
		Renderer.this.getPrimaryProgram().use();
	    }
	});
	System.out.println("...Done.");
	System.out.println("Initializing RenderList...");
	renderList[0] = new TRFutureTask(tr,new Callable<RenderList>(){
	    @Override
	    public RenderList call() throws Exception {
		return new RenderList(gl, primaryProgram,deferredProgram, depthQueueProgram, intermediateFrameBuffer, 
			    intermediateColorTexture,intermediateDepthTexture, intermediateNormTexture, 
			    intermediateTextureIDTexture, depthQueueFrameBuffer, depthQueueTexture , tr);
	    }});tr.getThreadManager().threadPool.submit(renderList[0]);
	    renderList[1] = new TRFutureTask(tr,new Callable<RenderList>(){
		    @Override
		    public RenderList call() throws Exception {
			return new RenderList(gl, primaryProgram,deferredProgram, depthQueueProgram, intermediateFrameBuffer, 
				    intermediateColorTexture,intermediateDepthTexture, intermediateNormTexture, 
				    intermediateTextureIDTexture, depthQueueFrameBuffer, depthQueueTexture, tr);
		    }});tr.getThreadManager().threadPool.submit(renderList[1]);
	if(System.getProperties().containsKey("org.jtrfp.trcl.core.RenderList.backfaceCulling")){
	    backfaceCulling = System.getProperty("org.jtrfp.trcl.core.RenderList.backfaceCulling").toUpperCase().contains("TRUE");
	}else backfaceCulling = true;
    }//end constructor

    private void ensureInit() {
	if (initialized)
	    return;
	final GL3 gl = gpu.getGl();

	gpu.memoryManager.get().map();

	System.out.println("Uploading vertex data to GPU...");
	//TriangleList.uploadAllListsToGPU(gl);
	System.out.println("...Done.");
	System.out.println("Uploading object definition data to GPU...");
	WorldObject.uploadAllObjectDefinitionsToGPU();
	System.out.println("...Done.");
	System.out.println("\t...World.init() complete.");

	try {
	    gpu.memoryManager.get().bindToUniform(1, primaryProgram,
		    primaryProgram.getUniform("rootBuffer"));
	    //primaryProgram.getUniform("textureMap").set((int) 0);// Texture unit
								// 0 mapped to
								// textureMap
	} catch (RuntimeException e) {
	    e.printStackTrace();
	}
	GLTexture.specifyTextureUnit(gl, 0);
	if(Texture.getGlobalTexture()!=null)//New texturing leaves this null.
	    Texture.getGlobalTexture().bind(gl);
	if (!primaryProgram.validate()) {
	    System.out.println(primaryProgram.getInfoLog());
	    System.exit(1);
	}
	System.out.println("...Done.");
	//gpu.getMemoryManager().map();
	initialized = true;
    }// end ensureInit()

    private void fpsTracking() {
	frameNumber++;
	final long dT = (long) (System.currentTimeMillis() - lastTimeMillis);
	if(dT<=0)return;
	final int fps = (int)(1000L / dT);
	meanFPS = meanFPS*.9+(double)fps*.1;
	if ((frameNumber %= 20) == 0) {
	    gpu.getTr().getReporter()
		    .report("org.jtrfp.trcl.core.Renderer.FPS", "" + meanFPS);
	    gpu.getTr().getReporter()
	    	.report("org.jtrfp.trcl.core.Renderer.numVisibleObjects", renderList[renderListToggle ? 0 : 1].get().getVisibleWorldObjectList().size());
	}
	lastTimeMillis = System.currentTimeMillis();
    }//end fpsTracking()
    private volatile int counter=0;//TODO: remove
    public void render() {
	long startTimeMillis = System.currentTimeMillis();//TODO: Remove
	counter++;//TODO: Remove
	if (!active)
	    return;
	final GL3 gl = gpu.getGl();
	gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
	startTimeMillis = System.currentTimeMillis();//TODO: Remove
	ensureInit();
	if(counter%50==0)System.out.println("Passed for ensureInit() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
	if(gpu.getTr().getTrConfig().isUsingTextureBufferUnmap()){
	    gpu.memoryManager.get().unmap();
	}
	if(counter%50==0)System.out.println("Passed for unmap() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
	final RenderList renderList = currentRenderList().get();
	startTimeMillis = System.currentTimeMillis();//TODO: Remove
	renderList.render(gl);
	if(counter%50==0)System.out.println("Passed for RenderList.render() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
	startTimeMillis = System.currentTimeMillis();//TODO: Remove
	gpu.memoryManager.get().map();
	if(counter%50==0)System.out.println("Passed for map() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
	startTimeMillis = System.currentTimeMillis();//TODO: Remove
	renderList.sendToGPU(gl);
	if(counter%50==0)System.out.println("Passed for sendToGPU() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
	fpsTracking();
	// Update GPU
	startTimeMillis = System.currentTimeMillis();//TODO: Remove
	setFogColor(gpu.getTr().getWorld().getFogColor());
	if(counter%50==0)System.out.println("Passed for setFogColor() "+(System.currentTimeMillis()-startTimeMillis));//TODO: Remove
    }//end render()

    public void activate() {
	active = true;
    }// TODO: Remove this when paged conversion is complete.
    
    public void temporarilyMakeImmediatelyVisible(final PositionedRenderable pr){
	gpu.getTr().getThreadManager().submitToGL(new Callable<Void>(){
	    @Override
	    public Void call() throws Exception {
		Renderer.this.currentRenderList().get().getSubmitter().submit(pr);
		return null;
	    }
	});
    }//end temporarilyMakeImmediatelyVisible(...)
    
    public void updateVisibilityList() {
	if(visibilityUpdateFuture!=null){if(!visibilityUpdateFuture.isDone())return;}
	if(!getBackRenderList().isDone())return;//Not ready.
	final RenderList rl = getBackRenderList().get();
	visibilityUpdateFuture = gpu.getTr().getThreadManager().threadPool.submit(new Runnable(){
	    @Override
	    public void run() {
		try{
		rl.reset();
		proximitySorter.setCenter(camera.getCameraPosition().toArray());
		rootGrid.cubesWithinRadiusOf(
			camera.getCameraPosition().add(
				camera.getLookAtVector().scalarMultiply(
					getCamera().getViewDepth() / 2.1)),
					proximitySorter
			);
		Renderer.this.gpu.getTr().getThreadManager().submitToGL(new Callable<Object>(){
		    @Override
		    public Object call() {
			proximitySorter.dumpPositionedRenderables(rl.getSubmitter());
			return null;
		    }//end gl call()
		}).get();
		proximitySorter.reset();
		toggleRenderList();
		}catch(Exception e){e.printStackTrace();}
	    }//end pool run()
	});
    }// end updateVisibilityList()
    
    public synchronized TRFutureTask<RenderList> currentRenderList(){
	return renderList[renderListToggle ? 0 : 1];
    }
    public synchronized TRFutureTask<RenderList> getBackRenderList(){
	return renderList[renderListToggle ? 1 : 0];
    }
    private synchronized void toggleRenderList(){
	renderListToggle = !renderListToggle;
    }

    public void setFogColor(Color c) {
	deferredProgram.use();
	fogColor.set((float) c.getRed() / 255f, (float) c.getGreen() / 255f,
		(float) c.getBlue() / 255f);
	primaryProgram.use();
    }
    
    public void setSunVector(Vector3D sv){
	deferredProgram.use();
	sunVector.set((float)sv.getX(),(float)sv.getY(),(float)sv.getZ());
	primaryProgram.use();
    }

    /**
     * @return the camera
     */
    public Camera getCamera() {
	return camera;
    }

    /**
     * @return the rootGrid
     */
    public RenderableSpacePartitioningGrid getRootGrid() {
	return rootGrid;
    }

    /**
     * @param rootGrid
     *            the rootGrid to set
     */
    public void setRootGrid(RenderableSpacePartitioningGrid rootGrid) {
	this.rootGrid = rootGrid;
    }

    /**
     * @return the primaryProgram
     */
    GLProgram getPrimaryProgram() {
        return primaryProgram;
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
}//end Renderer
