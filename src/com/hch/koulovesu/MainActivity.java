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
import java.util.HashMap;
import java.util.UUID;

import org.json.JSONArray;
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends SherlockActivity {

	private static final int REQUEST_GCM_ERROR_DIALOG = 0;
	private static final int PAGE_SECTORS = 0;
	private static final int PAGE_SOLUTIONS = 1;
	private static final int ACTION_UPDATE_SECTORS = 0;
	private static final int ACTION_UPDATE_SOLUTIONS = 1;
	
	private SectorListAdapter sectorListAdapter;
	private SolutionListAdapter solutionListAdapter;
	private Handler handler;
	private SharedPreferences preferences;
	
	private ListView sectorListView;
	private ListView solutionListView;
	private ViewPager viewPager;
	
	private boolean isSectorsUpdating = false;
	private boolean isSolutionsUpdating = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		if(Constants.STRICT_MODE_ENABLED) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		        .detectAll()
		        .penaltyLog()
		        .penaltyDialog()
		        .build());
		}
		
		//Setup ActionBar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);
		
		BitmapCache.initialize(this);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		sectorListView = new ListView(this);
		solutionListView = new ListView(this);
		
		sectorListAdapter = new SectorListAdapter(this);
		sectorListView.setAdapter(sectorListAdapter);
		
		solutionListAdapter = new SolutionListAdapter(this);
		solutionListView.setAdapter(solutionListAdapter);
		
		sectorListView.setOnItemClickListener(onItemClickListener);
		solutionListView.setOnItemClickListener(onItemClickListener);
		
		viewPager = (ViewPager) findViewById(R.id.viewpager);		
		viewPager.setAdapter(pagerAdapter);
		
		TitlePageIndicator pageIndicator = ((TitlePageIndicator)findViewById(R.id.indicator)); 
		pageIndicator.setViewPager(viewPager);
		pageIndicator.setOnPageChangeListener(onPageChangeListener);
		
		handler = new Handler();
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
				
				//Check for latest app version
				checkLatestVersion();
				
				//Fetch cached sector contents and solutions
				final String cachedContent = preferences.getString(Constants.PREFERENCE_LAST_SECTORS_CONTENT, null);
				if(cachedContent != null && !cachedContent.isEmpty()) {
					final ArrayList<Sector> sectors = getSectorsFromString(cachedContent);
					handler.post(new Runnable() {
						@Override
						public void run() {
							sectorListAdapter.update(sectors);
						}
			        });
				}
				
				final String cachedSolutions = preferences.getString(Constants.PREFERENCE_LAST_SOLUTIONS, null);
				if(cachedSolutions != null && !cachedSolutions.isEmpty()) {
					final ArrayList<Solution> solutions = Solution.fromJSONArrayString(cachedSolutions);
					if(solutions != null) {
						handler.post(new Runnable(){
							@Override
							public void run() {
								solutionListAdapter.update(solutions);
							}
						});
					}
				}
				
				//Update sectors from the Internet
				int lastUpdateTimestamp = preferences.getInt(Constants.PREFERENCE_LAST_SECTOR_UPDATE_TIMESTAMP, 0);
				if(Utils.getCurrentTimestamp() - lastUpdateTimestamp >= Constants.CONFIG_SECTOR_UPDATE_INTERVAL) {
					tryUpdateSectorsFromInternet();
				}
				
				//Update solutions
				int lastSolutionUpdateTimestamp = preferences.getInt(Constants.PREFERENCE_LAST_SOLUTIONS_UPDATE_TIMESTAMP, 0);
				if(Utils.getCurrentTimestamp() - lastSolutionUpdateTimestamp >= Constants.CONFIG_SOLUTION_UPDATE_INTERVAL) {
					updateSolutionsFromServer();
				}
				
			}
		}).start();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		switch(viewPager.getCurrentItem()) {
		case PAGE_SECTORS:
			if(!isSectorsUpdating) {
				menu.add(Menu.NONE, ACTION_UPDATE_SECTORS, Menu.NONE, "更新題目")
					.setIcon(R.drawable.ic_action_refresh)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			setSupportProgressBarIndeterminateVisibility(isSectorsUpdating);
			break;
		case PAGE_SOLUTIONS:
			if(!isSolutionsUpdating) {
				menu.add(Menu.NONE, ACTION_UPDATE_SOLUTIONS, Menu.NONE, "更新解答")
					.setIcon(R.drawable.ic_action_refresh)
					.setEnabled(!isSolutionsUpdating)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			setSupportProgressBarIndeterminateVisibility(isSolutionsUpdating);
			break;
		default:
			setSupportProgressBarIndeterminateVisibility(false);
			break;
		}
		
	
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case ACTION_UPDATE_SECTORS:
			tryUpdateSectorsFromInternet();
			break;
		case ACTION_UPDATE_SOLUTIONS:
			updateSolutionsFromServer();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void checkLatestVersion() {
		HttpResult result = ConnectionHelper.sendGetRequest("getLatestVersionNumber", true, null);
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
	}
	
	private void tryRegisterGCM() {
		
		//Get Device UUID
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

	
	private void tryUpdateSectorsFromInternet() {
		
		if(isSectorsUpdating) {
			return;
		}
		
		isSectorsUpdating = true;
		handler.post(new Runnable() {
			@Override
			public void run() {
				supportInvalidateOptionsMenu();
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
				        				        
				        final String oldContent = preferences.getString(Constants.PREFERENCE_LAST_SECTORS_CONTENT, null);
				        
				        if(!responsedText.equals(oldContent)) {
				        	
				        	Runnable runnable;
				        	try {
					        	final ArrayList<Sector> sectors = getSectorsFromString(responsedText);
					        	Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
								preferencesEditor.putInt(Constants.PREFERENCE_LAST_SECTOR_UPDATE_TIMESTAMP, Utils.getCurrentTimestamp());
								preferencesEditor.putString(Constants.PREFERENCE_LAST_SECTORS_CONTENT, responsedText);
								preferencesEditor.apply();
								runnable = new Runnable() {
									@Override
									public void run() {
										sectorListAdapter.update(sectors);
										Toast.makeText(MainActivity.this, "已更新", Toast.LENGTH_SHORT).show();
									}
								};			        	
				        	} catch (final Exception e) {
				        		runnable = new Runnable() {
									@Override
									public void run() {
										sectorListAdapter.notifyDataSetChanged();
										Toast.makeText(MainActivity.this, "無法更新 " + e.getMessage(), Toast.LENGTH_SHORT).show();
									}
								};
				        	}
					        handler.post(runnable);
				        }
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
				isSectorsUpdating = false;
				handler.post(new Runnable() {
					@Override
					public void run() {
						supportInvalidateOptionsMenu();
					}
				});;
			}
		}).start();
	}
	
	private ArrayList<Sector> getSectorsFromString(String dataString) {
		ArrayList<Sector> sectors = new ArrayList<Sector>();
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
		int maxWeek = -1;
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
		
		sectorListAdapter.setHighlightWeek(maxWeek);
		
		return sectors;
	}
	
	private void updateSolutionsFromServer() {
		
		if(isSolutionsUpdating) {
			return;
		}
		
		isSolutionsUpdating = true;
		handler.post(new Runnable() {
			@Override
			public void run() {
				supportInvalidateOptionsMenu();
			}
		});
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpResult httpResult = ConnectionHelper.sendGetRequest("getSolutions", true, null);
				if(httpResult != null && httpResult.success) {
					try {
						JSONArray rows = httpResult.result.getJSONArray("rows");
						final ArrayList<Solution> result = Solution.fromJSONArray(rows);
						if(result != null) {
							Editor editor = preferences.edit();
							editor.putInt(Constants.PREFERENCE_LAST_SOLUTIONS_UPDATE_TIMESTAMP, Utils.getCurrentTimestamp());
							editor.putString(Constants.PREFERENCE_LAST_SOLUTIONS, rows.toString());
							editor.apply();
							handler.post(new Runnable(){
								@Override
								public void run() {
									solutionListAdapter.update(result);
								}
							});
						}
					} catch(JSONException e) {
						e.printStackTrace();
					}
				}
				
				isSolutionsUpdating = false;
				handler.post(new Runnable() {
					@Override
					public void run() {
						supportInvalidateOptionsMenu();
					}
				});;
			}
		}).start();
	}
	
	private PagerAdapter pagerAdapter = new PagerAdapter() {

		public CharSequence getPageTitle(int position) {
			switch(position) {
			case PAGE_SECTORS:
				return "題目";
			case PAGE_SOLUTIONS:
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
			case PAGE_SECTORS:
				container.addView(sectorListView);
				return sectorListView;
			case PAGE_SOLUTIONS:
				container.addView(solutionListView);
				return solutionListView;
			default: 
				return null;
			}
		};
		
		public void destroyItem(ViewGroup container, int position, Object object) {
			switch(position) {
			case PAGE_SECTORS:
				container.removeView(sectorListView);
				break;
			case PAGE_SOLUTIONS:
				container.removeView(solutionListView);
				break;
			}
		}
	};
	
	private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
		@Override
		public void onPageSelected(int arg0) {
			supportInvalidateOptionsMenu();
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {}
		
		@Override
		public void onPageScrollStateChanged(int arg0) {}
	};
	
	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if(parent == sectorListView) {
				Sector sector = (Sector) sectorListAdapter.getItem(position);
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, ViewActivity.class);
				intent.putExtra("title", sector.getTitle());
				intent.putExtra("content", sector.message);
				MainActivity.this.startActivity(intent);
			} else if(parent == solutionListView) {
				Solution solution = (Solution)solutionListAdapter.getItem(position);
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, ViewActivity.class);
				intent.putExtra("title", solution.getTitle());
				intent.putExtra("userId", solution.author.getUserId());
				intent.putExtra("content", solution.content);
				MainActivity.this.startActivity(intent);
			}
		}
	};
}
