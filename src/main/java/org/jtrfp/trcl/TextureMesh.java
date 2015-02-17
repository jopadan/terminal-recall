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
package org.jtrfp.trcl;

import org.jtrfp.trcl.core.TextureDescription;

public interface TextureMesh {
    /**
     * Get the Texture at the mesh coordinates
     * 
     * @param x
     *            x-Cell coordinate to query
     * @param z
     *            z-Cell coordinate to query
     * @return
     * @since Oct 14, 2012
     */
    public TextureDescription textureAt(double x, double z);
}
