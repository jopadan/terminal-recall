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
package org.jtrfp.trcl.coll;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.IteratorUtils;

public class ListActionDispatcher<E> implements List<E> {
    protected final List<E>             cache;
    protected final Map<List<E>,Object> targetsMap;
    protected final Set<List<E>>        targets;
    private final int                   startIndex, endIndex;
    
    protected ListActionDispatcher(List<E> cache, Map<List<E>, Object> targetsMap, int startIndex, int endIndex){
	this.cache=cache;
	this.targetsMap=targetsMap;
	this.targets=targetsMap.keySet();
	this.startIndex = startIndex;
	this.endIndex = endIndex;
    }
    
    public ListActionDispatcher(List<E> cache){
	this(cache,new IdentityHashMap<List<E>,Object>(), 0, Integer.MAX_VALUE);
    }//end constructor
    
    public ListActionDispatcher() {
	this(new DummyList<E>("This ListActionDispatcher has a dummylist."));
    }
    
    private boolean isRoot(){
	return startIndex==0 && endIndex==Integer.MAX_VALUE;
    }
    
    private List<E> getCache(){
	if(isRoot())
	    return cache;
	return cache.subList(Math.min(startIndex, size()), Math.min(endIndex,cache.size()));
    }//end getCache()
    
    private List<E> getSubTarget(List<E> target){
	if(isRoot())
	    return target;
	return target.subList(startIndex, endIndex);
    }

    /**
     * Registers the List 
     * to be forwarded List operations given to this dispatcher. When prefilled, the supplied target is assumed
     * to be empty when added.
     * @param target The target List to which to forward List operations.
     * @param prefill Immediately perform addAll operation to the target of the cached state.
     * @return true if this Dispatcher did not already have the given target registered.
     * @since Mar 20, 2015
     */
    public boolean addTarget(List<E> target, boolean prefill){
	if(prefill) target.addAll(cache);
	boolean result = !targets.contains(target);
	targetsMap.put(target,null);
	return result;
    }
    
    public boolean removeTarget(List<E> target, boolean removeAll){
	if(removeAll && targets.contains(target))
	    target.removeAll(cache);
	return targets.remove(target);
    }
    
    @Override
    public boolean add(E e) {
	final boolean result = cache.add(e);
	for(List<E> targ:targets)
	    targ.add(e);
	return result;
    }
    @Override
    public void add(int index, E element) {
	index+=startIndex;
	cache.add(index,element);
	for(List<E> targ:targets)
	    targ.add(index,element);
    }
    @Override
    public boolean addAll(Collection<? extends E> c) {
	return addAll(startIndex, c);
    }
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
	index += startIndex;
	final boolean result = cache.addAll(index,c);
	for(List<E> targ:targets)
	    targ.addAll(index,c);
	return result;
    }
    @Override
    public void clear() {
	if(isRoot()){
	    cache.clear();
	    for(List<E> targ:targets)
		targ.clear();
	}//end if(root)
	else{
	    final int n = endIndex - startIndex;
	    for(int i=0; i<n; i++)
	     cache.remove(startIndex);
	}//end if(!root)
    }//end clear()
    @Override
    public boolean contains(Object o) {
	return getCache().contains(o);
    }
    @Override
    public boolean containsAll(Collection<?> c) {
	return getCache().containsAll(c);
    }
    @Override
    public E get(int index) {
	index+=startIndex;
	return cache.get(index);
    }
    @Override
    public int indexOf(Object o) {
	return getCache().indexOf(o);
    }
    @Override
    public boolean isEmpty() {
	return getCache().isEmpty();
    }
    @Override
    public Iterator<E> iterator() {
	return getCache().iterator();
    }
    @Override
    public int lastIndexOf(Object o) {
	return getCache().lastIndexOf(o);
    }
    @Override
    public ListIterator<E> listIterator() {
	return IteratorUtils.toListIterator(iterator());
    }
    @Override
    public ListIterator<E> listIterator(int index) {
	return getCache().subList(index, size()).listIterator();
    }
    @Override
    public boolean remove(Object o) {
	final boolean result = getCache().remove(o);
	for(List<E> targ:targets)
	    getSubTarget(targ).remove(o);
	return result;
    }
    @Override
    public E remove(int index) {
	final E result = getCache().remove(index);
	for(List<E> targ:targets)
	    try{getSubTarget(targ).remove(index);} catch(IndexOutOfBoundsException e){}//Ignore
	return result;
    }
    @Override
    public boolean removeAll(Collection<?> c) {
	final boolean result = getCache().removeAll(c);
	for(List<E> targ:targets)
	    getSubTarget(targ).removeAll(c);
	return result;
    }
    @Override
    public boolean retainAll(Collection<?> c) {
	final boolean result = getCache().retainAll(c);
	for(List<E> targ:targets)
	    getSubTarget(targ).retainAll(c);
	return result;
    }
    @Override
    public E set(int index, E element) {
	index += startIndex;
	final E result = cache.set(index, element);
	for(List<E> targ:targets)
	    targ.set(index,element);
	return result;
    }
    @Override
    public int size() {
	return isRoot()?cache.size():endIndex-startIndex;
    }
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
	return new ListActionDispatcher<E>(cache, targetsMap, startIndex+fromIndex, startIndex+toIndex);
    }
    @Override
    public Object[] toArray() {
	return cache.toArray();
    }
    @Override
    public <T> T[] toArray(T[] a) {
	return cache.toArray(a);
    }
    
    @Override
    public int hashCode(){
	return cache.hashCode();
    }
    
    @Override
    public boolean equals(Object o){
	return cache.equals(o);
    }
}//end ListActionDispatcher
