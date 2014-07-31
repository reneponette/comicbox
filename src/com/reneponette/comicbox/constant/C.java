package com.reneponette.comicbox.constant;

import java.io.File;

import android.os.Environment;

import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.utils.SimpleCrypto;

public class C {
	final static public String LAST_LOCAL_PATH = "lastLocalPath";
	final static public String LAST_DROPBOX_PATH = "lastDropboxPath";
	
    final static public String DROPBOX_APP_SECRET = SimpleCrypto.decrypt("77977797", "lAF0SSyB3juP6kjhIjWckg==");
    
    final static public String DEFAULT_NAME = "ComicBox";
    final static public String DEFAULT_LOCAL_PATH = new File(Environment.getExternalStorageDirectory(), DEFAULT_NAME).toString();
    final static public String DEFAULT_DROPBOX_PATH = "/";
    
    final static public String COMICS_CACHE_ROOT = new File(GlobalApplication.instance().getCacheDir(), "comics").toString();    

}
