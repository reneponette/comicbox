package com.reneponette.comicbox.cache;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor;
import com.reneponette.comicbox.manager.PausableThreadPoolExecutor.Type;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class PageBitmapLoader {

	public interface OnLoadBitmapListener {
		public void onLoadBitmap(Bitmap bitmap, String key);
	}

	// 썸네일 프리뷰 보여줄때..
	PageInfo pi;

	boolean autocrop;
	boolean preview;

	ImageView iv;
	OnLoadBitmapListener listener;

	public PageBitmapLoader(PageInfo pi, OnLoadBitmapListener l, boolean autocrop, boolean preview) {
		this.pi = pi;
		this.listener = l;
		this.autocrop = autocrop;
		this.preview = preview;
		
	}

	public PageBitmapLoader(PageInfo pi, ImageView iv, boolean autocrop, boolean preview) {
		this.pi = pi;
		this.iv = iv;
		this.autocrop = autocrop;
		this.preview = preview;		
		iv.setTag(pi.toString());
	}
	
	public void run() {
		if (pi == null)
			return;
		

		//TODO: 파라메터 널 체크 처리

		Bitmap cachedBitmap = BitmapCache.INSTANCE.getBitmapFromMemCache(pi.toString() + "/" + preview);
		if (cachedBitmap != null && iv != null) {
			iv.setImageBitmap(cachedBitmap);
			return;
		}

		if (iv != null)
			iv.setImageBitmap(null);

		PausableThreadPoolExecutor.instance(Type.DISK).execute(new Runnable() {

			@Override
			public void run() {
				Bitmap bitmap;

				switch (pi.getType()) {
				case IMG_ZIP:
					bitmap = ImageUtils.getBitmap(pi.getZipFile(), pi.getZipEntry(), pi.getBuildType(), autocrop, preview);
					break;
				case IMG_PDF:
					bitmap = ImageUtils.getBitmap(pi.getPdfCore(), pi.getPdfIndex(), pi.getBuildType(), autocrop, preview);
					break;
				case IMG_FILE:
					bitmap = ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), autocrop, preview);
					break;
				default:
					bitmap = null;
					break;
				}
				

				if (bitmap != null) {
					BitmapCache.INSTANCE.addBitmapToMemoryCache(pi.toString() + "/" + preview, bitmap);
				}
				notifyOnMainThread(bitmap);
			}
		});
	}

	private void notifyOnMainThread(final Bitmap bitmap) {
		GlobalApplication.instance().getHandler().post(new Runnable() {

			@Override
			public void run() {

				if (listener != null)
					listener.onLoadBitmap(bitmap, pi.toString());

				if (iv != null && pi.toString().equals(iv.getTag())) {
					iv.setImageBitmap(bitmap);
				}
			}
		});
	}
}
