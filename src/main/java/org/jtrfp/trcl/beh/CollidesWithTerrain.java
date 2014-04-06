package org.jtrfp.trcl.beh;

import java.util.Collection;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.InterpolatingAltitudeMap;
import org.jtrfp.trcl.Submitter;
import org.jtrfp.trcl.TerrainChunk;
import org.jtrfp.trcl.World;
import org.jtrfp.trcl.beh.phy.RotationalMomentumBehavior;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.obj.Player;
import org.jtrfp.trcl.obj.Velocible;
import org.jtrfp.trcl.obj.WorldObject;

public class CollidesWithTerrain extends Behavior {
    private static final double nudge=1;
    private boolean bounce=false;
    private boolean groundLock=false;
    private Vector3D surfaceNormalVar;
    public static final double CEILING_Y_NUDGE=-5000;
    private int tickCounter=0;
    private boolean autoNudge=false;
    private double nudgePadding=5000;
    @Override
    public void _tick(long tickTimeMillis){
	if(tickCounter++ % 2==0)return;
	final WorldObject p = getParent();
	final TR tr = p.getTr();
	final World world = tr.getWorld();
	final double [] thisPos=p.getPosition();
	final double groundHeightNorm =p.getTr().getAltitudeMap().heightAt((thisPos[0]/TR.mapSquareSize), 
		    (thisPos[2]/TR.mapSquareSize));
	final double groundHeight = groundHeightNorm*(world.sizeY/2);
	final double ceilingHeight = (1.99-p.getTr().getAltitudeMap().heightAt((thisPos[0]/TR.mapSquareSize), 
		    (thisPos[2]/TR.mapSquareSize)))*(world.sizeY/2)+CEILING_Y_NUDGE;
	final Vector3D groundNormal = (p.getTr().getAltitudeMap().normalAt((thisPos[0]/TR.mapSquareSize), 
	    (thisPos[2]/TR.mapSquareSize)));
	Vector3D downhillDirectionXZ=new Vector3D(groundNormal.getX(),0,groundNormal.getZ());
	if(downhillDirectionXZ.getNorm()!=0)downhillDirectionXZ=downhillDirectionXZ.normalize();
	else downhillDirectionXZ=Vector3D.PLUS_J;
	final boolean terrainMirror=tr.getOverworldSystem().isChamberMode();
	final double thisY=thisPos[1];
    	boolean groundImpact=thisY<(groundHeight+(autoNudge?nudgePadding:0));
    	final boolean ceilingImpact=(thisY>ceilingHeight&&terrainMirror);
	final Vector3D ceilingNormal = new Vector3D(groundNormal.getX(),-groundNormal.getY(),groundNormal.getZ());
	Vector3D surfaceNormal = groundImpact?groundNormal:ceilingNormal;
	if(terrainMirror && groundHeightNorm>.97){groundImpact=true; surfaceNormal=downhillDirectionXZ;}
	
    	if(groundLock){
    	    thisPos[1]=groundHeight;p.notifyPositionChange();return;
    	    }
    	
	if( groundImpact || ceilingImpact){//detect collision
	    double padding = autoNudge?nudgePadding:0;
	    padding *= groundImpact?1:-1;
	    thisPos[1]=(groundImpact?groundHeight:ceilingHeight)+padding;
	    p.notifyPositionChange();
	    //Call impact listeners
	    surfaceNormalVar=surfaceNormal;
	    final Behavior behavior = p.getBehavior();
	    behavior.probeForBehaviors(sub,SurfaceImpactListener.class);
	    
	    //if(p instanceof Player)System.out.println("Impact ceiling="+ceilingImpact+" ground="+groundImpact);
	    
	    //Reflect heading,top
	    if(bounce){
	    	final Vector3D oldHeading = p.getHeading();
	    	final Vector3D oldTop = p.getTop();
	    	final Vector3D newHeading = (surfaceNormal.scalarMultiply(surfaceNormal.dotProduct(oldHeading)*-2).add(oldHeading));
	    	final Velocible v=behavior.probeForBehavior(Velocible.class);
	    	final RotationalMomentumBehavior rmb = behavior.probeForBehavior(RotationalMomentumBehavior.class);
	    	if(rmb!=null){//If this is a spinning object, reverse its spin momentum
	    	    rmb.setLateralMomentum(rmb.getLateralMomentum()*-1);
	    	    rmb.setEquatorialMomentum(rmb.getEquatorialMomentum()*-1);
	    	    rmb.setPolarMomentum(rmb.getPolarMomentum()*-1);
	    	    }
	    	final Vector3D oldVelocity = v.getVelocity();
	    	v.setVelocity(surfaceNormal.scalarMultiply(surfaceNormal.dotProduct(oldVelocity)*-2).add(oldVelocity));
	    	p.setHeading(newHeading);
	    	final Rotation resultingRotation = new Rotation(oldHeading,newHeading);
	    	Vector3D newTop = resultingRotation.applyTo(oldTop);
		p.setTop(newTop);
	    	}//end if(bounce)
	    }//end if(collision)
    }//end _tick
    private final Submitter<SurfaceImpactListener>sub=new Submitter<SurfaceImpactListener>(){
	@Override
	public void submit(SurfaceImpactListener item) {
	    	item.collidedWithSurface(null,surfaceNormalVar.toArray());//TODO: Isolate which chunk and pass it
		}
	@Override
	public void submit(Collection<SurfaceImpactListener> items) {
	    	for(SurfaceImpactListener l:items){submit(l);}
		}
    };
    /**
     * @return the autoNudge
     */
    public boolean isAutoNudge() {
        return autoNudge;
    }
    /**
     * @param autoNudge the autoNudge to set
     */
    public CollidesWithTerrain setAutoNudge(boolean autoNudge) {
        this.autoNudge = autoNudge;
        return  this;
    }
    /**
     * @return the nudgePadding
     */
    public double getNudgePadding() {
        return nudgePadding;
    }
    /**
     * @param nudgePadding the nudgePadding to set
     */
    public CollidesWithTerrain setNudgePadding(double nudgePadding) {
        this.nudgePadding = nudgePadding;
        return this;
    }
}//end BouncesOffTerrain
