package com.reneponette.comicbox.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;
import com.reneponette.comicbox.utils.ZipUtils;

public class LocalZipPageBuilder extends PageBuilder {
	
	ZipFile zipFile;	
	
	@Override
	protected void onPrepare() {
		File file = new File(fileInfo.getPath());
		
		// 처음 파일을 보는 경우 자동으로 결정
		if (fileMeta.pagesPerScan == 0) {
			fileMeta.pagesPerScan = ImageUtils.pagesPerScan(file);
		}
		pagesPerScan = fileMeta.pagesPerScan;

		// 읽기 방향 결정
		ReadDirection computedDirection = fileMeta.readDirection;
		if (computedDirection == ReadDirection.NOTSET) {
			// 읽는 방향이 설정되어있지 않음 폴더 설정을 따름
			FileInfo parentInfo = FileInfoDAO.instance().getFileInfo(file.getParentFile());
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

		try {
			if(zipFile != null)
				zipFile.close();
			zipFile = new ZipFile(new File(fileInfo.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
			if (listener != null)
				listener.onFailBuild(GlobalApplication.instance().getString(R.string.cannot_read_file));
			return;			
		}

		runningThread = new Thread(new Runnable() {

			@Override
			public void run() {

				// 가끔 파일명 순서대로 튀어나오지 않아서 파일명으로 엔트리 정렬
				List<ZipEntry> entries = ZipUtils.enumerateAndSortZipEntries(zipFile, 0);

				for (ZipEntry ze : entries) {
					String name = ze.getName();
					if (name.contains("__MACOSX"))
						continue;
					if (StringUtils.isImageFileExt(name)) {

						if (getPagesPerScan() == 2) {
							if (getScanDirection() == ReadDirection.RTL) {
								addPageInfo(PageBuildType.RIGHT, ze, true);
								addPageInfo(PageBuildType.LEFT, ze, true);
							} else {
								addPageInfo(PageBuildType.LEFT, ze, false);
								addPageInfo(PageBuildType.RIGHT, ze, false);
							}
						} else {
							if (getReadDirection() == ReadDirection.RTL)
								addPageInfo(PageBuildType.WHOLE, ze, true);
							else
								addPageInfo(PageBuildType.WHOLE, ze, false);
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
		if (zipFile != null) {
			try {
				zipFile.close();
				zipFile = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
	
	private void addPageInfo(PageBuildType buildType, ZipEntry ze, final boolean prepend) {
		PageInfo info = new PageInfo(zipFile, ze);
		info.setBuildType(buildType);
		notify(info, prepend);
	}
	
}
