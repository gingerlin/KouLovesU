package com.hch.koulovesu;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONException;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.hch.koulovesu.ConnectionHelper.HttpResult;
import com.viewpagerindicator.TitlePageIndicator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends SherlockActivity {

	private static final int REQUEST_GCM_ERROR_DIALOG = 0;
	private ArrayList<Sector> sectors = new ArrayList<Sector>();
	private SectorListAdapter listAdapter;
	private Handler handler;
	
	private int maxWeek = 0;
	
	private ListView subjectListView;
	private ListView solutionListView;
	private ViewPager viewPager;
	
	private boolean isUpdating = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		//Setup ActionBar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);
		
		subjectListView = new ListView(this);
		listAdapter = new SectorListAdapter(subjectListView);
		subjectListView.setAdapter(listAdapter);
		
		solutionListView = new ListView(this);
		
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setAdapter(pagerAdapter);
		
		((TitlePageIndicator)findViewById(R.id.indicator)).setViewPager(viewPager);
		
		
		
		handler = new Handler();
		
		//checkLatestVersion();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//Try Register GCM in background
		new Thread(new Runnable() {
			@Override
			public void run() {
				tryRegisterGCM();
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				//Check latest version
				HttpResult result = ConnectionHelper.sendGetRequest("getLatestVersionNumber", null);
				if(result.success) {
					try {
						int latestAppVersion = result.result.getInt("latest_app_version");
						int currentAppVersion = Utils.getAppVersion(getApplicationContext());
						
						if(latestAppVersion > currentAppVersion) {
							final String appPackageName = getPackageName();
							Intent intent;
							try {
							    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
							} catch (android.content.ActivityNotFoundException anfe) {
							    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
							}
							MainActivity.this.startActivity(intent);
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), "請更新至最新的版本", Toast.LENGTH_LONG).show();
								}
							});
							finish();
							return;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				//Fetch cached contents
				final String cachedContent = Utils.readFromDisk(MainActivity.this);
				if(cachedContent != null && !cachedContent.isEmpty()) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							handleDataString(cachedContent);
						}
			        });
				}
				
				//Update from the Internet
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				int lastUpdateTimestamp = preferences.getInt(Constants.PREFERENCE_LAST_UPDATE_TIMESTAMP, 0);
				if(Utils.getCurrentTimestamp() - lastUpdateTimestamp >= Constants.CONFIG_UPDATE_INTERVAL) {
					update();
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 0, Menu.NONE, "Refresh")
			.setIcon(R.drawable.ic_action_refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case 0:
			update();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void tryRegisterGCM() {
		
		//Get Device UUID
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		
		final String deviceId;
		if(preferences.contains(Constants.PREFERENCE_DEVICE_UUID_ID)) {
			deviceId = preferences.getString(Constants.PREFERENCE_DEVICE_UUID_ID, null);
		} else {
			deviceId = UUID.randomUUID().toString();
			
			editor.putString(Constants.PREFERENCE_DEVICE_UUID_ID, deviceId);
			if(editor.commit()) {
		    	Log.i(Constants.TAG_GCM, "Saving device UUID succeeded");
		    } else {
		    	Log.e(Constants.TAG_GCM, "Failed saving device UUID");
		    	return;
		    }
		}
		if(deviceId == null) {
			Log.e(Constants.TAG_GCM, "Couldn't get device UUID");
			return;
		}
		
		//start checking GCM availability
		final int gcmAvailability = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		if(gcmAvailability == ConnectionResult.SUCCESS) {
			
			String cachedGCMId = preferences.getString(Constants.PREFERENCE_GCM_ID, null);
			int cachedAppVersion = preferences.getInt(Constants.PREFERENCE_GCM_REGISTERED_APP_VERSION, -1);
			int currentAppVersion = Utils.getAppVersion(MainActivity.this);
			
			Log.i(Constants.TAG_GCM, "GCM Available");
			if(cachedGCMId != null) {
				Log.i(Constants.TAG_GCM, "GCM ID : " + cachedGCMId);
			}
			
			if(cachedGCMId == null || currentAppVersion != cachedAppVersion) {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
				try {
					String regId = gcm.register(Constants.GCM_SENDER_ID);
					Log.i(Constants.TAG_GCM, "device registered, regId : " + regId);
					
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("reg_id", regId);
					params.put("device_id", deviceId);
					
					ConnectionHelper.HttpResult registerResult = ConnectionHelper.sendPostRequest("registerGCMId", params);
					if(registerResult.success) {
						//Registering RegId to Server succeeded, saved regId to preferences
						
					    editor.putString(Constants.PREFERENCE_GCM_ID, regId);
					    editor.putInt(Constants.PREFERENCE_GCM_REGISTERED_APP_VERSION, currentAppVersion);
					    
					    if(editor.commit()) {
					    	Log.i(Constants.TAG_GCM, "Saving regId succeeded, app version : " + currentAppVersion);
					    } else {
					    	Log.e(Constants.TAG_GCM, "Failed saving regId to preferences");
					    }
					    
					} else {
						Log.e(Constants.TAG_GCM, "Failed register GCM id to server");
					}
					
				} catch (IOException e) {
					Log.e(Constants.TAG_GCM, e.getMessage());
				}
			}
			
		} else if(GooglePlayServicesUtil.isUserRecoverableError(gcmAvailability)) {
			
			handler.post(new Runnable() {
				@Override
				public void run() {
					GooglePlayServicesUtil.getErrorDialog(gcmAvailability, MainActivity.this, REQUEST_GCM_ERROR_DIALOG).show();
				}
			});
			
		}
	}

	
	private void update() {
		
		if(isUpdating) {
			return;
		}
		
		isUpdating = true;
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				setSupportProgressBarIndeterminateVisibility(true);
			}
		});
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				URL url = null;
				try {
					url = new URL("http://www.cc.ntut.edu.tw/~jykuo/course/c10301.txt");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				if(url != null) {
					InputStream inputStream = null;
					try {
						URLConnection urlConnection = url.openConnection();
						inputStream = new BufferedInputStream(urlConnection.getInputStream());
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "Big5"));
			
				        final StringBuilder responsedTextBuilder = new StringBuilder();
				        String line;
				        
				        while ((line = reader.readLine()) != null) {
				        	responsedTextBuilder.append(line + "\n");
				        }
				        final String responsedText = responsedTextBuilder.toString();
				        
				        handler.post(new Runnable() {
							@Override
							public void run() {
								try {
									handleDataString(responsedText);
									Utils.saveToDisk(MainActivity.this, responsedText);
									
									Toast.makeText(MainActivity.this, "已更新", Toast.LENGTH_SHORT).show();
									
									Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
									preferencesEditor.putInt(Constants.PREFERENCE_LAST_UPDATE_TIMESTAMP, Utils.getCurrentTimestamp());
									preferencesEditor.apply();
								} catch (Exception e) {
									Toast.makeText(MainActivity.this, "無法更新 " + e.getMessage(), Toast.LENGTH_SHORT).show();
								}
							}
				        });
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if(inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				handler.post(new Runnable() {
					@Override
					public void run() {
						setSupportProgressBarIndeterminateVisibility(false);
					}
				});
				isUpdating = false;
			}
		}).start();
	}
	
	private void handleDataString(String dataString) {
		ArrayList<String> sectorStrings = new ArrayList<String>();
		
		char previousChar = 0;
		int repeatTimes = 0;
		int currentIndex = 0;
		
		while(true) {
			if(currentIndex == dataString.length()) {
				//last character
				sectorStrings.add(dataString);
				break;
			}
			
			char currentChar = dataString.charAt(currentIndex);
			if(currentChar == previousChar && currentChar != ' ') {
				repeatTimes ++;
				if(repeatTimes == 45) {
					//separator detected
					sectorStrings.add(dataString.substring(0, currentIndex - repeatTimes));
					dataString = dataString.substring(dataString.indexOf('\n', currentIndex));
					
					currentIndex = 0;
					repeatTimes = 0;
				}
			} else {
				repeatTimes = 0;
			}
			previousChar = currentChar;
			
			currentIndex ++;
		}
		
		sectors.clear();
		int currentWeek = -1;
		
		for(String sectorString : sectorStrings) {
			//Trim
			sectorString = Utils.trim(sectorString);
			if(sectorString.isEmpty()) {
				continue;
			}
			
			Sector sector = new Sector();
			
			//Check if its week line
			try {
				String firstLine = Utils.getFirstLine(sectorString);
				if(firstLine.startsWith("Week ")) {
					String weekString = firstLine.substring(5);
					sectorString = sectorString.substring(sectorString.indexOf('\n') + 1);
					sectorString = Utils.trim(sectorString);
					
					currentWeek = Integer.valueOf(weekString);
					maxWeek = Math.max(currentWeek, maxWeek);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			
			//Find number
			String numberString = Utils.getNumberString(sectorString);
			if(numberString != null) {
				sector.number = Integer.valueOf(numberString);
				sectorString = sectorString.substring(numberString.length() + 1);
				//
			} else {
				sector.number = -1;
			}
			
			if(Utils.getFirstLine(sectorString).contains("課堂練習")) {
				sector.week = -1;
			} else {
				sector.week = currentWeek;
			}
			
			sector.title = Utils.getFirstLine(sectorString);
			
			sectorString = sectorString.substring(sectorString.indexOf("\n") + 1);
			
			sectorString = Utils.trim(sectorString);
			
			sector.message = sectorString;
			sectors.add(sector);
		}
		
		listAdapter.notifyDataSetChanged();
	}
	
	private PagerAdapter pagerAdapter = new PagerAdapter() {

		public CharSequence getPageTitle(int position) {
			switch(position) {
			case 0:
				return "題目";
			case 1:
				return "參考解答";
			default: 
				return null;
			}
		};
		
		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		public Object instantiateItem(ViewGroup container, int position) {
			switch(position) {
			case 0:
				container.addView(subjectListView);
				return subjectListView;
			case 1:
				container.addView(solutionListView);
				return solutionListView;
			default: 
				return null;
			}
		};
		
		public void destroyItem(ViewGroup container, int position, Object object) {
			switch(position) {
			case 0:
				container.removeView(subjectListView);
				break;
			case 1:
				container.removeView(solutionListView);
				break;
			}
		}
	};
	
	
	class SectorListAdapter extends BaseAdapter {
		private class ViewHolder {
			TextView titleTextView;
			TextView subTitleTextView;
		}
		
		public SectorListAdapter(ListView listView) {
			listView.setOnItemClickListener(onItemClickListener);
		}
		
		@Override
		public int getCount() {
			return sectors.size();
		}

		@Override
		public Object getItem(int position) {
			return sectors.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder viewHolder;
			Sector sector = sectors.get(position);
			
			if(convertView == null) {
				convertView = MainActivity.this.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
				viewHolder = new ViewHolder();
				viewHolder.titleTextView = (TextView)convertView.findViewById(android.R.id.text1);
				viewHolder.subTitleTextView = (TextView)convertView.findViewById(android.R.id.text2);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			viewHolder.titleTextView.setText(sector.getTitle());
			viewHolder.subTitleTextView.setText(sector.message);
			
			viewHolder.subTitleTextView.setEllipsize(TruncateAt.END);
			viewHolder.subTitleTextView.setSingleLine();
			if(maxWeek == sector.week) {
				viewHolder.titleTextView.setTextColor(0xFF66CCCC);
			} else {
				viewHolder.titleTextView.setTextColor(Color.GRAY);
			}
			viewHolder.subTitleTextView.setTextColor(Color.DKGRAY);
			
			convertView.setPadding(25, 0, 25, 0);
			
			return convertView;
		}
		
		@Override
		public void notifyDataSetChanged() {
			Collections.sort(sectors, comparator);
			super.notifyDataSetChanged();
		}
		
		private Comparator<Sector> comparator = new Comparator<Sector>() {
			
			@Override
			public int compare(Sector lhs, Sector rhs) {
				if(lhs.number == -1 && lhs.week != -1 || rhs.number == -1 && rhs.week != -1) {
					return Integer.valueOf(rhs.week).compareTo(lhs.week);
				} else {
					return Integer.valueOf(rhs.number).compareTo(lhs.number);
				}
			}
		};
		
		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				notifyDataSetChanged();
				
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, ViewActivity.class);
				intent.putExtra("title", sectors.get(position).getTitle());
				intent.putExtra("message", sectors.get(position).message);
				MainActivity.this.startActivity(intent);
			}
		};
	};
}
