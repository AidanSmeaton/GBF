package teamk.glasgowbusfinder.data;

import android.content.Context;
import android.database.Cursor;

import com.google.android.maps.GeoPoint;

public class BusStop implements Comparable<BusStop> {
	
	private String stopCode;
	private String name;
	private String street;
	private String locality;
	private boolean favourite;
	private double distance;
	private double latitude;
	private double longitude;
	
	public BusStop(String stopCode, String name, String street, String locality,
			boolean favourite, float distance) {
		this.stopCode = stopCode;
		this.name = name;
		this.street = street;
		this.locality = locality;
		this.favourite = favourite;
		this.distance = distance;
	}
	
	public BusStop(String stopCode, String name, boolean favourite) {
		this.stopCode = stopCode;
		this.name = name;
		this.street = "";
		this.locality = "";
		this.favourite = favourite;
		this.distance = -1;
		this.latitude = -1;
		this.longitude = -1;
	}
	
	public BusStop(String stopCode, String name, String street, String locality,
			boolean favourite, double distance, double latitude, double longitude) {
		this.stopCode = stopCode;
		this.name = name;
		this.street = street;
		this.locality = locality;
		this.favourite = favourite;
		this.distance = distance;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public BusStop(String stopCode, Context context) {
		Cursor c = DatabaseHelper.getInstance(context).getBusStop(stopCode);
		c.moveToFirst();
		this.stopCode = stopCode;
		this.name = c.getString(DatabaseHelper.BUS_STOP_NAME);
		this.street = c.getString(DatabaseHelper.BUS_STOP_STREET);
		this.locality = c.getString(DatabaseHelper.BUS_STOP_LOCALITY);
		this.favourite = (c.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1);
		this.distance = -1;
		this.latitude = Double.parseDouble(c.getString(DatabaseHelper.BUS_STOP_LATITUDE));
		this.longitude = Double.parseDouble(c.getString(DatabaseHelper.BUS_STOP_LONGITUDE));
	}
	

	@Override
	public int compareTo(BusStop other) {
		if (distance > other.getDistance()) return 1;
		else if (distance == other.getDistance()) return 0;
		else return -1;
	}

	public GeoPoint getLocation() {
		return new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6));
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public boolean isFavourite() {
		return favourite;
	}

	public void setFavourite(boolean favourite) {
		this.favourite = favourite;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public void setStopCode(String stopCode) {
		this.stopCode = stopCode;
	}

	public String getStopCode() {
		return stopCode;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
}
