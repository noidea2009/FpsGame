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
 */
public class Vector2D {
 private final double x, y;

    public Vector2D(double v, double v1) {
        this.x = v;
        this.y = v1;
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
}

