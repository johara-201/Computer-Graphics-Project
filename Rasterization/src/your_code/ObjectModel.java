package your_code;

import java.io.IOException;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;

import app_interface.DisplayTypeEnum;
import app_interface.ExerciseEnum;
import app_interface.IntBufferWrapper;
import app_interface.OBJLoader;
import app_interface.TriangleFace;

//changes by Orian and Johara
public class ObjectModel {
	WorldModel worldModel;

	private int imageWidth;
	private int imageHeight;

	private List<VertexData> verticesData;
	private List<TriangleFace> faces;
	private IntBufferWrapper textureImageIntBufferWrapper;

	private Matrix4f modelM = new Matrix4f();
	private Matrix4f lookatM = new Matrix4f();
	private Matrix4f projectionM = new Matrix4f();
	private Matrix4f viewportM = new Matrix4f();
	private Vector3f boundingBoxDimensions;
	private Vector3f boundingBoxCenter;

	private Vector3f lightPositionEyeCoordinates = new Vector3f();

	public static ExerciseEnum exercise = ExerciseEnum.EX_9___Lighting;

	public ObjectModel(WorldModel worldModel, int imageWidth, int imageHeight) {
		this.worldModel = worldModel;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
	}

	void initTransfomations() {
		this.modelM.identity();
		this.modelM.identity();
		this.lookatM.identity();
		this.projectionM.identity();
		this.viewportM.identity();
	}

	void setModelM(Matrix4f modelM) {
		this.modelM = modelM;
	}

	void setLookatM(Matrix4f lookatM) {
		this.lookatM = lookatM;
	}

	void setProjectionM(Matrix4f projectionM) {
		this.projectionM = projectionM;
	}

	void setViewportM(Matrix4f viewportM) {
		this.viewportM = viewportM;
	}

	public Vector3f getBoundingBoxDimensions() {
		return boundingBoxDimensions;
	}

	public Vector3f getBoundingBoxCenter() {
		return boundingBoxCenter;
	}

	public boolean load(String fileName) {
		OBJLoader objLoader = new OBJLoader();
		try {
			objLoader.loadOBJ(fileName);
			verticesData = objLoader.getVertices();
			faces = objLoader.getFaces();
			boundingBoxDimensions = objLoader.getBoundingBoxDimensions();
			boundingBoxCenter = objLoader.getBoundingBoxCenter();
			textureImageIntBufferWrapper = objLoader.getTextureImageIntBufferWrapper();
			return true;
		} catch (IOException e) {
			// System.err.println("Failed to load the OBJ file.");
			return false;
		}
	}

	public boolean objectHasTexture() {
		return textureImageIntBufferWrapper != null;
	}

	public void render(IntBufferWrapper intBufferWrapper) {
		exercise = worldModel.exercise;

		// exercise 9.1
		Vector4f copyLightPosition=new Vector4f(worldModel.lightPositionWorldCoordinates.x,worldModel.lightPositionWorldCoordinates.y,worldModel.lightPositionWorldCoordinates.z,1);
		lookatM.transform(copyLightPosition); //calc the transformation and store in copy
		lightPositionEyeCoordinates = new Vector3f((copyLightPosition.x/copyLightPosition.w),(copyLightPosition.y/copyLightPosition.w),(copyLightPosition.z/copyLightPosition.w));
		//og code
		if (verticesData != null) {
			for (VertexData vertexData : verticesData) {
				vertexProcessing(intBufferWrapper, vertexData);
			}
			for (TriangleFace face : faces) {
				rasterization(intBufferWrapper, verticesData.get(face.indices[0]), verticesData.get(face.indices[1]),
						verticesData.get(face.indices[2]), face.color);
			}
		}
	}

	private void vertexProcessing(IntBufferWrapper intBufferWrapper, VertexData vertex) {

		// Initialize a 4D vector from the 3D vertex point
		Vector4f t = new Vector4f(vertex.pointObjectCoordinates, 1f);
		// Transform the vector t
		modelM.transform(t); // move to eye coordinates = in front of the camera
		lookatM.transform(t);
		vertex.pointEyeCoordinates = new Vector3f(t.x, t.y, t.z);
		// projection
		projectionM.transform(t); // cut out what's not in our view (limit the picture to be between 1 and -1)
		Vector4f ndc = new Vector4f((t.x) / (t.w), (t.y) / (t.w), (t.z) / (t.w), t.w); // normalize
		viewportM.transform(ndc); // fit the model to our screen
		vertex.pointWindowCoordinates = new Vector3f(ndc.x, ndc.y, ndc.z);

		// transformation normal from object coordinates to eye coordinates v->normal
		///////////////////////////////////////////////////////////////////////////////////
		transformNormalFromObjectCoordToEyeCoordAndDrawIt(intBufferWrapper, vertex);
		
		//exercise 9.2- gourard shading
		float temp= lightingEquation(vertex.pointEyeCoordinates, vertex.normalEyeCoordinates, lightPositionEyeCoordinates, worldModel.lighting_Diffuse, worldModel.lighting_Specular, worldModel.lighting_Ambient, worldModel.lighting_sHininess);
		vertex.lightingIntensity0to1=temp;

	}

	private void transformNormalFromObjectCoordToEyeCoordAndDrawIt(IntBufferWrapper intBufferWrapper,
			VertexData vertex) {
		// transformation normal from object coordinates to eye coordinates v->normal
		///////////////////////////////////////////////////////////////////////////////////
		// --> v->NormalEyeCoordinates
		Matrix4f modelviewM = new Matrix4f(lookatM).mul(modelM);
		Matrix3f modelviewM3x3 = new Matrix3f();
		modelviewM.get3x3(modelviewM3x3);
		vertex.normalEyeCoordinates = new Vector3f();
		modelviewM3x3.transform(vertex.normalObjectCoordinates, vertex.normalEyeCoordinates);
		if (worldModel.displayNormals) {
			// drawing normals
			Vector3f t1 = new Vector3f(vertex.normalEyeCoordinates);
			Vector4f point_plusNormal_eyeCoordinates = new Vector4f(t1.mul(0.1f).add(vertex.pointEyeCoordinates), 1);
			Vector4f t2 = new Vector4f(point_plusNormal_eyeCoordinates);
			// modelviewM.transform(t2);
			projectionM.transform(t2);
			if (t2.w != 0) {
				t2.mul(1 / t2.w);
			} else {
				System.err.println("Division by w == 0 in vertexProcessing normal transformation");
			}
			viewportM.transform(t2);
			Vector3f point_plusNormal_screen = new Vector3f(t2.x, t2.y, t2.z);
			drawLineDDA(intBufferWrapper, vertex.pointWindowCoordinates, point_plusNormal_screen, 0, 0, 1f);
		}

	}

	
	
	private void rasterization(IntBufferWrapper intBufferWrapper, VertexData vertex1, VertexData vertex2,
			VertexData vertex3, Vector3f faceColor) {

		Vector3f faceNormal = new Vector3f(vertex2.pointEyeCoordinates).sub(vertex1.pointEyeCoordinates)
					.cross(new Vector3f(vertex3.pointEyeCoordinates).sub(vertex1.pointEyeCoordinates))
					.normalize();

		if (worldModel.displayType == DisplayTypeEnum.FACE_EDGES) {
			intBufferWrapper.setPixel((int) vertex1.pointWindowCoordinates.x, (int) vertex1.pointWindowCoordinates.y,
					1f, 1f, 1f);
			intBufferWrapper.setPixel((int) vertex2.pointWindowCoordinates.x, (int) vertex2.pointWindowCoordinates.y,
					1f, 1f, 1f);
			intBufferWrapper.setPixel((int) vertex3.pointWindowCoordinates.x, (int) vertex3.pointWindowCoordinates.y,
					1f, 1f, 1f);

			drawLineDDA(intBufferWrapper, vertex1.pointWindowCoordinates, vertex2.pointWindowCoordinates, 1, 1, 1);
			drawLineDDA(intBufferWrapper, vertex1.pointWindowCoordinates, vertex3.pointWindowCoordinates, 1, 1, 1);
			drawLineDDA(intBufferWrapper, vertex2.pointWindowCoordinates, vertex3.pointWindowCoordinates, 1, 1, 1);

		} else {
			BarycentricCoordinates bc = new BarycentricCoordinates(vertex1.pointWindowCoordinates,
					vertex2.pointWindowCoordinates, vertex3.pointWindowCoordinates);
			Vector4i box = calcBoundingBox(vertex1.pointWindowCoordinates, vertex2.pointWindowCoordinates,
					vertex3.pointWindowCoordinates, imageWidth, imageHeight);
			
			//exercise 9.1
			float temp= lightingEquation(vertex1.pointEyeCoordinates, faceNormal, lightPositionEyeCoordinates, worldModel.lighting_Diffuse, worldModel.lighting_Specular, worldModel.lighting_Ambient, worldModel.lighting_sHininess);
			//end of 9.1
			
			for (int i = box.x; i < box.y; i++) // goes over the pixels from minX to MaxX
			{
				for (int j = box.z; j < box.w; j++)// goes over the pixels from minY to MaxY
				{
					bc.calcCoordinatesForPoint(i, j); // Computes and updates the barycentric coordinates
					if (bc.isPointInside()) {
						FragmentData fragmentData = new FragmentData();
						if (worldModel.displayType == DisplayTypeEnum.FACE_COLOR) {
							fragmentData.pixelColor = faceColor;
						}

						else if (worldModel.displayType == DisplayTypeEnum.INTERPOlATED_VERTEX_COLOR) {
							// exercise 7
							// we've already calculated the barycentric coordinates before, all thats left
							// is to interpolate
							fragmentData.pixelColor = bc.interpolate(vertex1.color, vertex2.color, vertex3.color);
						}

						else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_FLAT) {
							fragmentData.pixelIntensity0to1=temp;
						}
						else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_GOURARD) {
							fragmentData.pixelIntensity0to1=bc.interpolate(vertex1.lightingIntensity0to1, vertex2.lightingIntensity0to1, vertex3.lightingIntensity0to1);
						} 
						else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_PHONG) {
							fragmentData.normalEyeCoordinates=bc.interpolate(vertex1.normalEyeCoordinates,vertex2.normalEyeCoordinates, vertex3.normalEyeCoordinates).normalize();
							fragmentData.pointEyeCoordinates=bc.interpolate(vertex1.pointEyeCoordinates,vertex2.pointEyeCoordinates , vertex3.pointEyeCoordinates);
						
						} else if (worldModel.displayType == DisplayTypeEnum.TEXTURE) {
							fragmentData.textureCoordinates=bc.interpolate(vertex1.textureCoordinates,vertex2.textureCoordinates, vertex3.textureCoordinates);
							
						} else if (worldModel.displayType == DisplayTypeEnum.TEXTURE_LIGHTING) {
							fragmentData.normalEyeCoordinates=bc.interpolate(vertex1.normalEyeCoordinates,vertex2.normalEyeCoordinates, vertex3.normalEyeCoordinates).normalize();
							fragmentData.pointEyeCoordinates=bc.interpolate(vertex1.pointEyeCoordinates,vertex2.pointEyeCoordinates , vertex3.pointEyeCoordinates);
							fragmentData.textureCoordinates=bc.interpolate(vertex1.textureCoordinates,vertex2.textureCoordinates, vertex3.textureCoordinates);
							
						}

						// exercise 8
						float pixelDepth = bc.interpolate(vertex1.pointWindowCoordinates, vertex2.pointWindowCoordinates, vertex3.pointWindowCoordinates).z; // interpolating
																											// so we get
																											// the z
																											// value
						if (pixelDepth < worldModel.zBuffer[j][i]) // if the pixel is closer than the zbuffer= closer
																	// than the closest thing displayed
						{ // only pixels closer than the current zbuffer need to be displayed

							worldModel.zBuffer[j][i] = pixelDepth; // updating the zbuffer to be the closest pixel
							Vector3f pixelColor = fragmentProcessing(fragmentData);
							intBufferWrapper.setPixel((int) i, (int) j, pixelColor);
						}

					}
				}
			}

		}

	}

	private Vector3f fragmentProcessing(FragmentData fragmentData) {

		if (worldModel.displayType == DisplayTypeEnum.FACE_COLOR) {
			return fragmentData.pixelColor;

		} else if (worldModel.displayType == DisplayTypeEnum.INTERPOlATED_VERTEX_COLOR) {
			return fragmentData.pixelColor;

		} else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_FLAT) {
			return new Vector3f(fragmentData.pixelIntensity0to1);

		} else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_GOURARD) {
			return new Vector3f(fragmentData.pixelIntensity0to1);

		} else if (worldModel.displayType == DisplayTypeEnum.LIGHTING_PHONG) {
			Vector3f normal= new Vector3f(fragmentData.normalEyeCoordinates);
			Vector3f eyeCoord= new Vector3f(fragmentData.pointEyeCoordinates);
			// Compute the Phong lighting model
	        Vector3f color = lightingEquation(
	        	eyeCoord,      // Point in eye coordinates
	            normal,                                // Normal at the point
	            lightPositionEyeCoordinates,           // Light position in eye coordinates
	            new Vector3f(worldModel.lighting_Diffuse),  // Diffuse coefficient
	            new Vector3f(worldModel.lighting_Specular), // Specular coefficient
	            new Vector3f(worldModel.lighting_Ambient),  // Ambient coefficient
	            worldModel.lighting_sHininess          // Shininess factor
	        );

	        return color; // Return the calculated color


		} else if (worldModel.displayType == DisplayTypeEnum.TEXTURE) {
			int width= textureImageIntBufferWrapper.getImageWidth();
			int height= textureImageIntBufferWrapper.getImageHeight();
			Vector2f copyTextureCoordinates=new Vector2f(fragmentData.textureCoordinates);
			int x=Math.round((copyTextureCoordinates.x)*(width))-1;
			int y=Math.round((copyTextureCoordinates.y)*(height))-1;
			Vector3f pixel=textureImageIntBufferWrapper.getPixel(x, y);
			return pixel;

		} else if (worldModel.displayType == DisplayTypeEnum.TEXTURE_LIGHTING) {
			//for phong lighting
			
			Vector3f normal= new Vector3f(fragmentData.normalEyeCoordinates);
			Vector3f eyeCoord= new Vector3f(fragmentData.pointEyeCoordinates);
			// Compute the Phong lighting model
	        Vector3f color = lightingEquation(
	        	eyeCoord,      // Point in eye coordinates
	            normal,                                // Normal at the point
	            lightPositionEyeCoordinates,           // Light position in eye coordinates
	            new Vector3f(worldModel.lighting_Diffuse),  // Diffuse coefficient
	            new Vector3f(worldModel.lighting_Specular), // Specular coefficient
	            new Vector3f(worldModel.lighting_Ambient),  // Ambient coefficient
	            worldModel.lighting_sHininess          // Shininess factor
	        );
	        
	        //for texture
	        
	        int width= textureImageIntBufferWrapper.getImageWidth();
			int height= textureImageIntBufferWrapper.getImageHeight();
			Vector2f copyTextureCoordinates=new Vector2f(fragmentData.textureCoordinates);
			int x=Math.round((copyTextureCoordinates.x)*(width))-1;
			int y=Math.round((copyTextureCoordinates.y)*(height))-1;
			//avoid negative values
			x=Math.max(0, x);
			y=Math.max(0, y);
			
			Vector3f pixel=textureImageIntBufferWrapper.getPixel(x, y);
			
			return pixel.mul(color);
	        

		}
		return new Vector3f();

	}

	static void drawLineDDA(IntBufferWrapper intBufferWrapper, Vector3f p1, Vector3f p2, float r, float g, float b) {
		int x1round = Math.round(p1.x);
		int y1round = Math.round(p1.y);
		int x2round = Math.round(p2.x);
		int y2round = Math.round(p2.y);

		float dx, dy, a;
		float x, y;
		dx = x2round - x1round;
		dy = y2round - y1round;

		if ((dy < (-dx)) || (dy == -dx && dx < 0)) // special case 1- points going "backwards"
		{
			x1round = Math.round(p2.x);
			y1round = Math.round(p2.y);
			x2round = Math.round(p1.x);
			y2round = Math.round(p1.y);
			dx = x2round - x1round;
			dy = y2round - y1round;
		}

		if (dy <= dx) // first algorithm- going over x values
		{
			a = dy / dx;
			y = y1round;
			for (int i = x1round; i <= x2round; i++) // x values of line
			{
				intBufferWrapper.setPixel(i, Math.round(y), r, g, b);
				y = y + a;
			}

		}

		else // second algorithm- going over y values
		{
			a = dx / dy;
			x = x1round;
			for (int i = y1round; i <= y2round; i++) // y values of line
			{
				intBufferWrapper.setPixel(Math.round(x), i, r, g, b);
				x = x + a;
			}
		}

	}

	static Vector4i calcBoundingBox(Vector3f p1, Vector3f p2, Vector3f p3, int imageWidth, int imageHeight) {
		// minX maxX minY maxY

		// minX
		float tempMinX = Math.min(p1.x, p2.x);
		tempMinX = Math.min(tempMinX, (float) p3.x); // minimum between point
		tempMinX = Math.max(tempMinX, 0); // max between largest point and 0
		double minX = Math.floor(tempMinX);
		// minY
		float tempMinY = Math.min(p1.y, p2.y);
		tempMinY = Math.min(tempMinY, (float) p3.y); // minimum between point
		tempMinY = Math.max(tempMinY, 0); // max between largest point and 0
		double minY = Math.floor(tempMinY);

		// maxX
		float tempMaxX = Math.max(p1.x, p2.x);
		tempMaxX = Math.max(tempMaxX, (float) p3.x); // maxium between point
		tempMaxX = Math.min(tempMaxX, imageWidth - 1); // min between largest point and imageWidth
		double maxX = Math.ceil(tempMaxX);
		// maxY
		float tempMaxY = Math.max(p1.y, p2.y);
		tempMaxY = Math.max(tempMaxY, (float) p3.y); // maxium between point
		tempMaxY = Math.min(tempMaxY, imageHeight - 1); // min between largest point and imageHeight
		double maxY = Math.ceil(tempMaxY);

		return new Vector4i((int) minX, (int) maxX, (int) minY, (int) maxY);

	}

	float lightingEquation(Vector3f point, Vector3f PointNormal, Vector3f LightPos, float Kd, float Ks, float Ka,
			float shininess) {

		Vector3f color = lightingEquation(point, PointNormal, LightPos, new Vector3f(Kd), new Vector3f(Ks),
				new Vector3f(Ka), shininess);
		return color.get(0);
	}
	
	

	public static Vector3f lightingEquation(Vector3f point, Vector3f PointNormal, Vector3f LightPos, Vector3f Kd,
			Vector3f Ks, Vector3f Ka, float shininess) {

		
		// Diffuse reflection (exercise 4.1)
		// vectors copies
		Vector3f copyNormal = new Vector3f(PointNormal).normalize(); // NOTICE ITS CHANGED IN LINE 301
		Vector3f copyLightPos = (new Vector3f(LightPos).sub(point)).normalize();// NOTICE ITS CHANGED IN LINE 320 //light
																				// location minus intersection point to
																				// get light source vector

		Vector3f copyKd = new Vector3f(Kd); // NOTICE ITS CHANGED IN LINE 302
		Vector3f copyKs = new Vector3f(Ks);

		float cosinosValue = copyLightPos.dot(copyNormal); // N*L
		Vector3f returnedColor = copyKd.mul(Math.max(0, cosinosValue)); // Kd*(N*L)

		// Ambient reflection (exercise 4.2)
		returnedColor.add(Ka);

		// Specular reflection (exercise 4.3)
		copyNormal = new Vector3f(PointNormal).normalize(); // copy again because its not the og vector anymore NOTICE
															// ITS CHANGED IN LINE 323
		copyLightPos = new Vector3f(LightPos).sub(point).normalize(); // copy again
		// float LightN=copyLightPos.dot(copyNormal); //light*normal
		float LightN = cosinosValue;
		Vector3f R = new Vector3f(0, 0, 0);
		if (LightN > 0) // negative LIGHT*N
		{
			float LightN2 = (LightN * 2); // 2*(light*normal)
			R = new Vector3f(copyNormal).mul(LightN2).sub(copyLightPos).normalize();
		}
		Vector3f eyePosition = new Vector3f(0, 0, 0); // point of view
		Vector3f V = new Vector3f(eyePosition).sub(point).normalize();
		float RdotV = R.dot(V); // if v is negative the result will be negative- then in the next line we'll
								// take 0 instead
		float VRn = (float) Math.pow(Math.max(0, RdotV), shininess); // (v*r)^n - if v is negative or r was originally
																		// negative then 0
		Vector3f tempRes = new Vector3f(copyKs).mul(VRn);
		returnedColor.add(tempRes);

		return returnedColor;

	}
}

