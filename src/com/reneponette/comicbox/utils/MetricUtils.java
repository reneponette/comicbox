package com.reneponette.comicbox.utils;

import com.reneponette.comicbox.application.GlobalApplication;

import android.util.TypedValue;

public class MetricUtils {
	public static int dpToPixel(int dp) {
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, GlobalApplication.instance().getResources().getDisplayMetrics());
		return px;
	}
}
