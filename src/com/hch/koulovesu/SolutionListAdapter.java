package com.hch.koulovesu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class SolutionListAdapter extends BaseAdapter {
	
	private ArrayList<Solution> solutions = new ArrayList<Solution>();
	private LayoutInflater inflater;
	
	public SolutionListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return solutions.size();
	}

	@Override
	public Object getItem(int position) {
		return solutions.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		final ViewHolder viewHolder;
		final Solution solution = solutions.get(position);
		
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.item_solution_list, null);
			viewHolder = new ViewHolder();
			viewHolder.titleTextView = (TextView)convertView.findViewById(android.R.id.text1);
			viewHolder.authorTextView = (TextView)convertView.findViewById(android.R.id.text2);
			viewHolder.portraitImageView = (ImageView)convertView.findViewById(R.id.user_portrait);
			
			viewHolder.authorTextView.setEllipsize(TruncateAt.END);
			viewHolder.authorTextView.setSingleLine();
			viewHolder.titleTextView.setTextColor(Color.GRAY);
			viewHolder.authorTextView.setTextColor(Color.DKGRAY);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder)convertView.getTag();
		}
		viewHolder.titleTextView.setText(solution.getTitle());
		
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				return solution.author.getName();
			}
			protected void onPostExecute(String result) {
				viewHolder.authorTextView.setText(String.format("by %s", result));
			}
		}.execute(solution.author.getUserId());
		
		new AsyncTask<String, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(String... params) {
				return solution.author.getPortraitThumbnail();
			}
			protected void onPostExecute(Bitmap result) {
				viewHolder.portraitImageView.setImageBitmap(result);
			}
		}.execute(solution.author.getUserId());
		
		return convertView;
	}
	
	@Override
	public void notifyDataSetChanged() {
		Collections.sort(solutions, comparator);
		super.notifyDataSetChanged();
	}
	
	private Comparator<Solution> comparator = new Comparator<Solution>() {
		
		@Override
		public int compare(Solution lhs, Solution rhs) {
			return Integer.valueOf(rhs.number).compareTo(lhs.number);
		}
	};
	
	public void update(ArrayList<Solution> items) {
		solutions = items;
		notifyDataSetChanged();
	}
	
	private class ViewHolder {
		TextView titleTextView;
		TextView authorTextView;
		ImageView portraitImageView;
	}
};