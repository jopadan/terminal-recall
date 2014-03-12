/*******************************************************************************
 * This file is part of TERMINAL RECALL 
 * Copyright (c) 2012, 2013 Chuck Ritola.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the COPYING and CREDITS files for more details.
 * 
 * Contributors:
 *      chuck - initial API and implementation
 ******************************************************************************/
package org.jtrfp.trcl;

import java.awt.Color;

import org.jtrfp.trcl.gpu.Vertex;

public class LineSegment {
    private final Vertex[] vertices = new Vertex[2];
    // Bad values given to make unset-variable bugs obvious.
    private Color color = null;
    private double thickness = Double.POSITIVE_INFINITY;

    /**
     * @return the color
     */
    public Color getColor() {
	return color;
    }

    /**
     * @param color
     *            the color to set
     */
    public void setColor(Color color) {
	this.color = color;
    }

    /**
     * @return the thickness
     */
    public double getThickness() {
	return thickness;
    }

    /**
     * @param thickness
     *            the thickness to set
     */
    public void setThickness(double thickness) {
	this.thickness = thickness;
    }

    public LineSegment setVertex(Vertex vtx, int i) {
	vertices[i] = vtx;
	return this;
    }

    public Vertex getVertex(int i) {
	return vertices[i];
    }

}// end LineSegment
