package com.reneponette.comicbox.utils;

import android.util.Log;

public class Logger {
	public static void d(Object obj, String msg) {
		Log.d(obj.getClass().getName(), msg);
	}
	
	public static void e(Object obj, String msg) {
		Log.e(obj.getClass().getName(), msg);
	}
	
	public static void i(Object obj, String msg) {
		Log.i(obj.getClass().getName(), msg);
	}
}
