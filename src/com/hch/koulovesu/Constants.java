package com.hch.koulovesu;

public class Constants {
	
	public static final int CONFIG_UPDATE_INTERVAL = 60 * 10; // in seconds
	public static final int GCM_TYPE_MESSAGE = 0;
	public static final int GCM_TYPE_NEW_VERSION_AVAILABLE = 1;
	
	public static final String TAG_GCM = "gcm";
	public static final String GCM_SENDER_ID = "453015069493";
	public static final String GCM_DEFAULT_TITLE = "( ^.＜ )";
	public static final String GCM_MESSAGE_NEW_VERSION = "新版本可用!";
	public static final String PREFERENCE_DEVICE_UUID_ID				= "PREFERENCE_DEVICE_UUID_ID";
	public static final String PREFERENCE_GCM_ID						= "PREFERENCE_GCM_ID";
	public static final String PREFERENCE_GCM_REGISTERED_APP_VERSION	= "PREFERENCE_APP_VERSION";
	public static final String PREFERENCE_LAST_UPDATE_TIMESTAMP			= "PREFERENCE_LAST_UPDATE_TIMESTAMP";
	public static final String PREFERENCE_LAST_SECTORS_CONTENT_HASH		= "PREFERENCE_LAST_SECTORS_CONTENT_HASH";
}
