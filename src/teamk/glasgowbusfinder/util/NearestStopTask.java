package teamk.glasgowbusfinder.util;

import java.util.ArrayList;

import teamk.glasgowbusfinder.NearestStopActivity;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.maps.GeoPoint;

/**
 * Asynchronous task which finds the location of the stop
 * nearest to a given point.
 */
public class NearestStopTask extends AsyncTask<NearestStopArgs, Integer, BusStop> {
	/* 1 degree of latitude/longitude approx 100km. */
	private static final double GPS_DISTANCE = 0.005;
	private static final int GPS_DISTANCE_RATIO = 100000;
	private DatabaseHelper db;
	
	@Override
	protected BusStop doInBackground(NearestStopArgs... args) {
		db = DatabaseHelper.getInstance(args[0].getContext());
		
		GeoPoint current = args[0].getLocation();
		String searchRoute = args[0].getRoute();
		
		/* Check if we are searching for a stop on a route, or all stops. */
		boolean onRoute = (searchRoute != null && !searchRoute.equals(""));
		
		/* Convert from microdegrees to degrees */
		double myLat = current.getLatitudeE6() / 1e6;
		double myLon = current.getLongitudeE6() / 1e6;
		
		/* 
		 * Set the initial boundary within which to search. If
		 * no stops can be found, this grows until a stop is
		 * located.
		 */
		double bottomLeftLat = myLat - GPS_DISTANCE;
		double bottomLeftLon = myLon - GPS_DISTANCE;
		double upperRightLat = myLat + GPS_DISTANCE;
		double upperRightLon = myLon + GPS_DISTANCE;
		
		Location here = new Location("Here");
		here.setLatitude(myLat);
		here.setLongitude(myLon);
		
		/* 
		 * Look for nearest stop within 100sq metres.
		 * Increase boundary by 100sq metres if not found, until boundary is at max 10km.
		 */
		Cursor c = null;
		
		if (onRoute) {
			c = db.getBusStopsInBoundaryOnRoute(searchRoute, bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
		} else {
			c = db.getBusStopsInBoundary(bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
		}
		
		int distanceBoundary = (int) (GPS_DISTANCE * GPS_DISTANCE_RATIO);
		int totalDistanceBoundary = distanceBoundary;
		boolean found = false;
		
		/* 
		 * Grow the search boundary 20 times at the most. Any greater distance
		 * than this and the user is _nowhere_ near a stop.
		 */
		for (int i = 0; i < 20 && !found; i++, totalDistanceBoundary += distanceBoundary){
			bottomLeftLat -= GPS_DISTANCE;
			bottomLeftLon -= GPS_DISTANCE;
			upperRightLat += GPS_DISTANCE;
			upperRightLon += GPS_DISTANCE;
			
			c.close();
			
			if (onRoute){
				c = db.getBusStopsInBoundaryOnRoute(searchRoute, bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
			} else {
				c = db.getBusStopsInBoundary(bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
			}
			
			found = c.moveToFirst();
		}
		
		if (c != null) {
			c.close();
		}
		
		/* Grow the search boundary a further time, to take account for any stops that may
		*  be missed due to fact the search area is rectangular, not circular.
		*/
		bottomLeftLat -= GPS_DISTANCE;
		bottomLeftLon -= GPS_DISTANCE;
		upperRightLat += GPS_DISTANCE;
		upperRightLon += GPS_DISTANCE;
		
		if (onRoute){
			c = db.getBusStopsInBoundaryOnRoute(searchRoute, bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
		} else {
			c = db.getBusStopsInBoundary(bottomLeftLat, upperRightLat, bottomLeftLon, upperRightLon);
		}
		
		if (c.getCount() < 1){
			c.close();
			return null;
		}
		
		ArrayList<BusStop> stops = NearestStopActivity.arrangeByNearest(args[0].getContext(), c, here, totalDistanceBoundary);
		BusStop nearestStop = stops.get(0);
		
		c.close();
		return nearestStop;
	}
}
