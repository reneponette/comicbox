package com.reneponette.comicbox.utils;

import android.content.res.Resources;
import android.util.TypedValue;

import com.reneponette.comicbox.application.GlobalApplication;

public class MetricUtils {

	private static Resources getResources() {
		return GlobalApplication.instance().getResources();
	}

	public static int dpToPixel(int dp) {
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
		return px;
	}

	public static int getDisplayWidth() {
		return getResources().getDisplayMetrics().widthPixels;
	}
	
	public static int getDisplayHeight() {
		return getResources().getDisplayMetrics().heightPixels;
	}
	
	public static int getOrientation() {
		return getResources().getConfiguration().orientation;		
	}
}
