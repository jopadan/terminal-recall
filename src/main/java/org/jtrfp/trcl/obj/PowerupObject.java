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
package org.jtrfp.trcl.obj;

import java.io.FileNotFoundException;
import java.util.Arrays;

import org.jtrfp.trcl.beh.Behavior;
import org.jtrfp.trcl.beh.Cloakable;
import org.jtrfp.trcl.beh.CollisionBehavior;
import org.jtrfp.trcl.beh.DamageableBehavior;
import org.jtrfp.trcl.beh.DamageableBehavior.SupplyNotNeededException;
import org.jtrfp.trcl.beh.FacingObject;
import org.jtrfp.trcl.beh.TunnelRailed;
import org.jtrfp.trcl.beh.ui.AfterburnerBehavior;
import org.jtrfp.trcl.conf.TRConfigurationFactory.TRConfiguration;
import org.jtrfp.trcl.core.Features;
import org.jtrfp.trcl.core.TRFactory;
import org.jtrfp.trcl.core.TRFactory.TR;
import org.jtrfp.trcl.ext.tr.SoundSystemFactory.SoundSystemFeature;
import org.jtrfp.trcl.file.Powerup;
import org.jtrfp.trcl.file.Weapon;
import org.jtrfp.trcl.flow.GameVersion;
import org.jtrfp.trcl.game.TVF3Game;
import org.jtrfp.trcl.shell.GameShellFactory.GameShell;
import org.jtrfp.trcl.snd.SoundSystem;
import org.jtrfp.trcl.snd.SoundTexture;

public class PowerupObject extends WorldObject{
	private final Powerup powerupType;
	private final SoundTexture powerupSound;
	private GameShell gameShell;
	public PowerupObject(Powerup pt) throws FileNotFoundException {
		super();
		this.powerupType=pt;
		final TR tr = getTr();
		try{setModel(tr.getResourceManager().getBINModel(powerupType.getModel(), tr.getGlobalPaletteVL(), null, null));}
		catch(FileNotFoundException e) {throw e;}
		catch(Exception e){e.printStackTrace();}
		
		addBehavior(new PowerupBehavior());
		addBehavior(new TunnelRailed());
		addBehavior(new FacingObject().setTarget(getTr().mainRenderer.getCamera()));
		
		powerupSound=tr.getResourceManager().soundTextures.get("POWER-1.WAV");
		}//end constructor

	private class PowerupBehavior extends Behavior implements CollisionBehavior{
		@Override
		public void proposeCollision(WorldObject other){
			if(TRFactory.twosComplementDistance(other.getPosition(), getPosition())<CollisionManager.SHIP_COLLISION_DISTANCE)
				{if(other instanceof Player){
				    	Player p=(Player)other;
					try{applyToPlayer(p);
					destroy();}
					catch(SupplyNotNeededException e){}
					}//end if(Player)
				}//end if(close enough)
			}//end proposeCollision()
		
		public void applyToPlayer(Player p) throws SupplyNotNeededException{
			if(powerupType.getAfterburnerDelta()!=0){
			    AfterburnerBehavior ab = p.probeForBehavior(AfterburnerBehavior.class);
			    ab.addSupply(powerupType.getAfterburnerDelta());
			}
			if(powerupType.getInvincibilityTimeDeltaMillis()!=0){
			    DamageableBehavior db = p.probeForBehavior(DamageableBehavior.class);
			    db.addInvincibility(powerupType.getInvincibilityTimeDeltaMillis());
			}
			if(powerupType.getInvisibiltyTimeDeltaMillis()!=0){
			    p.probeForBehavior(Cloakable.class).addSupply(powerupType.getInvisibiltyTimeDeltaMillis());
			}
			if(powerupType.getShieldDelta()!=0){
			    DamageableBehavior db = p.probeForBehavior(DamageableBehavior.class);
			    db.unDamage(powerupType.getShieldDelta());
			}
			//wEAPON DELTAS
			final Weapon pWeapon=powerupType.getWeapon();
			if(pWeapon!=null){
			    if(pWeapon.getButtonToSelect()!=-1){
			    p.getWeapons()[pWeapon.getButtonToSelect()-1].
			    addSupply(powerupType.
				    getWeaponSupplyDelta());}}
			final TR tr = getParent().getTr();
			((TVF3Game)getGameShell().getGame()).getUpfrontDisplay().submitMomentaryUpfrontMessage(
				Features.get(tr, TRConfiguration.class)._getGameVersion()!=GameVersion.TV?
					powerupType.getF3Description():
					powerupType.getTvDescription());
			//SOUND FX
			Features.get(tr,SoundSystemFeature.class)
			 .enqueuePlaybackEvent(Features.get(tr,SoundSystemFeature.class).getPlaybackFactory()
				 .create(powerupSound, new double []{.5*SoundSystem.DEFAULT_SFX_VOLUME*3,.5*SoundSystem.DEFAULT_SFX_VOLUME*3}));
		}//end applyToPlayer()
	}//end PowerupBehavior

	public Powerup getPowerupType()
		{return powerupType;}
	
	public void reset(double[] ds){
		setPosition(Arrays.copyOf(ds,3));
		setActive(true);
		setVisible(true);
	    }//end reset()
	
	public GameShell getGameShell() {
		if(gameShell == null){
		    gameShell = Features.get(getTr(), GameShell.class);}
		return gameShell;
	    }
	    public void setGameShell(GameShell gameShell) {
		this.gameShell = gameShell;
	    }
	}//end PowerupObject
