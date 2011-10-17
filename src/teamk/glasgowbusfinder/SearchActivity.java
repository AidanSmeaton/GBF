package teamk.glasgowbusfinder;

import android.app.Activity;
import android.widget.EditText;
import android.widget.Toast;

public abstract class SearchActivity extends Activity {
	public static final int MIN_QUERY_LENGTH = 2;
	protected EditText searchBar;
	
	/**
	 * Called when the activity is
	 * resumed. Resets the search text.
	 */
	@Override
	public void onResume() {
		super.onResume();

		searchBar.setText("");
	}
	
	/**
	 * Determines if a String is a valid search term. The
	 * string must be at least a certain length and
	 * contain an arbitrary number of alphanumeric characters.
	 */
	public boolean isValidSearchText(String text) {
		if (text.length() >= MIN_QUERY_LENGTH) {			
			int nonWhitespace = 0;

			/*
			 * Only search if the text entered has a
			 * minimum amount of alphanumeric characters.
			 */
			for(char c : text.toCharArray()) {
				if (Character.isLetterOrDigit(c))
					nonWhitespace++;
			}

			if (nonWhitespace < MIN_QUERY_LENGTH) {
				Toast.makeText(this, R.string.invalid_search_not_enough_chars, Toast.LENGTH_LONG).show();
				
				return false;
			} else {
				return true;
			}
		} else if (text.length() > 0) {
			Toast.makeText(this, R.string.invalid_search_not_enough_chars, Toast.LENGTH_LONG).show();
			
			return false;
		} else {
			/* i.e. length() == 0 */
			return false;
		}
	}
}
