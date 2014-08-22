package com.reneponette.comicbox.controller;

import java.io.File;

import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;

public class LocalFolderPageBuilder extends PageBuilder {

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

		final File folder = fileInfo.getFile();

		runningThread = new Thread(new Runnable() {

			@Override
			public void run() {

				for (File f : folder.listFiles()) {
					String name = f.getName();
					if (name.contains("__MACOSX"))
						continue;
					if (StringUtils.isImageFileExt(name)) {

						if (getPagesPerScan() == 2) {
							if (getScanDirection() == ReadDirection.RTL) {
								addPageInfo(PageBuildType.RIGHT, f, true);
								addPageInfo(PageBuildType.LEFT, f, true);
							} else {
								addPageInfo(PageBuildType.LEFT, f, false);
								addPageInfo(PageBuildType.RIGHT, f, false);
							}
						} else {
							if (getReadDirection() == ReadDirection.RTL)
								addPageInfo(PageBuildType.WHOLE, f, true);
							else
								addPageInfo(PageBuildType.WHOLE, f, false);
						}
					}
				}

				addFinalPagesAndNotify();
			}
		});
		runningThread.start();		
	}
	
	@Override
	protected void onStop() {
		//
	}
}
