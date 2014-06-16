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
package org.jtrfp.trcl.core;

import java.lang.reflect.Field;

public interface AutoInitializable<TYPE> {
    public void autoInit(TYPE parent);
    
    public static class Initializer{
	public static <T>void initialize(T parent){
	    final Class clazz = parent.getClass();
	    for(Field f:clazz.getDeclaredFields()){
		if(AutoInitializable.class.isAssignableFrom(f.getType())){
		    try{
		    AutoInitializable<T> ai = (AutoInitializable<T>)f.get(parent);
		    ai.autoInit(parent);
		    }catch(IllegalAccessException e){throw new RuntimeException(e);}
		}//end if(matches type)
	    }//end for(fields)
	}//end Initializer
    }//end class Initializer
}
