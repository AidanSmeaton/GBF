package teamk.glasgowbusfinder;

import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.quickactions.BusStopListQuickAction;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Aidan Smeaton
 * @author Calum McCall
 * @author Euan Freeman
 */
public class FindStopResultsActivity extends SearchResultActivity {
	/* Identifier for the dialog. */
	private static final int PROGRESS_DIALOG_ID = 1;
	
	private ProgressDialog searchProgressDialog;
	private SearchTask searchTask;
	private String search;
	private String criteria;
	private QuickAction qa;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.findstopresults);
		
		search = getIntent().getStringExtra("search");
		criteria = getIntent().getStringExtra("criteria");
		
		ListView list = this.getListView();
		
		list.setOnItemClickListener(new OnItemClickListener() {
            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	TextView serviceTextView = (TextView) view.findViewById(R.id.searchresult_stopname);
            	CharSequence stopName = serviceTextView.getText();
            	
            	boolean favourite = ((Integer) view.getTag(R.id.stopfavourite) == 1);
            	
            	BusStop stop = new BusStop((String) view.getTag(R.id.stopcode), stopName.toString(), favourite);
            	
            	qa = new BusStopListQuickAction(view, stop, FindStopResultsActivity.this);
            	
            	qa.show();
            }
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (!isSearched()) {
			updateCursor();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		try {
			removeDialog(PROGRESS_DIALOG_ID);
			
			if (searchTask.getStatus() != AsyncTask.Status.FINISHED) {
				searchTask.cancel(true);
			}
		} catch (Exception e) {
			/* The dialog is already hidden. This exception
			 * can be ignored.
			 */
		}
		
		try {
			qa.dismiss();
		} catch (Exception e) {
			/* Either the popup was already hidden, or
			 * not even created. Can be safely ignored.
			 */
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG_ID:
			searchProgressDialog = new ProgressDialog(this);
			searchProgressDialog.setIndeterminate(true);
			searchProgressDialog.setTitle("");
			searchProgressDialog.setMessage(getString(R.string.search_message));
			
			return searchProgressDialog;
		default:
			return null;
		}
	}
	
	@Override
	public void updateCursor() {
		searchTask = new SearchTask();
		searchTask.execute(search, criteria);
	}
	
	/**
	 * Adapter class for a database cursor to display bus stops.
	 * 
	 * @author Euan Freeman
	 */
	private class FindStopAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		
		public FindStopAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			
			inflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			view.setTag(R.id.stopcode, cursor.getString(DatabaseHelper.BUS_STOP_CODE));
			
			TextView stopName = (TextView) view.findViewById(R.id.searchresult_stopname);
			
			stopName.setText(DatabaseHelper.getInstance(
    				FindStopResultsActivity.this).hasAlias(cursor.getString(DatabaseHelper.BUS_STOP_CODE)) ? 
    						cursor.getString(DatabaseHelper.BUS_STOP_ALIAS) :
    						cursor.getString(DatabaseHelper.BUS_STOP_NAME));
			
			TextView stopStreet = (TextView) view.findViewById(R.id.searchresult_stopstreet);
			stopStreet.setText(cursor.getString(DatabaseHelper.BUS_STOP_STREET));
			
			TextView stopRegion = (TextView) view.findViewById(R.id.searchresult_stopregion);
			stopRegion.setText(cursor.getString(DatabaseHelper.BUS_STOP_LOCALITY));
			
			ImageView star = (ImageView) view.findViewById(R.id.searchresult_star);
			
			if (cursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 0) {
				star.setVisibility(View.INVISIBLE);
			} else {
				star.setVisibility(View.VISIBLE);
			}
			
			
			
			view.setTag(R.id.stopfavourite, cursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.searchresultrow, null);
		}
	}

	/**
	 * @author Euan Freeman
	 */
	private class SearchTask extends AsyncTask<String, Integer, Cursor> {
		@Override
		protected void onPreExecute() {
			/* Show the progress dialog. */
			showDialog(PROGRESS_DIALOG_ID);
		}
		
		@Override
		protected Cursor doInBackground(String... searchTerms) {
			cursor = db.findStop(searchTerms[0], searchTerms[1]);
			
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor result) {
			/* If the cursor arrives closed, this activity was
			 * most likely destroyed before this task finished.
			 * This situation can be ignored.
			 */
			if (result.isClosed()) {
				return;
			}
			
			cursor = result;
			
			/* Update the list view now. */
			setListAdapter(new FindStopAdapter(FindStopResultsActivity.this, result));
			
			ListView list = FindStopResultsActivity.this.getListView();
			
			list.setOnItemClickListener(new OnItemClickListener() {
	            @Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            	TextView serviceTextView = (TextView) view.findViewById(R.id.searchresult_stopname);
	            	CharSequence stopName = serviceTextView.getText();
	            	
	            	boolean favourite = ((Integer) view.getTag(R.id.stopfavourite) == 1);
	            	
	            	BusStop stop = new BusStop((String) view.getTag(R.id.stopcode), stopName.toString(), favourite);
	            	
	            	qa = new BusStopListQuickAction(view, stop, FindStopResultsActivity.this);
	            	qa.show();
	            }
			});
			
			TextView resultCount = (TextView) findViewById(R.id.findstop_count);
			
			resultCount.setText(result.getCount() + " " + getString(R.string.results_for) + " \"" + search + "\"");
			
			setSearched(true);
			
			try {
				/* Dismiss the progress dialog. */
				dismissDialog(PROGRESS_DIALOG_ID);
			} catch (Exception e) {
				/* It's possible that this dialog
				 * has disappeared (i.e. interrupted
				 * by device rotation) before this task
				 * completed. This situation can be ignored
				 * because the download finished.
				 */
			}
		}
	}
}
