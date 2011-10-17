package teamk.glasgowbusfinder.mapoverlays;

import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.quickactions.MapBusStopQuickAction;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Map overlay for bus stops. Fetches stop information from
 * a database and only draws stops which are within the boundary
 * of the map shown on the screen.
 * 
 * @author Euan Freeman
 * @author Aidan Smeaton
 */
public class BusStopOverlay extends MapOverlay {
	protected final static int MIN_ROUTE_ZOOM_LEVEL = 12;
	
	private Drawable busStopDrawable;
	private Drawable busStopFavouriteDrawable;
	private Drawable busStopSelectedDrawable;
	private Drawable routeDrawable;
	private boolean drawFavouriteStopsOnly;
	private BusStopOverlayItem selected;
	private String route;

	/**
	 * Creates a new BusStopOverlay with a specified marker, with
	 * a point bound to the center bottom.
	 * @param defaultMarker
	 * @param context
	 * @param mapView
	 */
	public BusStopOverlay(Drawable busStopDrawable, Drawable busStopFavouriteDrawable,
							Drawable busStopSelectedDrawable, Context context, MapView mapView) {
		/* Bound the overlay point to the bottom center of marker */
		super(busStopDrawable, context, mapView);
		
		this.busStopDrawable = busStopDrawable;
		busStopDrawable.setBounds(0, 0, busStopDrawable.getIntrinsicWidth(), busStopDrawable.getIntrinsicHeight());
		super.boundCenterBottom(this.busStopDrawable);
		
		this.busStopFavouriteDrawable = busStopFavouriteDrawable;
		busStopFavouriteDrawable.setBounds(0, 0, busStopDrawable.getIntrinsicWidth(), busStopDrawable.getIntrinsicHeight());
		super.boundCenterBottom(this.busStopFavouriteDrawable);
		
		this.busStopSelectedDrawable = busStopSelectedDrawable;
		busStopSelectedDrawable.setBounds(0, 0, busStopDrawable.getIntrinsicWidth(), busStopDrawable.getIntrinsicHeight());
		super.boundCenterBottom(this.busStopSelectedDrawable);
		
		this.routeDrawable = mapView.getResources().getDrawable(R.drawable.route_stop);
		routeDrawable.setBounds(0, 0, routeDrawable.getIntrinsicWidth(), routeDrawable.getIntrinsicHeight());
		super.boundCenter(this.routeDrawable);
		
		drawFavouriteStopsOnly = false;
		selected = null;
		route = null;
	}
	
	/**
	 * Creates a new BusStopOverlay with a specified marker, with
	 * a point bound to the center bottom.
	 * @param defaultMarker
	 * @param context
	 * @param mapView
	 */
	public BusStopOverlay(Drawable busStopDrawable, Drawable busStopFavouriteDrawable,
							Drawable busStopSelectedDrawable, Context context, MapView mapView, String route) {
		/* Bound the overlay point to the bottom center of marker */
		this(busStopDrawable, busStopFavouriteDrawable, busStopSelectedDrawable, context, mapView);
		this.route = route;
	}
	
	@Override
	protected boolean onTap(int index) {
		/* Ignore clicks when a route is shown and user is zoomed out to view it */
		if (route != null && getMapView().getZoomLevel() < ZOOM_LIMIT) {
			return true;
		}
		
		BusStopOverlayItem item = (BusStopOverlayItem) getOverlayItems().get(index);
		
		if (selected != null) {
			if (selected.getStop().isFavourite()) {
				selected.setMarker(busStopFavouriteDrawable);
			} else {
				selected.setMarker(busStopDrawable);
			}
		}

		selected = item;
		
		item.setMarker(busStopSelectedDrawable);
		
		/* Center the selected point. */
		getMapView().getController().setCenter(item.getPoint());
		
		qa = new MapBusStopQuickAction(getMapView(), item);
		qa.show();
		
		return true;
	}

	/**
	 * Draws the bus-stops within the viewing boundary
	 * of the map. Clears the overlay, fetches all "visible"
	 * stops from the database, then populates the overlay.
	 */
	@Override
	public void drawItems() {
		getOverlayItems().clear();
		setLastFocusedIndex(-1);
		populate();

		int zoom = getMapView().getZoomLevel();
		
		/* Don't want to draw stops if we're zoomed out;
		 * screen would become way too cluttered.
		 */
		if (zoom > ZOOM_LIMIT) {
			GeoPoint mapCenter = getMapView().getMapCenter();
			int latitudeSpan = (getMapView().getLatitudeSpan() / 2) + 1;
			int longitudeSpan = (getMapView().getLongitudeSpan() / 2) + 1;
			int lat1 = mapCenter.getLatitudeE6() - latitudeSpan;
			int lat2 = mapCenter.getLatitudeE6() + latitudeSpan;
			int lon1 = mapCenter.getLongitudeE6() - longitudeSpan;
			int lon2 = mapCenter.getLongitudeE6() + longitudeSpan;

			if (getDatabase() == null) {
				setDatabase(DatabaseHelper.getInstance(getContext()));
				
				if (getDatabase() == null) {
					return;
				}
			}
			
			Cursor c = null;
			
			/* Determine whether to draw only stops on a particular route,
			 * or all visible stops.
			 */
			if (route != null){
				/* Divide by 1e6 as database uses degrees
				 * as opposed to Android's microdegrees. */
				c = getDatabase().getBusStopsInBoundaryOnRoute(route, lat1 / 1e6, lat2 / 1e6, lon1 / 1e6, lon2 / 1e6);
			} else {
				/* Divide by 1e6 as database uses degrees
				 * as opposed to Android's microdegrees. */
				c = getDatabase().getBusStopsInBoundary(lat1 / 1e6, lat2 / 1e6, lon1 / 1e6, lon2 / 1e6);
			}
			
			/* Add each record in the cursor as a new overlay item. */
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						BusStop stop = new BusStop(c.getString(DatabaseHelper.BUS_STOP_CODE),
											c.getString(DatabaseHelper.BUS_STOP_NAME),
											c.getString(DatabaseHelper.BUS_STOP_STREET),
											c.getString(DatabaseHelper.BUS_STOP_LOCALITY),
											c.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1,
											0);
						
						if(!drawFavouriteStopsOnly) {
							BusStopOverlayItem newOverlayItem = new BusStopOverlayItem(
									new GeoPoint((int) (c.getDouble(DatabaseHelper.BUS_STOP_LATITUDE) * 1e6),
											(int) (c.getDouble(DatabaseHelper.BUS_STOP_LONGITUDE) * 1e6)), stop);
							
							if (c.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1) {
								newOverlayItem.setMarker(busStopFavouriteDrawable);
							} else {
								newOverlayItem.setMarker(busStopDrawable);
							}
							
							addOverlayItem(newOverlayItem);
						} else {
							
							if (c.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1) {
								BusStopOverlayItem newOverlayItem = new BusStopOverlayItem(
										new GeoPoint((int) (c.getDouble(DatabaseHelper.BUS_STOP_LATITUDE) * 1e6),
												(int) (c.getDouble(DatabaseHelper.BUS_STOP_LONGITUDE) * 1e6)), stop);
								newOverlayItem.setMarker(busStopFavouriteDrawable);
								addOverlayItem(newOverlayItem);
							}
						}
					} while (c.moveToNext());
					
					if (selected != null) {
						selected.setMarker(busStopSelectedDrawable);
						
						addOverlayItem(selected);
					}
				}
				/* Added all the overlay items - now populate
				 * the overlay.
				 */
				super.populate();
				
				c.close();
			}
		} else if (zoom >= MIN_ROUTE_ZOOM_LEVEL && route != null) {
			GeoPoint mapCenter = getMapView().getMapCenter();
			int latitudeSpan = (getMapView().getLatitudeSpan() / 2) + 1;
			int longitudeSpan = (getMapView().getLongitudeSpan() / 2) + 1;
			int lat1 = mapCenter.getLatitudeE6() - latitudeSpan;
			int lat2 = mapCenter.getLatitudeE6() + latitudeSpan;
			int lon1 = mapCenter.getLongitudeE6() - longitudeSpan;
			int lon2 = mapCenter.getLongitudeE6() + longitudeSpan;
			
			if (getDatabase() == null) {
				setDatabase(DatabaseHelper.getInstance(getContext()));
				
				if (getDatabase() == null) {
					return;
				}
			}
			
			/* Divide by 1e6 as database uses degrees
			 * as opposed to Android's microdegrees. */
			Cursor c = getDatabase().getBusStopsInBoundaryOnRoute(route, lat1 / 1e6, lat2 / 1e6, lon1 / 1e6, lon2 / 1e6);
			
			/* Add each record in the cursor as a new overlay item. */
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						BusStop stop = new BusStop(c.getString(DatabaseHelper.BUS_STOP_CODE),
								c.getString(DatabaseHelper.BUS_STOP_NAME),
								c.getString(DatabaseHelper.BUS_STOP_STREET),
								c.getString(DatabaseHelper.BUS_STOP_LOCALITY),
								c.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1,
								0);
						
						BusStopOverlayItem newOverlayItem = new BusStopOverlayItem(
								new GeoPoint((int) (c.getDouble(DatabaseHelper.BUS_STOP_LATITUDE) * 1e6),
										(int) (c.getDouble(DatabaseHelper.BUS_STOP_LONGITUDE) * 1e6)), stop);
						
						newOverlayItem.setMarker(routeDrawable);
						
						addOverlayItem(newOverlayItem);
					} while (c.moveToNext());
				}
				/* Added all the overlay items - now populate
				 * the overlay.
				 */
				super.populate();
				
				c.close();
			}
		}
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		if (selected != null) {
			selected.setDrawable(busStopSelectedDrawable);
		}
	}
	
	public boolean drawingFavouriteStopsOnly() {
		return drawFavouriteStopsOnly;
	}

	public void setDrawFavouriteStopsOnly(boolean drawFavouriteStopsOnly) {
		this.drawFavouriteStopsOnly = drawFavouriteStopsOnly;
	}
	
	public void toggleDrawFavouriteStopsOnly() {
		setDrawFavouriteStopsOnly(!drawFavouriteStopsOnly);
		
		if (drawFavouriteStopsOnly) {
			selected = null;
		}
	}
}