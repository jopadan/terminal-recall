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

package org.jtrfp.trcl.core;

import java.awt.event.KeyEvent;

import org.jtrfp.trcl.beh.ui.UserInputRudderElevatorControlBehavior;
import org.jtrfp.trcl.beh.ui.UserInputThrottleControlBehavior;
import org.jtrfp.trcl.beh.ui.UserInputWeaponSelectionBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Inputs {
 
 @Autowired
 public Inputs(ControllerMapper mapper, KeyboardInputDevice kid, GamepadInputDevice gid, TR tr){
     final ControllerInput elevator = tr.getControllerInputs().getInput(UserInputRudderElevatorControlBehavior.ELEVATOR);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_UP)  , elevator, 1, 0);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_DOWN), elevator, -1, 0);
     final ControllerSource yCtrl = gid.getGamepadControllerSource("y");
     if(yCtrl!=null)
      mapper.mapControllerSourceToInput(yCtrl, elevator, -1, 0);
     
     final ControllerInput rudder = tr.getControllerInputs().getInput(UserInputRudderElevatorControlBehavior.RUDDER);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_LEFT) , rudder, 1, 0);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_RIGHT), rudder, -1, 0);
     final ControllerSource xCtrl = gid.getGamepadControllerSource("x");
     if(xCtrl!=null)
      mapper.mapControllerSourceToInput(gid.getGamepadControllerSource("x"), rudder, -1, 0);
     
     final ControllerInput throttleDelta = tr.getControllerInputs().getInput(UserInputThrottleControlBehavior.THROTTLE_DELTA);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_A) , throttleDelta, 1, 0);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_Z), throttleDelta, -1, 0);
     final ControllerInput throttle = tr.getControllerInputs().getInput(UserInputThrottleControlBehavior.THROTTLE_DELTA);
     final ControllerSource zCtrl = gid.getGamepadControllerSource("z");
     if(zCtrl!=null)
      mapper.mapControllerSourceToInput(gid.getGamepadControllerSource("z"), throttle, -1, 0);
     
     final ControllerInput fire = tr.getControllerInputs().getInput(UserInputWeaponSelectionBehavior.FIRE);
     final ControllerSource Actrl = gid.getGamepadControllerSource("A");
     if(Actrl!=null)
      mapper.mapControllerSourceToInput(Actrl, fire, 1, 0);
     mapper.mapControllerSourceToInput(kid.getKeyControllerSource(KeyEvent.VK_SPACE), fire, 1, 0);
 }//end constructor
}//end Inputs