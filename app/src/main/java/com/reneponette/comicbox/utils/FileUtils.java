package com.reneponette.comicbox.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.reneponette.comicbox.application.GlobalApplication;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

public class FileUtils {
	public static File saveBitmapToFileCache(Bitmap bitmap, String filename) {

		File coverDir = new File(GlobalApplication.instance().getCacheDir(), "cover");
		if (coverDir.exists() == false)
			coverDir.mkdirs();

		File fileCacheItem = new File(coverDir, filename);
		OutputStream out = null;

		try {
			fileCacheItem.createNewFile();
			out = new FileOutputStream(fileCacheItem);
			bitmap.compress(CompressFormat.JPEG, 100, out);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fileCacheItem;
	}
}
