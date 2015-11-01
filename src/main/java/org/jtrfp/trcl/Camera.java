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
package org.jtrfp.trcl;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.jtrfp.trcl.beh.FacingObject;
import org.jtrfp.trcl.beh.MatchDirection;
import org.jtrfp.trcl.beh.MatchPosition;
import org.jtrfp.trcl.beh.RotateAroundObject;
import org.jtrfp.trcl.beh.SkyCubeCloudModeUpdateBehavior;
import org.jtrfp.trcl.coll.CachedAdapter;
import org.jtrfp.trcl.coll.CollectionActionDispatcher;
import org.jtrfp.trcl.coll.CollectionActionUnpacker;
import org.jtrfp.trcl.coll.PredicatedORCollectionActionFilter;
import org.jtrfp.trcl.coll.ThreadEnforcementCollection;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.obj.Positionable;
import org.jtrfp.trcl.obj.RelevantEverywhere;
import org.jtrfp.trcl.obj.WorldObject;

import com.ochafik.util.CollectionAdapter;
import com.ochafik.util.listenable.Pair;

public class Camera extends WorldObject implements RelevantEverywhere{
    	//// PROPERTIES
    	public static final String FOG_ENABLED        = "fogEnabled";
    	public static final String CENTER_CUBE        = "centerCube";
    	public static final String ROOT_GRID          = "rootGrid";
    
	private volatile  RealMatrix completeMatrix;
	private volatile  double viewDepth;
	private volatile  RealMatrix projectionMatrix;
	private final	  GPU gpu;
	private volatile  int updateDebugStateCounter;
	private 	  RealMatrix rotationMatrix;
	private boolean	  fogEnabled = true;
	private CachedAdapter<Pair<Vector3D,CollectionActionDispatcher<Positionable>>,CollectionActionDispatcher<Positionable>> strippingAdapter = 
		new CachedAdapter<Pair<Vector3D,CollectionActionDispatcher<Positionable>>,CollectionActionDispatcher<Positionable>>(){
		    @Override
		    protected CollectionActionDispatcher<Positionable> _adapt(
			    Pair<Vector3D, CollectionActionDispatcher<Positionable>> value)
			    throws UnsupportedOperationException {
			return value.getValue();
		    }

		    @Override
		    protected Pair<Vector3D, CollectionActionDispatcher<Positionable>> _reAdapt(
			    CollectionActionDispatcher<Positionable> value)
			    throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		    }};
	private final CollectionActionDispatcher<CollectionActionDispatcher<Positionable>> relevanceCollections =
		new CollectionActionDispatcher<CollectionActionDispatcher<Positionable>>(new HashSet<CollectionActionDispatcher<Positionable>>());
	private final CollectionActionDispatcher<Pair<Vector3D,CollectionActionDispatcher<Positionable>>> relevancePairs = 
		new CollectionActionDispatcher<Pair<Vector3D,CollectionActionDispatcher<Positionable>>>(new HashSet<Pair<Vector3D,CollectionActionDispatcher<Positionable>>>());
	private final PredicatedORCollectionActionFilter<Pair<Vector3D,CollectionActionDispatcher<Positionable>>> 
	 visibilityFilter = new PredicatedORCollectionActionFilter<Pair<Vector3D,CollectionActionDispatcher<Positionable>>>(relevancePairs);
	private final CollectionAdapter<CollectionActionDispatcher<Positionable>,Pair<Vector3D,CollectionActionDispatcher<Positionable>>> pairStripper = 
		new CollectionAdapter<CollectionActionDispatcher<Positionable>,Pair<Vector3D,CollectionActionDispatcher<Positionable>>>(relevanceCollections, strippingAdapter.inverse());
	private final CollectionActionDispatcher<Positionable> flatRelevanceCollection = new CollectionActionDispatcher<Positionable>(new HashSet<Positionable>());
	private static double relevanceRadius = TR.visibilityDiameterInMapSquares*TR.mapSquareSize;
	private static final double RELEVANCE_RADIUS_CUBES = relevanceRadius/World.CUBE_GRANULARITY;
	private static final double RELEVANCE_RADIUS_TAXICAB_CUBES = 1.414 * RELEVANCE_RADIUS_CUBES; // 1.414 is radical-2
	private SpacePartitioningGrid rootGrid;
	private volatile Vector3D centerCube = Vector3D.NEGATIVE_INFINITY;
	// HARD REFERENCES - DO NOT REMOVE
	private final CenterCubeHandler     centerCubeHandler;
	private final CameraPositionHandler cameraPositionHandler;

    Camera(TR tr) {
	super(tr);
	this.gpu = tr!=null?tr.gpu.get():null;
	try{final Thread rt = World.relevanceThread.get();
	    World.relevanceExecutor.submit(new Runnable(){
	    @Override
	    public void run() {
		visibilityFilter.add(new VisibilityPredicate());
		relevancePairs.addTarget(pairStripper, true);
		relevanceCollections.addTarget(
			new ThreadEnforcementCollection(new CollectionActionUnpacker<Positionable>(flatRelevanceCollection),rt), true);
	    }}).get();}catch(Exception e){throw new RuntimeException(e);}
	
	addBehavior(new MatchPosition().setEnable(true));
	addBehavior(new MatchDirection()).setEnable(true);
	addBehavior(new FacingObject().setEnable(false));
	addBehavior(new RotateAroundObject().setEnable(false));
	addBehavior(new SkyCubeCloudModeUpdateBehavior());
	
	addPropertyChangeListener(CENTER_CUBE, centerCubeHandler = new CenterCubeHandler());
	addPropertyChangeListener(WorldObject.POSITION,cameraPositionHandler = new CameraPositionHandler());
    }//end constructor
    
    private final class VisibilityPredicate implements Predicate<Pair<Vector3D,CollectionActionDispatcher<Positionable>>>{
	@Override
	public boolean evaluate(
		Pair<Vector3D, CollectionActionDispatcher<Positionable>> object) {
	    final Vector3D cubePosition = object.getKey();
	    if(cubePosition.equals(World.VISIBLE_EVERYWHERE))
		return true;
	    //Rollover taxicab distance
	    final double rolloverDistance = Math.sqrt(
		    Math.pow(cubeRolloverDistance(cubePosition.getX()-centerCube.getX()),2)+
		    Math.pow(cubeRolloverDistance(cubePosition.getZ()-centerCube.getZ()),2));
	    return rolloverDistance < RELEVANCE_RADIUS_CUBES;
	}//end evaluate()
    }//end VisibilityPredicate
    
    public static double cubeRolloverDistance(double distance){
	distance = Math.abs(distance);
	final double rolloverPoint = World.WORLD_WIDTH_CUBES/2.;
	if(distance>rolloverPoint)
	    distance = World.WORLD_WIDTH_CUBES - distance;
	return distance;
    }//end cubeRolloverDistance(...)
    
    public void addGrid(final SpacePartitioningGrid<?> toAdd){
	if(toAdd==null) throw new NullPointerException("toAdd intolerably null.");
	try{World.relevanceExecutor.submit(new Runnable(){
	    @Override
	    public void run() {
		toAdd.getPackedObjectsDispatcher().addTarget(visibilityFilter.input, true);
	    }}).get();}catch(Exception e){throw new RuntimeException(e);}
    }
    public void removeGrid(final SpacePartitioningGrid<?> toRemove){
	if(toRemove==null) throw new NullPointerException("toRemove intolerably null.");
	try{World.relevanceExecutor.submit(new Runnable(){
	    @Override
	    public void run() {
		toRemove.getPackedObjectsDispatcher().removeTarget(visibilityFilter.input, true);
	    }}).get();}catch(Exception e){throw new RuntimeException(e);}
    }
    
    private final class CameraPositionHandler implements PropertyChangeListener{
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	    final String propertyName = evt.getPropertyName();
	    if(propertyName==WorldObject.POSITION){
		final double [] newValue = ((double [])evt.getNewValue());
		final int granularity = World.CUBE_GRANULARITY;
		final Vector3D newCenterCube = new Vector3D(
			Math.rint(newValue[0]/granularity),
			Math.rint(newValue[1]/granularity),
			Math.rint(newValue[2]/granularity));
		final Vector3D oldCenterCube = centerCube;
		pcs.firePropertyChange(CENTER_CUBE, oldCenterCube, newCenterCube);
	    }//end if(POSITION)
	}//end if propertyChange()
    }//end CameraPositionHandler
    
    private final class CenterCubeHandler implements PropertyChangeListener{
	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
	    // ATOMIC
	    World.relevanceExecutor.submit(new Runnable(){
		@Override
		public void run() {
		    centerCube=(Vector3D)evt.getNewValue();
		    visibilityFilter.reEvaluatePredicates();
		}});
	}//end propertyChange(...)
    }//end CenterCubeHandler

	private void updateProjectionMatrix(){
	    	final Component component = getTr().getRootWindow();
		final float fov = 70f;// In degrees
		final float aspect = (float) component.getWidth()
				/ (float) component.getHeight();
		final float zF = (float) (viewDepth * 1.5);
		final float zN = (float) (TR.mapSquareSize / 10);
		final float f = (float) (1. / Math.tan(fov * Math.PI / 360.));
		projectionMatrix = new Array2DRowRealMatrix(new double[][]
			{ new double[]
				{ f / aspect, 0, 0, 0 }, new double[]
				{ 0, f, 0, 0 }, new double[]
				{ 0, 0, (zF + zN) / (zN - zF), -1f }, new double[]
				{ 0, 0, (2f * zF * zN) / (zN - zF), 0 } }).transpose();
		}
	/**
	 * @return the lookAtVector
	 */
	public Vector3D getLookAtVector()
		{return getLookAt();}
	/**
	 * @param lookAtVector the lookAtVector to set
	 */
	public synchronized void setLookAtVector(Vector3D lookAtVector){
	    	double [] heading = super.getHeadingArray();
		heading[0] = lookAtVector.getX();
		heading[1] = lookAtVector.getY();
		heading[2] = lookAtVector.getZ();
		//cameraMatrix=null;
		}
	/**
	 * @return the upVector
	 */
	public Vector3D getUpVector()
		{return super.getTop();}
	/**
	 * @param upVector the upVector to set
	 */
	public synchronized void setUpVector(Vector3D upVector){
		super.setTop(upVector);
		//cameraMatrix=null;
		}
	/**
	 * @return the cameraPosition
	 */
	public Vector3D getCameraPosition()
		{return new Vector3D(super.getPosition());}

    /**
     * @param cameraPosition
     *            the cameraPosition to set
     */
    public void setPosition(Vector3D cameraPosition) {
	this.setPosition(cameraPosition.getX(), cameraPosition.getY(),
		cameraPosition.getZ());
    }

    @Override
    public synchronized void setPosition(double x, double y, double z) {
	super.setPosition(x, y, z);
	//cameraMatrix = null;
    }
	
	private RealMatrix applyMatrix(){
	        try{
		 Vector3D eyeLoc = getCameraPosition();
		 Vector3D aZ = getLookAtVector().negate();
		 Vector3D aX = getUpVector().crossProduct(aZ).normalize();
		 Vector3D aY = getUpVector();

		 rotationMatrix = new Array2DRowRealMatrix(new double[][]
			{ new double[]
				{ aX.getX(), aX.getY(), aX.getZ(), 0 }, new double[]
				{ aY.getX(), aY.getY(), aY.getZ(), 0 }, new double[]
				{ aZ.getX(), aZ.getY(), aZ.getZ(), 0 }, new double[]
				{ 0, 0, 0, 1 } });

		 RealMatrix tM = new Array2DRowRealMatrix(new double[][]
			{ new double[]
				{ 1, 0, 0, -eyeLoc.getX() }, new double[]
				{ 0, 1, 0, -eyeLoc.getY() }, new double[]
				{ 0, 0, 1, -eyeLoc.getZ() }, new double[]
				{ 0, 0, 0, 1 } });
		
		 return completeMatrix = getProjectionMatrix().multiply(rotationMatrix.multiply(tM));
	         }catch(MathArithmeticException e){}//Don't crash.
	        return completeMatrix;
		}//end applyMatrix()
	public synchronized void setViewDepth(double cameraViewDepth){
	    	this.viewDepth=cameraViewDepth;
		projectionMatrix=null;
		}
	
	private RealMatrix getProjectionMatrix(){
		if(projectionMatrix==null)updateProjectionMatrix();
		return projectionMatrix;
		}
	
	public double getViewDepth()
		{return viewDepth;}
	
	private synchronized RealMatrix getCompleteMatrix()
		{//if(cameraMatrix==null){
		    applyMatrix();
		    if(updateDebugStateCounter++ % 30 ==0){
			    getTr().getReporter().report("org.jtrfp.trcl.core.Camera.position", getPosition()[0]+" "+getPosition()[1]+" "+getPosition()[2]+" ");
			    getTr().getReporter().report("org.jtrfp.trcl.core.Camera.lookAt", getLookAt().toString());
			    getTr().getReporter().report("org.jtrfp.trcl.core.Camera.up", getTop().toString());
			}//}
		return completeMatrix;
		}
	public synchronized float [] getRotationMatrixAsFlatArray(float [] dest){
	    applyMatrix();
	    RealMatrix rm = rotationMatrix;
	    for(int i=0; i<16; i++){
		dest[i]=(float)rm.getEntry(i/4, i%4);
	    }//end for(16)
	    return dest;
	}// end getRotationMatrixAsFlatArray(...)
	
	public float [] getProjectionRotationMatrixAsFlatArray(){
	    applyMatrix();//getProjectionMatrix() doesn't implicitly apply matrix since it would cause a recursion loop
	    final float [] result = new float[16];
	    final RealMatrix mat = getProjectionMatrix().multiply(rotationMatrix);
	    for(int i=0; i<16; i++){
		result[i]=(float)mat.getEntry(i/4, i%4);
	    }//end for(16)
	    return result;
	}
	
	public float [] getCompleteMatrixAsFlatArray(){
	    final float [] result = new float[16];
	    final RealMatrix mat = getCompleteMatrix();
	    for(int i=0; i<16; i++){
		result[i]=(float)mat.getEntry(i/4, i%4);
	    }//end for(16)
	    return result;
	}

	/**
	 * @return the fogEnabled
	 */
	public boolean isFogEnabled() {
	    return fogEnabled;
	}

	/**
	 * @param fogEnabled the fogEnabled to set
	 */
	public Camera setFogEnabled(boolean fogEnabled) {
	    pcs.firePropertyChange(FOG_ENABLED, this.fogEnabled, fogEnabled);
	    this.fogEnabled = fogEnabled;
	    return this;
	}
	/*
	public ListenableCollection<ListenableCollection<PositionedRenderable>> getRelevantCubeCollection(){
	    return relevantCubeCollection;
	}*/

	/**
	 * @return the relevanceRadius
	 */
	public double getRelevanceRadius() {
	    return relevanceRadius;
	}

	/**
	 * @param relevanceRadius the relevanceRadius to set
	 */
	public void setRelevanceRadius(double relevanceRadius) {
	    this.relevanceRadius = relevanceRadius;
	}

	/**
	 * @return the rootGrid
	 */
	public SpacePartitioningGrid getRootGrid() {
	    return rootGrid;
	}

	/**
	 * @param rootGrid the rootGrid to set
	 */
	public void setRootGrid(SpacePartitioningGrid rootGrid) {
	    if(this.rootGrid==rootGrid)
		return;
	    pcs.firePropertyChange(ROOT_GRID, this.rootGrid, rootGrid);
	    if(this.rootGrid!=null)
	     removeGrid(this.rootGrid);
	    if(rootGrid!=null)
	     addGrid(rootGrid);
	    this.rootGrid = rootGrid;
	}
	/**
	 * @return the relevancePairs
	 */
	public CollectionActionDispatcher<Pair<Vector3D, CollectionActionDispatcher<Positionable>>> getRelevancePairs() {
	    return relevancePairs;
	}
	/**
	 * @return the relevanceCollections
	 */
	public CollectionActionDispatcher<CollectionActionDispatcher<Positionable>> getRelevanceCollections() {
	    return relevanceCollections;
	}
	/**
	 * @return the flatRelevanceCollection
	 */
	public CollectionActionDispatcher<Positionable> getFlatRelevanceCollection() {
	    return flatRelevanceCollection;
	}
}//end Camera
