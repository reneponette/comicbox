package com.reneponette.comicbox.controller;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.os.Handler;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.model.PageInfo.PageType;

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

		if (!(obj instanceof File || obj instanceof Entry))
			throw new RuntimeException("object should be File or Entry");

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

	public void removePageInfo(int position) {
		if (0 > position || pageInfoList.size() <= position)
			return;
		pageInfoList.remove(position);
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

	protected void addPageInfo(PageBuildType buildType, File file, final boolean prepend) {
		PageInfo info = new PageInfo(file);
		info.setBuildType(buildType);
		notify(info, prepend);
	}

	protected void addAdPageInfo(boolean prepend) {
		PageInfo info = new PageInfo(PageType.AD);
		notify(info, prepend);
	}

	protected void addEndPageInfo(boolean prepend) {
		PageInfo info = new PageInfo(PageType.END);
		notify(info, prepend);
	}

	protected void notify(final PageInfo info, final boolean prepend) {

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

	protected void addFinalPagesAndNotify() {
		// 끝페이지, 광고 페이지 삽입
//		addAdPageInfo(getReadDirection() == ReadDirection.RTL);
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
