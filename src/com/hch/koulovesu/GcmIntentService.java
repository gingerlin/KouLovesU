package com.hch.koulovesu;

import java.security.acl.LastOwnerException;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
        		Integer type;
        		try {
        			type = Integer.parseInt((String)extras.get("type"));
        		} catch (NumberFormatException e) {
        			type = Constants.GCM_TYPE_MESSAGE;
        		}
        		String message = (String)extras.get("message");
    			String title = (String)extras.get("title");
    			if(title == null) {
    				title = Constants.GCM_DEFAULT_TITLE;
    			}
    			
        		switch(type){
        		case Constants.GCM_TYPE_MESSAGE:
        			
        			sendNotification(message, title);
        			break;
        		case Constants.GCM_TYPE_NEW_VERSION_AVAILABLE:
        			Integer latestVersion;
        			try {
        				latestVersion = Integer.parseInt((String)extras.get("latestVersion"));
        				checkLatestVersion(message, title, latestVersion);
        			} catch(NumberFormatException e) {
        				e.printStackTrace();
        			}
        			break;
        		}
                
                Log.i(Constants.TAG_GCM, "Received: " + extras.toString());
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	private void sendNotification(String message, String title) {
		
		//int notificationId = (int) (Integer.MIN_VALUE + Math.pow(2, 32) * Math.random());
		int notificationId = Utils.getCurrentTimestamp();
		
		Log.i(Constants.TAG_GCM, "notification id : " + notificationId);
		
		NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId, new Intent(this, MainActivity.class), 0);
        		
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        	.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
	        .setSmallIcon(R.drawable.app_icon)
	        .setAutoCancel(true)
	        .setStyle(new NotificationCompat.BigTextStyle()
	        	.bigText(getResources().getString(R.string.app_name)))
	        .setContentTitle(title);
	    if(message != null) {
	    	builder.setContentText(message);
	    }

        builder.setContentIntent(contentIntent);
        notificationManager.notify(notificationId, builder.build());
    }
	
	private void checkLatestVersion(String message, String title, int latestVersion) {
		
		Log.i(Constants.TAG_GCM, String.format("Current Version : %d, Latest Version : %d", Utils.getAppVersion(this), latestVersion));
		
		if(latestVersion > Utils.getAppVersion(this)) {
			
			int notificationId = (int) (Integer.MIN_VALUE + Math.pow(2, 32) * Math.random());
			
			final String appPackageName = getPackageName();
			Intent intent;
			try {
			    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
			} catch (android.content.ActivityNotFoundException anfe) {
			    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
			}
			
			NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
	
	        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId, intent, 0);
	        		
	        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
	        	.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
		        .setSmallIcon(R.drawable.app_icon)
		        .setAutoCancel(true)
		        .setStyle(new NotificationCompat.BigTextStyle()
		        	.bigText(getResources().getString(R.string.app_name)))
		        .setContentTitle(title);
		        
		    if(message == null) {
		    	message = Constants.GCM_MESSAGE_NEW_VERSION;
		    }
		    builder.setContentText(message);
	
	        builder.setContentIntent(contentIntent);
	        notificationManager.notify(notificationId, builder.build());
        
		}
	}
}
