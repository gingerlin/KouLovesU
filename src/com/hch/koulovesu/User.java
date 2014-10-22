package com.hch.koulovesu;

import java.util.HashMap;

import org.json.JSONException;

import com.hch.koulovesu.ConnectionHelper.HttpResult;

import android.graphics.Bitmap;


public class User {
	private static HashMap<String, User> users = new HashMap<String, User>();
	
	private String userId;
	private String userName;
	private Bitmap portraitThumbnail;
	
	public static User get(String userId) {
		if(users.containsKey(userId)) {
			return users.get(userId);
		} else {
			User user = new User(userId);
			users.put(userId, user);
			return user;
		}
	}
	
	private User(String userId) {
		this.userId = userId;
	}
	
	public synchronized String getName() {
		if(userName == null) {
			HttpResult httpResult = ConnectionHelper.sendGetRequest("http://graph.facebook.com/" + userId, false, null);
			if(httpResult != null && httpResult.success) {
				try {
					userName = httpResult.result.getString("name");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return userName;
	}
	
	public synchronized Bitmap getPortraitThumbnail() {
		if(portraitThumbnail == null) {
			if(BitmapCache.isBitmapCached(userId, BitmapCache.TYPE_USER_THUMBNAIL)) {
				portraitThumbnail = BitmapCache.get(userId, BitmapCache.TYPE_USER_THUMBNAIL);
			} else {
				Bitmap bitmap = ConnectionHelper.getImage("http://graph.facebook.com/" + userId + "/picture?width=128&height=128", 128, 128);
				portraitThumbnail = Utils.createThumbnail(bitmap, 128);
				bitmap.recycle();
				if(portraitThumbnail != null) {
					BitmapCache.put(userId, BitmapCache.TYPE_USER_THUMBNAIL, portraitThumbnail);
				}
			}
		}
		
		return portraitThumbnail;
	}

	public String getUserId() {
		return userId;
	}
}
