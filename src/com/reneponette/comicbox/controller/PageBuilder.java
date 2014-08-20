package com.reneponette.comicbox.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.util.Log;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.OutlineActivityData;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.utils.StringUtils;

public class PageBuilder {
	FileInfo fileInfo;
	FileMeta fileMeta;
	List<PageInfo> pageInfoList;
	ReadDirection readDirection;
	ReadDirection scanDirection;
	int pagesPerScan;
	boolean autocrop;

	Thread runningThread;
	Handler handler;

	OnPageBuildListener listener;

	public interface OnPageBuildListener {

		public void onStartBuild();

		public void onFailBuild(String errStr);

		public void onAddPageInfo(PageInfo pageInfo);

		public void onFinishBuild();
	}

	public void setOnDataBuildListener(OnPageBuildListener l) {
		listener = l;
	}

	public PageBuilder(OnPageBuildListener l) {
		listener = l;
		init();
	}

	public PageBuilder() {
		init();
	}

	private void init() {
		pageInfoList = new LinkedList<PageInfo>();
		handler = GlobalApplication.instance().getHandler();
	}

	/*-------------------- methods----------------------*/

	/**
	 * @param obj
	 * @return
	 */
	public PageBuilder prepare(Object obj) {

		fileInfo = FileInfoDAO.instance().getFileInfo(obj);
		fileMeta = fileInfo.getMeta();

		onPrepare();
		return this;
	}

	protected void onPrepare() {
		throw new RuntimeException("should implement in subclass");
	}


	/**
	 * @param viewingPageIndex
	 */
	public void saveReadState(int viewingPageIndex) {
		fileMeta.lastReadPageIndex = viewingPageIndex;
		fileMeta.lastTotalPageCount = pageSize();
		fileMeta.lastReadDirection = readDirection;
		fileMeta.lastPagesPerScan = pagesPerScan;
		FileInfoDAO.instance().insertOrUpdate(fileInfo);
	}

	public List<PageInfo> getPageInfoList() {
		return pageInfoList;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public PageInfo getPageInfo(int position) {
		return pageInfoList.get(position);
	}

	public FileMeta getFileMeta() {
		return fileMeta;
	}

	public int getPagesPerScan() {
		return pagesPerScan;
	}

	public ReadDirection getReadDirection() {
		return readDirection;
	}

	public ReadDirection getScanDirection() {
		return scanDirection;
	}

	public boolean isAutocrop() {
		return autocrop;
	}

	public void setAutocrop(boolean autocrop) {
		this.autocrop = autocrop;
	}

	public int pageSize() {
		return pageInfoList.size();
	}

	protected void onBuild() {
		throw new RuntimeException("should implement in subclass");
	}

	public void build() {
		onBuild();
	}


	/**
	 * @param api
	 * @param cacheDir
	 */
	public void build(final DropboxAPI<AndroidAuthSession> api, final File cacheDir) {

	}

	protected void addPageInfo(PageBuildType buildType, File file, final boolean prepend) {
		PageInfo info = new PageInfo(file);
		fillPageInfoAndNotify(info, PageType.IMG_FILE, buildType, prepend);
	}

	protected void addAdPageInfo(boolean prepend) {
		PageInfo info = new PageInfo();
		fillPageInfoAndNotify(info, PageType.AD, null, prepend);
	}

	protected void addEndPageInfo(boolean prepend) {
		PageInfo info = new PageInfo();
		fillPageInfoAndNotify(info, PageType.END, null, prepend);
	}

	protected void fillPageInfoAndNotify(final PageInfo info, PageType pageType, PageBuildType buildType,
			final boolean prepend) {
		info.setType(pageType);
		info.setBuildType(buildType);

		handler.post(new Runnable() {

			@Override
			public void run() {
				if (prepend) {
					((LinkedList<PageInfo>) pageInfoList).addFirst(info);
				} else
					pageInfoList.add(info);

				if (listener != null) {
					listener.onAddPageInfo(info);
				}
			}
		});
	}

	protected void fillFinalPagesAndNotify() {
		// 끝페이지, 광고 페이지 삽입
		// addAdPageInfo(getReadDirection() == ReadDirection.RTL);
		addEndPageInfo(getReadDirection() == ReadDirection.RTL);

		handler.post(new Runnable() {

			@Override
			public void run() {

				if (listener != null) {
					listener.onFinishBuild();
					;
				}
			}
		});
	}

	protected void onStop() {
		throw new RuntimeException("should implement in subclass");
	}

	public void stop() {
		if (runningThread != null) {
			runningThread.interrupt();
			runningThread = null;
		}
		onStop();
	}

}
