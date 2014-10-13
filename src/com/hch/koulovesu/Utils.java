package com.hch.koulovesu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

public class Utils {
	
	private static Object diskLock = new Object();
	
	public static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	public static int getCurrentTimestamp() {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		return (int) (calendar.getTimeInMillis()/1000L);
	}
	
	public static String getNumberString(String input) {
		
		int emptyIndex = input.indexOf(' ');
		int newLineIndex = input.indexOf('\n');
		int dotIndex = input.indexOf('.');
		int minSeparateIndex = getPositiveMin(getPositiveMin(emptyIndex, newLineIndex), dotIndex);
		if(minSeparateIndex != -1) {
			try {
				String numberString = input.substring(0, minSeparateIndex);
				Integer.valueOf(numberString);
				return numberString;
			} catch (NumberFormatException e) {
				return null;
			}
			
		} else {
			return null;
		}
	}
	
	private static int getPositiveMin(int arg0, int arg1) 
	{
		if(arg0 >= 0) {
			if(arg1 >= 0) {
				//both are positive, return minimum
				return Math.min(arg0, arg1);
			} else {
				//arg0 is positive, arg1 is negative, return arg0
				return arg0;
			}
		} else {
			return arg1 >= 0 ? arg1 : -1;
		}
	}
	
	public static String getFirstLine(String input) {
		int eolIndex = input.indexOf('\n');
		if(eolIndex != -1) {
			return input.substring(0, eolIndex);
		} else {
			return input;
		}
	}
	
	public static String trim(String input) {
		while(!input.isEmpty() && (input.charAt(0) == ' ' || input.charAt(0) == '\n')) {
			if(input.length() > 1) {
				input = input.substring(1);
			} else {
				input = "";
				break;
			}
		}
		while(!input.isEmpty() && (input.charAt(input.length() - 1) == ' ' || input.charAt(input.length() - 1) == '\n')) {
			if(input.length() > 1) {
				input = input.substring(0, input.length() - 1);
			} else {
				input = "";
				break;
			}
		}
		return input;
	}
	
	public static String readFromDisk(Context context) {
		synchronized (diskLock) {
			StringBuilder text = new StringBuilder();
			
			BufferedReader bufferedReader = null;
			try {
			    bufferedReader = new BufferedReader(new FileReader(getCacheFile(context)));
			    String line;

			    while ((line = bufferedReader.readLine()) != null) {
			        text.append(line);
			        text.append('\n');
			    }
			}
			catch (IOException e) {
			    
			} finally {
				if(bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			return text.toString();
		}
	}
	
	public static void saveToDisk(final Context context, final String content) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (diskLock) {
					try {
						FileOutputStream out;
						out = new FileOutputStream(getCacheFile(context));
						out.write(content.getBytes());
				        out.close();  
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	private static File getCacheFile(Context context) {
		File cacheDirectory;  
	    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {  
	    	cacheDirectory = context.getExternalCacheDir();  
	    } else {  
	    	cacheDirectory = context.getCacheDir();  
	    }
	    return new File(cacheDirectory, "cache");
	}
}
