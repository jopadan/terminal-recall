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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.AbstractSubmitter;
import org.jtrfp.trcl.Camera;
import org.jtrfp.trcl.SpacePartitioningGrid;
import org.jtrfp.trcl.WeakPropertyChangeListener;
import org.jtrfp.trcl.beh.Behavior;
import org.jtrfp.trcl.beh.Cloakable;
import org.jtrfp.trcl.beh.CollidesWithTerrain;
import org.jtrfp.trcl.beh.CollidesWithTunnelWalls;
import org.jtrfp.trcl.beh.DamageableBehavior;
import org.jtrfp.trcl.beh.DamageableBehavior.SupplyNotNeededException;
import org.jtrfp.trcl.beh.DamagedByCollisionWithDEFObject;
import org.jtrfp.trcl.beh.DamagedByCollisionWithSurface;
import org.jtrfp.trcl.beh.DeathBehavior;
import org.jtrfp.trcl.beh.DeathListener;
import org.jtrfp.trcl.beh.ExplodesOnDeath;
import org.jtrfp.trcl.beh.FacingObject;
import org.jtrfp.trcl.beh.HeadingXAlwaysPositiveBehavior;
import org.jtrfp.trcl.beh.LoopingPositionBehavior;
import org.jtrfp.trcl.beh.MatchDirection;
import org.jtrfp.trcl.beh.MatchPosition;
import org.jtrfp.trcl.beh.ProjectileFiringBehavior;
import org.jtrfp.trcl.beh.RollLevelingBehavior;
import org.jtrfp.trcl.beh.RollNudgeOnDamage;
import org.jtrfp.trcl.beh.RotateAroundObject;
import org.jtrfp.trcl.beh.SFXOnDamage;
import org.jtrfp.trcl.beh.SpinCrashDeathBehavior;
import org.jtrfp.trcl.beh.UpdatesNAVRadar;
import org.jtrfp.trcl.beh.UpgradeableProjectileFiringBehavior;
import org.jtrfp.trcl.beh.phy.AccelleratedByPropulsion;
import org.jtrfp.trcl.beh.phy.BouncesOffSurfaces;
import org.jtrfp.trcl.beh.phy.HasPropulsion;
import org.jtrfp.trcl.beh.phy.MovesByVelocity;
import org.jtrfp.trcl.beh.phy.RotationalDragBehavior;
import org.jtrfp.trcl.beh.phy.RotationalMomentumBehavior;
import org.jtrfp.trcl.beh.phy.VelocityDragBehavior;
import org.jtrfp.trcl.beh.ui.AfterburnerBehavior;
import org.jtrfp.trcl.beh.ui.UpdatesHealthMeterBehavior;
import org.jtrfp.trcl.beh.ui.UpdatesThrottleMeterBehavior;
import org.jtrfp.trcl.beh.ui.UserInputRudderElevatorControlBehavior;
import org.jtrfp.trcl.beh.ui.UserInputThrottleControlBehavior;
import org.jtrfp.trcl.beh.ui.UserInputWeaponSelectionBehavior;
import org.jtrfp.trcl.core.Features;
import org.jtrfp.trcl.core.ResourceManager;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.core.ThreadManager;
import org.jtrfp.trcl.ext.tr.GamePauseFactory.GamePause;
import org.jtrfp.trcl.file.Weapon;
import org.jtrfp.trcl.game.TVF3Game;
import org.jtrfp.trcl.gpu.Model;
import org.jtrfp.trcl.miss.Mission;
import org.jtrfp.trcl.obj.Explosion.ExplosionType;
import org.jtrfp.trcl.pool.ObjectFactory;
import org.jtrfp.trcl.snd.SoundTexture;

public class Player extends WorldObject implements RelevantEverywhere{
    //private final Camera 	camera;
    //private int 		cameraDistance 			= 0;
    public static final int 	CLOAK_COUNTDOWN_START 		= ThreadManager.GAMEPLAY_FPS * 30;// 30sec
    public static final int 	INVINCIBILITY_COUNTDOWN_START 	= ThreadManager.GAMEPLAY_FPS * 30;// 30sec
    private final 		ProjectileFiringBehavior[] weapons = new ProjectileFiringBehavior[Weapon
                  		                                   .values().length];
    private final RunModeListener               runStateListener = new RunModeListener();
    private final WeakPropertyChangeListener    weakRunStateListener;
    private final HeadingXAlwaysPositiveBehavior headingXAlwaysPositiveBehavior;
    
    private static final double LEFT_X = -5000, RIGHT_X = 5000, TOP_Y = 2000, BOT_Y=-2000;

    public Player(final TR tr, final Model model) {
	super(tr, model);
	setVisible(false);
	DamageableBehavior db = new DamageableBehavior();
	addBehavior(db);
	String godMode = System.getProperty("org.jtrfp.trcl.godMode");
	if (godMode != null) {
	    if (godMode.toUpperCase().contains("TRUE")) {
		db.setEnable(false);
	    }
	}
	
	final ResourceManager rm = tr.getResourceManager();
	final ObjectFactory<String,SoundTexture> soundTextures = rm.soundTextures;
	
	addBehavior(new AccelleratedByPropulsion());
	addBehavior(new MovesByVelocity());
	addBehavior(new HasPropulsion());
	addBehavior(new CollidesWithTunnelWalls(true, true));
	addBehavior(new UserInputThrottleControlBehavior(tr.getControllerInputs()));
	addBehavior(new VelocityDragBehavior());
	addBehavior(new RollLevelingBehavior());
	addBehavior(new UserInputRudderElevatorControlBehavior(tr.getControllerInputs()));
	addBehavior(new RotationalMomentumBehavior());
	addBehavior(new RotationalDragBehavior());
	addBehavior(new CollidesWithTerrain().setTunnelEntryCapable(true).setIgnoreHeadingForImpact(false));
	addBehavior(new AfterburnerBehavior(tr.getControllerInputs()).
		setIgnitionSound  (soundTextures.get(AfterburnerBehavior.IGNITION_SOUND)).
		setExtinguishSound(soundTextures.get(AfterburnerBehavior.EXTINGUISH_SOUND)).
		setLoopSound      (soundTextures.get(AfterburnerBehavior.LOOP_SOUND)));
	addBehavior(new LoopingPositionBehavior());
	addBehavior(headingXAlwaysPositiveBehavior = (HeadingXAlwaysPositiveBehavior)new HeadingXAlwaysPositiveBehavior().setEnable(false));
	//Add a listener to control HeadingXAlwaysPositive
	//final Game game = tr.getGame();
	weakRunStateListener = new WeakPropertyChangeListener(runStateListener, tr);
	tr.addPropertyChangeListener(TR.RUN_STATE, weakRunStateListener);
	addBehavior(new UpdatesThrottleMeterBehavior().setController(((TVF3Game)tr.getGame()).getHUDSystem().getThrottleMeter()));
	addBehavior(new UpdatesHealthMeterBehavior().setController(((TVF3Game)tr.getGame()).getHUDSystem().getHealthMeter()));
	addBehavior(new DamagedByCollisionWithDEFObject());
	addBehavior(new DamagedByCollisionWithSurface());
	addBehavior(new BouncesOffSurfaces());
	addBehavior(new UpdatesNAVRadar());
	addBehavior(new Cloakable());
	//addBehavior(new SurfaceImpactSFXBehavior(tr));
	addBehavior(new RedFlashOnDamage());
	addBehavior(new RollNudgeOnDamage());
	final SpinCrashDeathBehavior scb = new SpinCrashDeathBehavior();
	scb.addPropertyChangeListener(SpinCrashDeathBehavior.TRIGGERED, new SpinCrashTriggerBehaviorListener());
	addBehavior(scb);
	addBehavior(new DeathBehavior());
	addBehavior(new ExplodesOnDeath(ExplosionType.Blast));
	addBehavior(new PlayerDeathListener());
	addBehavior(new SFXOnDamage());
	
	final Weapon[] allWeapons = Weapon.values();
	
	for (int i = 0; i < allWeapons.length; i++) {
	    final Weapon w = allWeapons[i];
	    if (w.getButtonToSelect() != -1) {
		final ProjectileFiringBehavior pfb;
		if (w.isLaser()) {// LASER
		    pfb = new UpgradeableProjectileFiringBehavior()
			    .setProjectileFactory(tr.getResourceManager()
				    .getProjectileFactories()[w.ordinal()]);
		    ((UpgradeableProjectileFiringBehavior) pfb)
			    .setMaxCapabilityLevel(2)
			    .setCapabilityLevel(i==0?0:-1)
			    .setFiringMultiplexMap(
				    new Vector3D[][] {
					    new Vector3D[] {
						    new Vector3D(RIGHT_X, BOT_Y, 0),
						    new Vector3D(LEFT_X, BOT_Y,
							    0) },// Level 0,
								 // single
					    new Vector3D[] {
						    new Vector3D(RIGHT_X, BOT_Y, 0),
						    new Vector3D(LEFT_X, BOT_Y,
							    0) },// Level 1,
								 // double
					    new Vector3D[] {
						    new Vector3D(RIGHT_X, BOT_Y, 0),
						    new Vector3D(LEFT_X, BOT_Y,
							    0),// Level 2 quad
						    new Vector3D(RIGHT_X, TOP_Y, 0),
						    new Vector3D(LEFT_X, TOP_Y, 0) } })//Level 2 cont'd
				.setTimeBetweenFiringsMillis(w.getFiringIntervalMS())
				.setSumProjectorVelocity(w.isSumWithProjectorVel());
		}// end if(isLaser)
		else {// NOT LASER
		    pfb = new ProjectileFiringBehavior().setFiringPositions(
			    new Vector3D[] { new Vector3D(RIGHT_X, BOT_Y, 0),
				    new Vector3D(LEFT_X, BOT_Y, 0) })
			    .setProjectileFactory(
				    tr.getResourceManager()
					    .getProjectileFactories()[w
					    .ordinal()])
		            .setTimeBetweenFiringsMillis(w.getFiringIntervalMS())
		            .setSumProjectorVelocity(w.isSumWithProjectorVel());
		    if (w == Weapon.DAM)
			pfb.setAmmoLimit(1);
		    if(w == Weapon.ION){
			pfb.setFiringDirections( new Vector3D [] {
				new Vector3D(-.5,0,1).normalize(),//LEFT
				Vector3D.PLUS_K, //CENTER
				new Vector3D(.5,0,1).normalize(),//RIGHT
				new Vector3D(0,-.3,1).normalize(),//DOWN
				new Vector3D(0,.3,1).normalize() //UP
			});
			pfb.setFiringPositions(new Vector3D[]{
				new Vector3D(0,BOT_Y,0),
				new Vector3D(0,BOT_Y,0),
				new Vector3D(0,BOT_Y,0),
				new Vector3D(0,BOT_Y,0),
				new Vector3D(0,BOT_Y,0)
				});
		    }//end if(ION)
		}
		addBehavior(pfb);
		weapons[w.getButtonToSelect() - 1] = pfb;
		if(System.getProperties().containsKey("org.jtrfp.trcl.allAmmo")){
		    if(System.getProperty("org.jtrfp.trcl.allAmmo").toUpperCase().contains("TRUE")){
			System.out.println("allAmmo cheat active for weapon "+w.getButtonToSelect());
			pfb.setAmmoLimit(Integer.MAX_VALUE);
			try{pfb.addSupply(Double.POSITIVE_INFINITY);}catch(SupplyNotNeededException e){}
		    }//end if(property=true)
		}//end if(allAmmo)
	    }// end if(hasButton)
	}//end for(Weapons)
	addBehavior(new UserInputWeaponSelectionBehavior(tr.getControllerInputs()).setBehaviors(weapons));
	
	defaultConfiguration();
    }//end constructor
    
    private class RunModeListener implements PropertyChangeListener {
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	    final Object newValue = evt.getNewValue();
	    final Object oldValue = evt.getOldValue();
	    if(newValue instanceof Mission.TunnelState         && !(oldValue instanceof Mission.TunnelState))
		headingXAlwaysPositiveBehavior.setEnable(true);
	    else if(!(newValue instanceof Mission.TunnelState) && (oldValue instanceof Mission.TunnelState))
		headingXAlwaysPositiveBehavior.setEnable(false);
	}//end propertyChange(...)
    }//end RunModeListener
    
    private void defaultConfiguration(){
		probeForBehavior(VelocityDragBehavior.class)
			.setDragCoefficient(.86);
		probeForBehavior(Propelled.class).setMinPropulsion(0);
		probeForBehavior(Propelled.class)
			.setMaxPropulsion(900000);
		probeForBehavior(RotationalDragBehavior.class)
			.setDragCoefficient(.86);
		setActive(false);
    }//end defaultConfiguration()
    
    public void resetVelocityRotMomentum(){
	//probeForBehavior(HasPropulsion.class).setPropulsion(0);
	probeForBehavior(RotationalMomentumBehavior.class).
		setEquatorialMomentum(0).
		setLateralMomentum(0).
		setPolarMomentum(0);
	probeForBehavior(MovesByVelocity.class).setVelocity(Vector3D.ZERO);
    }
    
    private class PlayerDeathListener extends Behavior implements DeathListener{
	@Override
	public void notifyDeath() {
	    new Thread(){
		@Override
		public void run(){
		    final Player thisPlayer = Player.this;
		    setName("Player Death Sequence Thread");
		    System.out.println("Player has died.");
		    try{Thread.sleep(3000);}
		    catch(InterruptedException e){}
		    //Reset player
		    final DamageableBehavior db = Player.this.probeForBehavior(DamageableBehavior.class);
		    db.setHealth(db.getMaxHealth());
		    Player.this.defaultConfiguration();
		    thisPlayer.probeForBehavior(SpinCrashDeathBehavior.class).
		      reset().
		      setEnable(true);
		    probeForBehaviors(new AbstractSubmitter<ProjectileFiringBehavior>(){
			@Override
			public void submit(ProjectileFiringBehavior item) {
			    item.setEnable(true);
			}}, ProjectileFiringBehavior.class);
		    probeForBehavior(ProjectileFiringBehavior.class).setEnable(true);
		    thisPlayer.probeForBehavior(DeathBehavior.class).reset();
		    //Reset camera
		    final Camera camera = Player.this.getTr().mainRenderer.get().getCamera(); 
		    Player.this.setVisible(false);
		    camera.probeForBehavior(MatchPosition.class) .setEnable(true);
		    camera.probeForBehavior(MatchDirection.class).setEnable(true);
		    camera.probeForBehavior(RotateAroundObject.class).
		     setEnable(false);
		    camera.probeForBehavior(FacingObject.class).
		     setEnable(false);
		    //Reset game
	            final TVF3Game game = (TVF3Game)Player.this.getTr().getGame();
	            final Mission mission = game.getCurrentMission();
	            Features.get(mission, GamePause.class).setPaused(true);
		    mission.abort();
		    final SpacePartitioningGrid grid = thisPlayer.probeForBehavior(DeathBehavior.class).getGridOfLastDeath();
		    grid.add(thisPlayer);
		    thisPlayer.setActive(true);
		    
		    try{game.setLevelIndex(game.getLevelIndex());
		        game.getCurrentMission().go();
		    }catch(Exception e){e.printStackTrace();}
		}//end run()
	    }.start();
	}//end notifyDeath()
    }//end PlayerDeathListener

    @Override
    public void setHeading(Vector3D lookAt) {
	/*camera.setLookAtVector(lookAt);
	camera.setPosition(new Vector3D(getPosition()).subtract(lookAt
		.scalarMultiply(cameraDistance)));*/
	super.setHeading(lookAt);
    }

    @Override
    public void setTop(Vector3D top) {
	//camera.setUpVector(top);
	super.setTop(top);
    }

    @Override
    public Player setPosition(double[] pos) {
	super.setPosition(pos);
	return this;
    }

    /**
     * @return the weapons
     */
    public ProjectileFiringBehavior[] getWeapons() {
	return weapons;
    }
    
    private class SpinCrashTriggerBehaviorListener implements PropertyChangeListener{
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
	    if(pce.getNewValue()==Boolean.TRUE){
		System.out.println("Player death sequence triggered.");
		final Camera camera = Player.this.getTr().mainRenderer.get().getCamera(); 
		Player.this.setVisible(true);
		    camera.probeForBehavior(MatchPosition.class) .setEnable(false);
		    camera.probeForBehavior(MatchDirection.class).setEnable(false);
		    camera.probeForBehavior(RotateAroundObject.class).
		            setTarget(Player.this).
		    	    setDistance(TR.mapSquareSize*1).
			    setAngularVelocityRPS(.1).
			    setEnable(true);
		    camera.probeForBehavior(FacingObject.class).
		      setTarget(Player.this).
		      setEnable(true);
	    }//end if(triggered)
	}//end propertyChange(...)
    }//end PropertyChangeListener
}// end Player
