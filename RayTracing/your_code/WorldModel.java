package your_code;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Matrix3f;
import org.joml.Vector3f;

import app_interface.ExerciseEnum;
import app_interface.Model;
import app_interface.ModelLight;
import app_interface.ModelMaterial;
import app_interface.ModelSphere;
import app_interface.SphereTexture;

//Johara Naser aldeen 314704453, ORIAN HAZIZA 21192128

//this class serve as the container of the return value of the intersection method
class IntersectionResults {
	/** The point of intersection on the ray */
	Vector3f intersectionPoint;
	/** The normal vector at the point of intersection */
	Vector3f normal;
	/** Flag indicating if the ray originated from outside of the sphere */
	boolean rayFromOutsideOfSphere;
	/** The ModelSphere object that was intersected */
	ModelSphere intersectedSphere;

	/** Constructor that sets all fields of the IntersectionResults object
	 * @param intersected            true if an intersection occurred, false otherwise
	 * @param intersectionPoint      the point of intersection on the ray
	 * @param normal                 the normal vector at the point of intersection
	 * @param rayFromOutsideOfSphere flag indicating if the ray originated from outside of the sphere
	 * @param intersectedSphere      the ModelSphere object that was intersected */
	IntersectionResults(boolean intersected, Vector3f intersectionPoint, Vector3f normal,
			boolean rayFromOutsideOfSphere, ModelSphere intersectedSphere) {
		this.intersectionPoint = intersectionPoint;
		this.normal = normal;
		this.rayFromOutsideOfSphere = rayFromOutsideOfSphere;
		this.intersectedSphere = intersectedSphere;
	}
}

//class holding the world model and render it
public class WorldModel {

	/** The Model object with all the details of the world model that will be rendered */
	Model model;

	/** Your selection enum type (specific to your implementation) */	
	private YourSelectionEnum yourSelection;

	/** The current exercise being executed (from ExerciseEnum) */	
	private static ExerciseEnum exercise;

	/** image width and height */
	private int imageWidth;
	private int imageHeight;

	/** SphereTexture object of the skybox of the world model */
	SphereTexture skyBoxImageSphereTexture;

	/** The depth of ray tracing used during rendering */
	private static int depthOfRayTracing;

	public WorldModel(int imageWidth, int imageHeight) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
	}

	public void setRenderingParams(int depthOfRayTracing) {
		WorldModel.depthOfRayTracing = depthOfRayTracing;
	}

	public void setExercise(ExerciseEnum exercise) {
		WorldModel.exercise = exercise;
	}

	public void setYourSelection(YourSelectionEnum sel) {
		this.yourSelection = sel;
	}

	/** Loads a model from a specified file 
	* @param fileName the path to the model file
	* @return true if the model was loaded successfully, false otherwise
	* @throws Exception if there is an error loading the model file	*/	
	public boolean load(String fileName) {
		try {
			model = new Model(fileName);
			skyBoxImageSphereTexture = new SphereTexture(model.skyBoxImageFileName);
			return true;
		} catch (Exception e) {
			System.err.println("Failed to load the model file: " + fileName + ".\nDescription: " + e.getMessage());
			return false;
		}
	}

	/** Renders the color of a specific pixel in the image
	* @param x the x coordinate of the pixel
	* @param y the y coordinate of the pixel
	* @return the Vector3f representing the color of the pixel */	
	public Vector3f renderPixel(int x, int y) {
		Vector3f color = new Vector3f();
		if (exercise == ExerciseEnum.EX_0___Starting_point)
			color.set(0);
		else if (exercise == ExerciseEnum.EX_1_0_Colors_one_color) {
		         	/** RED: color.set(1,0,0); */ 
			        /**White: color.set(1,1,1);*/
		         	/**GREY: color.set(0.5,0.5,0.5);*/
		            /**YELLOW*/	color.set(1,1,0);
			    
		} else if (exercise == ExerciseEnum.EX_1_1_Colors_Random_color) {
			       color.set((float) Math.random(),(float) Math.random(),(float) Math.random());
                   
		} else if (exercise == ExerciseEnum.EX_1_2_Colors_Color_space) {
			       Matrix3f m = new Matrix3f((-1.0f)/(imageWidth-1), (-1.0f)/(imageHeight-1), 1,
			    		                     1.0f/(imageWidth-1), 0, 0,
					                         0, 1.0f/(imageHeight-1), 0).transpose();
			       
			       
			       color=m.transform(new Vector3f(x,y,1));

		} else {
			Vector3f direction = calcPixelDirection(x, y, imageWidth, imageHeight, model.fovXdegree);
			color= rayTracing(new Vector3f(0, 0, 0), direction,model,skyBoxImageSphereTexture,0);
		}

		return color;
	}

	/** Performs ray tracing for a given ray.
	 * @param incidentRayOrigin The origin of the incident ray.
	 * @param incidentRayDirection The direction of the incident ray.
	 * @param model The model containing spheres and materials.
	 * @param skyBoxImageSphereTexture The texture for the skybox.
	 * @param depthLevel The current depth level of the recursion (for limiting recursion).
	 * @return The calculated color for the pixel based on ray tracing and lighting effects. */	
	private static Vector3f rayTracing(Vector3f incidentRayOrigin, Vector3f incidentRayDirection, Model model,
			SphereTexture skyBoxImageSphereTexture, int depthLevel) {
		
		if (depthLevel >= depthOfRayTracing){
	        return new Vector3f(0, 0, 0); // return black (no further reflected light)
	    }
		Vector3f returnedColor = new Vector3f(skyBoxImageSphereTexture.sampleDirectionFromMiddle(incidentRayDirection));
		IntersectionResults intersectionResults=rayIntersection(incidentRayOrigin,incidentRayDirection,model.spheres);
		if(intersectionResults==null) //no intersection- return skyBox color
		{
			return new Vector3f(skyBoxImageSphereTexture.sampleDirectionFromMiddle(new Vector3f(incidentRayDirection)));
		}
		
		ModelSphere intersectedSphere = intersectionResults.intersectedSphere;
		ModelMaterial intersectedSphereMaterial=model.materials.get(intersectedSphere.materialIndex);
		Vector3f intersectionPoint = intersectionResults.intersectionPoint;
		Vector3f intersectionNormal = intersectionResults.normal;
		SphereTexture intersectedSphereTexture=model.skyBoxImageSphereTextures.get(intersectedSphere.textureIndex);
		
		Vector3f color=intersectedSphereMaterial.color;
		Vector3f colorCopy=new Vector3f(color); //so we dont destroy color
		float kColor=intersectedSphereMaterial.kColor;
		returnedColor=new Vector3f(colorCopy).mul(kColor);
		
		//exercise 4
//		Vector3f lighting=lightingEquation(intersectionPoint,intersectionNormal,new Vector3f(model.lights.get(0).location),intersectedSphereMaterial.kd,intersectedSphereMaterial.ks,intersectedSphereMaterial.ka,
//				intersectedSphereMaterial.shininess);
//		lighting=new Vector3f(lighting).mul(intersectedSphereMaterial.kDirect);
//		returnedColor=new Vector3f(returnedColor).add(lighting);
		
		//exercise 5
		Vector3f calc=calcKdCombinedWithTexture(intersectionPoint,intersectedSphere.center,intersectedSphereTexture,intersectedSphereMaterial.kd,intersectedSphereMaterial.kTexture);
		//exercise 6
		boolean shadow=isPointInShadow(new Vector3f(model.lights.get(0).location),intersectionPoint,intersectionNormal,model);
		if(shadow==false) //point isnt in the shade- regular calculation of color
		{
			Vector3f lighting=lightingEquation(intersectionPoint,intersectionNormal,new Vector3f(model.lights.get(0).location),calc,intersectedSphereMaterial.ks,intersectedSphereMaterial.ka,
					intersectedSphereMaterial.shininess);
			lighting=new Vector3f(lighting).mul(intersectedSphereMaterial.kDirect);
			returnedColor=new Vector3f(returnedColor).add(lighting);
		}
		else //point is in the shade- need to return ambient light
		{
			returnedColor=new Vector3f(returnedColor).add(new Vector3f(intersectedSphereMaterial.ka).mul(intersectedSphereMaterial.kDirect));
		}
		
		//exercise 7
		Vector3f reflection=calcReflectedLight(incidentRayDirection,intersectionPoint,intersectionNormal,model,skyBoxImageSphereTexture,depthLevel);
		Vector3f refKref=new Vector3f(reflection).mul(intersectedSphereMaterial.kReflection); //Kreflection*reflectionResult
		returnedColor=new Vector3f(returnedColor).add(refKref);
		//exercise 8
		Vector3f transmittedColor = calcTransmissionLight(incidentRayDirection,intersectionPoint,intersectionNormal,intersectionResults.rayFromOutsideOfSphere,
		        intersectedSphereMaterial.refractiveIndex,model,skyBoxImageSphereTexture,depthLevel);
	    Vector3f transmission= new Vector3f(transmittedColor).mul(intersectedSphereMaterial.kTransmission);
      	returnedColor.add(transmission);
	
		return returnedColor;

	}

	
	/** Calculates the direction of a ray for a given pixel in the image.
	 * @param x The x-coordinate of the pixel.
	 * @param y The y-coordinate of the pixel.
	 * @param imageWidth The width of the image.
	 * @param imageHeight The height of the image.
	 * @param fovXdegree The horizontal field of view in degrees.
	 * @return The normalized direction vector of the ray for the given pixel. */	
	 static Vector3f calcPixelDirection(int x, int y, int imageWidth, int imageHeight, float fovXdegree) {
                   
		float fovXradian = (float) Math.toRadians(fovXdegree);
	    float fovYradian = fovXradian / imageWidth * imageHeight; 
	    float xLeft = (float) -Math.tan(fovXradian / 2);
	    float xRight = (float) Math.tan(fovXradian / 2);
	    float yBottom = (float) -Math.tan(fovYradian / 2);
	    float yTop = (float) Math.tan(fovYradian / 2);
	    float xDelta = (xRight - xLeft) / (imageWidth - 1);
	    float yDelta = (yTop - yBottom) / (imageHeight - 1);
	    
	    float px = xLeft + x * xDelta;
	    float py = yBottom + y * yDelta;
	    
	    Vector3f direction = new Vector3f(px, py, -1);
	    direction.normalize();
	    
		return direction;
	}

	/** Calculates the intersection(s) between a ray and a sphere.
	 * @param rayStart The starting point of the ray in world space.
	 * @param rayDirection The normalized direction vector of the ray.
	 * @param sphere The sphere to check for intersection.
	 * @return An IntersectionResults object containing information about the intersection,
	 *         or an empty IntersectionResults object if no intersection occurs. */	
	static IntersectionResults rayIntersection(Vector3f rayStart, Vector3f rayDirection, ModelSphere sphere) {
		
        Vector3f current= new Vector3f();
		sphere.center.sub(rayStart, current);
		
		float tm= current.dot(rayDirection);
		if(tm<0){
			return null;
		}
		
		rayDirection.mul(tm,current);
		current.add(rayStart);
		
		Vector3f pm= new Vector3f(current);
	    float pmDistance = pm.distance(sphere.center);
	    if (pmDistance > sphere.radius) {
	        return null; 
	    }

	
	    float root = (float) Math.sqrt(sphere.radius * sphere.radius - pmDistance * pmDistance);

	    Vector3f IntersectionPoint;
	    Vector3f normal;
	    boolean rayFromOutsideOfSphere;

	    if (root > tm) {
	
	        rayDirection.mul(root, current);
	        current.add(pm);
	        IntersectionPoint = new Vector3f(current);
	        sphere.center.sub(IntersectionPoint, current);
	        normal = new Vector3f(current).normalize();
	        rayFromOutsideOfSphere = false;
	        
	    } else {

	        rayDirection.mul(-root, current);
	        current.add(pm);
	        IntersectionPoint = new Vector3f(current);

	
	        IntersectionPoint.sub(sphere.center, current);
	        normal = new Vector3f(current).normalize();

	        rayFromOutsideOfSphere = true;
	    }

	
	    return new IntersectionResults(true,IntersectionPoint, normal, rayFromOutsideOfSphere,sphere);   
		                  
	}


	/** Finds the nearest intersection between a ray and a list of spheres.
	 * @param rayStart The starting point of the ray.
	 * @param rayDirection The normalized direction of the ray.
	 * @param spheres The list of spheres to check for intersections.
	 * @return An IntersectionResults object containing information about the nearest intersection,
	 *         or an empty IntersectionResults object if no intersection occurs. */	
	private static IntersectionResults rayIntersection(Vector3f rayStart, Vector3f rayDirection,
			List<ModelSphere> spheres) {
		
		IntersectionResults finalResult = null;
		float closestD = Float.MAX_VALUE; 
		for (ModelSphere sphere : spheres) {
			IntersectionResults result = rayIntersection(rayStart, rayDirection, sphere);
		    if (result != null) {
		    	 float distance = rayStart.distance(result.intersectionPoint);
		        
		        if (distance < closestD) {
		            closestD = distance;
		            finalResult = result;
		        }
		    }
		}

		return finalResult;

	}

	
	/** Calculates the lighting at a specific point.
	 * @param point The Eye space position of the point.
	 * @param pointNormal The normal vector at the point.
	 * @param lightPos The Eye space position of the light source.
	 * @param Kd The diffuse color coefficient of the material.
	 * @param Ks The specular color coefficient of the material.
	 * @param Ka The ambient color coefficient of the material.
	 * @param shininess The shininess parameter for specular highlights.
	 * @return The calculated lighting as a Vector3f representing color. */	
	 static Vector3f lightingEquation(Vector3f point, Vector3f PointNormal, Vector3f LightPos, Vector3f Kd,
			Vector3f Ks, Vector3f Ka, float shininess) {
		
		Vector3f returnedColor = new Vector3f();
		
		//vector L from point to light position normalized
	    Vector3f L = new Vector3f(LightPos).sub(point).normalize();
	    
	    //cos teta
	    float factor = Math.max(0, L.dot(PointNormal));
	    
	    //Diffuse lighting ,  ILight=1
	    Vector3f diffuseLight = new Vector3f(Kd).mul(factor);
        returnedColor.add(diffuseLight);
        
        //Ambient lighting ,  ILight=1
        Vector3f ambientLight = new Vector3f(Ka); 
        returnedColor.add(ambientLight);
        
        // Specular lighting ,  ILight=1
        
        // vector v when eye position is (0,0,0)
        Vector3f V = new Vector3f(0, 0, 0).sub(point).normalize(); 
        
        //vector of reflection
        Vector3f R = new Vector3f(PointNormal).mul(2 * L.dot(PointNormal)).sub(L).normalize(); 
        float Factor = (float) Math.pow(Math.max(0, V.dot(R)), shininess);
        
        Vector3f specularLight = new Vector3f(Ks).mul(Factor);
        returnedColor.add(specularLight);
        
        return returnedColor;
	}
	 
	 

		/**
		 * Calculates the combined diffuse reflection coefficient (Kd) for a point on the surface of a sphere,
		 * taking into account both the base material's Kd and the texture applied to the sphere.
		 * 
		 * @param intersectionPoint the point of intersection on the sphere's surface in 3D space.
		 * @param intersectedSphereCenter the center of the intersected sphere.
		 * @param intersectedSphereTexture the texture applied to the sphere, used to sample color based on direction.
		 * @param intersectedSphereKd the base diffuse reflection coefficient of the sphere's material.
		 * @param kTexture the blending factor between the base Kd and the texture color 
		 *                 (0 for only base Kd, 1 for only texture, values in between for blending).
		 * @return a {@link Vector3f} representing the combined Kd, computed as a weighted blend of the base Kd and the texture color.
		 */
		static Vector3f calcKdCombinedWithTexture(Vector3f intersectionPoint,Vector3f intersectedSphereCenter,SphereTexture intersectedSphereTexture,
				Vector3f intersectedSphereKd,float kTexture) {
			
			//exercise 5
			Vector3f copyIntersection=new Vector3f(intersectionPoint);
			Vector3f copySphereCenter=new Vector3f(intersectedSphereCenter); //CHANGED 
			Vector3f DirectionFromCenterToIntersection=new Vector3f(copyIntersection).sub(copySphereCenter).normalize();
			
			Vector3f textureColor=new Vector3f(intersectedSphereTexture.sampleDirectionFromMiddle(DirectionFromCenterToIntersection));
			Vector3f kdiffuse=new Vector3f(intersectedSphereKd).mul(1-kTexture).add(new Vector3f(textureColor).mul(kTexture));

			return kdiffuse;
		}	


		/**
		 * Determines whether a given point is in shadow with respect to a specified light source.
		 * 
		 * <p>The method calculates a shadow ray originating just above the surface of the given point
		 * (offset slightly along the surface normal to prevent self-intersection) and checks whether
		 * this ray intersects with any objects in the scene model.</p>
		 * 
		 * @param lightLocation the position of the light source in 3D space.
		 * @param point the position of the point being tested for shadow in 3D space.
		 * @param pointNormal the surface normal vector at the point, used to offset the shadow ray origin.
		 * @param model the {@link Model} representing the scene, containing the objects (e.g., spheres) to check for intersection.
		 * @return {@code true} if the point is in shadow (i.e., the shadow ray intersects with any objects), {@code false} otherwise.
		 */
		static boolean isPointInShadow(Vector3f lightLocation,Vector3f point,Vector3f pointNormal,Model model)
		{
			Vector3f p01=new Vector3f(point).add(new Vector3f(pointNormal).mul(0.01f)); //p=p+0.01Normal to slightly move the point
			Vector3f lightDirection=new Vector3f(lightLocation).sub(point).normalize();
			IntersectionResults res=rayIntersection(p01,lightDirection,model.spheres);
			if(res!=null)
			{
				return true;
			}
			return false;
		}	

		
		/**
		 * Calculates the reflected light at an intersection point, accounting for reflections
		 * within the scene and the skybox texture.
		 * 
		 * <p>The method computes the direction of the reflected ray based on the incident ray 
		 * and the surface normal at the intersection point. It then recursively traces the 
		 * reflected ray through the scene to compute the reflected light contribution.</p>
		 * 
		 * @param incidentRayDirection the direction of the incoming ray hitting the surface.
		 * @param intersectionPoint the point on the surface where the reflection occurs.
		 * @param intersectionNormal the normal vector at the intersection point.
		 * @param kReflection the reflection coefficient, controlling the intensity of the reflected light.
		 * @param model the {@link Model} representing the scene, containing objects for ray tracing.
		 * @param skyBoxImageSphereTexture the texture of the skybox used to simulate distant reflections.
		 * @param depthLevel the current recursion depth, used to limit the number of reflection bounces.
		 * @return a {@link Vector3f} representing the color/intensity of the reflected light at the intersection point.
		 */
		

		static Vector3f calcReflectedLight(Vector3f incidentRayDirection, Vector3f intersectionPoint, Vector3f intersectionNormal, Model model, SphereTexture skyBoxImageSphereTexture, 
		 int depthLevel) 
		{
			Vector3f normalizedIncidentRay = new Vector3f(incidentRayDirection).normalize();	    
		    Vector3f normalizedNormal = new Vector3f(intersectionNormal).normalize();

		    float Light2N = (new Vector3f(normalizedIncidentRay).dot(normalizedNormal)) * 2; // 2*(light*n)
		    Vector3f reflectedDirection = new Vector3f(normalizedIncidentRay).sub(new Vector3f(normalizedNormal).mul(Light2N)).normalize(); //light-2(light*n)*n
		    
	        Vector3f resFromRayTracing=rayTracing(intersectionPoint,new Vector3f(reflectedDirection),model,skyBoxImageSphereTexture,depthLevel+1);	    
		    return resFromRayTracing;
		}

		
		
		
		/**
		 * Calculates the transmitted (refracted) light at an intersection point, considering the refraction 
		 * properties of the intersected object and the scene environment.
		 * 
		 * <p>The method computes the direction of the transmitted ray based on the incident ray, 
		 * the surface normal, and the refractive index of the intersected sphere. It offsets the ray 
		 * origin slightly to prevent self-intersection and recursively traces the transmitted ray 
		 * through the scene to determine the contribution of transmitted light.</p>
		 * 
		 * @param incidentRayDirection the direction of the incoming ray hitting the surface.
		 * @param intersectionPoint the point on the surface where the refraction occurs.
		 * @param intersectionNormal the normal vector at the intersection point.
		 * @param intersectionFromOutsideOfSphere {@code true} if the intersection occurs when the ray 
		 *                                         enters the sphere from outside, {@code false} if exiting.
		 * @param refractiveIndexIntersectedSphere the refractive index of the intersected sphere material.
		 * @param kTransmission the transmission coefficient, controlling the intensity of the transmitted light.
		 * @param model the {@link Model} representing the scene, containing objects for ray tracing.
		 * @param skyBoxImageSphereTexture the texture of the skybox used to simulate distant light transmission.
		 * @param depthLevel the current recursion depth, used to limit the number of refraction bounces.
		 * @return a {@link Vector3f} representing the color/intensity of the transmitted light at the intersection point.
		 */
		static Vector3f calcTransmissionLight(Vector3f incidentRayDirection,  Vector3f intersectionPoint, Vector3f intersectionNormal, 
					boolean intersectionFromOutsideOfSphere, float refractiveIndexIntersectedSphere, Model model, SphereTexture skyBoxImageSphereTexture, int depthLevel) {

			
			//calc using snell's law
			Vector3f TransmissionRay=YoursUtilities.calcTransmissionRay(new Vector3f (incidentRayDirection),new Vector3f(intersectionNormal),refractiveIndexIntersectedSphere, intersectionFromOutsideOfSphere); 
			
			//move intersection point a bit (offset)
			Vector3f p=new Vector3f(intersectionPoint);
			if(intersectionFromOutsideOfSphere)
			{
				p.sub(new Vector3f(intersectionNormal).normalize().mul(0.01f)); //p=p-0.01Normal
			}
			else
			{
				p.add(new Vector3f(intersectionNormal).normalize().mul(0.01f)); //p=p+0.01Normal
			}
//						
//			if(intersectionFromOutsideOfSphere)
//			{
//				p.sub((new Vector3f(intersectionNormal).mul(0.01f))); //p=p-0.01Normal
//			}
//			else
//			{
//				p.add((new Vector3f(intersectionNormal).mul(0.01f))); //p=p+0.01Normal
//			}

			Vector3f resFromRayTracing=rayTracing(new Vector3f(p),new Vector3f(TransmissionRay),model,skyBoxImageSphereTexture,depthLevel+1);	    
		    return resFromRayTracing;
			//return new Vector3f();

		} 

}