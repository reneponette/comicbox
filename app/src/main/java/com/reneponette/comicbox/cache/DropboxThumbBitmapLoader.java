package com.reneponette.comicbox.cache;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.ThumbFormat;
import com.dropbox.client2.DropboxAPI.ThumbSize;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor.Type;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.utils.FileUtils;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxThumbBitmapLoader {

	public interface OnLoadBitmapListener {
		public void onLoadBitmap(Bitmap bitmap, String key);
	}

	FileInfo info;
	DropboxAPI<AndroidAuthSession> api;

	ImageView iv;
	OnLoadBitmapListener listener;

	private static Set<FileInfo> requestSet = new HashSet<FileInfo>();

	public DropboxThumbBitmapLoader(FileInfo info, DropboxAPI<AndroidAuthSession> api, OnLoadBitmapListener l) {
		this.info = info;
		this.listener = l;
		this.api = api;
	}

	public DropboxThumbBitmapLoader(FileInfo info, DropboxAPI<AndroidAuthSession> api, ImageView iv) {
		this.info = info;
		this.iv = iv;
		this.api = api;

		iv.setTag(info.getKey());
	}

	public void run() {
		if (info == null)
			return;

		Bitmap bitmap = BitmapCache.INSTANCE.getBitmapFromMemCache(info.getKey());
		if (bitmap != null) {
			if (listener != null)
				listener.onLoadBitmap(bitmap, info.getKey());
			if (iv != null)
				iv.setImageBitmap(bitmap);
			return;
		}

		if (iv != null) {
			//기본 이미지 설정
			iv.setScaleType(ScaleType.CENTER_INSIDE);
			if (info.getMeta().type == FileType.DIRECTORY) {
				iv.setImageResource(R.drawable.ic_folder);
			} else {
				iv.setImageResource(R.drawable.ic_comics);
			}
		}

		if (requestSet.contains(info))
			return;

		requestSet.add(info);

		PausableThreadPoolExecutor.instance(Type.NETWORK).execute(new Runnable() {

			@Override
			public void run() {
				Bitmap bitmap = null;

				// 디스크에 캐쉬되어있으면...
				if (StringUtils.isBlank(info.getMeta().coverPath) == false && iv != null) {
					bitmap = BitmapFactory.decodeFile(info.getMeta().coverPath);
					BitmapCache.INSTANCE.addBitmapToMemoryCache(info, bitmap);
					notifyOnMainThread(bitmap);
					return;
				}

				try {
					// 비트맵 처음 생성일 경우
					switch (info.getMeta().type) {
					case DIRECTORY:
						bitmap = ImageUtils.extractCoverFromFolder(api, info, C.COVER_W, C.COVER_H);
						break;
					case ZIP:
						DropboxInputStream dis = api.getFileStream(info.getPath(), null);
						bitmap = ImageUtils.extractCoverFromZip(dis, C.COVER_W, C.COVER_H);
						break;
					case JPG:
						bitmap = BitmapFactory.decodeStream(api.getThumbnailStream(info.getPath(), ThumbSize.BESTFIT_320x240, ThumbFormat.JPEG));
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
				} catch (DropboxException e) {
					e.printStackTrace();
				}

				requestSet.remove(info);
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
					iv.setScaleType(ScaleType.CENTER_CROP);
					iv.setImageBitmap(bitmap);
				}
			}
		});
	}

}
