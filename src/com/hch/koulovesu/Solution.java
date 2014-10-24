package com.hch.koulovesu;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
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
	
	public static ArrayList<Solution> fromJSONArray(JSONArray jsonArray) throws JSONException{
		ArrayList<Solution> result = new ArrayList<Solution>();
		for(int i = 0, length = jsonArray.length(); i < length; i++) {
			result.add(new Solution(jsonArray.getJSONObject(i)));
		}
		return result;
	}
	
	public static ArrayList<Solution> fromJSONArrayString(String jsonArrayString) {
		try {
			JSONArray array = new JSONArray(jsonArrayString);
			return fromJSONArray(array);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}

