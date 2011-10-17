package teamk.glasgowbusfinder;

import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;

public abstract class CursorListActivity extends ListActivity {
	protected DatabaseHelper db;
	protected Cursor cursor;
	protected QuickAction qa;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		db = DatabaseHelper.getInstance(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		/* Refresh the cursor in case the user has
		 * changed something in another activity.
		 */
		if (cursor != null)
			cursor.requery();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		/* Free up all resources associated
		 * with this cursor.
		 */
		if (cursor != null)
			cursor.close();
		
		try {
			qa.dismiss();
		} catch (Exception e) {
			/* Either the popup was already hidden, or
			 * not even created. Can be safely ignored.
			 */
		}
	}
	
	public void updateCursor() {
		cursor.requery();
	}
}
