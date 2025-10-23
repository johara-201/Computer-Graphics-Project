package your_code;

import org.joml.Vector3f;

import app_interface.ModelSphere;

public class UnitTests {

	        public static void testRayIntersection() {
	            // Test case 1: No intersection (ray misses the sphere)
	            Vector3f rayStart1 = new Vector3f(1, 1, 1);
	            Vector3f rayDirection1 = new Vector3f(1, 1, -1).normalize();
	            ModelSphere sphere1 = new ModelSphere(new Vector3f(0, 0, -10), 5, 0, 0);

	            IntersectionResults result1 = WorldModel.rayIntersection(rayStart1, rayDirection1, sphere1);
	            System.out.println("Test Case 1: No intersection (ray misses the sphere)");
	            System.out.format("Input: rayStart = [%.2f, %.2f, %.2f], rayDirection = [%.2f, %.2f, %.2f], sphereCenter = [%.2f, %.2f, %.2f], sphereRadius = %.2f%n",
	                    rayStart1.x, rayStart1.y, rayStart1.z, rayDirection1.x, rayDirection1.y, rayDirection1.z, sphere1.center.x, sphere1.center.y, sphere1.center.z, sphere1.radius);
	            System.out.format("Result  : intersected = %b%n", result1!=null);
	            System.out.format("Expected: intersected = false%n%n");

	            // Test case 2: One intersection (ray tangent to the sphere)
	            Vector3f rayStart2 = new Vector3f(-5, 0, -5);
	            Vector3f rayDirection2 = new Vector3f(1, 0, 0).normalize();
	            ModelSphere sphere2 = new ModelSphere(new Vector3f(0, 0, -10), 5, 0, 0);

	            IntersectionResults result2 = WorldModel.rayIntersection(rayStart2, rayDirection2, sphere2);
	            System.out.println("Test Case 2: One intersection (ray tangent to the sphere)");
	            System.out.format("Input: rayStart = [%.2f, %.2f, %.2f], rayDirection = [%.2f, %.2f, %.2f], sphereCenter = [%.2f, %.2f, %.2f], sphereRadius = %.2f%n",
	                    rayStart2.x, rayStart2.y, rayStart2.z, rayDirection2.x, rayDirection2.y, rayDirection2.z, sphere2.center.x, sphere2.center.y, sphere2.center.z, sphere2.radius);
	            System.out.format("Result  : intersected = %b, intersectionPoint = [%.2f, %.2f, %.2f], normal = [%.2f, %.2f, %.2f], linePointOutside = %b%n",
	                    result2!=null, result2.intersectionPoint.x, result2.intersectionPoint.y, result2.intersectionPoint.z, result2.normal.x, result2.normal.y, result2.normal.z, result2.rayFromOutsideOfSphere);
	            System.out.format("Expected: intersected = true, intersectionPoint = [0.00, 0.00, -5.00], normal = [0.00, 0.00, 1.00], linePointOutside = true%n%n");

	            // Test case 3: Two intersections (ray passes through the sphere)
	            Vector3f rayStart3 = new Vector3f(0, 0, -20);
	            Vector3f rayDirection3 = new Vector3f(0f, 0f, 1).normalize();
	            ModelSphere sphere3 = new ModelSphere(new Vector3f(0, 0, -10), 5, 0, 0);

	            IntersectionResults result3 = WorldModel.rayIntersection(rayStart3, rayDirection3, sphere3);
	            System.out.println("Test Case 3: Two intersections (ray passes through the sphere)");
	            System.out.format("Input: rayStart = [%.2f, %.2f, %.2f], rayDirection = [%.2f, %.2f, %.2f], sphereCenter = [%.2f, %.2f, %.2f], sphereRadius = %.2f%n",
	                    rayStart3.x, rayStart3.y, rayStart3.z, rayDirection3.x, rayDirection3.y, rayDirection3.z, sphere3.center.x, sphere3.center.y, sphere3.center.z, sphere3.radius);
	            System.out.format("Result  : intersected = %b, intersectionPoint = [%.2f, %.2f, %.2f], normal = [%.2f, %.2f, %.2f], linePointOutside = %b%n",
	                    result3!=null, result3.intersectionPoint.x, result3.intersectionPoint.y, result3.intersectionPoint.z, result3.normal.x, result3.normal.y, result3.normal.z, result3.rayFromOutsideOfSphere);
	            System.out.format("Expected: intersected = true, intersectionPoint = [0.00, 0.00, -15.00], normal = [0.00, 0.00, -1.00], linePointOutside = true%n%n");
	        }

	public static void main(String[] args) {
		testRayIntersection();
		
	}
}