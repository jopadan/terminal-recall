package org.jtrfp.trcl;

import java.awt.Dimension;
import java.io.IOException;

import org.jtrfp.jtrfp.FileLoadException;
import org.jtrfp.trcl.file.PUPFile.PowerupLocation;
import org.jtrfp.trcl.objects.BillboardSprite;


public class PowerupObject extends BillboardSprite
	{
	public PowerupObject(PowerupLocation loc, World world)
		{
		super(world.getTr());
		setBillboardSize(new Dimension(20000,20000));
		TextureDescription desc=Texture.getFallbackTexture();
		final int animationRate=500;
		try//TODO: refactor this to read from an array using an ordinal
			{
			switch(loc.getType())
				{
				case Afterburner:
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("AFTR0000.RAW"),
						frame("AFTR0001.RAW"),
						frame("AFTR0002.RAW"),
						frame("AFTR0003.RAW"),
						frame("AFTR0004.RAW"),
						frame("AFTR0005.RAW"),
						frame("AFTR0006.RAW"),
						frame("AFTR0007.RAW")
						});
					break;
				case invincibility:
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("INVI0000.RAW"),
						frame("INVI0001.RAW"),
						frame("INVI0002.RAW"),
						frame("INVI0003.RAW"),
						frame("INVI0004.RAW"),
						frame("INVI0005.RAW"),
						frame("INVI0006.RAW"),
						frame("INVI0007.RAW")
						});
					break;
				case invisibility://INVN0000.RAW
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("INVN0000.RAW"),
						frame("INVN0001.RAW"),
						frame("INVN0002.RAW"),
						frame("INVN0003.RAW"),
						frame("INVN0004.RAW"),
						frame("INVN0005.RAW"),
						frame("INVN0006.RAW"),
						frame("INVN0007.RAW")
						});
					break;
				case DAM://ART\MEGA0000.RAW aka FFF in Fury3
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("MEGA0000.RAW"),
						frame("MEGA0001.RAW"),
						frame("MEGA0002.RAW"),
						frame("MEGA0003.RAW"),
						frame("MEGA0004.RAW"),
						frame("MEGA0005.RAW"),
						frame("MEGA0006.RAW"),
						frame("MEGA0007.RAW")
						});
					break;
				case ION://ART\ANTI0000.RAW
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("ANTI0000.RAW"),
						frame("ANTI0001.RAW"),
						frame("ANTI0002.RAW"),
						frame("ANTI0003.RAW"),
						frame("ANTI0004.RAW"),
						frame("ANTI0005.RAW"),
						frame("ANTI0006.RAW"),
						frame("ANTI0007.RAW")
						});
					break;
				case MAM://AKA DOMs in Fury3
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("MISS0000.RAW"),
						frame("MISS0001.RAW"),
						frame("MISS0002.RAW"),
						frame("MISS0003.RAW"),
						frame("MISS0004.RAW"),
						frame("MISS0005.RAW"),
						frame("MISS0006.RAW"),
						frame("MISS0007.RAW")
						});
					break;		
				case Random:
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("MISS0000.RAW"),
						frame("ANTI0001.RAW"),
						frame("ENCA0002.RAW"),
						frame("PLAS0003.RAW"),
						frame("PLAS0004.RAW"),
						frame("LASE0005.RAW"),
						frame("GMIS0006.RAW"),
						frame("SUPM0007.RAW")
						});
						break;	
				case PowerCore://ART\ENCA0000.RAW
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("ENCA0000.RAW"),
						frame("ENCA0001.RAW"),
						frame("ENCA0002.RAW"),
						frame("ENCA0003.RAW"),
						frame("ENCA0004.RAW"),
						frame("ENCA0005.RAW"),
						frame("ENCA0006.RAW"),
						frame("ENCA0007.RAW")
						});
					break;
				case PAC://ART\PLAS0000.RAW
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("PLAS0000.RAW"),
						frame("PLAS0001.RAW"),
						frame("PLAS0002.RAW"),
						frame("PLAS0003.RAW"),
						frame("PLAS0004.RAW"),
						frame("PLAS0005.RAW"),
						frame("PLAS0006.RAW"),
						frame("PLAS0007.RAW")
						});
					break;
				case RTL:
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("LASE0000.RAW"),
						frame("LASE0001.RAW"),
						frame("LASE0002.RAW"),
						frame("LASE0003.RAW"),
						frame("LASE0004.RAW"),
						frame("LASE0005.RAW"),
						frame("LASE0006.RAW"),
						frame("LASE0007.RAW")
						});
					break;
				case SAD://GMIS0000.RAW
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("GMIS0000.RAW"),
						frame("GMIS0001.RAW"),
						frame("GMIS0002.RAW"),
						frame("GMIS0003.RAW"),
						frame("GMIS0004.RAW"),
						frame("GMIS0005.RAW"),
						frame("GMIS0006.RAW"),
						frame("GMIS0007.RAW")
						});
					break;
				case SWT:
				desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("SUPM0000.RAW"),
						frame("SUPM0001.RAW"),
						frame("SUPM0002.RAW"),
						frame("SUPM0003.RAW"),
						frame("SUPM0004.RAW"),
						frame("SUPM0005.RAW"),
						frame("SUPM0006.RAW"),
						frame("SUPM0007.RAW")
						});
					break;
				case shieldRestore:
					desc = new AnimatedTexture(new Sequencer(animationRate, 8, false),new Texture[]
						{
						frame("SHEI0000.RAW"),
						frame("SHEI0001.RAW"),
						frame("SHEI0002.RAW"),
						frame("SHEI0003.RAW"),
						frame("SHEI0004.RAW"),
						frame("SHEI0005.RAW"),
						frame("SHEI0006.RAW"),
						frame("SHEI0007.RAW")
						});
					break;
				}//end switch(powerup type)
			//Do something with desc
			setTexture(desc,true);
			}
		catch(Exception e)
			{e.printStackTrace();}
		}//end constructor

	private Texture frame(String name) throws IllegalAccessException, IOException, FileLoadException
		{return (Texture)getTr().getResourceManager().getRAWAsTexture(name, getTr().getGlobalPalette(), GammaCorrectingColorProcessor.singleton, getTr().getGPU().takeGL());}
	}//end PowerupObject
