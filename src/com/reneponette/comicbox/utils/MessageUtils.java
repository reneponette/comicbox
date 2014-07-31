package com.reneponette.comicbox.utils;

import android.content.Context;
import android.widget.Toast;

public class MessageUtils {

	public static void toast(Context c, String message) {
		Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
	}
}
