package teamk.glasgowbusfinder.mapoverlays;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.quickactions.MapSubwayQuickAction;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class SubwayOverlay extends MapOverlay {
	public SubwayOverlay(Drawable defaultMarker, Context context, MapView mapView) {
		/* Bound the overlay point to the bottom center of marker */
		super(defaultMarker, context, mapView);
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = getOverlayItems().get(index);
		
		/* Center the selected point. */
		getMapView().getController().setCenter(item.getPoint());
		
		qa = new MapSubwayQuickAction(getMapView(), item);
		qa.show();
		
		return true;
	}
	
	/**
	 * Draws each subway station visible on
	 * the section of the map being viewed.
	 */
	@Override
	public void drawItems() {
		getOverlayItems().clear();
		super.setLastFocusedIndex(-1);
		super.populate();

		/* Don't want to draw stops if we're zoomed out;
		 * screen would become way too cluttered.
		 */
		if (getMapView().getZoomLevel() > ZOOM_LIMIT) {
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
			Cursor c = getDatabase().getSubwayStationsInBoundary(lat1 / 1e6, lat2 / 1e6, lon1 / 1e6, lon2 / 1e6);
			
			/* Add each record in the cursor as a new overlay item. */
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						getOverlayItems().add(new OverlayItem(
										new GeoPoint((int) (c.getDouble(DatabaseHelper.SUBWAY_STATION_LATITUDE) * 1e6),
													(int) (c.getDouble(DatabaseHelper.SUBWAY_STATION_LONGITUDE) * 1e6)),
										c.getString(DatabaseHelper.SUBWAY_STATION_NAME), ""));
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
}