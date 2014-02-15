package org.jtrfp.trcl.beh;

import java.util.Collection;

import org.jtrfp.trcl.Submitter;
import org.jtrfp.trcl.obj.Player;

public class DamageableBehavior extends Behavior{
    	private int maxHealth=65535;
	private int health=maxHealth;
	private long invincibilityExpirationTime=System.currentTimeMillis()+100;//Safety time in case init causes damage

	public DamageableBehavior impactDamage(int dmg){
	    if(isEnabled())generalDamage(dmg);
	    return this;
	}
	
	public DamageableBehavior shearDamage(int dmg){
	    if(isEnabled())generalDamage(dmg);
	    return this;
	}
	
	protected void generalDamage(int dmg){
	    if(!isEnabled())return;
	    if(isInvincible())return;
	    health-=dmg;
		if(health<=0){
		    getParent().destroy();
		    getParent().getBehavior().probeForBehaviors(deathSub, DeathListener.class);
		}//end if(dead)
		else{
		    if(getParent() instanceof Player)addInvincibility(2500);//Safety/Escape
		}//end (!dead)
	}//end generalDamage(...)

	public boolean isInvincible(){
	    return invincibilityExpirationTime>System.currentTimeMillis();
	}
	
	public int getHealth(){
		return health;
		}

	public void unDamage(int amt) throws SupplyNotNeededException{
	    	if(!isEnabled())return;
	    	if(amt==maxHealth){unDamage();return;}
	    	if(health+amt>maxHealth)throw new SupplyNotNeededException();
		health+=amt;
		}

	public void unDamage() throws SupplyNotNeededException{
	    	if(!isEnabled())return;
	    	if(health>=maxHealth)throw new SupplyNotNeededException();
		health=maxHealth;
		}
	public DamageableBehavior setHealth(int val){
	    health=val;return this;
	}
	
	private final Submitter<DeathListener> deathSub = new Submitter<DeathListener>(){

	    @Override
	    public void submit(DeathListener item) {
		item.notifyDeath();
	    }

	    @Override
	    public void submit(Collection<DeathListener> items) {
		for(DeathListener l:items){submit(l);}
	    }
	};

	public void addInvincibility(int invincibilityTimeDeltaMillis) {
	    ensureIsInvincible();
	    invincibilityExpirationTime+=invincibilityTimeDeltaMillis;
	}

	protected void ensureIsInvincible() {
	    if(!isInvincible())invincibilityExpirationTime=System.currentTimeMillis()+10;//10 for padding
	}

	/**
	 * @return the maxHealth
	 */
	public int getMaxHealth() {
	    return maxHealth;
	}

	/**
	 * @param maxHealth the maxHealth to set
	 */
	public DamageableBehavior setMaxHealth(int maxHealth) {
	    this.maxHealth = maxHealth;
	    return this;
	}
	
	
	public static class SupplyNotNeededException extends Exception{
	    
	}
    }//end DamageableBehavior
