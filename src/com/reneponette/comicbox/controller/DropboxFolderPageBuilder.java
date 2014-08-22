package com.reneponette.comicbox.controller;

import java.io.File;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxFolderPageBuilder extends PageBuilder {
	
	/*---------------------------------------------------------------------------*/
	DropboxAPI<AndroidAuthSession> api;
	File cacheDir;
	/*---------------------------------------------------------------------------*/	
	
	@Override
	protected void onPrepare() {
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
		readDirection = ReadDirection.LTR;
		scanDirection = computedDirection;
		
		//
		cacheDir = new File(GlobalApplication.instance().getCacheDir(), "comics/"
				+ StringUtils.getMD5(getFileInfo().getName()));
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		removeOtherCacheDir();

		// dropbox
		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		api = new DropboxAPI<AndroidAuthSession>(session);		
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
						if(entry.path.endsWith(".jpg") || entry.path.endsWith(".JPG"))
							addPageInfo(PageBuildType.WHOLE, entry.path, false);
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
	
	private void removeOtherCacheDir() {
		for (File f : cacheDir.getParentFile().listFiles()) {
			if (f.isHidden())
				continue;
			if (f.isFile())
				continue;
			if (f.getName().equals(cacheDir.getName()))
				continue;
			boolean success = f.delete();
			if (!success) {
				for (File imageFile : f.listFiles()) {
					if (imageFile.isDirectory())
						continue;
					success = imageFile.delete();
				}
			}
		}
	}
	
	private void addPageInfo(PageBuildType buildType, String dropboxPath, final boolean prepend) {
		PageInfo info = new PageInfo(dropboxPath);
		info.setBuildType(buildType);
		notify(info, prepend);
	}	
	
}
