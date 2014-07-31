package com.reneponette.comicbox.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxComicsDownloader {

	public interface OnLoadComicsListener {
		public void onLoadComics(File comics);

		public void onProgress(long bytes, long total);
	}

	FileInfo info;
	DropboxAPI<AndroidAuthSession> api;
	boolean cancelled;

	OnLoadComicsListener listener;
	private Thread runningThread;

	public DropboxComicsDownloader(FileInfo info, DropboxAPI<AndroidAuthSession> api, OnLoadComicsListener l) {
		this.info = info;
		this.listener = l;
		this.api = api;
	}

	public void run() {
		if (info == null)
			return;

		// 디스크에 캐쉬되어있으면...
		if (StringUtils.isBlank(info.getMeta().cachePath) == false) {
			File f = new File(info.getMeta().cachePath);
			if (f.exists() && listener != null) {
				listener.onLoadComics(f);
				return;
			}
		}

		runningThread = new Thread(new Runnable() {

			@Override
			public void run() {
				File file = null;
				try {
					info.getCacheDir().mkdirs();
					file = info.getCacheOuputFile();
					
					FileOutputStream fos = new FileOutputStream(file);
					api.getFile(info.getPath(), null, fos, new ProgressListener() {

						@Override
						public void onProgress(final long bytes, final long total) {
							if (listener != null) {
								GlobalApplication.instance().getHandler().post(new Runnable() {

									@Override
									public void run() {
										listener.onProgress(bytes, total);
									}
								});
							}
						}
					});

				} catch (FileNotFoundException e) {
					Log.e("download", "Couldn't create a local file");
				} catch (DropboxException e) {
					e.printStackTrace();
				}
				
				
				final File f = file;
				if (listener != null && !cancelled) {
					GlobalApplication.instance().getHandler().post(new Runnable() {

						@Override
						public void run() {
							listener.onLoadComics(f);
						}
					});					
				}
			}
		});
		runningThread.start();
	}
	
	public void stop() {
		if(runningThread != null)
			runningThread.interrupt();
		
		cancelled = true;
	}
}
