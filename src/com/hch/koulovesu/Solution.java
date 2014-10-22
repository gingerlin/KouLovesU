package com.hch.koulovesu;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

import com.hch.koulovesu.ConnectionHelper.HttpResult;

class Solution {
	String id;
	User author;
	String title;
	String content;
	int number;
	
	public Solution(JSONObject json) throws JSONException {
		id = json.getString("id");
		author = User.get(json.getString("author_id"));
		title = json.getString("title");
		content = json.getString("content");
		number = json.getInt("number");
	}
	
	public String getTitle() {
		if(number != -1) {
			String titleString = String.format(Locale.TAIWAN, "ÃD¥Ø %d", number);
			if(!title.isEmpty()) {
				titleString += " : " + title;
			}
			return titleString;
		} else {
			return title;
		}
	}
	
	
}

