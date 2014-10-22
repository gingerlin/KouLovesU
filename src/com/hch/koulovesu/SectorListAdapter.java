package com.hch.koulovesu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class SectorListAdapter extends BaseAdapter {
	
	private ArrayList<Sector> sectors = new ArrayList<Sector>();
	private LayoutInflater inflater;
	private int highlightWeek = -1;
	
	public SectorListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
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
			convertView = inflater.inflate(android.R.layout.simple_list_item_2, null);
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
		if(sector.week != -1 && highlightWeek == sector.week) {
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
	
	public void update(ArrayList<Sector> items) {
		sectors = items;
		notifyDataSetChanged();
	}
	
	public void setHighlightWeek(int weekNumber) {
		highlightWeek = weekNumber;
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
	
	private class ViewHolder {
		TextView titleTextView;
		TextView subTitleTextView;
	}
};