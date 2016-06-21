/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2016 Chuck Ritola
 * Part of the jMenuSystemFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/

package org.jtrfp.trcl.conf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jtrfp.trcl.core.Feature;
import org.jtrfp.trcl.core.FeatureFactory;
import org.jtrfp.trcl.core.Features;
import org.jtrfp.trcl.core.TRFactory.TR;
import org.jtrfp.trcl.gui.ConfigWindowFactory.ConfigWindow;
import org.jtrfp.trcl.gui.MenuSystem;
import org.springframework.stereotype.Component;

@Component
public class ConfigMenuItemFactory implements FeatureFactory<MenuSystem> {
    public static final String []   CONFIG_MENU_PATH = new String[]{"File","Configure..."};
    public class ConfigMenu implements Feature<MenuSystem>{
	private final ConfigMenuItemListener	configMenuItemListener = new ConfigMenuItemListener();
	private ConfigWindow configWindow;
	private TR tr;

	@Override
	public void apply(MenuSystem target) {
	    target.addMenuItem(CONFIG_MENU_PATH);
	    target.setMenuItemEnabled(true, CONFIG_MENU_PATH);
	    target.addMenuItemListener(configMenuItemListener, CONFIG_MENU_PATH);
	}

	@Override
	public void destruct(MenuSystem target) {
	    // TODO Auto-generated method stub

	}

	private class ConfigMenuItemListener implements ActionListener{
	    @Override
	    public void actionPerformed(ActionEvent e) {
		getConfigWindow().setVisible(true);
	    }
	}//end ConfigMenuItemListener
	
	private ConfigWindow getConfigWindow(){
	    if(configWindow == null)
	     configWindow = Features.get(getTr(), ConfigWindow.class);
	    return configWindow;
	}
	
	private TR getTr(){
	    if(tr == null)
	     tr = Features.get(Features.getSingleton(), TR.class);
	    return tr;
	}

    }//end Feature

    @Override
    public Feature<MenuSystem> newInstance(MenuSystem target) {
	return new ConfigMenu();
    }

    @Override
    public Class<MenuSystem> getTargetClass() {
	return MenuSystem.class;
    }

    @Override
    public Class<? extends Feature> getFeatureClass() {
	return ConfigMenu.class;
    }
}//end ConfigMenuItemFactory
