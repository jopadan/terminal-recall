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
package org.jtrfp.trcl.gui;

import java.awt.geom.Point2D;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

public interface BriefingLayout {
 public Point2D.Double getTextPosition();
 public int            getNumCharsPerLine();
 public int            getNumLines();
 public double         getFontSizeGL();
 public Rotation       cameraHeadingAdjust();
}//end BriefingLayout