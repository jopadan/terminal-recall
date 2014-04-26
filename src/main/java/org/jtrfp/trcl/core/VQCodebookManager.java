package org.jtrfp.trcl.core;

import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

import org.jtrfp.trcl.gpu.GLTexture;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.pool.IndexPool;

public class VQCodebookManager {
    private final 	IndexPool 	codebook256Indices = new IndexPool();
    private final 	GLTexture 	rgbaTexture,esTuTvTexture,indentationTexture;
    public static final int 		TILE_PAGE_SIDE_LENGTH_TEXELS	=128;
    public static final int 		TILE_SIDE_LENGTH		=4;
    public static final int 		NUM_TILES_PER_AXIS		=TILE_PAGE_SIDE_LENGTH_TEXELS/TILE_SIDE_LENGTH;
    public static final int 		NUM_TILE_PAGES			=128;
    public static final int 		TILES_PER_PAGE 			=NUM_TILES_PER_AXIS*NUM_TILES_PER_AXIS;
    public static final int		MIP_DEPTH			=1;

    public VQCodebookManager(TR tr) {
	final GPU gpu = tr.getGPU();
	rgbaTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{TILE_PAGE_SIDE_LENGTH_TEXELS,TILE_PAGE_SIDE_LENGTH_TEXELS,NUM_TILE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
	esTuTvTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{TILE_PAGE_SIDE_LENGTH_TEXELS,TILE_PAGE_SIDE_LENGTH_TEXELS,NUM_TILE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
	indentationTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{TILE_PAGE_SIDE_LENGTH_TEXELS,TILE_PAGE_SIDE_LENGTH_TEXELS,NUM_TILE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
    }//end constructor

    public VQCodebookManager setRGBA(int tileID, ByteBuffer rgba) {
	subImageAutoMip(tileID,rgba,rgbaTexture,4);
	return this;
    }// end setRGBA(...)

    public VQCodebookManager setESTuTv(int tileID, ByteBuffer ESTuTv) {
	subImageAutoMip(tileID,ESTuTv,esTuTvTexture,4);
	return this;
    }// end setECTuTv(...)

    public VQCodebookManager setIndentation(int tileID, ByteBuffer indentation) {
	subImageAutoMip(tileID,indentation,indentationTexture,1);
	return this;
    }// end setProtrusion(...)

    private void subImage(final int tileID, final ByteBuffer texels,
	    final GLTexture tex, int mipLevel) {
	final int x = tileID % NUM_TILES_PER_AXIS;
	final int z = tileID / TILES_PER_PAGE;
	final int y = (tileID % TILES_PER_PAGE) / NUM_TILES_PER_AXIS;
	texels.clear();
	tex.bind().subImage(new int[] { x, y, z },
		new int[] { TILE_SIDE_LENGTH, TILE_SIDE_LENGTH, 1 }, GL3.GL_RGBA,
		0, texels);
    }// end subImage(...)

    private void subImageAutoMip(final int tileID, final ByteBuffer texels,
	    final GLTexture tex, int byteSizedComponentsPerTexel) {
	ByteBuffer wb = ByteBuffer.allocate(texels.capacity());
	ByteBuffer intermediate = ByteBuffer.allocate(texels.capacity() / 4);
	texels.clear();
	wb.put(texels);
	int sideLen = (int)Math.sqrt(texels.capacity() / byteSizedComponentsPerTexel);
	for (int mipLevel = 0; mipLevel < MIP_DEPTH; mipLevel++) {
	    subImage(tileID, texels, tex, mipLevel);
	    mipDown(wb, intermediate, sideLen, byteSizedComponentsPerTexel);
	    wb.clear();
	    intermediate.clear();
	    wb.put(intermediate);
	}// end for(mipLevel)
    }// end subImageAutoMip(...)

    private void mipDown(ByteBuffer in, ByteBuffer out, int sideLen,
	    int componentsPerTexel) {
	int outX, outY, inX, inY, inIndex, outIndex;
	final int newSideLen = sideLen / 2;
	for (int y = 0; y < newSideLen; y++)
	    for (int x = 0; x < newSideLen; x++) {
		inX = x * 2;
		inY = y * 2;
		outX = x;
		outY = y;
		int component = 0;
		for (int cIndex = 0; cIndex < componentsPerTexel; cIndex++) {
		    for (int sy = 0; sy < 2; sy++)
			for (int sx = 0; sx < 2; sx++) {
			    inIndex = ((inX + sx) + (inY + sy) * sideLen);
			    inIndex *= componentsPerTexel;
			    inIndex += cIndex;
			    component += in.get(inIndex);
			}// end for(sx)
		    outIndex = (outX + outY * newSideLen);
		    outIndex *= componentsPerTexel;
		    outIndex += cIndex;
		    component /= 4;
		    out.put(outIndex, (byte) component);
		}// end for(cIndex)
	    }// end for(x)
    }// end mipDown(...)

    public int newCodebook256() {
	return codebook256Indices.pop();
    }// end newTile()

    public void releaseTile(int tileToRelease) {
	codebook256Indices.free(tileToRelease);
    }// end releaseTile(...)
    
    public GLTexture getRGBATexture()		{return rgbaTexture;}
    public GLTexture getESTuTvTexture()		{return esTuTvTexture;}
    public GLTexture getIndentationTexture()	{return indentationTexture;}

}// end TextureTileManager
