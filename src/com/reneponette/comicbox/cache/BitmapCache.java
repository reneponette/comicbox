package com.reneponette.comicbox.cache;

import com.reneponette.comicbox.db.FileInfo;

import android.graphics.Bitmap;
import android.util.LruCache;

public enum BitmapCache {

	INSTANCE;
	
	private BitmapCache() {
	    // Get max available VM memory, exceeding this amount will throw an
	    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
	    // int in its constructor.
	    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

	    // Use 1/8th of the available memory for this memory cache.
	    final int cacheSize = maxMemory / 8;

	    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than
	            // number of items.
	            return bitmap.getByteCount() / 1024;
	        }
	    };		
	}
	
	private LruCache<String, Bitmap> mMemoryCache;


	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	        mMemoryCache.put(key, bitmap);
	    }
	}

	public Bitmap getBitmapFromMemCache(String key) {
	    return mMemoryCache.get(key);
	}
	
	public Bitmap removeBitmapFromMemCache(String key) {
		return mMemoryCache.remove(key);
	}
	
	public void addBitmapToMemoryCache(FileInfo info, Bitmap bitmap) {
	    if (getBitmapFromMemCache(info.getKey()) == null) {
	        mMemoryCache.put(info.getKey(), bitmap);
	    }
	}

	public Bitmap getBitmapFromMemCache(FileInfo info) {
	    return mMemoryCache.get(info.getKey());
	}
	
	public Bitmap removeBitmapFromMemCache(FileInfo info) {
		return mMemoryCache.remove(info.getKey());
	}	
	
}
	
	
