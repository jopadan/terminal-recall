package org.jtrfp.trcl.beh;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.obj.Player;
import org.jtrfp.trcl.obj.Projectile;
import org.jtrfp.trcl.obj.WorldObject;

public class DestructibleWallBehavior extends Behavior {
    private static final double THICKNESS_X=4000;
    @Override
    public void _proposeCollision(WorldObject other){
	final Vector3D otherPos=other.getPosition();
	final WorldObject p = getParent();
	final Vector3D thisPos=p.getPosition();
	if(otherPos.getX()>thisPos.getX()&& otherPos.getX()<thisPos.getX()+THICKNESS_X){
    	    if(other instanceof Player){
    	        final Player player=(Player)other;
    	        player.getBehavior().probeForBehavior(DamageableBehavior.class).impactDamage(1024);
    	        }//end if(Player)
    	    else if(other instanceof Projectile){
    		other.getBehavior().probeForBehavior(ProjectileBehavior.class).forceCollision(p);
    	    	}//end if(Projectile)
    	}//end if(in range)
    }//end _proposeCollision(...)
}//end DestructibleAllBehavior
