terminal-recall
===============
![build status](https://travis-ci.org/jtrfp/terminal-recall.svg?branch=master)

An unofficial engine remake for Terminal Velocity and Fury3 written in Java. It aims to accept POD files for the original games or third-party creations and play them with updated graphics and sound, similar to the way Spring RTS enhances the Total Annihilation game.

See files also: CREDITS, COPYING, LICENSE*

Terminal Recall (TRCL) is part of the Java Terminal Reality File Parsers project (jTRFP.org).

TRCL is currently in a playable development phase. Because it is not yet close to being released (it changes too much), it must be built from source.

##Not A Source-Port
This is a [cleanroom](https://en.wikipedia.org/wiki/Clean_room_design) re-implementation of the engine based on reverse-engineering specs.

##Build Instructions
You will need:
* Java JDK 7 or later (http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html)
* Git (http://git-scm.com/downloads)
* Maven (https://maven.apache.org/download.cgi)
* nVIDIA, Intel, or AMD (fglrx if using linux) video card with drivers capable of OpenGL 3.3.
* Windows or Linux.

In a folder/directory of your choice run:
```
git clone https://github.com/jtrfp/terminal-recall.git
```
... this will create a directory called terminal-recall and download the project to that directory.
```
    cd terminal-recall
    mvn clean install
```

... there should be a lot of output as it builds. Nothing is actually 'install'ed.
Expect to see the message 'BUILD SUCCESS'. If it outputs BUILD FAILURE come to the Terminal Recall gitHub issues section (https://github.com/jtrfp/terminal-recall/issues?page=1), search for an existing issue matching yours, or file a new issue and post the output and we'll hopefully figure it out.

Example:
```
    cd target
    java -jar RunMe.jar
```
The POD files should be registered via File->Configure menu such that the game can find them. This only needs to be done once so long as the config file isn't corrupted or lost. Note that the original games need at least STARTUP.POD and the specific POD for the game, such as FURY3.POD or TV.POD.

If its been a few weeks since first downloading and you wish to update to the newest version, cd into the  terminal-recall directory and run:

```
    git pull
    mvn clean install
```

... this will only download whatever has changed since the last download and rebuild the program appropriately.

If the dash in the terminal-recall directory name is an inconvenience in Windows it can be renamed without issue after downloading.

If moving the RunMe.jar file to another location, be sure to also move the 'lib' directory. All other files are unneeded for running.


##Disclaimer

This project and its contributors are not affiliated with nor represent Microsoft, Apogee Software Ltd. (3DRealms) or Terminal Reality Inc. Terminal Recall is not supported by any of the aforementioned. Terminal Velocity, and their original content are property of Terminal Reality Inc. Always support the original content creators when possible.

##Trademarks

Microsoft is a registered trademark of the Microsoft Corporation. Fury3 was a registered trademark of the Microsoft Corporation, canceled in 2003. Terminal Reality and Terminal Velocity are registered trademarks of Terminal Reality Inc. 3DRealms is a registered trademark of Apogee Software Ltd.
