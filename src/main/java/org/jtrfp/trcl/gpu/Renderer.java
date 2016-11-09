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
package org.jtrfp.trcl.gpu;

import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.opengl.GL3;

import org.apache.commons.collections4.collection.PredicatedCollection;
import org.apache.commons.collections4.functors.InstanceofPredicate;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.Camera;
import org.jtrfp.trcl.GridCubeProximitySorter;
import org.jtrfp.trcl.ObjectListWindow;
import org.jtrfp.trcl.SpacePartitioningGrid;
import org.jtrfp.trcl.World;
import org.jtrfp.trcl.coll.CollectionActionDispatcher;
import org.jtrfp.trcl.core.NotReadyException;
import org.jtrfp.trcl.core.TRFutureTask;
import org.jtrfp.trcl.core.ThreadManager;
import org.jtrfp.trcl.gui.ReporterFactory.Reporter;
import org.jtrfp.trcl.obj.Positionable;
import org.jtrfp.trcl.obj.PositionedRenderable;
import org.jtrfp.trcl.prop.SkyCube;
import org.jtrfp.trcl.tools.Util;

import com.ochafik.util.Adapter;
import com.ochafik.util.listenable.AdaptedCollection;

public final class Renderer {
    private       	RendererFactory		rendererFactory;
    //private 		RenderableSpacePartitioningGrid rootGrid;
    private final	GridCubeProximitySorter proximitySorter = new GridCubeProximitySorter();
    private		GLFrameBuffer		renderingTarget;
    private 		boolean 		initialized = false;
    private     	GPU 			gpu;
    public      	TRFutureTask<RenderList> renderList;
    private 		int			frameNumber;
    private 		long			lastTimeMillis;
    private		double			meanFPS;
                        float[]			cameraMatrixAsFlatArray	   = new float[16];
                        float	[]		camRotationProjectionMatrix= new float[16];
    private		TRFutureTask<Void>	relevanceUpdateFuture,relevanceCalcTask;
    private 		SkyCube			skyCube;
    final 		AtomicLong		nextRelevanceCalcTime = new AtomicLong(0L);
    //private final	CollisionManager        collisionManager;
    private		Camera			camera = null;
    private        	PredicatedCollection<Positionable> relevantPositioned;
    private      	Reporter		reporter;
    private     	ThreadManager		threadManager;
    private             String                  debugName;
    private boolean                             enabled = false;
    private             World                   world;
    private             ObjectListWindow        objectListWindow;
    private             Byte                    stencilID = null;
    
    private static final Adapter<Positionable,PositionedRenderable> castingAdapter = new Adapter<Positionable,PositionedRenderable>(){
	@Override
	public PositionedRenderable adapt(Positionable value)
		throws UnsupportedOperationException {
	    return (PositionedRenderable)value;
	}
	@Override
	public Positionable reAdapt(PositionedRenderable value)
		throws UnsupportedOperationException {
	    return (Positionable)value;
	}
    };//end castingAdapter
    
    public void ensureInit() {
	if (initialized)
	    return;
	Util.assertPropertiesNotNull(this, "gpu", "world", "threadManager");
	final World world = getWorld();
	final GPU gpu = getGpu();
	final ThreadManager threadManager = getThreadManager();
	Camera camera = world.newCamera();//TODO: Remove after redesign.
	camera.setDebugName(getDebugName());
	//setCamera(tr.getWorld().newCamera());//TODO: Use after redesign
	System.out.println("...Done.");
	System.out.println("Initializing RenderList...");
	renderList = new TRFutureTask<RenderList>(new Callable<RenderList>(){
	    @Override
	    public RenderList call() throws Exception {
		final RenderList rl = new RenderList(gpu, Renderer.this, getObjectListWindow(), getThreadManager());
		rl.setReporter(getReporter());
		return rl;
	    }});
	threadManager.threadPool.submit(renderList);

	if(getSkyCube() == null)
	    setSkyCube(new SkyCube(gpu));
	relevantPositioned =
		PredicatedCollection.predicatedCollection(
			new AdaptedCollection<PositionedRenderable,Positionable>(renderList.get().getVisibleWorldObjectList(),Util.bidi2Backward(castingAdapter),Util.bidi2Forward(castingAdapter)),
			new InstanceofPredicate(PositionedRenderable.class));
	setCamera(camera);
	assert camera!=null;
	gpu.memoryManager.get().map();
	initialized = true;
    }// end ensureInit()

    private void fpsTracking() {
	final Reporter reporter = getReporter();
	if(reporter == null)
	    return;
	frameNumber++;
	final boolean isKeyFrame = (frameNumber % 20) == 0;
	if (isKeyFrame) {
	    final long dT = System.currentTimeMillis() - lastTimeMillis;
		if(dT<=0)return;
		final int fps = (int)(20.*(1000. / (double)dT));
	    reporter.report("org.jtrfp.trcl.core.Renderer."+debugName+" FPS", "" + fps);
	    final Collection<PositionedRenderable> coll = renderList.get().getVisibleWorldObjectList();
	    synchronized(coll){
	    reporter.report("org.jtrfp.trcl.core.Renderer."+debugName+" numVisibleObjects", coll.size()+"");
	    SpacePartitioningGrid spg = getCamera().getRootGrid();
	    if(spg!=null)
	     reporter.report("org.jtrfp.trcl.core.Renderer."+debugName+" rootGrid", spg.toString());
	    }
	    lastTimeMillis = System.currentTimeMillis();
	}//end if(key frame)
    }//end fpsTracking()
    
    public void setCamera(Camera toUse){
	final PredicatedCollection<Positionable> relevantPositioned = getRelevantPositioned();
	if(this.camera!=null)
	    this.camera.getFlatRelevanceCollection().removeTarget(relevantPositioned, true);
	this.camera=toUse;
	toUse.getFlatRelevanceCollection().addTarget(relevantPositioned, true);
    }
    
    public final Callable<?> render = new Callable<Void>(){
	@Override
	public Void call() throws Exception {
	    final GL3 gl = gpu.getGl();
	    try{ensureInit();
	        final RenderList rl = renderList.getRealtime();
	        rl.sendToGPU(gl);
	        //Make sure memory on the GPU is up-to-date by flushing stale pages to GPU mem.
	        gpu.memoryManager.getRealtime().flushStalePages();
	        rl.render(gl);
	        // Update texture codepages
	        gpu.textureManager.getRealtime().vqCodebookManager.getRealtime().refreshStaleCodePages();
	        fpsTracking();
	    }catch(NotReadyException e){}
	    return null;
	}};
    
    public void setSunVector(Vector3D sv){
	rendererFactory.getDeferredProgram().use();
	rendererFactory.getSunVectorUniform().set((float)sv.getX(),(float)sv.getY(),(float)sv.getZ());
	gpu.defaultProgram();
    }

    /**
     * @return the rootGrid
     */
    /*
    public RenderableSpacePartitioningGrid getRootGrid() {
	return rootGrid;
    }
*/
    /**
     * @param rootGrid
     *            the rootGrid to set
     */
    /*
    public void setRootGrid(RenderableSpacePartitioningGrid rootGrid) {
	this.rootGrid = rootGrid;
	if(getCamera().getContainingGrid()!=null)
	    getCamera().getContainingGrid().remove(getCamera());
	rootGrid.add(getCamera());//TODO: Remove later
    }
    */

    /**
     * @return the cameraMatrixAsFlatArray
     */
    public float[] getCameraMatrixAsFlatArray() {
        return cameraMatrixAsFlatArray;
    }

    /**
     * @return the camRotationProjectionMatrix
     */
    public float[] getCamRotationProjectionMatrix() {
        return camRotationProjectionMatrix;
    }

    /**
     * @return the skyCube
     */
    public SkyCube getSkyCube() {
        return skyCube;
    }

    /**
     * @param skyCube the skyCube to set
     */
    public void setSkyCube(SkyCube skyCube) {
        this.skyCube = skyCube;
    }

    public Renderer setSunColor(final Color color) {
	gpu.submitToGL(new Callable<Void>(){
	    @Override
	    public Void call() throws Exception {
		rendererFactory.getDeferredProgram().use();
		rendererFactory.getDeferredProgram().getUniform("sunColor").set(color.getRed()/128f, color.getGreen()/128f, color.getBlue()/128f);
		gpu.defaultProgram();
		return null;
	    }
	}).get();
	return this;
    }

    public Renderer setAmbientLight(final Color color) {
	gpu.submitToGL(new Callable<Void>(){
	    @Override
	    public Void call() throws Exception {
		rendererFactory.getDeferredProgram().use();
		rendererFactory.getDeferredProgram().getUniform("ambientLight").set(color.getRed()/128f, color.getGreen()/128f, color.getBlue()/128f);
		gpu.defaultProgram();
		return null;
	    }
	}).get();
	return this;
    }//end setAmbientLight

    /**
     * @return the renderingTarget
     */
    public GLFrameBuffer getRenderingTarget() {
        return renderingTarget;
    }

    /**
     * @param renderingTarget the renderingTarget to set
     */
    public Renderer setRenderingTarget(GLFrameBuffer renderingTarget) {
        this.renderingTarget = renderingTarget;
        return this;
    }
    
    private final Object relevanceUpdateLock = new Object();
    

    public RendererFactory getRendererFactory() {
	return rendererFactory;
    }

    public TRFutureTask<RenderList> getRenderList() {
	return renderList;
    }
    
    public Camera getCamera() {
	return camera;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
	if(this.enabled == enabled)
	    return;
        this.enabled = enabled;
        if(isEnabled())
         threadManager.addRepeatingGLTask(render);
        else
         threadManager.removeRepeatingGLTask(render);
        getCamera().setActive(isEnabled());
    }
    
    @Override
    public String toString(){
	return "Renderer debugName="+debugName+" hash="+hashCode();
    }

    public String getDebugName() {
        return debugName;
    }

    public Reporter getReporter() {
        return reporter;
    }

    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public RendererFactory getFactory() {
        return rendererFactory;
    }

    public void setFactory(RendererFactory factory) {
        this.rendererFactory = factory;
    }

    public PredicatedCollection<Positionable> getRelevantPositioned() {
        return relevantPositioned;
    }

    public void setRelevantPositioned(
    	PredicatedCollection<Positionable> relevantPositioned) {
        this.relevantPositioned = relevantPositioned;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public void setThreadManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }

    public ObjectListWindow getObjectListWindow() {
        return objectListWindow;
    }

    public void setObjectListWindow(ObjectListWindow objectListWindow) {
        this.objectListWindow = objectListWindow;
    }

    public void setRendererFactory(RendererFactory rendererFactory) {
        this.rendererFactory = rendererFactory;
    }

    public void setDebugName(String debugName) {
        this.debugName = debugName;
    }

    public GPU getGpu() {
        return gpu;
    }

    public void setGpu(GPU gpu) {
        this.gpu = gpu;
    }

    public Byte getStencilID() {
	return stencilID;
    }

    public void setStencilID(Byte stencilID) {
        this.stencilID = stencilID;
    }
}//end Renderer
