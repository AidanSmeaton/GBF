package teamk.glasgowbusfinder;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This activity allows the user to search for a
 * bus service. If the service is not known about,
 * a toast notification is shown. If it is, the
 * route is shown on the map.
 * 
 * @author Euan Freeman
 */
public class FindServiceActivity extends SearchActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findservice);
		
		searchBar = (EditText) findViewById(R.id.findservice_edittext_search);

		searchBar.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
					searchBar = (EditText) findViewById(R.id.findservice_edittext_search);

					searchClicked(searchBar);
					
					return true;
				}
				return false;
			}
		});
	}
	
	public void searchClicked(View view) {
		searchBar = (EditText) findViewById(R.id.findservice_edittext_search);

		String text = searchBar.getText().toString().trim();
		
		/* http://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard/1109108#1109108 */
		if (text.length() > 0) {
			/* Get the on-screen keyboard and hide it, if shown */
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			
			if (imm != null) {
				imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
			}
		} else {
			return;
		}
		
		if (DatabaseHelper.getInstance(this).serviceExists(text)) {
			Intent routeIntent = new Intent(this, StopMapActivity.class);
            routeIntent.putExtra("route", text);
            this.startActivity(routeIntent);
		} else {
			/* Clear the search bar contents */
			searchBar.setText("");
			
			Toast.makeText(this, getString(R.string.no_service_message_short) + " " + text, Toast.LENGTH_LONG).show();
		}
	}
}
