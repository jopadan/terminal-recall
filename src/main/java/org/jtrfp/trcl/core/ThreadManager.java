/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2022 Chuck Ritola
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jtrfp.trcl.AbstractSubmitter;
import org.jtrfp.trcl.Submitter;
import org.jtrfp.trcl.core.TRFactory.TR;
import org.jtrfp.trcl.gpu.ProvidesGLThread;
import org.jtrfp.trcl.gpu.Renderer;
import org.jtrfp.trcl.obj.Player;
import org.jtrfp.trcl.obj.PositionedRenderable;
import org.jtrfp.trcl.obj.RelevantEverywhere;
import org.jtrfp.trcl.obj.WorldObject;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.FPSAnimator;

public class ThreadManager {
    public static final int RENDER_FPS 			= 60;
    public static final int GAMEPLAY_FPS 		= 60;
    public static final int RENDERLIST_REFRESH_FPS 	= 1;
    private       TR 			tr;
    private final Timer 		lightweightTimer 	= new Timer("LightweightTimer");
    private final Timer 		gameplayTimer 		= new Timer("GameplayTimer");
    private long 			lastGameplayTickTime 		= 0;
    private long 			timeInMillisSinceLastGameTick 	= 0L;
    //private int 			counter 			= 0;
    private Thread 			renderingThread;
    private FPSAnimator			animator;
    //private GPUFeature                  gpuFeature;
    private boolean[] 			paused = new boolean[]{false};
    public final ThreadPoolExecutor	threadPool 			= 
	    new ThreadPoolExecutor(
		    20,
		    20,
		    1,
		    TimeUnit.DAYS,new ArrayBlockingQueue<Runnable>(30000,true));
    public final ThreadPoolExecutor	gpuMemAccessThreadPool 		= 
	    new ThreadPoolExecutor(
		    30,
		    30,
		    1,
		    TimeUnit.DAYS,new ArrayBlockingQueue<Runnable>(30000,true));
    
    public final Object			   gameStateLock	   = new Object();
    public final Queue<TRFutureTask<?>>    pendingGPUMemAccessTasks= new ArrayBlockingQueue<TRFutureTask<?>>(30000,true);
    public final Queue<TRFutureTask<?>>    activeGPUMemAccessTasks = new ArrayBlockingQueue<TRFutureTask<?>>(30000,true);
    public final ArrayList<Callable<?>>repeatingGPUMemAccessTasks  = new ArrayList<Callable<?>>();
    public final List<Callable<?>>repeatingGLTasks	           = new CopyOnWriteArrayList<Callable<?>>();
    public final List<Callable<?>>intermittentGLTasks	           = new CopyOnWriteArrayList<Callable<?>>();
    public final Deque<Callable<?>>intermittentGLTasksToExecute	   = new ArrayDeque<Callable<?>>();
    private static long maxIntermittentExecutionTimeMS		   = 20;
    private long timeElapsedInMillisSinceLastGameTickAfterLastPause = 0L;
    
    private final Submitter<TRFutureTask<?>> pendingGPUMemAccessTaskSubmitter = new AbstractSubmitter<TRFutureTask<?>>(){
	@Override
	public void submit(TRFutureTask<?> item) {
		pendingGPUMemAccessTasks.add(item);
	}//end submit(...)
    };
    private final Submitter<TRFutureTask<?>> activeGPUMemAccessTaskSubmitter = new AbstractSubmitter<TRFutureTask<?>>(){
	@Override
	public void submit(TRFutureTask<?> item) {
		activeGPUMemAccessTasks.add(item);
		gpuMemAccessThreadPool.submit(item);
	}//end submit(...)
    };
    private AtomicReference<Submitter<TRFutureTask<?>>>	currentGPUMemAccessTaskSubmitter 
    	= new AtomicReference<Submitter<TRFutureTask<?>>>(activeGPUMemAccessTaskSubmitter);
    private final long startupTimeMillis = System.currentTimeMillis();
    
    public ThreadManager() {
    }// end constructor
    
    public TR getTr(){
	return tr;
    }
    
    public void setTr(TR tr){
	this.tr=tr;
    }
    
    private final ArrayList<PositionedRenderable> visibilityListBuffer = new ArrayList<PositionedRenderable>();
    
    private void gameplay() {
	final long tickTimeInMillis = System.currentTimeMillis();
	if(lastGameplayTickTime != 0)
	    timeInMillisSinceLastGameTick = tickTimeInMillis - lastGameplayTickTime;
	else
	    timeInMillisSinceLastGameTick = 0;
	boolean alreadyVisitedPlayer=false;
	Renderer [] renderers = new Renderer[]{tr.mainRenderer/* ,tr.secondaryRenderer*/ };//TODO: This is hacky.
	visibilityListBuffer.clear();
	synchronized(paused){
	synchronized(gameStateLock){
	    for(Renderer renderer:renderers){
		if(renderer.isEnabled()){
		    final Collection<PositionedRenderable> vl = 
				renderer.
				getVisibleWorldObjectList();
		    synchronized(vl)
			 {visibilityListBuffer.addAll(vl);}
			    for (PositionedRenderable pr:visibilityListBuffer) {
			    boolean multiplePlayer=false;
			    final WorldObject wo = (WorldObject)pr;
			    if (wo.isActive() || wo instanceof RelevantEverywhere)
				if(wo instanceof Player){
				    if(alreadyVisitedPlayer)
					multiplePlayer=true;
				    else alreadyVisitedPlayer=true;
				}//end if(Player)
				if(!multiplePlayer&&!paused[0])
				    wo.tick(tickTimeInMillis);
			 }// end for(worldObjects)
		}//end if(renderer active)
		renderer.getCamera().tick(tickTimeInMillis);
	    }//end for(renderers)
	}//end sync(gameStateLock)//relevance changes outside of this cause errors!
	//if(game.getPlayer()!=null && !paused[0])
	    //tr.getCollisionManager().performCollisionTests();
	    tr.getCollisionManager().newPerformCollisionTests();
	}// end sync(paused)
	lastGameplayTickTime = tickTimeInMillis;
    }// end gameplay()
    /*
    public <T> GLFutureTask<T> submitToGL(Callable<T> c){
	final GLFutureTask<T> result = new GLFutureTask<T>(tr.getRootWindow().getCanvas(),c);
	if(isGLThread())
	    if(gpuFeature.getGl().getContext().isCurrent()){
		result.run();
		return result;
	    }else{
		final GLContext context = gpuFeature.getGl().getContext();
		context.makeCurrent();
		result.run();
		context.release();
	    }
	if(!result.enqueue())
	    throw new RuntimeException("Canvas.invoke() threw false - will not execute.");
	return result;
    }//end submitToGL(...)
    
    */
    
    public boolean isGLThread(){
	return Thread.currentThread()==renderingThread;
    }
    
    public <T> TRFutureTask<T> submitToThreadPool(Callable<T> c){
	return submitToThreadPool(true,c);
    }//end submitToThreadPool(...)

    public void start() {
	threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler(){
	    @Override
	    public void rejectedExecution(Runnable r, ThreadPoolExecutor exec) {
		try{Thread.sleep(150);
		 exec.execute(r);
		}//Wait 150ms
		catch(InterruptedException e){e.printStackTrace();}
	    }});
	gpuMemAccessThreadPool.setRejectedExecutionHandler(new RejectedExecutionHandler(){
	    @Override
	    public void rejectedExecution(Runnable r, ThreadPoolExecutor exec) {
		try{Thread.sleep(150);
		 exec.execute(r);
		}//Wait 150ms
		catch(InterruptedException e){e.printStackTrace();}
	    }});
	threadPool.prestartAllCoreThreads();
	gpuMemAccessThreadPool.prestartAllCoreThreads();
	
	gameplayTimer.schedule(new TimerTask(){
	    @Override
	    public void run() {
		if(tr.mainRenderer==null)
		    return;
		try{gameplay();
		}catch(Exception e){tr.showStopper(e);}
	    }}, 0, 1000/GAMEPLAY_FPS);
	animator = new FPSAnimator(tr.getRootWindow().getAutoDrawable(),RENDER_FPS);
	animator.start();
	tr.getRootWindow().addWindowListener(new WindowAdapter(){//TODO: This should be somewhere else
	    @Override
	    public void windowClosing(WindowEvent e){
		System.out.println("WindowClosing...");
		gameplayTimer.cancel();
		animator.stop();
	    }
	});
	tr.getRootWindow().getAutoDrawable().addGLEventListener(new GLEventListener() {
	    @Override
	    @ProvidesGLThread
	    public void init(final GLAutoDrawable drawable) {
		System.out.println("ThreadManager.GLEventListener.init()");
	    }//end init()

	    @Override
	    @ProvidesGLThread
	    public void dispose(GLAutoDrawable drawable) {
		System.out.println("ThreadManager.GLEventListener.dispose()");
	    }
	    
	    @Override
	    @ProvidesGLThread
	    public void display(GLAutoDrawable drawable) {
		renderingThread=Thread.currentThread();
		renderingThread.setName("ThreadManager.display()");
		attemptRender();
	    }//end display()

	    @Override
	    @ProvidesGLThread
	    public void reshape(GLAutoDrawable drawable, int x, int y,
		    int width, int height) {
	    }
	});
	lastGameplayTickTime = System.currentTimeMillis();
	//gpuFeature = Features.get(tr, GPUFeature.class);//TODO: This probably should be somewhere else
    }// end start()
    
    private void attemptRender() {
	// Swap submitters
	try {
	    synchronized (currentGPUMemAccessTaskSubmitter) {
		currentGPUMemAccessTaskSubmitter
			.set(pendingGPUMemAccessTaskSubmitter);
	    }//end sync()
	    while (!activeGPUMemAccessTasks.isEmpty())
		if (!activeGPUMemAccessTasks.peek().isDone())
		    return;// Abort. Not ready to go yet.
		else activeGPUMemAccessTasks.poll();
	     ///////// activeGPUMemAccessTasks should be empty beyond this
	    assert activeGPUMemAccessTasks.isEmpty() : "ThreadManager.activeGPUMemAccessTasks intolerably not empty.";
	    //// GL ONLY
	    synchronized(intermittentGLTasksToExecute) {
		synchronized(intermittentGLTasks) {
		    if(intermittentGLTasksToExecute.isEmpty())
			intermittentGLTasksToExecute.addAll(intermittentGLTasks);
		}//end sync(intermittentGLTasks)
		if(!intermittentGLTasksToExecute.isEmpty())
		 intermittentGLTasksToExecute.remove().call();
	    }//end sync(GL Tasks To Execute)
	    
	    //synchronized(repeatingGLTasks){
	     for(Callable<?> c:repeatingGLTasks)
		c.call();//}
	    synchronized (currentGPUMemAccessTaskSubmitter) {
		currentGPUMemAccessTaskSubmitter
			.set(activeGPUMemAccessTaskSubmitter);
	    }
	    while (!pendingGPUMemAccessTasks.isEmpty())
		activeGPUMemAccessTaskSubmitter.submit(pendingGPUMemAccessTasks
			.poll());
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }// end attemptRender()
    
    public long getElapsedTimeInMillisSinceLastGameTick() {
	return timeInMillisSinceLastGameTick;
    }
    
    /**
     * General-use Timer to be used for short, non-timing-critical operations.
     * @return
     * @since Jun 8, 2014
     */
    public Timer getLightweightTimer() {
	return lightweightTimer;
    }
    
    public static final ThreadLocal<TRFutureTask<?>> trFutureTaskIdentities = new ThreadLocal<TRFutureTask<?>>();
    public void notifyGPUMemAccessFault() {
	System.err.println("pending tasks:");
	for(TRFutureTask<?> t: pendingGPUMemAccessTasks)
	    System.err.println(t);
	System.err.println("active tasks:");
	for(TRFutureTask<?> t: activeGPUMemAccessTasks)
	    System.err.println(t);
	System.err.println("This task: "+trFutureTaskIdentities.get());
	tr.showStopper(new RuntimeException("Writing to GPU while rendering!"));
    }//end notifyGPUMemAccessFault()

    public long getMillisSinceStartup() {
	return System.currentTimeMillis()-startupTimeMillis;
    }

    public void setPaused(boolean paused) {
	synchronized(this.paused){
	    this.paused[0] = paused;
	    //Retain the tick-time-passed between pause-ings so we don't get a sudden jerk when un-pausing
	    if(paused)
		timeElapsedInMillisSinceLastGameTickAfterLastPause = this.getElapsedTimeInMillisSinceLastGameTick();
	    if(!paused)
		lastGameplayTickTime=System.currentTimeMillis() - timeElapsedInMillisSinceLastGameTickAfterLastPause;
	    }//end sync
    }//end setPaused(...)

    public <T>TRFutureTask<T> submitToThreadPool(boolean handleException,
	    Callable<T> callable) {
	final TRFutureTask<T> result = new TRFutureTask<T>(callable,handleException?tr:null);
	threadPool.submit(result);
	return result;
    }
    public void addIntermittentGLTask(Callable<?> task) {
	if(task==null)
	    throw new NullPointerException("Passed task intolerably null.");
	synchronized(intermittentGLTasks){
	 if(intermittentGLTasks.contains(task))
	    return;
	 intermittentGLTasks.add(task);}
    }//end addIntermittentGLTask(...)
    
    public void removeIntermittentGLTask(Callable<?> task) {
	if(task==null)
	    throw new NullPointerException("Passed task intolerably null.");
	synchronized(intermittentGLTasks){
	 intermittentGLTasks.remove(task);}
    }//end removeIntermittentGLTask(...)
    
    public void addRepeatingGLTask(Callable<?> task){
	if(task==null)
	    throw new NullPointerException("Passed task intolerably null.");
	synchronized(repeatingGLTasks){
	 if(repeatingGLTasks.contains(task))
	    return;
	 repeatingGLTasks.add(task);}
    }//end addRepeatingGLTask(...)
    
    public void removeRepeatingGLTask(Callable<?> task){
	if(task==null)
	    throw new NullPointerException("Passed task intolerably null.");
	repeatingGLTasks.remove(task);
    }//end removeRepeatingGLTask(...)
    
}// end ThreadManager
