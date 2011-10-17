package teamk.glasgowbusfinder;

import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.location.LocationServiceChecker;
import teamk.glasgowbusfinder.location.MyLocationListener;
import teamk.glasgowbusfinder.util.LocationConverter;
import teamk.glasgowbusfinder.util.NearestStopArgs;
import teamk.glasgowbusfinder.util.NearestStopTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SplashActivity extends Activity {
	private LocationManager lm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		DatabaseHelper.getInstance(this);
		
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		/* This app needs GPS enabled; check
		 * if the service is enabled. */
		LocationServiceChecker.getInstance(this).checkForService(LocationServiceChecker.GPS_LOCATION_SERVICE);
		
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, MyLocationListener.getInstance());
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, MyLocationListener.getInstance());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		lm.removeUpdates(MyLocationListener.getInstance());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.dashboardmenu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.dashboard_information:
			infoClicked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void infoClicked() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.disclaimer);
		dialog.setTitle(R.string.disclaimer);
		dialog.setCancelable(true);
		
		Button closeButton = (Button) dialog.findViewById(R.id.disclaimer_close);
		
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
	}
	
	public void findStopClicked(View v) {
		/*
		Intent findStopIntent = new Intent(this, FindStopActivity.class);
		this.startActivity(findStopIntent);
		*/
		
		Intent findIntent = new Intent(this, TabbedSearchActivity.class);
		findIntent.putExtra("firstTab", "normalSearch");
		startActivity(findIntent);
	}
	
	public void mapClicked(View v) {
		Intent mapIntent = new Intent(this, StopMapActivity.class);
		this.startActivity(mapIntent);
	}
	
	public void favouritesClicked(View v) {
		Intent favouritesIntent = new Intent(this, FavouritesActivity.class);
		this.startActivity(favouritesIntent);
	}
	
	public void alertsClicked(View v) {
		Intent alertsIntent = new Intent(this, AlertsActivity.class);
		this.startActivity(alertsIntent);
	}
	
	public void nearestStopClicked(View v) {
		Location current = MyLocationListener.getInstance().getLocation();
		
		if (current == null) {
			Toast.makeText(this, R.string.waiting_for_fix, Toast.LENGTH_LONG).show();
			
			return;
		}
		
		NearestStopTask nst = new NearestStopTask();
		NearestStopArgs nsa = new NearestStopArgs(getApplicationContext(), LocationConverter.locationToGeoPoint(current), "");
		BusStop nearestStop;
		
		nst.execute(nsa);
		
		try {
			nearestStop = nst.get();
		} catch (Exception e) {
			Toast.makeText(this, R.string.cannot_find_nearest_stop, Toast.LENGTH_LONG).show();
			
			return;
		}
		
		if (nearestStop == null) {
			Toast.makeText(this, R.string.cannot_find_nearest_stop, Toast.LENGTH_LONG).show();
			
			return;
		}
		
		Intent departuresIntent = new Intent(this, TabbedStopActivity.class);
		departuresIntent.putExtra("stopcode", nearestStop.getStopCode());
		departuresIntent.putExtra("startTab", TabbedStopActivity.DEPARTURES);
		startActivity(departuresIntent);
	}
	
	public void findServiceClicked(View v) {
		Intent findServiceIntent = new Intent(this, FindServiceActivity.class);
		this.startActivity(findServiceIntent);
	}
}
