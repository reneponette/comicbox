package com.reneponette.comicbox.cache;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor.Type;
import com.reneponette.comicbox.utils.FileUtils;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;

public class LocalThumbBitmapLoader {

	public interface OnLoadBitmapListener {
		public void onLoadBitmap(Bitmap bitmap, String key);
	}

	FileInfo info;

	ImageView iv;
	OnLoadBitmapListener listener;
	boolean cacheFirst = true;

	public LocalThumbBitmapLoader(FileInfo info, OnLoadBitmapListener l) {
		this.info = info;
		this.listener = l;
	}

	public LocalThumbBitmapLoader(FileInfo info, ImageView iv) {
		this.info = info;
		this.iv = iv;

		iv.setTag(info.getKey());
	}

	public LocalThumbBitmapLoader lookupCacheFirst(boolean b) {
		cacheFirst = b;
		return this;
	}

	public void run() {
		if (info == null)
			return;

		if (cacheFirst) {
			Bitmap bitmap = BitmapCache.INSTANCE.getBitmapFromMemCache(info.getKey());
			if (bitmap != null) {
				if (listener != null)
					listener.onLoadBitmap(bitmap, info.getKey());
				if (iv != null)
					iv.setImageBitmap(bitmap);
				return;
			}
		}

		if (iv != null)
			iv.setImageBitmap(null);

		PausableThreadPoolExecutor.instance(Type.DISK).execute(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmap;

				// 디스크에 캐쉬되어있으면...
				if (StringUtils.isBlank(info.getMeta().coverPath) == false && iv != null) {
					bitmap = BitmapFactory.decodeFile(info.getMeta().coverPath);
					BitmapCache.INSTANCE.addBitmapToMemoryCache(info, bitmap);
					notifyOnMainThread(bitmap);
					return;
				}

				// 비트맵 처음 생성일 경우
				switch (info.getMeta().type) {
				case DIRECTORY:
					bitmap = ImageUtils.extractCoverFromFolder(info.getFile(), false);
					break;
				case ZIP:
					bitmap = ImageUtils.extractCoverFromZip(info.getFile());
					break;
				case PDF:
					bitmap = ImageUtils.extractCoverFromPdf(GlobalApplication.instance(), info.getFile());
					break;
				case JPG:
					bitmap = ImageUtils.extractCoverFromJpg(info.getFile());
					break;
				default:
					bitmap = null;
					break;
				}

				if (bitmap != null) {
					File coverFile = FileUtils.saveBitmapToFileCache(bitmap, info.getKey());
					if (coverFile != null) {
						info.getMeta().coverPath = coverFile.getAbsolutePath();
						FileInfoDAO.instance().insertOrUpdate(info);
					}

					BitmapCache.INSTANCE.addBitmapToMemoryCache(info, bitmap);
					notifyOnMainThread(bitmap);
				}
			}
		});

	}

	private void notifyOnMainThread(final Bitmap bitmap) {
		GlobalApplication.instance().getHandler().post(new Runnable() {

			@Override
			public void run() {
				if (listener != null)
					listener.onLoadBitmap(bitmap, info.getKey());
				if (iv != null && info.getKey().equals(iv.getTag())) {
					iv.setImageBitmap(bitmap);
				}
			}
		});
	}
}
