package com.hch.koulovesu;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {

	public GcmIntentService() {
		super("KouLovesU GCM Intent Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
        	if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                sendNotification(extras.getString("message", null), extras.getString("title", Constants.GCM_DEFAULT_TITLE));
                Log.i(Constants.TAG_GCM, "Received: " + extras.toString());
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	private void sendNotification(String message, String title) {
		
		int notificationId = (int) (Integer.MIN_VALUE + Math.pow(2, 32) * Math.random());
		
		Log.i(Constants.TAG_GCM, "notification id : " + notificationId);
		
		NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId, new Intent(this, MainActivity.class), 0);
        		
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        	.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
	        .setSmallIcon(R.drawable.app_icon)
	        .setAutoCancel(true)
	        .setStyle(new NotificationCompat.BigTextStyle()
	        	.bigText(getResources().getString(R.string.app_name)))
	        .setContentTitle(title)
	        
	        .setVibrate(new long[]{250, 500, 500});
	    if(message != null) {
	    	builder.setContentText(message);
	    }

        builder.setContentIntent(contentIntent);
        notificationManager.notify(notificationId, builder.build());
    }

}
