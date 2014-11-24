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

import java.beans.Transient;
import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;

import javax.swing.DefaultListModel;

import org.jtrfp.trcl.flow.GameVersion;

public class TRConfiguration{
    	private GameVersion gameVersion=GameVersion.F3;
    	private Boolean usingTextureBufferUnmap,
    			debugMode,
    			waitForProfiler;
    	private int targetFPS =60;
    	private String skipToLevel;
    	private String voxFile;
    	private boolean audioLinearFiltering=false;
    	private HashSet<String> missionList = new HashSet<String>();
    	//private HashSet<String> podList     = new HashSet<String>();
    	private DefaultListModel<String> podList=new DefaultListModel<String>();
    	private double modStereoWidth=.3;
    	public static final String AUTO_DETECT = "Auto-detect";
    	
	public TRConfiguration(){//DEFAULTS
	    missionList.add(AUTO_DETECT);
	    missionList.add("Fury3");
	    missionList.add("TV");
	    missionList.add("FurySE");
	}

	@Transient
	public GameVersion getGameVersion() {
	    return gameVersion;
	}
	
	@Transient
	public void setGameVersion(GameVersion gameVersion){
	    this.gameVersion=gameVersion;
	}
	
	public boolean isUsingTextureBufferUnmap() {
	    if(usingTextureBufferUnmap!=null)return usingTextureBufferUnmap;
	    boolean result=true;
	    if(System.getProperties().containsKey("org.jtrfp.trcl.Renderer.unmapTextureBuffer")){
		if(System.getProperty("org.jtrfp.trcl.Renderer.unmapTextureBuffer").toUpperCase().contains("FALSE"))
		    result=false;
	    }//end if(contains key)
	    usingTextureBufferUnmap=result;
	    return result;
	}//end isUsingTextureBufferUnmap()
	
	public boolean isDebugMode() {
	    if(debugMode!=null)return debugMode;
	    boolean result=false;
	    if(System.getProperties().containsKey("org.jtrfp.trcl.debugMode")){
		if(System.getProperty("org.jtrfp.trcl.debugMode").toUpperCase().contains("TRUE"))
		    result=true;
	    }//end if(contains key)
	    debugMode=result;
	    return result;
	}//end isDebugMode()

	public int getTargetFPS() {
	    return targetFPS;
	}//end getTargetFPS()

	public String skipToLevel() {
	    if(skipToLevel==null){
		 skipToLevel = System.getProperty("org.jtrfp.trcl.flow.Game.skipToLevel");
	    }//end if(skipToLevel==null)
	    return skipToLevel;
	}//end skipToLevel

	/**
	 * @return the voxFile
	 */
	public String getVoxFile() {
	    String result = voxFile;
	    if(result==null)
		result = voxFile = AUTO_DETECT;
	    return result;
	}

	/**
	 * @param voxFile the voxFile to set
	 */
	public void setVoxFile(String voxFile) {
	    this.voxFile = voxFile;
	}

	public boolean isWaitForProfiler() {
	    if(waitForProfiler!=null)return waitForProfiler;
	    boolean result=false;
	    if(System.getProperties().containsKey("org.jtrfp.trcl.dbg.waitForProfiler")){
		if(System.getProperty("org.jtrfp.trcl.dbg.waitForProfiler").toUpperCase().contains("TRUE"))
		    result=true;
	    }//end if(contains key)
	    waitForProfiler=result;
	    return result;
	}

	/**
	 * @return the audioLinearFiltering
	 */
	public boolean isAudioLinearFiltering() {
	    return audioLinearFiltering;
	}

	/**
	 * @param audioLinearFiltering the audioLinearFiltering to set
	 */
	public void setAudioLinearFiltering(boolean audioLinearFiltering) {
	    this.audioLinearFiltering = audioLinearFiltering;
	}

	/**
	 * @return the missionList
	 */
	public HashSet<String> getMissionList() {
	    return missionList;
	}

	/**
	 * @param missionList the missionList to set
	 */
	public void setMissionList(HashSet<String> missionList) {
	    this.missionList = missionList;
	}

	/**
	 * @return the modStereoWidth
	 */
	public double getModStereoWidth() {
	    return modStereoWidth;
	}

	/**
	 * @param modStereoWidth the modStereoWidth to set
	 */
	public void setModStereoWidth(double modStereoWidth) {
	    this.modStereoWidth = modStereoWidth;
	}
	
	public static File getConfigFilePath(){
	     String homeProperty = System.getProperty("user.home");
	     if(homeProperty==null)homeProperty="";
	     return new File(homeProperty+File.separator+"settings.config.trcl.xml");
	 }
	
	public static TRConfiguration getConfig(){
	     TRConfiguration result=null;
	     File fp = TRConfiguration.getConfigFilePath();
	     if(fp.exists()){
		 try{FileInputStream is = new FileInputStream(fp);
		    XMLDecoder xmlDec = new XMLDecoder(is);
		    result=(TRConfiguration)xmlDec.readObject();
		    xmlDec.close();
		    is.close();
		}catch(Exception e){e.printStackTrace();}
	     }//end if(exists)
	     if(result==null)
		result = new TRConfiguration();
	     return result;
	 }//end getConfig()

	/**
	 * @return the podList
	 */
	public DefaultListModel getPodList() {
	    return podList;
	}

	/**
	 * @param podList the podList to set
	 */
	public void setPodList(DefaultListModel podList) {
	    this.podList = podList;
	}
}//end TRConfiguration
