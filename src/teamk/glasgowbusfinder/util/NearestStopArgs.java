package teamk.glasgowbusfinder.util;

import android.content.Context;

import com.google.android.maps.GeoPoint;

/**
 * This class represents a collection of arguments
 * to a NearestStopTask instance.
 * 
 * @author Euan Freeman
 */
public class NearestStopArgs {
	private Context context;
	private GeoPoint location;
	private String route;
	
	public NearestStopArgs(Context context, GeoPoint location, String route) {
		super();
		this.context = context;
		this.location = location;
		this.route = route;
	}

	public Context getContext() {
		return context;
	}

	public GeoPoint getLocation() {
		return location;
	}

	public String getRoute() {
		return route;
	}
}
