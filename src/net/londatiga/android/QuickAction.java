package net.londatiga.android;

import android.content.Context;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.ViewGroup;

import java.util.ArrayList;

import teamk.glasgowbusfinder.R;

/**
 * Popup window, shows action list as icon and text (QuickContact / Twitter
 * app).
 * 
 * @author Lorensius. W. T
 */
public class QuickAction extends CustomPopupWindow {
	private final View root;
	private final ImageView mArrowUp;
	private final ImageView mArrowDown;
	protected final Animation mTrackAnim;
	private final LayoutInflater inflater;
	private final Context context;

	public static final int ANIM_GROW_FROM_LEFT = 1;
	public static final int ANIM_GROW_FROM_RIGHT = 2;
	public static final int ANIM_GROW_FROM_CENTER = 3;
	public static final int ANIM_AUTO = 4;

	private int animStyle;
	private boolean animateTrack;
	private ViewGroup mTrack;
	private ArrayList<ActionItem> actionList;

	/**
	 * Constructor
	 * 
	 * @param anchor
	 *            {@link View} on where the popup should be displayed
	 */
	public QuickAction(View anchor) {
		super(anchor);

		actionList = new ArrayList<ActionItem>();
		context = anchor.getContext();
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		root = inflater.inflate(R.layout.quickaction, null);

		mArrowDown = (ImageView) root.findViewById(R.id.arrow_down);
		mArrowUp = (ImageView) root.findViewById(R.id.arrow_up);

		setContentView(root);

		mTrackAnim = AnimationUtils.loadAnimation(anchor.getContext(), R.anim.rail);

		mTrackAnim.setInterpolator(new Interpolator() {
			@Override
			public float getInterpolation(float t) {
				// Pushes past the target area, then snaps back into place.
				// Equation for graphing: 1.2-((x*1.6)-1.1)^2
				final float inner = (t * 1.55f) - 1.1f;

				return 1.2f - inner * inner;
			}
		});

		mTrack = (ViewGroup) root.findViewById(R.id.tracks);
		animStyle = ANIM_AUTO;
		animateTrack = true;
	}

	/**
	 * Animate track
	 * 
	 * @param animateTrack
	 *            flag to animate track
	 */
	public void animateTrack(boolean animateTrack) {
		this.animateTrack = animateTrack;
	}

	/**
	 * Set animation style
	 * 
	 * @param animStyle
	 *            animation style, default is set to ANIM_AUTO
	 */
	public void setAnimStyle(int animStyle) {
		this.animStyle = animStyle;
	}

	/**
	 * Add action item
	 * 
	 * @param action
	 *            {@link ActionItem}
	 */
	public void addActionItem(ActionItem action) {
		actionList.add(action);
	}

	/**
	 * Show popup window
	 */
	public void show() {
		preShow();

		int[] location = new int[2];

		anchor.getLocationOnScreen(location);

		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ anchor.getWidth(), location[1] + anchor.getHeight());

		root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();

		int xPos = 0;
		int yPos = (screenHeight / 2) + context.getResources().getDrawable(R.drawable.ic_bus).getIntrinsicHeight();

		showArrow(R.id.arrow_up, anchorRect.centerX());
		setAnimationStyle(screenWidth, anchorRect.centerX(), true);
		
		showAt(xPos, yPos);
	}
	
	public void showAt(int xPos, int yPos) {
		window.showAtLocation(this.anchor, Gravity.NO_GRAVITY, xPos, yPos);
		
		if (animateTrack)
			mTrack.startAnimation(mTrackAnim);
	}

	/**
	 * Set animation style
	 * 
	 * @param screenWidth
	 *            Screen width
	 * @param requestedX
	 *            distance from left screen
	 * @param onTop
	 *            flag to indicate where the popup should be displayed. Set TRUE
	 *            if displayed on top of anchor and vice versa
	 */
	protected void setAnimationStyle(int screenWidth, int requestedX,
			boolean onTop) {
		int arrowPos = requestedX - mArrowUp.getMeasuredWidth() / 2;

		switch (animStyle) {
		case ANIM_GROW_FROM_LEFT:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left
					: R.style.Animations_PopDownMenu_Left);
			break;

		case ANIM_GROW_FROM_RIGHT:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right
					: R.style.Animations_PopDownMenu_Right);
			break;

		case ANIM_GROW_FROM_CENTER:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center
					: R.style.Animations_PopDownMenu_Center);
			break;

		case ANIM_AUTO:
			if (arrowPos <= screenWidth / 4) {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left
						: R.style.Animations_PopDownMenu_Left);
			} else if (arrowPos > screenWidth / 4
					&& arrowPos < 3 * (screenWidth / 4)) {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center
						: R.style.Animations_PopDownMenu_Center);
			} else {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopDownMenu_Right
						: R.style.Animations_PopDownMenu_Right);
			}

			break;
		}
	}

	protected void refreshActionList() {
		clearTrack();
		
		createActionList();
	}
	
	protected void clearTrack() {
		mTrack.removeViews(1, actionList.size());
	}
	
	/**
	 * Create action list
	 * 
	 */
	protected void createActionList() {
		View view;
		String title;
		Drawable icon;
		OnClickListener listener;
		int index = 1;

		for (int i = 0; i < actionList.size(); i++) {
			title = actionList.get(i).getTitle();
			icon = actionList.get(i).getIcon();
			listener = actionList.get(i).getListener();

			view = getActionItem(title, icon, listener);

			view.setFocusable(true);
			view.setClickable(true);

			mTrack.addView(view, index);

			index++;
		}
	}

	/**
	 * Get action item {@link View}
	 * 
	 * @param title
	 *            action item title
	 * @param icon
	 *            {@link Drawable} action item icon
	 * @param listener
	 *            {@link View.OnClickListener} action item listener
	 * @return action item {@link View}
	 */
	private View getActionItem(String title, Drawable icon,
			OnClickListener listener) {
		LinearLayout container = (LinearLayout) inflater.inflate(
				R.layout.action_item, null);
		ImageView img = (ImageView) container.findViewById(R.id.icon);
		TextView text = (TextView) container.findViewById(R.id.title);

		if (icon != null) {
			img.setImageDrawable(icon);
		} else {
			img.setVisibility(View.GONE);
		}

		if (title != null) {
			text.setText(title);
		} else {
			text.setVisibility(View.GONE);
		}

		if (listener != null) {
			container.setOnClickListener(listener);
		}

		return container;
	}

	/**
	 * Show arrow
	 * 
	 * @param whichArrow
	 *            arrow type resource id
	 * @param requestedX
	 *            distance from left screen
	 */
	protected void showArrow(int whichArrow, int requestedX) {
		final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp
				: mArrowDown;
		final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown
				: mArrowUp;

		final int arrowWidth = mArrowUp.getMeasuredWidth();

		showArrow.setVisibility(View.VISIBLE);

		ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) showArrow
				.getLayoutParams();

		param.leftMargin = requestedX - arrowWidth / 2;

		hideArrow.setVisibility(View.INVISIBLE);
	}
	
	protected View getRoot() {
		return root;
	}
	
	protected Context getContext() {
		return context;
	}
	
	/**
	 * Adds an action item to the head of the action list.
	 * 
	 * @param ActionItem to be added to this quick-action popup
	 * 
	 * @author Euan Freeman
	 */
	protected void addActionItemToHead(ActionItem item) {
		actionList.add(0, item);
	}
	
	/**
	 * Adds an action item to the tail of the action list.
	 * 
	 * @param ActionItem to be added to this quick-action popup
	 * 
	 * @author Euan Freeman
	 */
	protected void addActionItemToTail(ActionItem item) {
		actionList.add(actionList.size() - 1, item);
	}
}