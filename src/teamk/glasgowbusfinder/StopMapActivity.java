package teamk.glasgowbusfinder;

import java.util.List;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.location.MyLocationListener;
import teamk.glasgowbusfinder.mapoverlays.BusStopOverlay;
import teamk.glasgowbusfinder.mapoverlays.FixedMyLocationOverlay;
import teamk.glasgowbusfinder.mapoverlays.SubwayOverlay;
import teamk.glasgowbusfinder.ui.preferences.MapPreferencesActivity;
import teamk.glasgowbusfinder.util.NearestStopArgs;
import teamk.glasgowbusfinder.util.NearestStopTask;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * This activity displays a map which shows bus stops.
 * Selecting a bus stop takes the user to the departures
 * board for that stop.
 * 
 * @author Euan Freeman
 * @author Calum McCall
 * @author Aidan Smeaton
 * 
 * @see http://developer.android.com/guide/tutorials/views/hello-mapview.html
 */
public class StopMapActivity extends MapActivity {
	/* This number is used in a calculation to determine
	 * how much to move the map on trackball movement.
	 * Larger values decrease movement speed. */
	private static final int MOVEMENT_DELTA = 100;
	
	/* If the map is zoomed out less than this level,
	 * selecting "Current Location" will zoom in to the
	 * default zoom level.
	 */
	private static final int MAX_ZOOMED_OUT_LEVEL = 12;
	
	/* Default level to zoom to. */
	private static final int DEFAULT_ZOOM_LEVEL = 17;
	
	/* Default level to zoom to for displaying a route. */
	private static final int DEFAULT_ROUTE_ZOOM_LEVEL = 14;
	
	private FixedMyLocationOverlay myLocationOverlay;
	private MapView mapView;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private BusStopOverlay busStopsOverlay;
	private SubwayOverlay subwayOverlay;
	private Drawable busStopDrawable;
	private Drawable busStopFavouriteDrawable;
	private Drawable busStopSelectedDrawable;
	private Drawable subwayDrawable;
	private DatabaseHelper db;
	private String stopCode;
	private String route;
	
	/* Nearest Stop task - an asyncronous task for finding nearest bus stop */
	private NearestStopTask nearestStopTask;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		
		db = DatabaseHelper.getInstance(this);
		
		mapView = (MapView) findViewById(R.id.mapview);
		
		/* Enable zoom controls. */
		mapView.setBuiltInZoomControls(true);
		mapView.displayZoomControls(true);
		mapController = mapView.getController();
		mapController.setZoom(DEFAULT_ZOOM_LEVEL);
		
		/* See if this activity was called with GPS coordinates to go to. */
		stopCode = getIntent().getStringExtra("stopcode");
		route = getIntent().getStringExtra("route");
		
		if (stopCode != null) {
			/* This map was called with a stop code - find and navigate to this stop. */
			initMyLocationOverlay(false);
			
			Cursor c = db.getBusStop(stopCode);
			
			if (c.moveToFirst()) {
				/* Move the map to this stop. */
				mapController.setCenter(new GeoPoint(
						(int) (c.getDouble(DatabaseHelper.BUS_STOP_LATITUDE) * 1e6),
						(int) (c.getDouble(DatabaseHelper.BUS_STOP_LONGITUDE) * 1e6)));
			}
		} else {
			/* Default to just under the "Glasgow" label on the map,
			 * as a fallback if no GPS fix is available. */
			mapController.setCenter(new GeoPoint((int) (55.864724 * 1e6), (int) (-4.25711 * 1e6)));
			
			/* No coordinates provided. Wait for the location
			 * overlay to go the first location fix.
			 */
	        initMyLocationOverlay(true);
		}
		
		/* Set up map overlays. */
		mapOverlays = mapView.getOverlays();
		
		TextView routeLabel = (TextView) findViewById(R.id.mapview_routelabel);
		
		/* If a particular route is being shown on map,
		 * determine whether or not to display the
		 * route information header.
		 */
		if (route != null){
			routeLabel.setVisibility(View.VISIBLE);
			routeLabel.setText(getString(R.string.showing_route) + " " + route);
			
			/* Adjust the zoom level to the default route display zoom level */
			mapController.setZoom(DEFAULT_ROUTE_ZOOM_LEVEL);
		} else {
			routeLabel.setVisibility(View.INVISIBLE);
		}
		
		createAndAddBusStopOverlay();
	}
	
	/**
	 * Called when this activity gets paused. Turns
	 * off location updates to conserve battery.
	 */
	@Override
	public void onPause() {
		super.onPause();
		
		myLocationOverlay.disableMyLocation();
		
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.removeUpdates(MyLocationListener.getInstance());
	}
	
	/**
	 * Called when this activity gets resumed. Turns
	 * location updates back on. Inspects the shared
	 * preferences to decide which overlays to show.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		myLocationOverlay.enableMyLocation();
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		/* Check if the Show Subway preference is enabled. */
		if (sp.getBoolean(getString(R.string.pref_mapShowSubway), true)) {
			if (subwayOverlay == null) {
				createAndAddSubwayOverlay();
			} else {
				/* Check if it's already shown - we dont
				 * want to show it twice.
				 */
				if (! mapOverlays.contains(subwayOverlay)) {
					mapOverlays.add(subwayOverlay);
				}
			}
		} else {
			if (subwayOverlay != null) {
				mapOverlays.remove(subwayOverlay);
			}
		}
		
		/* Check if user has chosen to only show favourites */
		busStopsOverlay.setDrawFavouriteStopsOnly(sp.getBoolean(getString(R.string.pref_mapFavOnly), false));
		
		/* Update the overlay */
		busStopsOverlay.refresh();
		
		/* It's desirable to have MyLocationListener request
		 * frequent location updates while on the map. This
		 * means that the non-map location information will
		 * be updated for when other activities require it.
		 */
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 20, 0, MyLocationListener.getInstance());
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20, 0, MyLocationListener.getInstance());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		/*
		 * Try to dismiss both the busStopsOverlay and subwayOverlay
		 * quick action popups, incase they were showing at the
		 * time the activity got destroyed.
		 */
		try {
			busStopsOverlay.getQuickAction().dismiss();
		} catch (Exception e) {
			/* Either the popup was already hidden, or
			 * not even created. Can be safely ignored.
			 */
		}
		
		try {
			subwayOverlay.getQuickAction().dismiss();
		} catch (Exception e) {
			/* Either the popup was already hidden, or
			 * not even created. Can be safely ignored.
			 */
		}
		
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.removeUpdates(MyLocationListener.getInstance());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mapmenu, menu);

		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mapmenu_currentlocation:
			goToCurrentLocation();
			
			return true;
		case R.id.mapmenu_key:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.mapkey);
			dialog.setTitle(R.string.map_key);
			dialog.setCancelable(true);
			
			Button closeButton = (Button) dialog.findViewById(R.id.mapkey_close);
			
			closeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			dialog.show();
			
			return true;
		case R.id.mapmenu_mapmode:
			mapView.setSatellite(!mapView.isSatellite());
			return true;
		case R.id.mapmenu_neareststop:
			goToNearestStop();
			return true;
		case R.id.mapmenu_preferences:
			Intent mapPrefIntent = new Intent(this, MapPreferencesActivity.class);
			this.startActivity(mapPrefIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/*
	 * Creates the bus stop map overlay and
	 * adds to the map.
	 */
	private void createAndAddBusStopOverlay() {
		/* Set up the drawables required by this overlay */
		busStopDrawable = this.getResources().getDrawable(R.drawable.ic_bus);
		busStopFavouriteDrawable = this.getResources().getDrawable(R.drawable.ic_bus_favourite);
		busStopSelectedDrawable = this.getResources().getDrawable(R.drawable.ic_bus_selected);
		
		busStopsOverlay = new BusStopOverlay(busStopDrawable, busStopFavouriteDrawable, busStopSelectedDrawable, this, mapView, route);
		busStopsOverlay.refresh();
		
		mapOverlays.add(busStopsOverlay);
	}
	
	/*
	 * Creates the subway map overlay and adds
	 * to the map.
	 */
	private void createAndAddSubwayOverlay() {
		subwayDrawable = this.getResources().getDrawable(R.drawable.ic_subway);
		subwayOverlay = new SubwayOverlay(subwayDrawable, this, mapView);
		
		mapOverlays.add(subwayOverlay);
	}
	
	/*
	 * Navigates to the current location of the device. If
	 * no location fix is available, the user is informed of
	 * this. As soon as a fix is available, the map navigates
	 * to the device location
	 */
	private void goToCurrentLocation() {
		GeoPoint myLocation = myLocationOverlay.getMyLocation();
		
		if (myLocation != null) {
			navigateTo(myLocation);
			
			adjustZoom();
		} else {
			Toast.makeText(mapView.getContext(), R.string.no_location_message, Toast.LENGTH_LONG).show();
			
			myLocationOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					navigateTo(myLocationOverlay.getMyLocation());
					
					adjustZoom();
				}
			});
		}
	}
	
	/*
	 * Navigates to the nearest stop to the device. If
	 * a route is shown, finds the nearest stop on the
	 * route. Otherwise searches all stops.
	 */
	private void goToNearestStop() {
		String shownRoute = (route == null) ? "" : route;
		
		GeoPoint currentLocation = myLocationOverlay.getMyLocation();
		
		if (currentLocation == null) {
			Toast.makeText(mapView.getContext(), R.string.no_location_message, Toast.LENGTH_LONG).show();
			
			return;
		}
		
		NearestStopArgs args = new NearestStopArgs(getApplicationContext(), currentLocation, shownRoute);
		
		nearestStopTask = new NearestStopTask();
		nearestStopTask.execute(args);
		
		GeoPoint nearestStopLocation;
		
		try {
			nearestStopLocation = nearestStopTask.get().getLocation();
		} catch (Exception e) {
			Toast.makeText(mapView.getContext(), R.string.cannot_find_nearest_stop, Toast.LENGTH_LONG).show();
			
			return;
		}
		
		if (nearestStopLocation != null){
			navigateTo(nearestStopLocation);
		} else {
			Toast.makeText(this, getString(R.string.no_nearest_stop), Toast.LENGTH_LONG).show();
		}
	}
	
	/*
	 * Automatically adjusts the zoom level when zoomed out
	 * so far that no detail is shown. Zooms to an
	 * amount where relevant detail is shown, depending on
	 * whether or not the map is showing a route or just stops.
	 */
	private void adjustZoom() {
		/* If zoomed out to the point where no extra detail is displayed... */
		if (mapView.getZoomLevel() < MAX_ZOOMED_OUT_LEVEL) {
			if (route != null && route.length() > 0) {
				/* ... a route is shown, so zoom in to see the route */
				mapController.setZoom(MAX_ZOOMED_OUT_LEVEL);
			} else {
				/* ... no route is shown, so zoom in to stop-level */
				mapController.setZoom(DEFAULT_ZOOM_LEVEL);
			}
		}
	}
	
	private void navigateTo(GeoPoint point) {
		/*
		 * Inner Runnable will force the map to update
		 * if it arrives at it's destination point.
		 */
		mapController.animateTo(point, new Runnable() {
			@Override
			public void run() {
				adjustZoom();
				busStopsOverlay.refresh();
				mapView.postInvalidate();
				
				/* Simulate a tap event to try and "select"
				 * whatever was navigated to.
				 *
				 * These events are dispatched from the centre of the map,
				 * as trackball movement ensures the "pointer" / "cursor"
				 * is always at the center.
				 */
				if (!busStopsOverlay.onTap(mapView.getMapCenter(), mapView)) {
					if (subwayOverlay != null) {
						subwayOverlay.onTap(mapView.getMapCenter(), mapView);
					}
				}
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			onTrackballClick(event);
			break;
		case MotionEvent.ACTION_MOVE:
			onTrackballMove(event);
			break;
		default:
			return false;
		}

		return true;
	}
	
	/*
	 * Responds to the user pressing the trackball.
	 */
	private void onTrackballClick(MotionEvent event) {
		mapView.dispatchTouchEvent(event);
		
		/* These events are dispatched from the centre of the map,
		 * as trackball movement ensures the "pointer" / "cursor"
		 * is always at the center.
		 */
		if (!subwayOverlay.onTap(mapView.getMapCenter(), mapView))
			busStopsOverlay.onTap(mapView.getMapCenter(), mapView);
	}
	
	/*
	 * Responds to the user moving the trackball. Scrolls
	 * the map in response to trackball movement.
	 */
	private void onTrackballMove(MotionEvent event) {
		double xOffset = event.getX();
		double yOffset = event.getY();
		
		double longitudeSpan = (mapView.getLongitudeSpan() / 2);
		double latitudeSpan = (mapView.getLatitudeSpan() / 2);
		
		mapController.scrollBy((int) (xOffset * longitudeSpan / MOVEMENT_DELTA),
								(int) (yOffset * latitudeSpan / MOVEMENT_DELTA));
	}
	
	/**
	 * Initialise the location overlay, going to current location if desired.
	 * We may not always want to do this upon creating the map activity,
	 * for example, if the map activity is called to show a specific location.
	 * 
	 * @param showFirstFix If true, go to the first location fix available.
	 * 
	 * @see http://consultingblogs.emc.com/harolee/archive/2009/01/08/android-how-to-use-mylocationoverlay-to-find-where-i-am.aspx
	 */
	private void initMyLocationOverlay(boolean showFirstFix) {
		myLocationOverlay = new FixedMyLocationOverlay(this, mapView);
		
		mapView.getOverlays().add(myLocationOverlay);
		
		myLocationOverlay.enableMyLocation();
		
		if (showFirstFix) {
			myLocationOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					navigateTo(myLocationOverlay.getMyLocation());
				}
			});
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isRouteDisplayed() {
		/* Note: this overrides some Google code,
		 * a "route" is not the same as a route in
		 * the context of this app.
		 */
		return false;
	}
}