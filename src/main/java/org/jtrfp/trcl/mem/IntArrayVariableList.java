/*******************************************************************************
 * This file is part of TERMINAL RECALL
 * Copyright (c) 2012-2015 Chuck Ritola
 * Part of the jTRFP.org project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     chuck - initial API and implementation
 ******************************************************************************/

package org.jtrfp.trcl.mem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jtrfp.trcl.mem.MemoryWindow.IntArrayVariable;

public class IntArrayVariableList implements List<Integer> {
 private final ArrayList<Integer> cache = new ArrayList<Integer>();
 private final IntArrayVariable   delegate;
 private final int                objectIndex;
 
 public IntArrayVariableList(IntArrayVariable delegate, int objectIndex){
     this.delegate    = delegate;
     this.objectIndex = objectIndex;
 }
 
 public synchronized void flush(){
     for(int i=0; i<cache.size(); i++)
	 delegate.setAt(objectIndex, i, cache.get(i));
 }//end flush()

/**
 * @param index
 * @param element
 * @see java.util.ArrayList#add(int, java.lang.Object)
 */
public synchronized void add(int index, Integer element) {
    if(element==null)
	throw new NullPointerException("Element is intolerably null.");
    cache.add(index, element);
}

/**
 * @param e
 * @return
 * @see java.util.ArrayList#add(java.lang.Object)
 */
public synchronized boolean add(Integer e) {
    if(e==null)
	throw new NullPointerException("Element is intolerably null.");
    return cache.add(e);
}

/**
 * @param c
 * @return
 * @see java.util.ArrayList#addAll(java.util.Collection)
 */
public synchronized boolean addAll(Collection<? extends Integer> c) {
    if(c.contains(null))
	throw new NullPointerException("Element is intolerably null.");
    return cache.addAll(c);
}

/**
 * @param index
 * @param c
 * @return
 * @see java.util.ArrayList#addAll(int, java.util.Collection)
 */
public synchronized boolean addAll(int index, Collection<? extends Integer> c) {
    if(c.contains(null))
   	throw new NullPointerException("Element is intolerably null.");
    return cache.addAll(index, c);
}

/**
 * 
 * @see java.util.ArrayList#clear()
 */
public synchronized void clear() {
    cache.clear();
}

/**
 * @param o
 * @return
 * @see java.util.ArrayList#contains(java.lang.Object)
 */
public synchronized boolean contains(Object o) {
    return cache.contains(o);
}

/**
 * @param arg0
 * @return
 * @see java.util.AbstractCollection#containsAll(java.util.Collection)
 */
public synchronized boolean containsAll(Collection<?> arg0) {
    return cache.containsAll(arg0);
}

/**
 * @param minCapacity
 * @see java.util.ArrayList#ensureCapacity(int)
 */
public synchronized void ensureCapacity(int minCapacity) {
    cache.ensureCapacity(minCapacity);
}

/**
 * @param index
 * @return
 * @see java.util.ArrayList#get(int)
 */
public synchronized Integer get(int index) {
    return cache.get(index);
}

/**
 * @param o
 * @return
 * @see java.util.ArrayList#indexOf(java.lang.Object)
 */
public synchronized int indexOf(Object o) {
    return cache.indexOf(o);
}

/**
 * @return
 * @see java.util.ArrayList#isEmpty()
 */
public synchronized boolean isEmpty() {
    return cache.isEmpty();
}

/**
 * @return
 * @see java.util.ArrayList#iterator()
 */
public synchronized Iterator<Integer> iterator() {
    return cache.iterator();
}

/**
 * @param o
 * @return
 * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
 */
public synchronized int lastIndexOf(Object o) {
    return cache.lastIndexOf(o);
}

/**
 * @return
 * @see java.util.ArrayList#listIterator()
 */
public synchronized ListIterator<Integer> listIterator() {
    return cache.listIterator();
}

/**
 * @param index
 * @return
 * @see java.util.ArrayList#listIterator(int)
 */
public synchronized ListIterator<Integer> listIterator(int index) {
    return cache.listIterator(index);
}

/**
 * @param index
 * @return
 * @see java.util.ArrayList#remove(int)
 */
public synchronized Integer remove(int index) {
    return cache.remove(index);
}

/**
 * @param o
 * @return
 * @see java.util.ArrayList#remove(java.lang.Object)
 */
public synchronized boolean remove(Object o) {
    return cache.remove(o);
}

/**
 * @param c
 * @return
 * @see java.util.ArrayList#removeAll(java.util.Collection)
 */
public synchronized boolean removeAll(Collection<?> c) {
    return cache.removeAll(c);
}

/**
 * @param c
 * @return
 * @see java.util.ArrayList#retainAll(java.util.Collection)
 */
public synchronized boolean retainAll(Collection<?> c) {
    return cache.retainAll(c);
}

/**
 * @param index
 * @param element
 * @return
 * @see java.util.ArrayList#set(int, java.lang.Object)
 */
public synchronized Integer set(int index, Integer element) {
    if(element==null)
	throw new NullPointerException("Element is intolerably null.");
    return cache.set(index, element);
}

/**
 * @return
 * @see java.util.ArrayList#size()
 */
public synchronized int size() {
    return cache.size();
}

/**
 * @param fromIndex
 * @param toIndex
 * @return
 * @see java.util.ArrayList#subList(int, int)
 * @deprecated
 */
public synchronized List<Integer> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
}

/**
 * @return
 * @see java.util.ArrayList#toArray()
 */
public synchronized Object[] toArray() {
    return cache.toArray();
}

/**
 * @param a
 * @return
 * @see java.util.ArrayList#toArray(T[])
 */
public synchronized <T> T[] toArray(T[] a) {
    return cache.toArray(a);
}

/**
 * 
 * @see java.util.ArrayList#trimToSize()
 */
public synchronized void trimToSize() {
    cache.trimToSize();
}
 
 
}//end IntArrayVariableList
