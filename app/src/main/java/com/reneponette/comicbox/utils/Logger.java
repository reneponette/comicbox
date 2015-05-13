package com.reneponette.comicbox.utils;

import com.reneponette.comicbox.constant.C;

import android.util.Log;

public class Logger {
	public static void d(Object obj, String msg) {
		if (C.DEBUG)
			Log.d(obj.getClass().getName(), msg);
	}

	public static void e(Object obj, String msg) {
		if (C.DEBUG)
			Log.e(obj.getClass().getName(), msg);
	}

	public static void i(Object obj, String msg) {
		if (C.DEBUG)
			Log.i(obj.getClass().getName(), msg);
	}
}
