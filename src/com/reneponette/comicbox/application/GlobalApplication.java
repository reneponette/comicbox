package com.reneponette.comicbox.application;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.crashlytics.android.Crashlytics;
import com.reneponette.comicbox.db.DBOpenHelper;


public class GlobalApplication extends Application {

	private static volatile GlobalApplication instance = null;
	
	private Handler handler = new Handler(Looper.getMainLooper());	

	public final static GlobalApplication instance() {
		return instance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		
		
		DBOpenHelper.getInstance();
		Crashlytics.start(this);
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		
		DBOpenHelper.getInstance().close();;
	}
	

	public Handler getHandler() {
		return handler;
	}


}
