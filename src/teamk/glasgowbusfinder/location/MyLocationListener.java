package teamk.glasgowbusfinder.location;

import java.util.ArrayList;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Listen for location updates and offer this to
 * any class which registers to receive updates.
 * 
 * @author Euan Freeman
 */
public class MyLocationListener implements LocationListener {
	private static ArrayList<LocationUpdateObserver> observers;
	private static Location lastFix;
	private static MyLocationListener instance;
	
	private MyLocationListener() {
		/* Set a small starting size; this isn't expected
		 * to grow much.
		 */
		observers = new ArrayList<LocationUpdateObserver>(3);
		
		lastFix = null;
	}
	
	public static MyLocationListener getInstance() {
		if (instance == null) {
			instance = new MyLocationListener();
		}
		
		return instance;
	}
	
	public Location getLocation() {
		return lastFix;
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		
		lastFix = loc;
				
		for (LocationUpdateObserver observer : observers) {
			observer.locationUpdate(loc);
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		
	}
}
