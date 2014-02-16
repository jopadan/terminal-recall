package org.jtrfp.trcl.beh;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.math.Vect3D;
import org.jtrfp.trcl.obj.Player;
import org.jtrfp.trcl.obj.Velocible;
import org.jtrfp.trcl.obj.WorldObject;

public class AutoFiring extends Behavior implements AIFiringBehavior {
    private double maxFiringDistance=TR.mapSquareSize*10;
    private double minFiringDistance=TR.mapSquareSize*0;
    private int lastIndexVisited=0;
    private boolean smartFiring=false;
    private boolean [] firingPattern = new boolean []
	    {true,true,false,false,false,true,false,false,true,false,false,false,false};
    private int timePerPatternEntry=400;
    private int totalFiringPatternTimeMillis=firingPattern.length*timePerPatternEntry;
    private ProjectileFiringBehavior projectileFiringBehavior;
    private final double [] firingVector = new double[3];
    private final double [] headingDelta = new double[3];
    private int patternOffsetMillis=0;
    private double maxFireVectorDeviation=1.1;
    private boolean berzerk=false;
    @Override
    public void _tick(long timeMillis){
	//System.out.println("AutoFiring: Tick");
	final WorldObject thisObject = getParent();
	final double [] thisPos = thisObject.getPosition();
	final Player player = thisObject.getTr().getPlayer();
	final double [] playerPos = player.getPosition();
	final double dist = Vect3D.distance(thisPos, playerPos);
	if(dist<maxFiringDistance||dist>minFiringDistance){
	    //System.out.println("AutoFiring: Within distance.");
	    final int patIndex=(int)(((timeMillis+patternOffsetMillis)%totalFiringPatternTimeMillis)/timePerPatternEntry);
	    if(patIndex!=lastIndexVisited){//end if(lastVisited)
		//System.out.println("AutoFiring: New index in firing pattern.");
		if(firingPattern[patIndex]){
		    //System.out.println("AutoFiring: Firing pattern says yes.");
		    Vector3D result;
		    if(smartFiring){
			final Vector3D playerVelocity = player.getBehavior().probeForBehavior(Velocible.class).getVelocity();
			final double projectileSpeed = projectileFiringBehavior.getProjectileFactory().getWeapon().getSpeed()/TR.crossPlatformScalar; 
			Vector3D virtualPlayerPos = interceptOf(new Vector3D(playerPos),playerVelocity,new Vector3D(thisPos),projectileSpeed);
			if(virtualPlayerPos==null)virtualPlayerPos=new Vector3D(playerPos);
			Vect3D.subtract(virtualPlayerPos.toArray(), thisPos, firingVector);}
		    else{Vect3D.subtract(playerPos, thisPos, firingVector); }
		    result = new Vector3D(Vect3D.normalize(firingVector,firingVector));
		    final double [] objectHeading = thisObject.getHeadingArray();
		    Vect3D.subtract(objectHeading, result.toArray(), headingDelta);
		    if(Vect3D.norm(headingDelta)>maxFireVectorDeviation)return;
		    if(berzerk)result=new Vector3D(Math.random(),Math.random(),Math.random()).normalize();
		    projectileFiringBehavior.requestFire(result);}}
	    lastIndexVisited=patIndex;
	}//end in range
    }//end _tick(...)
    
    /**
     * Adapted from <a href='http://jaran.de/goodbits/2011/07/17/calculating-an-intercept-course-to-a-target-with-constant-direction-and-velocity-in-a-2-dimensional-plane/'>this article.</a>
     * @param targetPos
     * @param targetVel
     * @param attackPos
     * @param attackSpeed
     * @return
     * @since Feb 13, 2014
     */
    private static Vector3D interceptOf(final Vector3D targetPos, final Vector3D targetVel, final Vector3D attackPos, final double attackSpeed) {
	final double dX = targetPos.getX()-attackPos.getX();
	final double dY = targetPos.getY()-attackPos.getY();
	final double dZ = targetPos.getZ()-attackPos.getZ();

	final double h1 = targetVel.getX()*targetVel.getX() + targetVel.getY()*targetVel.getY() + targetVel.getZ() * targetVel.getZ() - attackSpeed * attackSpeed;
	final double h2 = dX*targetVel.getX() + dY*targetVel.getY() + dZ*targetVel.getZ();
	double t;
	if (h1 == 0)t = -(dX*dX + dY*dY + dZ*dZ) / 2*h2;
	else {  final double minusPHalf = -h2/h1;
		final double disc = minusPHalf*minusPHalf-(dX*dX+dY*dY+dZ*dZ)/h1;
		if (disc<0)return null;

		final double root = Math.sqrt(disc);
		final double t1 = minusPHalf + root;
		final double t2 = minusPHalf - root;
		final double tMin = Math.min(t1, t2);
		final double tMax = Math.max(t1, t2);
		t = tMin>0?tMin:tMax;
		
		if (t<0) return null;
	}//end else(calculate full)
	return new Vector3D(targetPos.getX() + t*targetVel.getX(), targetPos.getY()+t*targetVel.getY(), targetPos.getZ() + t*targetVel.getZ());
}//and interceptOf
    
    /**
     * @return the maxFiringDistance
     */
    public double getMaxFiringDistance() {
        return maxFiringDistance;
    }
    /**
     * @param maxFiringDistance the maxFiringDistance to set
     */
    public AutoFiring setMaxFiringDistance(double maxFiringDistance) {
        this.maxFiringDistance = maxFiringDistance;
        return this;
    }
    /**
     * @return the minFiringDistance
     */
    public double getMinFiringDistance() {
        return minFiringDistance;
    }
    /**
     * @param minFiringDistance the minFiringDistance to set
     */
    public AutoFiring setMinFiringDistance(double minFiringDistance) {
        this.minFiringDistance = minFiringDistance;
        return this;
    }
    /**
     * @return the firingPattern
     */
    public boolean[] getFiringPattern() {
        return firingPattern;
    }
    /**
     * @param firingPattern the firingPattern to set
     */
    public AutoFiring setFiringPattern(boolean[] firingPattern) {
        this.firingPattern = firingPattern;
        totalFiringPatternTimeMillis=firingPattern.length*timePerPatternEntry;
        return this;
    }
    /**
     * @return the timePerPatternEntry
     */
    public int getTimePerPatternEntry() {
        return timePerPatternEntry;
    }
    /**
     * @param timePerPatternEntry the timePerPatternEntry to set
     */
    public AutoFiring setTimePerPatternEntry(int timePerPatternEntry) {
        this.timePerPatternEntry = timePerPatternEntry;
        totalFiringPatternTimeMillis=firingPattern.length*timePerPatternEntry;
        return this;
    }
    /**
     * @return the totalFiringPatternTimeMillis
     */
    public int getTotalFiringPatternTimeMillis() {
        return totalFiringPatternTimeMillis;
    }
    
    /**
     * @return the projectileFiringBehavior
     */
    public ProjectileFiringBehavior getProjectileFiringBehavior() {
        return projectileFiringBehavior;
    }
    /**
     * @param projectileFiringBehavior the projectileFiringBehavior to set
     */
    public AutoFiring setProjectileFiringBehavior(
    	ProjectileFiringBehavior projectileFiringBehavior) {
        this.projectileFiringBehavior = projectileFiringBehavior;
        return this;
    }
    /**
     * @return the patternOffsetMillis
     */
    public int getPatternOffsetMillis() {
        return patternOffsetMillis;
    }
    /**
     * @param patternOffsetMillis the patternOffsetMillis to set
     */
    public AutoFiring setPatternOffsetMillis(int patternOffsetMillis) {
        this.patternOffsetMillis = patternOffsetMillis;
        return this;
    }
    /**
     * @return the smartFiring
     */
    public boolean isSmartFiring() {
        return smartFiring;
    }
    /**
     * Compensates for distance when calculating a firing vector. Use with care, it's quite brutal.
     * @param smartFiring the smartFiring to set
     */
    public AutoFiring setSmartFiring(boolean smartFiring) {
        this.smartFiring = smartFiring;
        return this;
    }

    /**
     * @return the maxFireVectorDeviation
     */
    public double getMaxFireVectorDeviation() {
        return maxFireVectorDeviation;
    }

    /**
     * @param maxFireVectorDeviation the maxFireVectorDeviation to set
     */
    public AutoFiring setMaxFireVectorDeviation(double maxFireVectorDeviation) {
        this.maxFireVectorDeviation = maxFireVectorDeviation;
        return this;
    }

    public AutoFiring setBerzerk(boolean berzerk) {
	this.berzerk=berzerk;
	return this;
    }
}//end AutoFiring
