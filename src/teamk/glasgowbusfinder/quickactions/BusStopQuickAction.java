package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.TabbedStopActivity;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Quick-Action popup for a bus stop.
 * 
 * @author Euan Freeman
 */
public class BusStopQuickAction extends QuickAction {
	private ActionItem favouriteActionItem;
	private ActionItem aliasActionItem;
	private ActionItem removeAliasActionItem;
	private ActionItem departuresActionItem;
	private ActionItem stopInfoActionItem;
	protected BusStop stop;
	
	public BusStopQuickAction(View view, BusStop stop) {
		super(view);
		
		/* Stop Information actionitem */
		stopInfoActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_info));
		stopInfoActionItem.setTitle(view.getResources().getString(R.string.stop_information));
		
		stopInfoActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onStopInfoClick(v);
			}
		});
		
		addActionItem(stopInfoActionItem);
		
		/* View Departures actionitem */
		departuresActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_departures));
		departuresActionItem.setTitle(view.getResources().getString(R.string.departures_title));
		
		departuresActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDeparturesClick(v);
			}
		});
		
		addActionItem(departuresActionItem);
		
		/* Add alias actionitem */
		aliasActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_alias));
		
		/* Determine if alias action item should say Add or Edit alias. */
		if (DatabaseHelper.getInstance(view.getContext()).hasAlias(stop.getStopCode())) {
			aliasActionItem.setTitle(view.getResources().getString(R.string.edit_alias));
			aliasActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_alias_edit));
		} else {
			aliasActionItem.setTitle(view.getResources().getString(R.string.add_alias));
			aliasActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_alias));
		}
		
		aliasActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAliasClick(v);
			}
		});
		
		addActionItem(aliasActionItem);
		
		if (DatabaseHelper.getInstance(getContext()).hasAlias(stop.getStopCode())) {
			/* Add remove alias actionitem */
			removeAliasActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_alias_cancel));
			
			removeAliasActionItem.setTitle(view.getResources().getString(R.string.remove_alias));
			
			removeAliasActionItem.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onRemoveAliasClick(v);
				}
			});
			
			addActionItem(removeAliasActionItem);
		}
		
		/* Toggle Favourites actionitem */
		favouriteActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_favourite));
		
		/* Determine if favourite action item should say Add or Remove favourite. */
		if (stop.isFavourite()) {
			favouriteActionItem.setTitle(view.getResources().getString(R.string.remove_favourite));
			favouriteActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_favourite_remove));
		} else {
			favouriteActionItem.setTitle(view.getResources().getString(R.string.add_favourite));
			favouriteActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_favourite));
		}
		
		favouriteActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onFavouritesClick(v);
			}
		});
		
		addActionItem(favouriteActionItem);
		
		/* Add all items to the action list */
		createActionList();
		
		setAnchor(view);
		createWindow();
		
		/* Set the stop specific properties of this quick action */
		this.stop = stop;
		
		TextView quickActionTitle = (TextView) getRoot().findViewById(R.id.quickaction_stopname);
		quickActionTitle.setText(stop.getName());
	}
	
	/**
	 * Callback for clicking the Add / Edit alias action
	 * item. Shows a popup which allows the user to add
	 * or edit the nickname for the chosen stop.
	 * @param view  
	 */
	protected void onAliasClick(View view) {
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
				
				DatabaseHelper.getInstance(BusStopQuickAction.this.getContext()).updateAlias(stop.getStopCode(), newAlias);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Ignore
			}
		});

		alert.show();
	}
	
	/**
	 * Callback for clicking the remove alias action items.
	 */
	protected void onRemoveAliasClick(View view) {
		DatabaseHelper.getInstance(BusStopQuickAction.this.getContext()).updateAlias(stop.getStopCode(), "");
		
		dismiss();
	}
	
	/**
	 * Callback for clicking the Add / Remove Favourite action items.
	 * Updates the favourite property of the stop.
	 */
	protected void onFavouritesClick(View view) {
		dismiss();
		
		stop.setFavourite(! stop.isFavourite());
		
		DatabaseHelper.getInstance(this.getContext()).updateFavourite(stop.getStopCode(), stop.isFavourite());
	}
	
	/**
	 * Callback for clicking the Departures action item. Creates
	 * an Intent to show the DeparturesActivity.
	 */
	protected void onDeparturesClick(View view) {
		dismiss();
		
		Intent departuresIntent = new Intent(getContext(), TabbedStopActivity.class);
		departuresIntent.putExtra("stopcode", stop.getStopCode());
		departuresIntent.putExtra("startTab", TabbedStopActivity.DEPARTURES);
		getContext().startActivity(departuresIntent);
	}
	
	/**
	 * Callback for clicking the Stop Info action item. Creates
	 * an Intent to show the StopInfoActivity.
	 */
	protected void onStopInfoClick(View view) {
		dismiss();
		
		Intent departuresIntent = new Intent(getContext(), TabbedStopActivity.class);
		departuresIntent.putExtra("stopcode", stop.getStopCode());
		departuresIntent.putExtra("startTab", TabbedStopActivity.INFORMATION);
		getContext().startActivity(departuresIntent);
	}
	
	/**
	 * Show popup window
	 */
	@Override
	public void show() {
		preShow();

		int[] location = new int[2];

		anchor.getLocationOnScreen(location);

		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ anchor.getWidth(), location[1] + anchor.getHeight());

		getRoot().setLayoutParams(
				new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
		getRoot().measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int rootWidth = getRoot().getMeasuredWidth();
		int rootHeight = getRoot().getMeasuredHeight();

		int screenWidth = windowManager.getDefaultDisplay().getWidth();

		int xPos = (screenWidth - rootWidth) / 2;
		int yPos = anchorRect.top - rootHeight;

		boolean onTop = true;

		// display on bottom
		if (rootHeight > anchor.getTop()) {
			yPos = anchorRect.bottom;
			onTop = false;
		}

		showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up),
				anchorRect.centerX());
		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);

		showAt(xPos, yPos);
	}
}
