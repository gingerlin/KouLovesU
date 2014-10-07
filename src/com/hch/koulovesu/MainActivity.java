package com.hch.koulovesu;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends SherlockActivity {

	private ArrayList<Sector> sectors = new ArrayList<Sector>();
	private SectorListAdapter listAdapter;
	private Handler handler;
	
	private int maxWeek = 0;
	
	private static Object diskLock = new Object();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Setup ActionBar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);
		
		ListView listView = (ListView)findViewById(R.id.listview);
		listAdapter = new SectorListAdapter(listView);
		listView.setAdapter(listAdapter);
		
		handler = new Handler();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String cachedContent = readFromDisk(MainActivity.this);
				if(cachedContent != null && !cachedContent.isEmpty()) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							handleDataString(cachedContent);
						}
			        });
				}
				update();
			}
		}).start();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		
	}
	
	private void update() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();
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
				        saveToDisk(MainActivity.this, responsedText);
				        handler.post(new Runnable() {
							@Override
							public void run() {
								handleDataString(responsedText);
								Toast.makeText(MainActivity.this, "已更新", Toast.LENGTH_SHORT).show();
							}
				        });
				        Looper.loop();
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
				if(repeatTimes == 10) {
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
			sectorString = trim(sectorString);
			if(sectorString.isEmpty()) {
				continue;
			}
			
			Sector sector = new Sector();
			
			//Check if its week line
			try {
				String firstLine = getFirstLine(sectorString);
				if(firstLine.startsWith("Week ")) {
					String weekString = firstLine.substring(5);
					sectorString = sectorString.substring(sectorString.indexOf('\n') + 1);
					sectorString = trim(sectorString);
					
					currentWeek = Integer.valueOf(weekString);
					maxWeek = Math.max(currentWeek, maxWeek);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			
			//Find number
			String numberString = getNumberString(sectorString);
			if(numberString != null) {
				sector.number = Integer.valueOf(numberString);
				sectorString = sectorString.substring(numberString.length() + 1);
				//
			} else {
				sector.number = -1;
			}
			

			sector.week = currentWeek;
			
			sector.title = getFirstLine(sectorString);
			
			sectorString = trim(sectorString);
			
			sector.message = sectorString.substring(sectorString.indexOf("\n") + 1);
			sectors.add(sector);
		}
		
		listAdapter.notifyDataSetChanged();
	}
	
	private static String getNumberString(String input) {
		
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
	
	private static String getFirstLine(String input) {
		return input.substring(0, input.indexOf("\n"));
	}
	
	private static String trim(String input) {
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
	
	private static String readFromDisk(Context context) {
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
	
	private static void saveToDisk(final Context context, final String content) {
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
	
	class Sector {
		String title;
		String message;
		int number;
		int week;
	}
	
	class SectorListAdapter extends BaseAdapter {
		class ViewHolder {
			TextView titleTextView;
			TextView subTitleTextView;
		}
		
		int selectedIndex = -1;
		
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
			if(sector.number != -1) {
				String titleString = String.format("作業 %d", sector.number);
				if(!sector.title.isEmpty()) {
					titleString += " : " + sector.title;
				}
				viewHolder.titleTextView.setText(titleString);
			} else {
				viewHolder.titleTextView.setText(sector.title);
			}
			viewHolder.subTitleTextView.setText(sector.message);
			
			if(selectedIndex == position) {
				viewHolder.subTitleTextView.setEllipsize(TruncateAt.START);
				viewHolder.subTitleTextView.setSingleLine(false);
				if(maxWeek == sector.week) {
					viewHolder.titleTextView.setTextColor(0xFF66CCCC);
				} else {
					viewHolder.titleTextView.setTextColor(Color.WHITE);
				}
				viewHolder.subTitleTextView.setTextColor(Color.WHITE);
				
				convertView.setPadding(25, 50, 25, 50);
				
			} else {
				viewHolder.subTitleTextView.setEllipsize(TruncateAt.END);
				viewHolder.subTitleTextView.setSingleLine();
				if(maxWeek == sector.week) {
					viewHolder.titleTextView.setTextColor(0xFF66CCCC);
				} else {
					viewHolder.titleTextView.setTextColor(Color.GRAY);
				}
				viewHolder.subTitleTextView.setTextColor(Color.DKGRAY);
				
				convertView.setPadding(25, 0, 25, 0);
			}
			
			return convertView;
		}
		
		@Override
		public void notifyDataSetChanged() {
			Collections.sort(sectors, comparator);
			super.notifyDataSetChanged();
		}
		
		private Comparator<Sector> comparator = new Comparator<MainActivity.Sector>() {
			
			@Override
			public int compare(Sector lhs, Sector rhs) {
				return Integer.valueOf(lhs.number).compareTo(rhs.number);
			}
		};
		
		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(selectedIndex == position) {
					selectedIndex = -1;
				} else {
					selectedIndex = position;
				}
				notifyDataSetChanged();
			}
		};
	};
}
