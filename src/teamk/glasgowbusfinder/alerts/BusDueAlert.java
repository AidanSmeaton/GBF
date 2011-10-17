package teamk.glasgowbusfinder.alerts;

import teamk.glasgowbusfinder.AlertsActivity;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.util.TimeFormatter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BusDueAlert extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		int alertId = intent.getIntExtra("alertId", -1);
		
		assert(alertId != -1);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (! sp.getBoolean(context.getString(R.string.pref_alertsEnable), true)) {
			/* User has notifications disabled, so the alert must just be used
			 * as a "reminder" within the app. Do not send any notification.
			 */
			return;
		}
		
		if (! DatabaseHelper.getInstance(context).alertExists(alertId)) {
			/* User must have cancelled this alert. Don't bother sending notification. */
			return;
		}
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent alertsIntent = new Intent(context, AlertsActivity.class);
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, alertsIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		
		String serviceId = intent.getStringExtra("serviceId");
		String destination = intent.getStringExtra("destination");
		String time = intent.getStringExtra("time");
		
		String tickerText = String.format("Bus %s due soon", serviceId);
		String notificationSender = "Glasgow Bus Finder";
		String notificationMessage = String.format("%s (%s) due at %s", serviceId, destination, TimeFormatter.getHoursAndMins(time));
		
		Notification notification = new Notification(R.drawable.notification_icon, tickerText, System.currentTimeMillis());
		notification.setLatestEventInfo(context, notificationSender, notificationMessage, contentIntent);
		
		/* Enable vibration and LED flash */
		notification.vibrate = new long[] {900, 200, 800, 200, 700, 200, 800, 200, 900};
		
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.defaults |= Notification.DEFAULT_SOUND;
		
		/* Sent notification */
		notificationManager.notify((int) alertId, notification);
	}
}
