package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import teamk.glasgowbusfinder.CursorListActivity;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.StopMapActivity;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 * Quick-Action popup specialised for CursorListActivities. These
 * activities use a cursor to update information. This popup also has
 * a "View On Map" action item, not found in the map popup.
 * 
 * @author Euan Freeman
 */
public class BusStopListQuickAction extends BusStopQuickAction {
	private ActionItem mapActionItem;
	private CursorListActivity activity;
	
	public BusStopListQuickAction(View view, BusStop stop, CursorListActivity activity) {
		super(view, stop);
		
		this.activity = activity;
		
		/* View on map actionitem */
		mapActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_map));
		mapActionItem.setTitle(view.getResources().getString(R.string.view_on_map));
		
		mapActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onMapClick(v);
			}
		});
		
		clearTrack();
		
		addActionItemToHead(mapActionItem);
		
		createActionList();
	}
	
	/**
	 * Callback for clicking the Map action item. Creates
	 * an Intent to show the StopMapActivity.
	 */
	protected void onMapClick(View view) {
		dismiss();
		
		Intent mapIntent = new Intent(getContext(), StopMapActivity.class);
		mapIntent.putExtra("stopcode", stop.getStopCode());
		getContext().startActivity(mapIntent);
	}
	
	@Override
	protected void onFavouritesClick(View view) {
		super.onFavouritesClick(view);
		
		if (activity != null)
			activity.updateCursor();
	}
	
	@Override
	protected void onAliasClick(View view) {
		/*
		 * It's not particularly elegant the way we over-write this code just
		 * for the sake of introducing 2 lines into an inner class... but it's
		 * far less complex than any highly-coupled solution.
		 */
		dismiss();
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this.getContext());

		alert.setTitle(R.string.edit_alias);
		alert.setMessage(R.string.enter_alias);

		final EditText alias = new EditText(this.getContext());
		
		String knownAlias = DatabaseHelper.getInstance(view.getContext()).getAlias(stop.getStopCode());
		
		if (knownAlias != null) {
			alias.setText(knownAlias);
		}
		
		alert.setView(alias);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newAlias = alias.getText().toString();

				if (newAlias.length() > DatabaseHelper.MAX_ALIAS_LENGTH) {
					newAlias = newAlias.substring(0, DatabaseHelper.MAX_ALIAS_LENGTH);
				}
				
				DatabaseHelper.getInstance(BusStopListQuickAction.this.getContext()).updateAlias(stop.getStopCode(), newAlias);
				
				if (activity != null)
					activity.updateCursor();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Ignore
			}
		});

		alert.show();
	}
	
	@Override
	protected void onRemoveAliasClick(View view) {
		super.onRemoveAliasClick(view);
		
		if (activity != null)
			activity.updateCursor();
	}
}
