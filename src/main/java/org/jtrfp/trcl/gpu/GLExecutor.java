/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2016 Chuck Ritola
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/

package org.jtrfp.trcl.gpu;

import java.util.concurrent.Callable;

import javax.media.opengl.GL;

import org.jtrfp.trcl.core.GLFutureTask;
import org.jtrfp.trcl.gui.GLExecutable;

public interface GLExecutor<GL_TYPE extends GL> {
    public <T> GLFutureTask<T> submitToGL(Callable<T> c);
    public <T> GLFutureTask<T> submitToGL(GLExecutable<T, ? extends GL_TYPE> executable);
    public void executeOnEachRefresh(GLExecutable<?, ? extends GL_TYPE> executable, double orderPriority);
    public void executeOnResize(GLExecutable<?, ? extends GL_TYPE> executable);
    public void executeOnDispose(GLExecutable<?, ? extends GL_TYPE> executable);
}//end GLExecutor
