/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2016 Chuck Ritola
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jtrfp.trcl.conf.ConfigRootFeature;
import org.jtrfp.trcl.core.TRFactory.TR;
import org.springframework.stereotype.Component;

@Component
public class TRConfigRootFactory implements FeatureFactory<Features>, LoadOrderAware{
    public class TRConfigRoot extends ConfigRootFeature<Features> implements GraphStabilizationListener{
	//private final Map<Class,Map<String,Object>> configurations = new HashMap<Class,Map<String,Object>>();

	@Override
	public void destruct(Features target) {
	    // TODO Auto-generated method stub

	}
	
	@Override
	public void apply(Features target){
	    super.apply(target);
	}

	@Override
	protected String getDefaultSaveURI() {
	    String homeProperty = System.getProperty("user.home");
		if(homeProperty==null)homeProperty="";
	    return homeProperty+File.separator+"settings.config.trcl.xml";
	}//end getDefaultSaveURI
	
	@Override
	public void graphStabilized(Object target){
	    super.loadConfigurations();
	}
    }//end TRConfigRoot

    @Override
    public Feature<Features> newInstance(Features target) {
	return new TRConfigRoot();
    }

    @Override
    public Class<Features> getTargetClass() {
	return Features.class;
    }

    @Override
    public Class<? extends Feature> getFeatureClass() {
	return TRConfigRoot.class;
    }

    @Override
    public int getFeatureLoadPriority() {
	return LoadOrderAware.LAST;
    }
}//end TRConfigRootFactory