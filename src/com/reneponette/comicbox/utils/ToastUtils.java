package com.reneponette.comicbox.utils;

import com.reneponette.comicbox.application.GlobalApplication;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

	public static void toast(String message) {
		Toast.makeText(GlobalApplication.instance().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
