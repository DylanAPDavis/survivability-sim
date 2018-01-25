package netlab.topology.elements;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import java.io.Serializable;

/******************************************************************************
 *  Compilation:  javac Location.java
 *  Execution:    java Location
 *
 *  Immutable data type for a named location: name and
 *  (latitude, longitude).
 *
 *  % java LocationTest
 *  172.367 miles from
 *  PRINCETON_NJ (40.366633, 74.640832) to ITHACA_NY (42.443087, 76.488707)
 *
 * https://introcs.cs.princeton.edu/java/44st/Location.java.html
 ******************************************************************************/
@Data
@Builder
@NoArgsConstructor
public class Location implements Serializable{
    private double longitude;
    private double latitude;
    private static double metersPerKilometer = 1000;

    // create and initialize a point with given name and
    // (latitude, longitude) specified in degrees
    public Location(double latitude, double longitude) {
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    // return distance between this location and that location
    // measured in kilometers
    public double distanceTo(Location target) {
        /*
        double lat1 = Math.toRadians(this.latitude);
        double lon1 = Math.toRadians(this.longitude);
        double lat2 = Math.toRadians(that.latitude);
        double lon2 = Math.toRadians(that.longitude);

        // great circle distance in radians, using law of cosines formula
        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        // each degree on a great circle of Earth is 60 nautical miles
        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        */
        GeodesicData geoLine = Geodesic.WGS84.Inverse(this.latitude, this.longitude, target.latitude, target.longitude);
        double distanceMeters = geoLine.s12;
        return distanceMeters / metersPerKilometer;
    }


    public Location locationBetweenGivenDistanceKM(Location target, Double distance){
        GeodesicData geoLine = Geodesic.WGS84.Inverse(this.latitude, this.longitude, target.latitude, target.longitude);
        GeodesicData g = Geodesic.WGS84.Direct(this.latitude, this.longitude, geoLine.azi1, distance * metersPerKilometer);

        double newLat = g.lat2;
        double newLon = g.lon2;
        Location newLocation = new Location(newLat, newLon);
        return newLocation;
    }

    // return string representation of this point
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }


}
