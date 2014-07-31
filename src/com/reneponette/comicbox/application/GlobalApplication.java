package com.reneponette.comicbox.application;

import com.reneponette.comicbox.db.DBOpenHelper;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;


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
