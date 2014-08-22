package com.reneponette.comicbox.controller;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.OutlineActivityData;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.utils.ImageUtils;

public class LocalPdfPageBuilder extends PageBuilder {
	
	MuPDFCore core;	
	
	@Override
	protected void onPrepare() {
		// 처음 파일을 보는 경우 자동으로 결정
		if (fileMeta.pagesPerScan == 0) {
			fileMeta.pagesPerScan = ImageUtils.pagesPerScan(fileInfo.getFile());
		}
		pagesPerScan = fileMeta.pagesPerScan;

		// 읽기 방향 결정
		ReadDirection computedDirection = fileMeta.readDirection;
		if (computedDirection == ReadDirection.NOTSET) {
			// 읽는 방향이 설정되어있지 않음 폴더 설정을 따름
			FileInfo parentInfo = FileInfoDAO.instance().getFileInfo(fileInfo.getFile().getParentFile());
			computedDirection = parentInfo.getMeta().readDirection;
		}
		if (computedDirection == ReadDirection.NOTSET) {
			// 아직까지도 미정이면 왼쪽에서 오른쪽이 디폴트
			computedDirection = ReadDirection.LTR;
		}
		readDirection = computedDirection;
		scanDirection = computedDirection;
	}
	
	
	@Override
	protected void onBuild() {
		pageInfoList.clear();
		if (listener != null)
			listener.onStartBuild();

		runningThread = new Thread(new Runnable() {

			@Override
			public void run() {

				core = openFile(fileInfo.getPath());
				if (core != null && core.countPages() == 0) {
					core = null;
					return;
				}

				if (core == null) {
					if (listener != null)
						listener.onFailBuild(GlobalApplication.instance().getString(R.string.cannot_read_file));
					return;
				}

				for (int i = 0; i < core.countPages(); i++) {
					if (getPagesPerScan() == 2) {
						if (getScanDirection() == ReadDirection.RTL) {
							addPageInfo(PageBuildType.RIGHT, i, true);
							addPageInfo(PageBuildType.LEFT, i, true);
						} else {
							addPageInfo(PageBuildType.LEFT, i, false);
							addPageInfo(PageBuildType.RIGHT, i, false);
						}
					} else {
						if (getReadDirection() == ReadDirection.RTL)
							addPageInfo(PageBuildType.WHOLE, i, true);
						else
							addPageInfo(PageBuildType.WHOLE, i, false);
					}
				}

				addFinalPagesAndNotify();
			}
		});
		runningThread.start();
	}
	
	@Override
	protected void onStop() {
		core = null;
	}
	
	private MuPDFCore openFile(String path) {
		System.out.println("Trying to open " + path);
		try {
			core = new MuPDFCore(GlobalApplication.instance(), path);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
		return core;
	}
	
	private void addPageInfo(PageBuildType buildType, int pdfIndex, final boolean prepend) {
		PageInfo info = new PageInfo(fileInfo.getName(), core, pdfIndex);
		info.setBuildType(buildType);
		notify(info, prepend);
	}
}
