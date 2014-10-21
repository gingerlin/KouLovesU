package com.hch.koulovesu;

import java.util.Locale;

class Sector {
	String title;
	String message;
	int number;
	int week;
	
	String getTitle() {
		if(number != -1) {
			String titleString = String.format(Locale.TAIWAN, "§@·~ %d", number);
			if(!title.isEmpty()) {
				titleString += " : " + title;
			}
			return titleString;
		} else if(week != -1) {
			String titleString = String.format(Locale.TAIWAN, "²Ä%d¶g", week);
			if(!title.isEmpty()) {
				titleString += " : " + title;
			}
			return titleString;
		} else {
			return title;
		}
	}
}

