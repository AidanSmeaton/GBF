package teamk.glasgowbusfinder.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Scanner;

import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.util.TimeFormatter;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

/**
 * Copies an already existing SQLite database into this
 * application. Provides a singleton instance to a
 * database helper which provides cursors to the database.
 * @see http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
 * 
 * @author Juan-Manuel Flux�
 * 
 * @author Euan Freeman (queries)
 * @author Aidan Smeaton (queries)
 */
public class DatabaseHelper extends SQLiteOpenHelper {
	public static final int BUS_STOP_ID = 0;
	public static final int BUS_STOP_CODE = 1;
	public static final int BUS_STOP_LONGITUDE = 2;
	public static final int BUS_STOP_LATITUDE = 3;
	public static final int BUS_STOP_NAME = 4;
	public static final int BUS_STOP_INDICATOR = 5;
	public static final int BUS_STOP_LANDMARK = 6;
	public static final int BUS_STOP_STREET = 7;
	public static final int BUS_STOP_LOCALITY = 8;
	public static final int BUS_STOP_FAVOURITE = 9;
	public static final int BUS_STOP_ALIAS = 10;
	public static final int SUBWAY_STATION_ID = 0;
	public static final int SUBWAY_STATION_NAME = 1;
	public static final int SUBWAY_STATION_LATITUDE = 2;
	public static final int SUBWAY_STATION_LONGITUDE = 3;
	public static final int SUBWAY_STATION_PARKANDRIDE = 4;
	public static final int SUBWAY_STATION_BUSINTERCHANGE = 5;
	public static final int SUBWAY_STATION_RAILINTERCHANGE = 6;
	public static final int STOPSERVICES_STOP_ID = 0;
	public static final int STOPSERVICES_STOP_CODE = 1;
	public static final int STOPSERVICES_SERVICE_ID = 2;
	public static final int ALERT_ID = 0;
	public static final int ALERT_SERVICE = 1;
	public static final int ALERT_DESTINATION = 2;
	public static final int ALERT_STOP_CODE = 3;
	public static final int ALERT_TIME = 4;
	public static final int ALERT_MINUTES_BEFORE = 5;
	
	public static final int MAX_ALIAS_LENGTH = 50;
	
	//The Android's default system path of your application database.
	private static final String DB_PATH = "/data/data/teamk.glasgowbusfinder/databases/";
	private static final String DB_NAME = "glasgowbusfinder.db";
	private static DatabaseHelper instance;
	private static boolean exists;
	private SQLiteDatabase db;
	private final Context context;
	
	private DatabaseHelper(Context context) {
		super(context, DB_NAME, null, 1);
		this.context = context;
		
		exists = checkDatabaseExists();
	}
	
	/**
	 * Returns the singleton DataBaseHelper, instantiating
	 * if not yet done so.
	 * @param context Application context.
	 * @return The DataBaseHelper singleton.
	 */
	public static DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context);
			
			try {
				instance.createDatabase();
			} catch (IOException ioe) {
				/* Critical error - database could not be created. This
				 * is probably due to lack of space.
				 */
				throw new Error("Unable to create database");
			}
			
			if (!exists) {
				return null;
			}
		}
		
		return instance;
	}
	
	/**
	 * Creates an empty database on the system and
	 * populates it from a pre-prepared database.
	 */
	private void createDatabase() throws IOException{
		if(checkDatabaseExists()){
			/* Database exists, so we can just open it */
			instance.openDatabase();
		} else {
			/* Database does not exist - create a new one */
			this.getReadableDatabase();
			
			/* Set up an AsyncTask to go off and copy our
			 * pre-prepared database into the data
			 * section of this application.
			 */
			CreateDatabaseTask createDatabaseTask = new CreateDatabaseTask();
			createDatabaseTask.execute(0);
		}
	}

	/**
	 * Checks if the database already exists.
	 */
	public static boolean checkDatabaseExists() {
		SQLiteDatabase checkDB = null;

		/* Opening the database will fail if it doesn't
		 * exist, so we can catch and ignore that error.
		 */
		try {
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			/* Database does't exist yet - we can
			 * ignore this exception. This function
			 * is not for creating the database.
			 */
		}

		if (checkDB != null) {
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies each database file assets-folder to a single file
	 * in the data storage for this app.
	 * 
	 * @see http://stackoverflow.com/questions/2860157/load-files-bigger-than-1m-from-assets-folder/3093966#3093966
	 * @author Seva Alekseyev
	 */
	private void copyDatabase() throws IOException{
		File dbFile = new File(DB_PATH + DB_NAME);
		AssetManager am = context.getAssets();
		OutputStream os = new FileOutputStream(dbFile);
		dbFile.createNewFile();
		
		byte[] b = new byte[1024];
		int i, r;
		
		String[] Files = am.list("");
		Arrays.sort(Files);
		
		for (i = 1; i < 10; i++) {
			String fn = String.format("%d.db", i);
			if (Arrays.binarySearch(Files, fn) < 0)
				break;
			InputStream is = am.open(fn);
			while ((r = is.read(b)) != -1)
				os.write(b, 0, r);
			is.close();
		}
		os.close();
	}

	/**
	 * Attempts to open the database in read-write mode.
	 */
	private void openDatabase() throws SQLException{
		String myPath = DB_PATH + DB_NAME;
		db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
	}

	/**
	 * Close the open database.
	 */
	@Override
	public synchronized void close() {
		if (db != null)
			db.close();

		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}

	/* Everything which follows is mostly query related. */
	
	/**
	 * Returns a cursor to a single bus stop, searching
	 * by the stop code.
	 * 
	 * @param stopCode Unique code of a bus stop.
	 * 
	 * @return Cursor to the bus stop.
	 */
	public Cursor getBusStop(String stopCode) {
		return db.query(context.getString(R.string.db_table_busstops), null, "StopCode = ?", new String[] {stopCode}, null, null, null);
	}
	
	/**
	 * Returns a cursor to service data for a single bus
	 * stop, searching by the stop code.
	 * 
	 * @param stopCode Unique code of a bus stop.
	 * 
	 * @return Cursor to the bus stop services.
	 */
	public Cursor getBusStopServices(String stopCode) {
		return db.query(context.getString(R.string.db_table_stopservices), null, "StopCode = ?", new String[] {stopCode}, null, null, null);
	}
	
	/**
	 * Returns a cursor to services known about.
	 * 
	 * @return Cursor to the bus stop services.
	 */
	public Cursor getServices() {
		return db.rawQuery("SELECT DISTINCT ServiceID FROM " + context.getString(R.string.db_table_stopservices), null);
	}
	
	/**
	 * Returns a cursor to a single subway station, searching
	 * by the subway station name.
	 * 
	 * @param stationName Name of subway station.
	 * 
	 * @return Cursor to the subway station.
	 */
	public Cursor getSubwayStation(String stationName) {
		return db.query(context.getString(R.string.db_table_subwaystations), null, "Name = ?", new String[] {stationName}, null, null, null);
	}
	
	/**
	 * Returns an ArrayList of all subway stations in the
	 * database.
	 * 
	 * @return ArrayList containing all subway stations.
	 */
	public Cursor getSubwayStations() {
		return db.query(context.getString(R.string.db_table_subwaystations), null, null, null, null, null, null);
	}
	
	/**
	 * Returns a Cursor to bus stops bounded by
	 * a minimum and maximum latitude and longitude.
	 * 
	 * @param lat1 Minimum latitude
	 * @param lat2 Maximum latitude
	 * @param lon1 Minimum longitude
	 * @param lon2 Maximum longitude
	 * 
	 * @return Cursor to bus stops within the boundary.
	 */
	public Cursor getSubwayStationsInBoundary(double lat1, double lat2, double lon1, double lon2) {
		return db.rawQuery("SELECT * FROM " + context.getString(R.string.db_table_subwaystations) +
								" WHERE " + "Latitude > " + lat1 + " AND " +
								"Longitude > " + lon1 + " AND " +
								"Latitude < " + lat2 + " AND " +
								"Longitude < " + lon2, null);
	}
	
	/**
	 * Returns a Cursor to all bus stops in the database. I
	 * recommend you use getBusStopsInBoundary() if drawing on map.
	 * 
	 * @see getBusStopsInBoundary();
	 * 
	 * @return Cursor to all bus stops.
	 */
	public Cursor getBusStops() {
		return db.query(context.getString(R.string.db_table_busstops), null, null, null, null, null, null);
	}
	
	/**
	 * Returns a Cursor to bus stops bounded by
	 * a minimum and maximum latitude and longitude.
	 * 
	 * @param lat1 Minimum latitude
	 * @param lat2 Maximum latitude
	 * @param lon1 Minimum longitude
	 * @param lon2 Maximum longitude
	 * 
	 * @return Cursor to bus stops within the boundary.
	 */
	public Cursor getBusStopsInBoundary(double lat1, double lat2, double lon1, double lon2) {
		return db.rawQuery("SELECT * FROM " + context.getString(R.string.db_table_busstops) +
								" WHERE " + "Latitude > " + lat1 + " AND " +
								"Longitude > " + lon1 + " AND " +
								"Latitude < " + lat2 + " AND " +
								"Longitude < " + lon2, null);
	}
	
	/**
	 * Returns a Cursor to all "favourite" bus stops.
	 * @return Cursor to favourite bus stops.
	 */
	public Cursor getFavouriteBusStops() {
		return db.query(context.getString(R.string.db_table_busstops), null, "StopFavourite = 1", null, null, null, context.getString(R.string.db_busstops_Name) + ", " + context.getString(R.string.db_busstops_Alias));
	}
	
	/**
	 * Returns a Cursor to favourite bus stops bounded by
	 * a minimum and maximum latitude and longitude.
	 * @param lat1 Minimum latitude
	 * @param lat2 Maximum latitude
	 * @param lon1 Minimum longitude
	 * @param lon2 Maximum longitude
	 * @return Cursor to favourite bus stops within the boundary.
	 */
	public Cursor getFavouriteBusStopsInBoundary(double lat1, double lat2, double lon1, double lon2) {
		return db.rawQuery("SELECT * FROM " + context.getString(R.string.db_table_busstops) +
								" WHERE " + "Latitude >= " + lat1 + " AND " +
								"Latitude <= " + lat2 + " AND " +
								"Longitude >= " + lon1 + " AND " +
								"Longitude <= " + lon2 + " AND " +
								"StopFavourite = 1", null);
	}
	
	
	/**
	 * Returns a Cursor to bus stops on a particular route,
	 * bounded by a minimum and maximum latitude and longitude.
	 * 
	 * @param route Unique route number
	 * @param lat1 Minimum latitude
	 * @param lat2 Maximum latitude
	 * @param lon1 Minimum longitude
	 * @param lon2 Maximum longitude
	 * 
	 * @return Cursor to favourite bus stops within the boundary.
	 */
	public Cursor getBusStopsInBoundaryOnRoute(String route, double lat1, double lat2, double lon1, double lon2) {
		return db.rawQuery("SELECT busstops.* FROM busstops, stopservices " +
							"WHERE busstops.StopCode = stopservices.StopCode " +
							"AND stopservices.ServiceID = '" + route + "' AND " +
							"Latitude >= " + lat1 + " AND " +
							"Latitude <= " + lat2 + " AND " +
							"Longitude >= " + lon1 + " AND " +
							"Longitude <= " + lon2, null);
	}
	
	/**
	 * Update the Favourite field of the bus stop
	 * identified by stopCode.
	 * 
	 * @param stopCode Bus stop code
	 * @param isFavourite Whether or not this stop is a favourite
	 * 
	 * @return true if a record was updated successfully.
	 */
	public boolean updateFavourite(String stopCode, boolean isFavourite) {
		ContentValues values = new ContentValues();
		values.put("StopFavourite", (isFavourite ? 1 : 0));
		
		return db.update(context.getString(R.string.db_table_busstops), values, "StopCode = ?", new String[] {stopCode}) > 0;
	}
	
	/**
	 * Returns whether or not the stop is a favourite.
	 * 
	 * @param stopCode Bus stop code
	 * 
	 * @return true if the stop is favourite
	 */
	public boolean isFavourite(String stopCode) {
		Cursor c = getBusStop(stopCode);
		
		if (c != null) {
			try {
				c.moveToFirst();
				
				boolean isFavourite = c.getInt(BUS_STOP_FAVOURITE) == 1;
				
				c.close();
				
				return isFavourite;
			} catch (Exception e) {
				/* Exception occured while checking if stop is
				 * a favourite.
				 */
				
				return false;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns whether or not the stop has an alias set.
	 * 
	 * @param stopCode Bus stop code
	 * 
	 * @return True if the stop has an alias
	 */
	public boolean hasAlias(String stopCode) {
		Cursor c = getBusStop(stopCode);
		
		if (c != null) {
			try {
				c.moveToFirst();
				
				boolean hasAlias = c.getString(BUS_STOP_ALIAS) != null && c.getString(BUS_STOP_ALIAS).length() > 0;
				
				c.close();
				
				return hasAlias;
			} catch (Exception e) {
				/* Exception occured while checking for alias */
				
				return false;
			}
		}
		
		return false;
	}
	
	/**
	 * Update the alias for the bus stop identified by stopCode.
	 * 
	 * @param stopCode Bus stop code
	 * @param alias New name for the stop
	 * 
	 * @return true if a record was updated successfully.
	 */
	public boolean updateAlias(String stopCode, String alias) {
		ContentValues values = new ContentValues();
		values.put(context.getString(R.string.db_busstops_Alias), alias.trim());
		
		return db.update(context.getString(R.string.db_table_busstops), values, "StopCode = ?", new String[] {stopCode}) > 0;
	}
	
	/**
	 * Gets the alias assigned to a given
	 * stop, identified by it's code.
	 * 
	 * @param stopCode Unique identifier for the bus stop.
	 * 
	 * @return The alias if it exists, otherwise null.
	 */
	public String getAlias(String stopCode) {
		Cursor stop = getBusStop(stopCode);
		
		if (stop != null) {
			try {
				stop.moveToFirst();
				
				String alias = stop.getString(BUS_STOP_ALIAS);
				
				stop.close();
				
				return alias;
			} catch (Exception e) {
				/* Problem happened trying to find stop */
				
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the name to be used to identify a stop. If
	 * an alias is set for this stop, this is returned.
	 * Otherwise, the original stop name is returned.
	 * 
	 * @param stopCode Unique identifier for the bus stop.
	 * 
	 * @return A name for the given stopCode. Returns null if
	 * stop could not be found.
	 */
	public String getAliasOrName(String stopCode) {
		Cursor stop = getBusStop(stopCode);
		
		if (stop != null) {
			try {
				stop.moveToFirst();
				
				String name = null;
				
				if (stop.getString(BUS_STOP_ALIAS) != null && stop.getString(BUS_STOP_ALIAS).length() > 0) {
					name = stop.getString(BUS_STOP_ALIAS);
				} else {
					name = stop.getString(BUS_STOP_NAME);
				}
				
				stop.close();
				
				return name;
			} catch (Exception e) {
				/* Problem happened trying to find stop */
				
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Determines if the database contains information
	 * about a given service name.
	 * 
	 * @param serviceId Name of the service (e.g. "44a" or "66"
	 * 
	 * @return True if the service exists
	 */
	public boolean serviceExists(String serviceId) {
		Cursor c = db.query(context.getString(R.string.db_table_stopservices),
				new String[] {context.getString(R.string.db_stopservices_ServiceID)},
				context.getString(R.string.db_stopservices_ServiceID) + " = ?",
				new String[] {String.valueOf(serviceId)}, null, null, null);
		
		boolean exists = false;
		
		if (c != null) {
			try {
				exists = c.getCount() > 0;
				
				c.close();
			} catch (Exception e) {
				/* Problem occurred whilst checking cursor.
				 * Can safely ignore and assume the service
				 * does not exist.
				 */
			}
		}
		
		return exists;
	}
	
	/**
	 * Adds an alert to the database.
	 * 
	 * @param index       Unique identifier for the alert
	 * @param service     Service identifier which this alert is for (e.g. "44a", "66")
	 * @param destination Destination name for the alert service
	 * @param stopCode    Unique identifier for the stop which this alert is for
	 * @param time        Time at which bus is due
	 * @param minutes     How many minutes before bus is due to send alert
	 */
	public void addAlert(int index, String service, String destination, String stopCode, String time, long minutes) {
		ContentValues cv = new ContentValues(6);
		cv.put("_id", index);
		cv.put("service", service);
		cv.put("destination", destination);
		cv.put("stopcode", stopCode);
		cv.put("time", time);
		cv.put("minutesbefore", minutes);
		
		db.insert(context.getString(R.string.db_table_alert), null, cv);
	}
	
	/**
	 * Deletes an alert from the database.
	 * 
	 * @param id Unique identifier of the alert to be deleted.
	 */
	public void deleteAlert(int id) {
		db.delete(context.getString(R.string.db_table_alert), "_id = ?", new String[] {String.valueOf(id)});
		
	}
	
	/**
	 * Returns all alerts in the database.
	 * 
	 * @return Cursor to all alerts
	 */
	public Cursor getAlerts() {
		return db.query(context.getString(R.string.db_table_alert), null, null, null, null, null, "time");
	}
	
	public boolean alertExists(long alertId) {
		String[] selectionArgs = {String.valueOf(alertId)};
		Cursor c = db.query(context.getString(R.string.db_table_alert),
				null, context.getString(R.string.db_alert_Identifier) + " = ?",
				selectionArgs, null, null, null);
		
		if (c == null) {
			return false;
		}
		
		if (c.moveToFirst() == false || c.getCount() == 0) {
			/* No records exist for this alert Id */
			c.close();
			return false;
		} else {
			c.close();
			return true;
		}
	}
	
	/**
	 * Returns a Cursor to an alert for a
	 * given departure and stopCode.
	 * 
	 * @param departure Departure object
	 * @param stopCode  Unique identifier for bus stop
	 * 
	 * @return Cursor to the alert
	 */
	public Cursor getAlert(Departure departure, String stopCode) {
		String[] selectionArgs = {
				departure.getService(),
				departure.getDestination(),
				TimeFormatter.formatTime(departure.getTime()),
				stopCode
			};
			
		return db.query(context.getString(R.string.db_table_alert), null,
				"service = ? AND destination = ? AND time = ? AND stopcode = ?",
				selectionArgs, null, null, null);
	}
	
	/**
	 * Returns a Cursor to an alert for a
	 * given alertID.
	 * 
	 * @param alertID Unique identifier for alert
	 * 
	 * @return Cursor to the alert
	 */
	public Cursor getAlert(int alertID) {
		
		String[] selectionArgs = { String.valueOf(alertID) };
		
		return db.query(context.getString(R.string.db_table_alert), null,
				"_id = ?", selectionArgs, null, null, null);
	}
	
	
	/**
	 * Determines if a given Departure object has
	 * an alert set.
	 * 
	 * @param departure Departure object
	 * 
	 * @return True if this departure has an alert set
	 */
	public boolean hasAlert(Departure departure, String stopCode) {
		Cursor c = getAlert(departure, stopCode);
		
		boolean hasAlert = c.getCount() > 0;
		
		c.close();
		
		return hasAlert;
	}
	
	
	/**
	 * Returns a list of stops whose street names and/or
	 * stop names at least partially match the search query.
	 * 
	 * @param searchQuery the user's search query
	 * @param criteria
	 * 
	 * @return stops matching the query
	 */
	public Cursor findStop(String search, String criteria) {
		String queryString = "SELECT DISTINCT * FROM " + context.getString(R.string.db_table_busstops) +	" WHERE ";
		
		// parse query for individual keywords
		Scanner searchScanner = new Scanner(search);
		ArrayList<String> searchQueries = new ArrayList<String>();
		searchQueries.add(search); // add the whole 
		while (searchScanner.hasNext()){
			searchQueries.add(searchScanner.next());
		}				
		
		// for each keyword
		for (String searchQuery : searchQueries){
			
			if (criteria.charAt(0) == '1') {
				queryString += context.getString(R.string.db_busstops_Street)
						+ " LIKE '%" + searchQuery + "%'" + " OR ";
			}
			
			if (criteria.charAt(1) == '1') {
				queryString += context.getString(R.string.db_busstops_Locality)
						+ " LIKE '%" + searchQuery + "%'" + " OR ";
			}
			
			if (criteria.charAt(2) == '1') {
				queryString += context.getString(R.string.db_busstops_Name)
				+ " LIKE '%" + searchQuery + "%'" + " OR ";
				queryString += context.getString(R.string.db_busstops_Alias)
				+ " LIKE '%" + searchQuery + "%'" + " OR ";
				queryString += context.getString(R.string.db_busstops_Landmark)
						+ " LIKE '%" + searchQuery + "%'" + " OR ";
			}
			
			if (criteria.charAt(3) == '1') {
				queryString += context.getString(R.string.db_busstops_StopCode)
						+ " = '" + searchQuery + "'" + " OR ";
			}
			
		}
		
		int len = queryString.length();
		queryString = queryString.substring(0,len-4); // remove last " OR "
				
		return db.rawQuery(queryString, null);
	}
	
	
	/**
	 * Removes alerts for buses which were due
	 * 'expiry' minutes ago.
	 * 
	 * @param expiry   Time in minutes after which to remove old alerts.
	 */
	public void removeExpiredEvents(int expiry) {
		if (expiry < 0)
			return;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		/* Add 'expiry' minutes onto the current date and time */
		GregorianCalendar gc = new GregorianCalendar();
		gc.add(GregorianCalendar.MINUTE, -expiry);
		
		/* Format this as a string */
		String expiryTimeStr = sdf.format(gc.getTime());
		
		/* Delete the alerts for which the time is less
		 * than the expiry time. Expiry time is calculated
		 * as current time - 'expiry' minutes.
		 */
		db.delete(context.getString(R.string.db_table_alert), "time <= ?", new String[] {expiryTimeStr});
	}
	
	/* AsyncTask to create database on first run. */
	private class CreateDatabaseTask extends AsyncTask<Integer, Integer, Boolean> {
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(context,
					context.getString(R.string.preparing_database),
					context.getString(R.string.preparing_database_message), true);
		}
		
		@Override
		protected Boolean doInBackground(Integer... args) {
			try {
				copyDatabase();
			} catch (IOException e) {
				/* An error occurred while copying the database */
				e.printStackTrace();
			}
			
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			dialog.dismiss();
			
			exists = true;
			instance.openDatabase();
		}
	}
}