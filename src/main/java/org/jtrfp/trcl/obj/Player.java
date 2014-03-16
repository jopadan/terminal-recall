package org.jtrfp.trcl.obj;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.Model;
import org.jtrfp.trcl.beh.AfterburnerBehavior;
import org.jtrfp.trcl.beh.AutoLeveling;
import org.jtrfp.trcl.beh.AutoLeveling.LevelingAxis;
import org.jtrfp.trcl.beh.Cloakable;
import org.jtrfp.trcl.beh.CollidesWithTerrain;
import org.jtrfp.trcl.beh.CollidesWithTunnelWalls;
import org.jtrfp.trcl.beh.DamageableBehavior;
import org.jtrfp.trcl.beh.DamageableBehavior.SupplyNotNeededException;
import org.jtrfp.trcl.beh.DamagedByCollisionWithGameplayObject;
import org.jtrfp.trcl.beh.DamagedByCollisionWithSurface;
import org.jtrfp.trcl.beh.HeadingXAlwaysPositiveBehavior;
import org.jtrfp.trcl.beh.LoopingPositionBehavior;
import org.jtrfp.trcl.beh.ProjectileFiringBehavior;
import org.jtrfp.trcl.beh.UpdatesNAVRadar;
import org.jtrfp.trcl.beh.UpgradeableProjectileFiringBehavior;
import org.jtrfp.trcl.beh.phy.AccelleratedByPropulsion;
import org.jtrfp.trcl.beh.phy.BouncesOffSurfaces;
import org.jtrfp.trcl.beh.phy.HasPropulsion;
import org.jtrfp.trcl.beh.phy.MovesByVelocity;
import org.jtrfp.trcl.beh.phy.RotationalDragBehavior;
import org.jtrfp.trcl.beh.phy.RotationalMomentumBehavior;
import org.jtrfp.trcl.beh.phy.VelocityDragBehavior;
import org.jtrfp.trcl.beh.ui.UpdatesHealthMeterBehavior;
import org.jtrfp.trcl.beh.ui.UpdatesThrottleMeterBehavior;
import org.jtrfp.trcl.beh.ui.UserInputRudderElevatorControlBehavior;
import org.jtrfp.trcl.beh.ui.UserInputThrottleControlBehavior;
import org.jtrfp.trcl.beh.ui.WeaponSelectionBehavior;
import org.jtrfp.trcl.core.Camera;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.core.ThreadManager;
import org.jtrfp.trcl.file.Weapon;

public class Player extends WorldObject {
    private final Camera camera;
    private int cameraDistance = 0;
    private static final int SINGLE_SKL = 0;
    public static final int CLOAK_COUNTDOWN_START = ThreadManager.GAMEPLAY_FPS * 30;// 30sec
    public static final int INVINCIBILITY_COUNTDOWN_START = ThreadManager.GAMEPLAY_FPS * 30;// 30sec
    private final ProjectileFiringBehavior[] weapons = new ProjectileFiringBehavior[Weapon
	    .values().length];

    public Player(TR tr, Model model) {
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
	
	addBehavior(new AccelleratedByPropulsion());
	addBehavior(new MovesByVelocity());
	addBehavior(new HasPropulsion());
	addBehavior(new CollidesWithTunnelWalls(true, true));
	addBehavior(new UserInputThrottleControlBehavior());
	addBehavior(new VelocityDragBehavior());
	addBehavior(new AutoLeveling()
		.setLevelingAxis(LevelingAxis.CROSS)
		.setLevelingVector(Vector3D.PLUS_I)
		.setRetainmentCoeff(1, .977, 1));
	addBehavior(new UserInputRudderElevatorControlBehavior());
	addBehavior(new RotationalMomentumBehavior());
	addBehavior(new RotationalDragBehavior());
	addBehavior(new CollidesWithTerrain());
	addBehavior(new AfterburnerBehavior());
	addBehavior(new LoopingPositionBehavior());
	addBehavior(new HeadingXAlwaysPositiveBehavior().setEnable(false));
	addBehavior(new UpdatesThrottleMeterBehavior().setController(tr
		.getHudSystem().getThrottleMeter()));
	addBehavior(new UpdatesHealthMeterBehavior().setController(tr
		.getHudSystem().getHealthMeter()));
	addBehavior(new DamagedByCollisionWithGameplayObject());
	addBehavior(new DamagedByCollisionWithSurface());
	addBehavior(new BouncesOffSurfaces());
	addBehavior(new UpdatesNAVRadar());
	addBehavior(new Cloakable());
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
			    .setFiringMultiplexMap(
				    new Vector3D[][] {
					    new Vector3D[] {
						    new Vector3D(5000, -3000, 0),
						    new Vector3D(-5000, -3000,
							    0) },// Level 0,
								 // single
					    new Vector3D[] {
						    new Vector3D(5000, -3000, 0),
						    new Vector3D(-5000, -3000,
							    0) },// Level 1,
								 // double
					    new Vector3D[] {
						    new Vector3D(5000, -3000, 0),
						    new Vector3D(-5000, -3000,
							    0),// Level 2 quad
						    new Vector3D(5000, 3000, 0),
						    new Vector3D(-5000, 3000, 0) } });// Level
										      // 2
										      // cont'd
		}// end if(isLaser)
		else {// NOT LASER
		    pfb = new ProjectileFiringBehavior().setFiringPositions(
			    new Vector3D[] { new Vector3D(5000, -3000, 0),
				    new Vector3D(-5000, -3000, 0) })
			    .setProjectileFactory(
				    tr.getResourceManager()
					    .getProjectileFactories()[w
					    .ordinal()]);
		    if (w == Weapon.DAM)
			pfb.setAmmoLimit(1);
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
	}
	addBehavior(new WeaponSelectionBehavior().setBehaviors(weapons));
	camera = tr.getRenderer().getCamera();
	getBehavior().probeForBehavior(VelocityDragBehavior.class)
		.setDragCoefficient(.86);
	getBehavior().probeForBehavior(Propelled.class).setMinPropulsion(0);
	getBehavior().probeForBehavior(Propelled.class)
		.setMaxPropulsion(900000);
	getBehavior().probeForBehavior(RotationalDragBehavior.class)
		.setDragCoefficient(.86);

    }//end constructor

    @Override
    public void setHeading(Vector3D lookAt) {
	camera.setLookAtVector(lookAt);
	camera.setPosition(new Vector3D(getPosition()).subtract(lookAt
		.scalarMultiply(cameraDistance)));
	super.setHeading(lookAt);
    }

    @Override
    public void setTop(Vector3D top) {
	camera.setUpVector(top);
	super.setTop(top);
    }

    @Override
    public Player setPosition(double[] pos) {
	super.setPosition(pos);
	return this;
    }

    @Override
    public WorldObject notifyPositionChange() {
	camera.setPosition(new Vector3D(getPosition()).subtract(getLookAt()
		.scalarMultiply(cameraDistance)));
	return super.notifyPositionChange();
    }

    /**
     * @return the weapons
     */
    public ProjectileFiringBehavior[] getWeapons() {
	return weapons;
    }
}// end Player
