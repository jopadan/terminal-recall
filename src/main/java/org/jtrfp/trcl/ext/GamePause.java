/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2015 Chuck Ritola
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/
package org.jtrfp.trcl.ext;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.flow.Game;
import org.jtrfp.trcl.flow.IndirectProperty;
import org.jtrfp.trcl.flow.Mission;

import com.jogamp.newt.event.KeyEvent;

public class GamePause implements Extension<TR> {
    IndirectProperty<Game> gameIP;
    IndirectProperty<Mission>currentMissionIP;
    ActionListener pauseAL;
    JMenuItem game_pause;
    PropertyChangeListener gamePCL, pausePCL, satViewPCL;
    
    @Override
    public void init(TR tr) {
    }

    @Override
    public Class<TR> getExtendedClass() {
	return TR.class;
    }

    @Override
    public String getHumanReadableName() {
	return "Game Pause";
    }

    @Override
    public String getDescription() {
	return "Adds Game->Pause menu item, F3 pause, and pause when minimized.";
    }

    @Override
    public void apply(final TR tr) {
	game_pause = new JMenuItem("Pause");
	
	gameIP = new IndirectProperty<Game>();
	currentMissionIP = new IndirectProperty<Mission>();
	tr.addPropertyChangeListener(TR.GAME, gameIP);
	gameIP.addTargetPropertyChangeListener(Game.CURRENT_MISSION, currentMissionIP);
	
	game_pause.setAccelerator(KeyStroke.getKeyStroke("F3"));
	final Action pauseAction = new AbstractAction("Pause Button"){
	    private static final long serialVersionUID = -5172325691052703896L;
	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		tr.getThreadManager().submitToThreadPool(new Callable<Void>(){
		    @Override
		    public Void call() throws Exception {
			final Game game = tr.getGame();
			game.setPaused(!game.isPaused());
			return null;
		    }});
	    }};
	game_pause.addActionListener(pauseAL = new ActionListener(){
	    @Override
	    public void actionPerformed(ActionEvent evt) {
		pauseAction.actionPerformed(evt);
	    }});
	final String pauseKey = "PAUSE_KEY";
	game_pause.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P,0), pauseKey);
	game_pause.getActionMap().put(pauseKey, pauseAction);
	game_pause.setEnabled(false);
	try {
	    SwingUtilities.invokeAndWait(new Runnable(){
		@Override
		public void run() {
		    tr.getMenuSystem().getGameMenu().add(game_pause);
		}});
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
	pausePCL = new PropertyChangeListener(){
	    @Override
	    public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName().contentEquals("paused"))
		    game_pause.setText((Boolean)evt.getNewValue()==true?"Unpause":"Pause");
	    }//end if(paused)
	};//end pausePCL
	tr.addPropertyChangeListener("game", gamePCL = new PropertyChangeListener(){
	    @Override
	    public void propertyChange(PropertyChangeEvent evt) {
		game_pause.setEnabled(evt.getNewValue()!=null);
	    }});
	gameIP.addTargetPropertyChangeListener(Game.PAUSED, pausePCL);
	
	currentMissionIP.addTargetPropertyChangeListener(Mission.SATELLITE_VIEW, satViewPCL = new PropertyChangeListener(){
	    @Override
	    public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getNewValue()==Boolean.TRUE)
		    game_pause.setEnabled(false);
		if(evt.getNewValue()==Boolean.FALSE && tr.getGame().getCurrentMission().getMissionMode() instanceof Mission.GameplayMode)
		    game_pause.setEnabled(true);
	    }});
    }//end apply(...)

    @Override
    public void remove(final TR tr) {//UNTESTED
	tr.removePropertyChangeListener(gameIP);
	gameIP    .removeTargetPropertyChangeListener(currentMissionIP);
	game_pause.removeActionListener(pauseAL);
	try {
	    SwingUtilities.invokeAndWait(new Runnable(){
	        @Override
	        public void run() {
	    	tr.getMenuSystem().getGameMenu().remove(game_pause);
	        }});
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
	tr.removePropertyChangeListener(gamePCL);
	gameIP.removeTargetPropertyChangeListener(pausePCL);
	currentMissionIP.removeTargetPropertyChangeListener(satViewPCL);
	gameIP          =null;
	currentMissionIP=null;
	pauseAL         =null;
	game_pause      =null;
	gamePCL         =null;
	pausePCL        =null;
	satViewPCL      =null;
    }

}//end GamePause
