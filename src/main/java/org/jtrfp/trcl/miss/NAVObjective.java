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
package org.jtrfp.trcl.miss;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jtrfp.trcl.OverworldSystem;
import org.jtrfp.trcl.Tunnel;
import org.jtrfp.trcl.beh.CustomDeathBehavior;
import org.jtrfp.trcl.beh.CustomNAVTargetableBehavior;
import org.jtrfp.trcl.beh.DamageableBehavior;
import org.jtrfp.trcl.beh.RemovesNAVObjectiveOnDeath;
import org.jtrfp.trcl.beh.tun.TunnelEntryListener;
import org.jtrfp.trcl.core.Features;
import org.jtrfp.trcl.core.TRFactory;
import org.jtrfp.trcl.core.TRFactory.TR;
import org.jtrfp.trcl.file.Location3D;
import org.jtrfp.trcl.file.NAVData;
import org.jtrfp.trcl.file.NAVData.NAVSubObjectData;
import org.jtrfp.trcl.file.TDFFile.ExitMode;
import org.jtrfp.trcl.file.TDFFile.TunnelLogic;
import org.jtrfp.trcl.flow.TransientExecutor;
import org.jtrfp.trcl.game.TVF3Game;
import org.jtrfp.trcl.gui.ReporterFactory.Reporter;
import org.jtrfp.trcl.miss.TunnelSystemFactory.TunnelSystem;
import org.jtrfp.trcl.obj.Checkpoint;
import org.jtrfp.trcl.obj.DEFObject;
import org.jtrfp.trcl.obj.Jumpzone;
import org.jtrfp.trcl.obj.TunnelEntranceObject;
import org.jtrfp.trcl.obj.TunnelExitObject;
import org.jtrfp.trcl.obj.WorldObject;
import org.jtrfp.trcl.shell.GameShellFactory.GameShell;

public abstract class NAVObjective {
    private static final double CHECKPOINT_HEIGHT_PADDING=70000;
    public abstract String getDescription();
    public abstract WorldObject getTarget();
    
    protected NAVObjective(Reporter reporter, Factory f){
	if(f!=null)
	 reporter.report("org.jtrfp.trcl.flow.NAVObjective."+f.counter+".desc", getDescription());
	
	if(getTarget()!=null && f!=null && reporter != null){
	    final double [] loc = getTarget().getPosition();
	    reporter.report("org.jtrfp.trcl.flow.NAVObjective."+f.counter+".loc", "X="+loc[0]+" Y="+loc[1]+" Z="+loc[2]);
	    f.counter++;}
    }
    public static class Factory{
	private final TR tr;//for debug
	private Tunnel currentTunnel;
	int counter;
	private WorldObject worldBossObject,bossChamberExitShutoffTrigger;
	private final String debugName;
	private NAVObjective mostRecentExitObjective;
	public Factory(TR tr, String debugName){
	    this.tr=tr;
	    this.debugName = debugName;
	}//end constructor
	
	public void create(Reporter reporter, NAVSubObjectData NAVSubObjectData, List<NAVObjective>indexedNAVObjectiveList, Map<NAVObjective, NAVSubObjectData> navMap){
	        final GameShell gameShell = Features.get(tr,GameShell.class);
	        final TVF3Game game = (TVF3Game)gameShell.getGame();
	        final Mission mission = game.getCurrentMission();
		final OverworldSystem overworld=mission.getOverworldSystem();
		final List<DEFObject> defs = overworld.getDefList();
		if(NAVSubObjectData instanceof NAVData.DestroyTarget){///////////////////////////////////////////
		    NAVData.DestroyTarget tgt = (NAVData.DestroyTarget)NAVSubObjectData;
		    int [] targs =  tgt.getTargets();
		    for(int i=0; i<targs.length;i++){
			final WorldObject targ = defs.get(targs[i]);
			final NAVObjective objective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Destroy Target";
			    }
			    @Override
			    public WorldObject getTarget() {
				return targ;
			    }
			};//end new NAVObjective
			indexedNAVObjectiveList.add(objective);
			navMap.put(objective, NAVSubObjectData);
			targ.addBehavior(new RemovesNAVObjectiveOnDeath(objective,((TVF3Game)gameShell.getGame()).getCurrentMission()));
			targ.addBehavior(new CustomDeathBehavior(new Runnable(){
			    @Override
			    public void run(){
				((TVF3Game)((TVF3Game)gameShell.getGame())).getUpfrontDisplay()
					.submitMomentaryUpfrontMessage("Target Destroyed");
			    }//end run()
			}));
		    }//end for(targs)
		} else if(NAVSubObjectData instanceof NAVData.EnterTunnel){///////////////////////////////////////////
		    NAVData.EnterTunnel tun = (NAVData.EnterTunnel)NAVSubObjectData;
		    //Entrance and exit locations are already set up.
		    final Location3D 	loc3d 	= tun.getLocationOnMap();
		    /*final Vector3D modernLoc = new Vector3D(
				    TRFactory.legacy2Modern(loc3d.getX()),
				    TRFactory.legacy2Modern(loc3d.getY()),
				    TRFactory.legacy2Modern(loc3d.getZ()));
		    */
		    /*final TunnelEntranceObject teo = ((TVF3Game)tr.getGameShell().getGame()).getCurrentMission().getTunnelEntranceObject(
			    new Point((int)(modernLoc.getX()/TRFactory.mapSquareSize),(int)(modernLoc.getZ()/TRFactory.mapSquareSize)));
		    */
		    //final Mission mission = ((TVF3Game)gameShell.getGame()).getCurrentMission();
		    final TunnelSystem ts = Features.get(mission, TunnelSystem.class);
		    final TunnelEntranceObject teo = ts.getNearestTunnelEntrance(loc3d.getX(),loc3d.getY(),loc3d.getZ());
		    currentTunnel=teo.getSourceTunnel();
		    		/*final TunnelEntranceObject tunnelEntrance 
		    				= currentTunnel.getEntranceObject();
		    final double [] entPos=tunnelEntrance.getPosition();
		    entPos[0]=TRFactory.legacy2Modern(loc3d.getZ());
		    entPos[1]=TRFactory.legacy2Modern(loc3d.getY());
		    entPos[2]=TRFactory.legacy2Modern(loc3d.getX());
		    entPos[1]=((TVF3Game)tr.getGameShell().getGame()).
			    getCurrentMission().
			    getOverworldSystem().
			    getAltitudeMap().
			    heightAt(
				TRFactory.legacy2MapSquare(loc3d.getZ()), 
				TRFactory.legacy2MapSquare(loc3d.getX()))*(tr.getWorld().sizeY/2)+TunnelEntranceObject.GROUND_HEIGHT_PAD;
		    tunnelEntrance.notifyPositionChange();
		    */
		    final NAVObjective enterObjective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Enter Tunnel";
			    }
			    @Override
			    public WorldObject getTarget() {
				return teo;
			    }
		    };//end new NAVObjective tunnelEntrance
		    navMap.put(enterObjective, NAVSubObjectData);
		   //tunnelEntrance.setNavObjectiveToRemove(enterObjective,true);
		    final WorldObject tunnelEntranceObject = teo;
		    currentTunnel.addTunnelEntryListener(new TunnelEntryListener(){
			@Override
			public void notifyTunnelEntered(Tunnel tunnel) {
			    if(((TVF3Game)gameShell.getGame()).getCurrentMission().getRemainingNAVObjectives().get(0).getTarget()==tunnelEntranceObject){
				((TVF3Game)gameShell.getGame()).getCurrentMission().removeNAVObjective(enterObjective);
				tunnel.removeTunnelEntryListener(this);
			    }
			}});
		    indexedNAVObjectiveList.add(enterObjective);
		    final TunnelExitObject tunnelExit = currentTunnel.getExitObject();
		    mostRecentExitObjective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Exit Tunnel";
			    }
			    @Override
			    public WorldObject getTarget() {
				return tunnelExit;
			    }
		    };//end new NAVObjective tunnelExit
		    //final Point tunnelPoint = new Point((int)TRFactory.legacy2MapSquare(loc3d.getZ()),(int)TRFactory.legacy2MapSquare(loc3d.getX()));
		    /*
		    //if(mission.getTunnelEntrancePortal(new Point((int)TRFactory.legacy2MapSquare(loc3d.getZ()),(int)TRFactory.legacy2MapSquare(loc3d.getX())))==null){
			//TODO
			final Camera tunnelCam = tr.secondaryRenderer.get().getCamera();
			final PortalExit portalExit = tunnelExit.getPortalExit();
			//if(tunnelExit.isMirrorTerrain())
			// portalExit.setRootGrid(((TVF3Game)tr.getGameShell().getGame()).getCurrentMission().getOverworldSystem().getMirroredTerrainGrid());
			//else
			 //portalExit.setRootGrid(((TVF3Game)tr.getGameShell().getGame()).getCurrentMission().getOverworldSystem());
			portalExit.setRootGrid(currentTunnel);//DEBUG
			final Vector3D exitLocation = tunnelExit.getExitLocation();
			//portalExit.setPosition(exitLocation.toArray());
			System.out.println("NAVObjective setPosition="+exitLocation);
			//portalExit.setPosition(Tunnel.TUNNEL_START_POS.toArray());//DEBUG
			Vector3D heading = new NormalMap(((TVF3Game)tr.getGameShell().getGame()).getCurrentMission().getOverworldSystem().getAltitudeMap()).normalAt(exitLocation.getX(), exitLocation.getZ());
			//Vector3D heading = Tunnel.TUNNEL_START_DIRECTION.getHeading();//DEBUG
			//portalExit.setHeading(heading);
			
			if(heading.getY()<.99&heading.getNorm()>0)//If the ground is flat this doesn't work.
				 portalExit.setTop(Vector3D.PLUS_J.crossProduct(heading).crossProduct(heading).negate());
			portalExit.notifyPositionChange();
			//tunnelExit.setPortalExit(portalExit);
			//final PortalEntrance entrance = new PortalEntrance(tr,portalModel,exit,tr.mainRenderer.getCamera());
			//mission.registerTunnelEntrancePortal(tunnelPoint, exit);
		    //}
			*/
		    indexedNAVObjectiveList.add(mostRecentExitObjective);
		    tunnelExit.setNavObjectiveToRemove(mostRecentExitObjective,true);
		    
		    tunnelExit.setMirrorTerrain(currentTunnel.getSourceTunnel().getExitMode()==ExitMode.exitToChamber);
		    
		    if(currentTunnel.getSourceTunnel().getEntranceLogic()==TunnelLogic.visibleUnlessBoss){
			teo.setVisible(true);
			bossChamberExitShutoffTrigger.addBehavior(new CustomNAVTargetableBehavior(new Runnable(){
			    @Override
			    public void run() {
				//tunnelEntrance.getBehavior().probeForBehavior(TunnelEntranceBehavior.class).setEnable(false);
				teo.setVisible(false);}
			}));
			//Avoid memory leaks
			final WeakReference<Mission> weakMission = new WeakReference<Mission>(mission);
			worldBossObject.addBehavior(new CustomDeathBehavior(new Runnable(){
			    @Override
			    public void run(){
				final Mission mission = weakMission.get();
				if(mission!=null)
				 mission.exitBossMode();
				teo.setActive(true);
				teo.setVisible(true);
			    }
			}));
		    }else if(currentTunnel.getSourceTunnel().getEntranceLogic()==TunnelLogic.visible){//end if(visibleUnlessBoss)
			teo.setActive(true);
			teo.setVisible(true);
		    }
		} else if(NAVSubObjectData instanceof NAVData.Boss){///////////////////////////////////////////
		    //final Mission mission = ((TVF3Game)gameShell.getGame()).getCurrentMission();
		    final WeakReference<Mission> wMission = new WeakReference<Mission>(mission);
		    final NAVData.Boss bos = (NAVData.Boss)NAVSubObjectData;
		    boolean first=true;
		    final int [] bossTargs = bos.getTargets();
		    final DEFObject bossObject = defs.get(bos.getBossIndex());
		    if(bossTargs!=null){
		     for(final int target:bos.getTargets()){
			final WorldObject shieldGen = defs.get(target);
			final NAVObjective objective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Destroy Shield";
			    }
			    @Override
			    public WorldObject getTarget() {
				return shieldGen;
			    }
			};//end new NAVObjective
			navMap.put(objective, NAVSubObjectData);
			((DEFObject)shieldGen).setShieldGen(true);
			if(first){
			    bossChamberExitShutoffTrigger=shieldGen;
			    shieldGen.addBehavior(new CustomNAVTargetableBehavior(new Runnable(){
				@Override
				public void run(){
				    final Executor executor = TransientExecutor.getSingleton();
				    synchronized(executor){
					executor.execute(new Runnable(){
					    @Override
					    public void run() {
						wMission.get().enterBossMode(bos.getMusicFile(), bossObject);
						((TVF3Game)gameShell.getGame()).getUpfrontDisplay()
						.submitMomentaryUpfrontMessage("Mission Objective");
					    }});
				    }//end sync
				}//end run()
			    }));
			    first=false;
			}//end if(first)
			shieldGen.addBehavior(new RemovesNAVObjectiveOnDeath(objective,mission));
			bossChamberExitShutoffTrigger.addBehavior(new CustomNAVTargetableBehavior(new Runnable(){
			    @Override
			    public void run() {
				shieldGen.probeForBehavior(DamageableBehavior.class).setEnable(true);
				shieldGen.setActive(true);
			    }
			}));
			indexedNAVObjectiveList.add(objective);
		     }//end for(targets)
		    }//end if(bos.targets() !=null))
		    else {// No shield gens, just mark the boss.
			bossChamberExitShutoffTrigger=bossObject;
			    bossObject.addBehavior(new CustomNAVTargetableBehavior(new Runnable(){
				@Override
				public void run(){
				    final Executor executor = TransientExecutor.getSingleton();
				    synchronized(executor){
					executor.execute(new Runnable(){
					    @Override
					    public void run() {
						bossObject.setIgnoringProjectiles(false);
						wMission.get().enterBossMode(bos.getMusicFile(),bossObject);
						((TVF3Game)gameShell.getGame()).getUpfrontDisplay()
						    .submitMomentaryUpfrontMessage("Mission Objective");
					    }});
				    }//end sync
				}//end run()
			    }));
		    }
		    bossObject.setIgnoringProjectiles(true);
		    final NAVObjective objective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Destroy Boss";
			    }
			    @Override
			    public WorldObject getTarget() {
				return bossObject;
			    }
			};//end new NAVObjective
			navMap.put(objective, NAVSubObjectData);
			indexedNAVObjectiveList.add(objective);
			bossObject.addBehavior(new RemovesNAVObjectiveOnDeath(objective,mission));
			//bossObject.addBehavior(new ChangesBehaviorWhenTargeted(true,DamageableBehavior.class));
			bossObject.addBehavior(new CustomDeathBehavior(new Runnable(){
			    @Override
			    public void run(){
				wMission.get().exitBossMode();
			    }//end run()
			}));
			
			if(bossTargs!=null){
			 if(bossTargs.length==0){
			    bossChamberExitShutoffTrigger=bossObject;}}
			else bossChamberExitShutoffTrigger=bossObject;
			worldBossObject = bossObject;
			bossChamberExitShutoffTrigger.addBehavior(new CustomNAVTargetableBehavior(new Runnable(){
			    @Override
			    public void run() {
				bossObject.setActive(true);}
			}));
		} else if(NAVSubObjectData instanceof NAVData.Checkpoint){///////////////////////////////////////////
		    final NAVData.Checkpoint cp = (NAVData.Checkpoint)NAVSubObjectData;
		    final Location3D loc3d = cp.getLocationOnMap();
		    final Checkpoint chk = new Checkpoint();
		    chk.setDebugName("NAVObjective."+debugName);
		    final double [] chkPos = chk.getPosition();
		    chkPos[0]=TRFactory.legacy2Modern(loc3d.getZ());
		    chkPos[1]=TRFactory.legacy2Modern(loc3d.getY()+CHECKPOINT_HEIGHT_PADDING);
		    chkPos[2]=TRFactory.legacy2Modern(loc3d.getX());
		    chk.notifyPositionChange();
		    chk.setIncludeYAxisInCollision(false);
		    final NAVObjective objective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Checkpoint";
			    }
			    @Override
			    public WorldObject getTarget() {
				return chk;
			    }
		    };//end new NAVObjective
		    navMap.put(objective, NAVSubObjectData);
		    chk.setObjectiveToRemove(objective,((TVF3Game)gameShell.getGame()).getCurrentMission());
		    overworld.add(chk);
		    indexedNAVObjectiveList.add(objective);
		} else if(NAVSubObjectData instanceof NAVData.ExitTunnel){///////////////////////////////////////////
		    if( currentTunnel == null ) {
			System.err.println("XIT found without preceeding tunnel. Ignoring...");
			return;
		    }
		    NAVData.ExitTunnel xit = (NAVData.ExitTunnel)NAVSubObjectData;
		    Location3D loc3d = xit.getLocationOnMap();
		    navMap.put(mostRecentExitObjective, xit);
		    currentTunnel.
		     getExitObject().
		     setExitLocation(
			    new Vector3D(TRFactory.legacy2Modern(loc3d.getZ()),TRFactory.legacy2Modern(loc3d.getY()),TRFactory.legacy2Modern(loc3d.getX())));
		} else if(NAVSubObjectData instanceof NAVData.Jumpzone){///////////////////////////////////////////
		    final NAVData.Jumpzone xit = (NAVData.Jumpzone)NAVSubObjectData;
		    final Location3D loc3d = xit.getLocationOnMap();
		    final Jumpzone chk = new Jumpzone();
		    chk.setMission(((TVF3Game)gameShell.getGame()).getCurrentMission());
		    final double [] chkPos = chk.getPosition();
		    chkPos[0]=TRFactory.legacy2Modern(loc3d.getZ());
		    chkPos[1]=TRFactory.legacy2Modern(loc3d.getY());
		    chkPos[2]=TRFactory.legacy2Modern(loc3d.getX());
		    chk.notifyPositionChange();
		    chk.setVisible(false);
		    try{//Start placing the jump zone.
		    //WorldObject jumpZone = new WorldObject(tr,tr.getResourceManager().getBINModel("JUMP-PNT.BIN", tr.getGlobalPaletteVL(), tr.gpu.get().getGl()));
		    //jumpZone.setPosition(chk.getPosition());
		    //jumpZone.setVisible(true);
		    //overworld.add(jumpZone);
		    final NAVObjective objective = new NAVObjective(reporter, this){
			    @Override
			    public String getDescription() {
				return "Fly To Jump Zone";
			    }
			    @Override
			    public WorldObject getTarget() {
				return chk;
			    }
		    };//end new NAVObjective
		    navMap.put(objective, NAVSubObjectData);
		    chk.setObjectiveToRemove(objective);
		    chk.setIncludeYAxisInCollision(false);
		    overworld.add(chk);
		    indexedNAVObjectiveList.add(objective);
		    }catch(Exception e){e.printStackTrace();}
		}else{System.err.println("Unrecognized NAV objective: "+NAVSubObjectData);}
	    }//end create()
    }//end Factory
}//end NAVObjective
