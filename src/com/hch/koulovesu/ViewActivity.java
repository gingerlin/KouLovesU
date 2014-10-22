package com.hch.koulovesu;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;


public class ViewActivity extends SherlockActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Setup ActionBar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
		
		String title = getIntent().getExtras().getString("title");
		actionBar.setTitle(title);
		
		final String userId = getIntent().getExtras().getString("userId");
		if(userId != null) {
			new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					return String.format("by %s", User.get(userId).getName());
				}
				
				@Override
				protected void onPostExecute(String result) {
					actionBar.setSubtitle(result);
					super.onPostExecute(result);
				}
			}.execute();
			
		}
		
		String content = getIntent().getExtras().getString("content");
		TextView textView = new TextView(this);
		textView.setText(content);
		
		ScrollView scrollView = new ScrollView(this);
		float screenDensity = getResources().getDisplayMetrics().density;
		int padding = (int) (16 * screenDensity);
		scrollView.setPadding(padding, padding, padding, padding);
		scrollView.addView(textView);
		
		setContentView(scrollView);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
