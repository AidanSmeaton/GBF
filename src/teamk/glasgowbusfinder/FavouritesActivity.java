package teamk.glasgowbusfinder;

import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.quickactions.BusStopListQuickAction;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This activity displays a list of bus stops, in the context
 * of each stop being a "favourite" stop.
 * 
 * @author Euan Freeman
 */
public class FavouritesActivity extends CursorListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favourites);
		
		cursor = db.getFavouriteBusStops();

		setListAdapter(new FavouriteAdapter(this, cursor));
		
		ListView list = this.getListView();
		
		list.setOnItemClickListener(new OnItemClickListener() {
            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	TextView stopnameTextView = (TextView) view.findViewById(R.id.favourites_stopname);
            	CharSequence stopName = stopnameTextView.getText();
            	
            	BusStop stop = new BusStop((String) view.getTag(R.id.stopcode), stopName.toString(), true);
            	
            	qa = new BusStopListQuickAction(view, stop, FavouritesActivity.this);
            	qa.show();
            }
		});
	}
	
	/**
	 * Adapter class for a database cursor to the
	 * users favourite bus stops.
	 * @author Euan Freeman
	 */
	private class FavouriteAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		
		public FavouriteAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			
			inflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			view.setTag(R.id.stopcode, cursor.getString(DatabaseHelper.BUS_STOP_CODE));
			
			TextView stopName = (TextView) view.findViewById(R.id.favourites_stopname);
			TextView stopName2 = (TextView) view.findViewById(R.id.favourites_stopname2);			
			
			stopName.setText(
					DatabaseHelper.getInstance(context).getAliasOrName(
							cursor.getString(DatabaseHelper.BUS_STOP_CODE)));
			
			/* if there is an available alias */
			if (db.hasAlias(cursor.getString(DatabaseHelper.BUS_STOP_CODE))){
				stopName.setTypeface(Typeface.DEFAULT_BOLD); // make alias bold
				stopName2.setText("(" + cursor.getString(DatabaseHelper.BUS_STOP_NAME) + ")");				
			}
			
			/* if there is no alias, reset stopName and clear stopName2 */
			else {
				stopName.setTypeface(Typeface.DEFAULT);
				stopName2.setText("");
			}
			
			TextView stopStreet = (TextView) view.findViewById(R.id.favourites_stopstreet);
			stopStreet.setText(cursor.getString(DatabaseHelper.BUS_STOP_STREET));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.favouritesrow, null);
		}
	}
}
