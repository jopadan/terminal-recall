package org.jtrfp.trcl.gpu;

import java.awt.Component;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import org.jtrfp.trcl.World;

import com.jogamp.opengl.util.FPSAnimator;

public class GPU
	{
	static {GLProfile.initSingleton();}
	private final GLProfile glProfile = GLProfile.get(GLProfile.GL2GL3);
	private final GLCapabilities capabilities = new GLCapabilities(glProfile);
	private final GLCanvas canvas = new GLCanvas(capabilities);
	private ByteOrder byteOrder;
	
	public static final int FPS=60;
	
	private GL3 gl;
	public GL3 takeGL()
		{
		if(gl==null)
			{GL gl1;
			//In case GL is not ready, wait and try again.
			try{for(int i=0; i<10; i++){gl1=canvas.getGL();if(gl1!=null)
				{gl=gl1.getGL3();
				canvas.setGL(gl=new DebugGL3(gl));
				break;
				} Thread.sleep(100);}}
			catch(InterruptedException e){e.printStackTrace();}
			}//end if(!null)
		if(!gl.getContext().isCurrent())gl.getContext().makeCurrent();
		return gl;
		}
	public void releaseGL()
		{if(gl.getContext().isCurrent())gl.getContext().release();}
	
	public GLCapabilities getCapabilities(){return capabilities;}
	
	public int glGet(int key)
		{
		IntBuffer buf = IntBuffer.wrap(new int[1]);
		gl.glGetIntegerv(key, buf);
		return buf.get(0);
		}
	
	public String glGetString(int key)
		{
		return gl.glGetString(key);
		}
	
	public Component getComponent(){return canvas;}
	
	private final FPSAnimator animator = new FPSAnimator(canvas,FPS);
	
	public ByteOrder getByteOrder()
		{
		if(byteOrder==null)
			{byteOrder = System.getProperty("sun.cpu.endian").contentEquals("little")?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN;}
		return byteOrder;
		}
	public void addGLEventListener(GLEventListener l)
		{canvas.addGLEventListener(l);}
	public void startAnimator()
		{
		animator.start();
		}
	public int getFrameRate(){return FPS;}
	public GLTexture newTexture()
		{return new GLTexture(this);}
	public int newTextureID()
		{
		IntBuffer ib= IntBuffer.allocate(1);
		gl.glGenTextures(1, ib);
		ib.clear();
		return ib.get();
		}
	public GLFragmentShader newFragmentShader()
		{return new GLFragmentShader(this);}
	public GLVertexShader newVertexShader()
		{return new GLVertexShader(this);}
	public GLProgram newProgram()
		{return new GLProgram(this);}
	}//end GPU
