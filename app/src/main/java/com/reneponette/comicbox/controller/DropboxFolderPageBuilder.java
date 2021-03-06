package com.reneponette.comicbox.controller;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxFolderPageBuilder extends PageBuilder {
	
	/*---------------------------------------------------------------------------*/
	DropboxAPI<AndroidAuthSession> api;
//	File cacheDir;
	/*---------------------------------------------------------------------------*/
	
	public DropboxFolderPageBuilder(DropboxAPI<AndroidAuthSession> api) {
		this.api = api;
	}
	
	
	@Override
	protected void onPrepare() {
		
		Logger.d(this, "fileMeta.pagesPerScan = " + fileMeta.pagesPerScan);
		// 처음 파일을 보는 경우 자동으로 결정
		if (fileMeta.pagesPerScan == 0) {
			fileMeta.pagesPerScan = 1;
		}
		pagesPerScan = fileMeta.pagesPerScan;

		// 읽기 방향 결정
		ReadDirection computedDirection = fileMeta.readDirection;
		if (computedDirection == ReadDirection.NOTSET) {
			// 읽는 방향이 설정되어있지 않음 폴더 설정을 따름
			Entry parentEntry = new Entry();
			parentEntry.path = StringUtils.getParentPath(fileInfo.getPath());
			FileInfo parentInfo = FileInfoDAO.instance().getFileInfo(parentEntry);
			computedDirection = parentInfo.getMeta().readDirection;
		}
		if (computedDirection == ReadDirection.NOTSET) {
			// 아직까지도 미정이면 왼쪽에서 오른쪽이 디폴트
			computedDirection = ReadDirection.LTR;
		}
		// 그리고 무조건 스트리밍은 왼->오 로 고정!
		readDirection = computedDirection;
		scanDirection = computedDirection;
	}

	@Override
	protected void onBuild() {
		pageInfoList.clear();
		if (listener != null)
			listener.onStartBuild();

		runningThread = new Thread() {

			@Override
			public void run() {
				
				try {
					Entry resultEntry = api.metadata(getFileInfo().getPath(), 1000, null, true, null);
					
					for(Entry entry : resultEntry.contents) {
						if(entry.path.endsWith(".jpg") || entry.path.endsWith(".JPG")) {
							
							if (getFileInfo().getMeta().pagesPerScan == 2) {
 
								if (getScanDirection() == ReadDirection.RTL) {
									addPageInfo(PageBuildType.RIGHT, entry.path, true);
									addPageInfo(PageBuildType.LEFT, entry.path, true);
								} else {
									addPageInfo(PageBuildType.LEFT, entry.path, false);
									addPageInfo(PageBuildType.RIGHT, entry.path, false);
								}
							} else {
								if (getReadDirection() == ReadDirection.RTL)
									addPageInfo(PageBuildType.WHOLE, entry.path, true);
								else
									addPageInfo(PageBuildType.WHOLE, entry.path, false);
							}
						}
					}
					
				} catch (DropboxException e) {
					e.printStackTrace();
				}
				
				addFinalPagesAndNotify();
			}
		};
		runningThread.start();	
	}

	@Override
	protected void onStop() {
		
	}
	
	
	private void addPageInfo(PageBuildType buildType, String dropboxPath, final boolean prepend) {
		PageInfo info = new PageInfo(dropboxPath);
		info.setBuildType(buildType);
		notify(info, prepend);
	}
	
}
