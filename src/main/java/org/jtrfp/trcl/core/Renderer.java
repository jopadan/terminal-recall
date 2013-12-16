package org.jtrfp.trcl.core;

import java.awt.Color;
import java.io.File;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.jtrfp.jfdt.Parser;
import org.jtrfp.trcl.RenderableSpacePartitioningGrid;
import org.jtrfp.trcl.TR;
import org.jtrfp.trcl.Texture;
import org.jtrfp.trcl.TriangleList;
import org.jtrfp.trcl.gpu.GLFragmentShader;
import org.jtrfp.trcl.gpu.GLProgram;
import org.jtrfp.trcl.gpu.GLTexture;
import org.jtrfp.trcl.gpu.GLUniform;
import org.jtrfp.trcl.gpu.GLVertexShader;
import org.jtrfp.trcl.gpu.GPU;
import org.jtrfp.trcl.gpu.GlobalDynamicTextureBuffer;
import org.jtrfp.trcl.objects.WorldObject;

public class Renderer
	{
	private final Camera camera;
	private final GLProgram shaderProgram;
	private final GLUniform fogStart,fogEnd,fogColor;
	private Color _fogColor = Color.black;
	private boolean initialized=false;
	private final GPU gpu;
	
	public Renderer(GPU gpu)
		{this.gpu=gpu;this.camera=new Camera(gpu);
		final GL3 gl = gpu.getGl();
		//Fixed pipeline behavior
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glClearColor(0f, 0f, 0f, 0f);
		
		//Generate shader program
		GLVertexShader vertexShader = gpu.newVertexShader();
		GLFragmentShader fragmentShader = gpu.newFragmentShader();
		shaderProgram = gpu.newProgram();
		
		Parser p = new Parser();
		try{vertexShader.setSource(p.readUTF8FileToString(new File("texturedVertexShader.glsl")));
		fragmentShader.setSource(p.readUTF8FileToString(new File("texturedFragShader.glsl")));}
		catch(Exception e){e.printStackTrace();}
		shaderProgram.attachShader(vertexShader);
		shaderProgram.attachShader(fragmentShader);
		shaderProgram.link();
		shaderProgram.use();
		
		fogStart=shaderProgram.getUniform("fogStart");
		fogEnd=shaderProgram.getUniform("fogEnd");
		fogColor=shaderProgram.getUniform("fogColor");
		
		System.out.println("Initializing RenderList...");
		renderList = new RenderList(gl, shaderProgram);
		}
	private final RenderList renderList;
	private RenderableSpacePartitioningGrid rootGrid;
	
	private void ensureInit()
		{if(initialized) return;
		final GL3 gl = gpu.getGl();
		
		GlobalDynamicTextureBuffer.getTextureBuffer().map();
		System.out.println("Uploading vertex data to GPU...");
		TriangleList.uploadAllListsToGPU(gl);
		System.out.println("...Done.");
		System.out.println("Uploading object defintion data to GPU...");
		WorldObject.uploadAllObjectDefinitionsToGPU();
		System.out.println("...Done.");
		System.out.println("\t...World.init() complete.");
		GlobalDynamicTextureBuffer.getTextureBuffer().unmap();
		
		try{
		GlobalDynamicTextureBuffer.getTextureBuffer().bindToUniform(1, shaderProgram, shaderProgram.getUniform("rootBuffer"));
		shaderProgram.getUniform("textureMap").set((int)0);//Texture unit 0 mapped to textureMap
		}
	catch (RuntimeException e)
		{e.printStackTrace();}
	GLTexture.specifyTextureUnit(gl, 0);
	Texture.getGlobalTexture().bind(gl);
	if(!shaderProgram.validate())
		{
		System.out.println(shaderProgram.getInfoLog());
		System.exit(1);
		}
	System.out.println("...Done.");
		initialized=true;
		}//end ensureInit()
	
	public void render()
		{
		ensureInit();
		final GL3 gl = gpu.getGl();
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		final double cameraViewDepth = camera.getViewDepth();
		fogStart.set((float) (cameraViewDepth * 1.2) / 5f);
		fogEnd.set((float) (cameraViewDepth * 1.5) * 1.3f);
		shaderProgram.getUniform("fogEnd").set((float) (cameraViewDepth * 1.5) * 1.3f);
		rootGrid.itemsWithinRadiusOf(
				camera.getCameraPosition().add(
						camera.getLookAtVector().scalarMultiply(getCamera().getViewDepth() / 2.1)),
						renderList.getSubmitter());
		renderList.sendToGPU(gl);
		GlobalDynamicTextureBuffer.getTextureBuffer().unmap();

		// Render objects
		renderList.render(gl);
		}

	public void setFogColor(Color c)
		{
		fogColor.set(
				(float) _fogColor.getRed() / 255f, 
				(float) _fogColor.getGreen() / 255f, 
				(float) _fogColor.getBlue() / 255f);
		}
	/**
	 * @return the camera
	 */
	public Camera getCamera()
		{
		return camera;
		}

	/**
	 * @return the rootGrid
	 */
	public RenderableSpacePartitioningGrid getRootGrid()
		{
		return rootGrid;
		}

	/**
	 * @param rootGrid the rootGrid to set
	 */
	public void setRootGrid(RenderableSpacePartitioningGrid rootGrid)
		{
		this.rootGrid = rootGrid;
		}
	}
