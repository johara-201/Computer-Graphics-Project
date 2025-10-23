package your_code;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import app_interface.DisplayTypeEnum;
import app_interface.ExerciseEnum;
import app_interface.IntBufferWrapper;
import app_interface.ProjectionTypeEnum;

public class WorldModel {

	// type of rendering
	public ProjectionTypeEnum projectionType;
	public DisplayTypeEnum displayType;
	public boolean displayNormals;
	public YourSelectionEnum yourSelection;
	
	// camera location parameters
	public Vector3f cameraPos = new Vector3f();
	public Vector3f cameraLookAtCenter = new Vector3f();
	public Vector3f cameraUp = new Vector3f();
	public float horizontalFOV;

	// transformation parameters
	public float modelScale;

	// lighting parameters
	public float lighting_Diffuse;
	public float lighting_Specular;
	public float lighting_Ambient;
	public float lighting_sHininess;
	public Vector3f lightPositionWorldCoordinates = new Vector3f();
	
	public ExerciseEnum exercise;

	private int imageWidth;
	private int imageHeight;

	private ObjectModel object1;
	
	float zBuffer[][];
	
	private int counter = 0;
	
	public WorldModel(int imageWidth, int imageHeight) {
		this.imageWidth  = imageWidth;
		this.imageHeight = imageHeight;
		this.zBuffer = new float[imageWidth][imageHeight];
	}


	public boolean load(String fileName) {
		object1 = new ObjectModel(this, imageWidth, imageHeight);
		return object1.load(fileName);
	}
	
	public boolean modelHasTexture() {
		return object1.objectHasTexture();
	}
	
	public void render(IntBufferWrapper intBufferWrapper) {
		counter+=1;
		intBufferWrapper.imageClear();
		clearZbuffer();
		object1.initTransfomations();

		if (exercise.ordinal() == ExerciseEnum.EX_3_1_Object_transformation___translation.ordinal()) {
			
			  //translation matrix moves object back and forth
	        float translate = (float)(Math.sin(counter * 0.05) * 100); // between -100 and 100
	        Matrix4f translateMatrix = new Matrix4f().translate(translate, 0, 0);
	        object1.setModelM(translateMatrix);
		}
	
		if (exercise.ordinal() == ExerciseEnum.EX_3_2_Object_transformation___scale.ordinal()) {
			
			//scale factor between 0.9 and 1.1
	        float scale = 1.0f + (float)(Math.sin(counter * 0.05) * 0.1); 
	        Matrix4f scaleMatrix = new Matrix4f().scale(scale);
	        object1.setModelM(scaleMatrix);

		}

		if (exercise.ordinal() == ExerciseEnum.EX_3_3_Object_transformation___4_objects.ordinal()) {
			
			//4 scaled and translated objects
			
			//half size
		    float scale = 0.5f; 
	        Matrix4f baseScale = new Matrix4f().scale(scale);
	        
	        // Top left object
	        //Matrix4f topLeft = new Matrix4f(baseScale).translate(-10, 600, 0);
	        Matrix4f topLeft = new Matrix4f(baseScale).translate(0, 600, 0);
	        object1.setModelM(topLeft);
	        object1.render(intBufferWrapper);

	        // Top right object
	        Matrix4f topRight = new Matrix4f(baseScale).translate(600, 600, 0);
	        object1.setModelM(topRight);
	        object1.render(intBufferWrapper);

	        // Bottom left object
	        //Matrix4f bottomLeft = new Matrix4f(baseScale).translate(-10, -20, 0);
	        Matrix4f bottomLeft = new Matrix4f(baseScale).translate(0, 0, 0);
	        object1.setModelM(bottomLeft);
	        object1.render(intBufferWrapper);

	        // Bottom right object 
	        //Matrix4f bottomRight = new Matrix4f(baseScale).translate(600, -20, 0);
	        Matrix4f bottomRight = new Matrix4f(baseScale).translate(600, 0, 0);
	        object1.setModelM(bottomRight);
			

		}

		if(projectionType==ProjectionTypeEnum.ORTHOGRAPHIC) {
			
			object1.setProjectionM(new Matrix4f().ortho(-1.5f, 1.5f, -1.5f, 1.5f, 0, 100f));

		}
		
		//here - ex 4
		Matrix4f viewPort=YoursUtilities.createViewportMatrix(0, 0, imageWidth, imageHeight);
		object1.setViewportM(new Matrix4f(viewPort));
		
		//ex5 - section a 
		Matrix4f lookAtMatrix = new Matrix4f().lookAt(
		        cameraPos,           
		        cameraLookAtCenter,  
		        cameraUp             
		    );
		    object1.setLookatM(lookAtMatrix);

		if(projectionType==ProjectionTypeEnum.PERSPECTIVE) {
			//ex 6
		    float aspect = 1.0f; // imageWidth = imageHeight;  
		    float fovY = (float) Math.toRadians(30); 
		    float zNear = 1.0f; 
		    float zFar = 100.0f;    

		    Matrix4f perspectiveMatrix = new Matrix4f().perspective(fovY, aspect, zNear, zFar);

		    object1.setProjectionM(perspectiveMatrix);
  
		}
		
		object1.render(intBufferWrapper);
	}
	
	private void clearZbuffer() {
		for(int i=0; i<imageHeight; i++)
			for(int j=0; j<imageWidth; j++)
				zBuffer[i][j] = 1;
	}	
}
