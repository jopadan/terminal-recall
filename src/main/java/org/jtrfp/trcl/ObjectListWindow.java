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

import org.jtrfp.trcl.core.RenderList;
import org.jtrfp.trcl.core.TR;
import org.jtrfp.trcl.gpu.GlobalDynamicTextureBuffer;
import org.jtrfp.trcl.mem.MemoryWindow;
import org.jtrfp.trcl.mem.SubByteBuffer;

public class ObjectListWindow extends MemoryWindow {
    public ObjectListWindow() {
	init();
    }// end constructor

    public final ByteArrayVariable opaqueIDs = new ByteArrayVariable(
	    OBJECT_LIST_SIZE_BYTES_PER_PASS);
    public final ByteArrayVariable blendIDs = new ByteArrayVariable(
	    OBJECT_LIST_SIZE_BYTES_PER_PASS);

    public static final int OBJECT_LIST_SIZE_BYTES_PER_PASS = RenderList.NUM_BLOCKS_PER_PASS * 4;
    static {
	GlobalDynamicTextureBuffer
		.addAllocationToFinalize(ObjectListWindow.class);
    }

    public static void finalizeAllocation(TR tr) {
	ObjectListWindow olw = tr.getObjectListWindow();
	int bytesToAllocate = olw.getObjectSizeInBytes() * olw.getNumObjects();

	System.out.println("ObjectList: Allocating " + bytesToAllocate
		+ " bytes of GPU resident RAM.");
	olw.setBuffer(new SubByteBuffer(GlobalDynamicTextureBuffer
		.getLogicalMemory(), GlobalDynamicTextureBuffer
		.requestAllocation(bytesToAllocate)));
	tr.getReporter().report(
		"org.jtrfp.trcl.GlobalObjectList.arrayOffsetBytes",
		String.format("%08X", olw.getPhysicalAddressInBytes(0)));
    }
}// end GlobalObjectList
