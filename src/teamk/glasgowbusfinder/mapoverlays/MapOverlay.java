package teamk.glasgowbusfinder.mapoverlays;

import java.util.ArrayList;

import net.londatiga.android.QuickAction;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * An abstract map overlay which only draws overlay items
 * beyond a particular zoom level, or upon moving the map
 * more than a specified amount.
 * 
 * @author Euan Freeman
 */
public abstract class MapOverlay extends ItemizedOverlay<OverlayItem> {
	protected final static int MINIMUM_REDRAW_MOVE = 50;
	protected final static int ZOOM_LIMIT = 16;
	private Context context;
	private MapView mapView;
	private GeoPoint lastDrawnCenter;
	private int lastDrawnZoom;
	private DatabaseHelper database;
	private ArrayList<OverlayItem> overlayItems;
	protected QuickAction qa;
	
	public MapOverlay(Drawable defaultMarker, Context context, MapView mapView) {
		/* Bound the overlay point to the bottom center of marker */
		super(boundCenterBottom(defaultMarker));
		this.context = context;
		
		overlayItems = new ArrayList<OverlayItem>();
		
		this.mapView = mapView;
		
		lastDrawnCenter = mapView.getMapCenter();
		lastDrawnZoom = -1; // Set to -1, force it to draw on create
		
		database = DatabaseHelper.getInstance(context);
	}

	public QuickAction getQuickAction() {
		return qa;
	}
	
	protected abstract void drawItems();
	
	protected void addOverlayItem(OverlayItem item) {
		overlayItems.add(item);
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return overlayItems.get(i);
	}

	@Override
	public int size() {
		return overlayItems.size();
	}
	
	/* Getters and setters */
	
	public Context getContext() {
		return context;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}

	public MapView getMapView() {
		return mapView;
	}

	public void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	public DatabaseHelper getDatabase() {
		return database;
	}

	public void setDatabase(DatabaseHelper db) {
		this.database = db;
	}

	public ArrayList<OverlayItem> getOverlayItems() {
		return overlayItems;
	}

	public void setOverlayItems(ArrayList<OverlayItem> overlayItems) {
		this.overlayItems = overlayItems;
	}

	/**
	 * Draw the overlay if necessary. Will only redraw if the zoom
	 * has changed, or if the user has moved the map by at least a
	 * certain amount.
	 * @param canvas The Canvas on which to draw.
	 * @param mapView The MapView currently being viewed by the user.
	 * @param shadow
	 */
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		
		if (shadow)
			return;
		
		int currentZoom = mapView.getZoomLevel();
		GeoPoint currentCenter = mapView.getMapCenter();
		
		int redraw_threshold = 0;
		
		if (currentZoom >= ZOOM_LIMIT) {
			redraw_threshold = MINIMUM_REDRAW_MOVE;
		} else {
			redraw_threshold = MINIMUM_REDRAW_MOVE * (ZOOM_LIMIT - currentZoom + 3);
		}
		
		/* Check if the user zoomed in or out, or if they
		 * panned around the map. If so, re-populate the overlay.
		 * This method gets called a LOT though, so to save expensive
		 * database queries, we'll only draw the stops again when
		 * the user has panned more than 50m from the last point
		 * at which stops were drawn.
		 */
		if (currentZoom != lastDrawnZoom || distanceBetween(lastDrawnCenter, currentCenter) > redraw_threshold) {
			lastDrawnZoom = currentZoom;
			lastDrawnCenter = currentCenter;
			drawItems();
		}
	}
	
	public void refresh() {
		drawItems();
	}
	
	/**
	 * Calculate the approximate distance between two
	 * objects of type GeoPoint.
	 * @param p1 First point.
	 * @param p2 Second point.
	 * @return Distance between the points as a float.
	 */
	public static float distanceBetween(GeoPoint p1, GeoPoint p2) {
		float[] result = new float[1];
		
		Location.distanceBetween(
				p1.getLatitudeE6() / 1e6, p1.getLongitudeE6() / 1e6,
				p2.getLatitudeE6() / 1e6, p2.getLongitudeE6() /  1e6, result);
		
		return result[0];
	}
}
