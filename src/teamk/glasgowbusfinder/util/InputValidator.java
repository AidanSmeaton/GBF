package teamk.glasgowbusfinder.util;

import teamk.glasgowbusfinder.R;
import android.content.Context;
import android.widget.Toast;

public class InputValidator {
	
	/**
	 * Method to check that user input is a valid integer
	 */
	public static boolean validateInteger(Context c, String enteredText) {
		
		/* -? is optional negative symbol
		 * \\d+ means one or more integers
		 */
		if (enteredText.length() > 0 && enteredText.matches("-?\\d+")) {
			if (Integer.parseInt(enteredText) >= 0) {
				return true;
			} else {
				Toast.makeText(c,
						c.getResources().getString(R.string.must_be_positive_number),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(c,
					enteredText+" "+c.getResources().getString(R.string.is_an_invalid_number),
					Toast.LENGTH_SHORT).show();
		}
		
		return false;
	}

}
