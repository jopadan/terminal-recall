package org.jtrfp.trcl.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

import org.jtrfp.trcl.gpu.GLTexture;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.pool.IndexPool;

public class VQCodebookManager {
    private final 	IndexPool 	codebook256Indices = new IndexPool();
    private final 	GLTexture 	rgbaTexture,esTuTvTexture,indentationTexture;
    public static final int 		CODE_PAGE_SIDE_LENGTH_TEXELS	=128;
    public static final int 		CODE_SIDE_LENGTH		=4;
    public static final int 		NUM_CODES_PER_AXIS		=CODE_PAGE_SIDE_LENGTH_TEXELS/CODE_SIDE_LENGTH;
    public static final int 		NUM_CODE_PAGES			=1024;
    public static final int 		CODES_PER_PAGE 			=NUM_CODES_PER_AXIS*NUM_CODES_PER_AXIS;
    public static final int		MIP_DEPTH			=1;

    public VQCodebookManager(TR tr) {
	final GPU gpu = tr.gpu.get();
	rgbaTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{CODE_PAGE_SIDE_LENGTH_TEXELS,CODE_PAGE_SIDE_LENGTH_TEXELS,NUM_CODE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
	esTuTvTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{CODE_PAGE_SIDE_LENGTH_TEXELS,CODE_PAGE_SIDE_LENGTH_TEXELS,NUM_CODE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
	indentationTexture = gpu.
		newTexture().
		setBindingTarget(GL3.GL_TEXTURE_2D_ARRAY).
		bind().
		setInternalColorFormat(GL3.GL_RGBA4).
		configure(new int[]{CODE_PAGE_SIDE_LENGTH_TEXELS,CODE_PAGE_SIDE_LENGTH_TEXELS,NUM_CODE_PAGES}, 1).
		setMagFilter(GL3.GL_LINEAR).
		setMinFilter(GL3.GL_LINEAR).
		setWrapS(GL3.GL_CLAMP_TO_EDGE).
		setWrapT(GL3.GL_CLAMP_TO_EDGE);
    }//end constructor

    public VQCodebookManager setRGBA(int codeID, ByteBuffer rgba) {
	subImageAutoMip(codeID,rgba,rgbaTexture,4);
	return this;
    }// end setRGBA(...)

    public VQCodebookManager setESTuTv(int codeID, ByteBuffer ESTuTv) {
	subImageAutoMip(codeID,ESTuTv,esTuTvTexture,4);
	return this;
    }// end setECTuTv(...)

    public VQCodebookManager setIndentation(int codeID, ByteBuffer indentation) {
	subImageAutoMip(codeID,indentation,indentationTexture,1);
	return this;
    }// end setProtrusion(...)

    private void subImage(final int codeID, final ByteBuffer texels,
	    final GLTexture tex, int mipLevel) {
	final int x = (codeID % NUM_CODES_PER_AXIS)*CODE_SIDE_LENGTH;
	final int z = codeID / CODES_PER_PAGE;
	final int y = ((codeID % CODES_PER_PAGE) / NUM_CODES_PER_AXIS)*CODE_SIDE_LENGTH;
	texels.clear();
	if(z>NUM_CODE_PAGES){
	    throw new OutOfMemoryError("Ran out of codebook pages. Requested index to write: "+z+" max: "+NUM_CODE_PAGES);
	}
	if(x>CODE_PAGE_SIDE_LENGTH_TEXELS || y > CODE_PAGE_SIDE_LENGTH_TEXELS ){
	    throw new RuntimeException("One or more texel coords intolerably out of range: x="+x+" y="+y);
	}
	tex.bind().subImage(new int[] { x, y, z },
		new int[] { CODE_SIDE_LENGTH, CODE_SIDE_LENGTH, 1 }, GL3.GL_RGBA,
		0, texels);
    }// end subImage(...)

    private void subImageAutoMip(final int codeID, final ByteBuffer texels,
	    final GLTexture tex, int byteSizedComponentsPerTexel) {
	ByteBuffer wb = ByteBuffer.allocate(texels.capacity());
	ByteBuffer intermediate = ByteBuffer.allocate(texels.capacity() / 4);
	texels.clear();wb.clear();
	wb.put(texels);
	int sideLen = (int)Math.sqrt(texels.capacity() / byteSizedComponentsPerTexel);
	for (int mipLevel = 0; mipLevel < MIP_DEPTH; mipLevel++) {
	    wb.clear();
	    subImage(codeID, wb, tex, mipLevel);
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
    }// end newCODE()

    public void releaseCodebook256(int codebook256ToRelease) {
	codebook256Indices.free(codebook256ToRelease);
    }// end releaseCODE(...)
    
    public GLTexture getRGBATexture()		{return rgbaTexture;}
    public GLTexture getESTuTvTexture()		{return esTuTvTexture;}
    public GLTexture getIndentationTexture()	{return indentationTexture;}

    public ByteBuffer []dumpPagesToBuffer() throws IOException {
	ByteBuffer buf = ByteBuffer.allocate(4 * CODE_PAGE_SIDE_LENGTH_TEXELS * CODE_PAGE_SIDE_LENGTH_TEXELS * NUM_CODE_PAGES);
	final ByteBuffer[] result = new ByteBuffer[NUM_CODE_PAGES];
	for(int pg=0; pg<NUM_CODE_PAGES; pg++){
	    buf.position(4 * CODE_PAGE_SIDE_LENGTH_TEXELS * CODE_PAGE_SIDE_LENGTH_TEXELS * pg);
	    buf.limit(4 * CODE_PAGE_SIDE_LENGTH_TEXELS * CODE_PAGE_SIDE_LENGTH_TEXELS * (pg+1));
	    result[pg]=buf.slice();
	    result[pg].clear();
	}
	rgbaTexture.getTextureImageRGBA(buf);
	return result;
    }//end dumpPageToPNG(...)

}// end VQCodebookManager
