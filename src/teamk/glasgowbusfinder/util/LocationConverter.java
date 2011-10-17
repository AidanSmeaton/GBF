package teamk.glasgowbusfinder.util;

import android.location.Location;

import com.google.android.maps.GeoPoint;

/**
 * Utility class which converts between the two types
 * of geographical location classes used by Android.
 * 
 * @author Euan Freeman
 *
 * @see GeoPoint
 * @see Location
 */
public class LocationConverter {
	/**
	 * Converts a Location object to a GeoPoint
	 * with equivalent position.
	 * 
	 * @param loc Location object
	 * 
	 * @return GeoPoint representation of Location position
	 */
	public static GeoPoint locationToGeoPoint(Location loc) {
		return new GeoPoint((int) (loc.getLatitude() * 1e6), (int) (loc.getLongitude() * 1e6));
	}
	
	/**
	 * Converts a GeoPoint object to a Location
	 * with equivalent position.
	 * 
	 * @param point GeoPoint object
	 * 
	 * @return Location representation of GeoPoint position
	 */
	public static Location geoPointToLocation(GeoPoint point) {
		Location loc = new Location("");
		
		loc.setLatitude(point.getLatitudeE6() / 1e6);
		loc.setLongitude(point.getLongitudeE6() / 1e6);
		
		return loc;
	}
}
