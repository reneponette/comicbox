package com.reneponette.comicbox.constant;

import java.io.File;

import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.utils.SimpleCrypto;

public class C {
	final static public boolean DEBUG = true;
	
	final static public boolean ENABLE_ADAM = true;
	final static public boolean ENABLE_DROPBOX = true;
	final static public boolean ENABLE_GOOGLE_DRIVE = false;
	final static public boolean ENABLE_FTP = true;
	
	final static public String LAST_LOCAL_PATH = "lastLocalPath";
	final static public String LAST_DROPBOX_PATH = "lastDropboxPath";
	
    final static public String DROPBOX_APP_SECRET = SimpleCrypto.decrypt("77977797", "lAF0SSyB3juP6kjhIjWckg==");
    
    final static public String DEFAULT_NAME = "ComicBox";
    final static public String LOCAL_ROOT_PATH = "/sdcard";
    final static public String DEFAULT_LOCAL_PATH = LOCAL_ROOT_PATH + "/" + DEFAULT_NAME;
    final static public String DEFAULT_DROPBOX_PATH = "/";
    
    final static public String COMICS_CACHE_ROOT = new File(GlobalApplication.instance().getCacheDir(), "comics").toString();
    
    final static public int COVER_W = 400;
    final static public int COVER_H = 600;

}
