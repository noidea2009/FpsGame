package util;

/**
 * Responsibility:
 * Reusable math utility
 *
 * Fields:
 * double x, y;
 * Methods:
 * double length()
 * double distance(Vector2D other)
 * Vector2D normalize()
 * Vector2D rotate(double angleRadians)
 * static Vector2D fromAngle(double angleRadians)
 * double getX()
 * double getY()
 * double dot(Vector2D other)
 * Vector2D scale(double factor)
 * Vector2D add(Vector2D other)
 */
public class Vector2D {
    private final double x, y;

    public Vector2D(double v, double v1) {
        this.x = v;
        this.y = v1;
    }

    /**
     * Returns the x component of the vector.
     *
     * @return x component
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the y component of the vector.
     *
     * @return y component
     */
    public double getY() {
        return y;
    }

    /**
     * Calculates the Euclidean length (magnitude) of the vector.
     * Formula: sqrt(x^2 + y^2)
     *
     * @return the magnitude of the vector
     */
    public double length(){
        return Math.sqrt(x*x + y*y);
    }

    /**
     * Calculates the Euclidean distance from this vector to another.
     * Formula: sqrt((dx)^2 + (dy)^2)
     *
     * @param other the other point to measure to
     * @return the distance between the two points
     */
    public double distance(Vector2D other){
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Returns a new vector in the same direction with magnitude 1.
     *
     * @return a new normalized vector
     * @throws ArithmeticException if vector is zero-length
     */
    public  Vector2D normalize(){
        double len = length();
        if(len == 0) throw new ArithmeticException("Cannot normalize zero vectors");
        return new Vector2D(x / len, y / len);
    }


    /**
     * Returns a new vector rotated by the given angle (in radians).
     *
     * Uses rotation matrix:
     * x' = x*cos(θ) - y*sin(θ)
     * y' = x*sin(θ) + y*cos(θ)
     *
     * Useful for rotating player direction or calculating rays within field of view.
     *
     * @param angleRadians the rotation angle in radians (positive = counter-clockwise)
     * @return a new rotated vector
     */
    public Vector2D rotate(double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        //let α = original angle and θ = angleRadians
        //x' = r*cos(α)*cos(θ) - r*sin(α)*sin(θ)
        //   = (r*cos(α))*cos(θ) - (r*sin(α))*sin(θ)
        //   = x*cos(θ) - y*sin(θ)  ← double newX = this.x * cos - this.y * sin;
        //
        //y' = r*sin(α)*cos(θ) + r*cos(α)*sin(θ)
        //   = (r*sin(α))*cos(θ) + (r*cos(α))*sin(θ)
        //   = y*cos(θ) + x*sin(θ)
        //   = x*sin(θ) + y*cos(θ)  ← double newY = this.x * sin + this.y * cos;
        double newX = this.x * cos - this.y * sin;
        double newY = this.x * sin + this.y * cos;

        // returns the rotated Vector
        return new Vector2D(newX, newY);
    }

    /**
     * Builds a unit vector pointing in the direction of the given angle.
     * Formula: (cos(angleRadians), sin(angleRadians))
     *
     * Intended for converting a facing angle or a per-column ray angle
     * into a direction vector usable by a DDA raycasting loop.
     *
     * @param angleRadians direction angle, in radians
     * @return a new unit vector pointing along the given angle
     */
    public static Vector2D fromAngle(double angleRadians) {
        return new Vector2D(Math.cos(angleRadians), Math.sin(angleRadians));
    }

    /**
     * Calculates the dot product between this vector and another.
     * Formula: x1*x2 + y1*y2
     *
     * Used for projecting a hit-direction vector onto a facing-direction
     * vector, which produces the perpendicular distance needed to correct
     * fisheye distortion in a raycasting renderer.
     *
     * @param other the vector to compute the dot product with
     * @return the scalar dot product of the two vectors
     */
    public double dot(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    /**
     * Returns a new vector scaled by the given factor.
     * Formula: (x*factor, y*factor)
     *
     * Useful for extending a unit direction vector by a distance,
     * such as stepping a ray forward from its origin to a hit point.
     *
     * @param factor the scalar multiplier
     * @return a new scaled vector
     */
    public Vector2D scale(double factor) {
        return new Vector2D(x * factor, y * factor);
    }

    /**
     * Returns a new vector representing the sum of this vector and another.
     * Formula: (x1+x2, y1+y2)
     *
     * Useful for combining a position vector with a scaled direction
     * vector to compute a world-space point, such as a raycast hit
     * coordinate.
     *
     * @param other the vector to add
     * @return a new vector representing the component-wise sum
     */
    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }
}