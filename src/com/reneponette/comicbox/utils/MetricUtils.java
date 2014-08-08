package com.reneponette.comicbox.utils;

import android.content.Context;
import android.util.TypedValue;

public class MetricUtils {
	public static int dpToPixel(Context context, int dp) {
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
		return px;
	}
}
