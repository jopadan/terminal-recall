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

package org.jtrfp.trcl.dbg;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;

import org.jtrfp.trcl.gpu.GLFrameBuffer;

public class TRBeanUtils {
    public static PropertyEditor getDefaultPropertyEditor(Object o){
	    try{final PropertyEditorSupport pe = (PropertyEditorSupport)PropertyEditorManager.findEditor(o.getClass());
	    pe.setSource(o);
	    return pe;}
	catch(Exception e){e.printStackTrace();}
	return null;
    }//end getDefaultPropertyEditor(...)
}//end TRBeanUtils
