package teamk.glasgowbusfinder;

import android.os.Bundle;

/**
 * Base class for activities which display search
 * results for bus stops.
 * @author Euan Freeman
 */
public abstract class SearchResultActivity extends CursorListActivity {
	private boolean searched;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		cursor = null;
		
		searched = false;
	}
	
	public boolean isSearched() {
		return searched;
	}

	public void setSearched(boolean searched) {
		this.searched = searched;
	}
}
