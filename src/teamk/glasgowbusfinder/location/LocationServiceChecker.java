package teamk.glasgowbusfinder.location;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;

/**
 * Checks to see if an Android location service is enabled.
 * 
 * @author Euan Freeman
 */
public class LocationServiceChecker {
	/* Unique identifier for the network location service. */
	public static final int NETWORK_LOCATION_SERVICE = 0;
	/* Unique identifier for the GPS location service. */
	public static final int GPS_LOCATION_SERVICE = 1;
	
	private static LocationServiceChecker instance;
	private static Context context;
	
	private LocationServiceChecker(Context context) {
		LocationServiceChecker.context = context;
	}
	
	public static LocationServiceChecker getInstance(Context context) {
		if (instance == null) {
			instance = new LocationServiceChecker(context);
		} else {
			LocationServiceChecker.context = context;
		}
		
		return instance;
	}
	
	/**
	 * Check if a location service is enabled.
	 * 
	 * @see http://advback.com/android/checking-if-gps-is-enabled-android/
	 */
	public void checkForService(int serviceType) {
		if (serviceType < NETWORK_LOCATION_SERVICE || serviceType > GPS_LOCATION_SERVICE) {
			return;
		}
		
		LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		String locationService;
		
		if (serviceType == NETWORK_LOCATION_SERVICE) {
			locationService = LocationManager.NETWORK_PROVIDER;
		} else {
			locationService = LocationManager.GPS_PROVIDER;
		}
		
		if (!locManager.isProviderEnabled(locationService)){
			/* GPS is disabled; ask user to enable it. */
			createGpsDisabledAlert();
		}
	}
	
	/*
	 * Source: http://advback.com/android/checking-if-gps-is-enabled-android/
	 */
	private void createGpsDisabledAlert(){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Your GPS is disabled! Would you like to enable it?");
		builder.setCancelable(false);
		
		builder.setPositiveButton("Enable GPS",
				new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int id){
				showGpsOptions();
			}
		});
		
		builder.setNegativeButton("Do nothing",
				new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int id){
				dialog.cancel();
			}
		});
		
		AlertDialog alert = builder.create();
		alert.show();
	}

	/*
	 * Source: http://advback.com/android/checking-if-gps-is-enabled-android/
	 */
	private void showGpsOptions(){
		Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		context.startActivity(gpsOptionsIntent);
	}
}
