package org.jtrfp.trcl.obj;

import java.util.Arrays;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.beh.Behavior;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.obj.Explosion.ExplosionType;

public class SpawnsRandomExplosionsAndDebris extends Behavior {
    private final ExplosionFactory explosions;
    private final DebrisFactory debris;
    public SpawnsRandomExplosionsAndDebris(TR tr){
	this.explosions=tr.getResourceManager().getExplosionFactory();
	this.debris=tr.getResourceManager().getDebrisFactory();
    }
    @Override
    public void _tick(long timeMillis){
	if(Math.random()<.1){
	    explosions.triggerExplosion(Arrays.copyOf(getParent().getPosition(), 3), ExplosionType.Blast);}
	if(Math.random()<.1){
	    explosions.triggerExplosion(Arrays.copyOf(getParent().getPosition(), 3), ExplosionType.Billow);}
	if(Math.random()<.2){
	    debris.spawn(Arrays.copyOf(getParent().getPosition(), 3), new Vector3D(
		    Math.random()*50000,
		    Math.random()*50000,
		    Math.random()*50000));}
    }//end _tick(...)
}//end SpawnsRandomExplosionsAndDebris
