package com.reneponette.comicbox.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxZipPageBuilder extends PageBuilder {

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
				Set<String> nameSet = new HashSet<String>();
				for (File imageFile : cacheDir.listFiles()) {
					if (imageFile.isHidden())
						continue;
					if (imageFile.isDirectory())
						continue;

					Options opts = new Options();
					opts.inSampleSize = 8;
					Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
					addPageWithBitmap(bitmap, imageFile);
					nameSet.add(imageFile.getName());
				}

				ZipArchiveInputStream zis;
				try {

					ZipArchiveEntry ze;
					zis = new ZipArchiveInputStream(api.getFileStream(getFileInfo().getPath(), null), "utf-8");
					while ((ze = zis.getNextZipEntry()) != null) {
						Log.e(this.getClass().getName(), "entry name = " + ze.getName());

						if (isInterrupted()) {
							Log.e(this.getClass().getName(), "interrupted");
							zis.close();
							return;
						}

						// 파일 엔트리를 하나 읽음
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int count;
						while ((count = zis.read(buffer)) != -1) {
							baos.write(buffer, 0, count);
						}

						// 일단 바이트 배열로 변환
						String filename = ze.getName();
						byte[] bytes = baos.toByteArray();
						baos.close();

						// 이미 디스크에 캐쉬 되어있음 건너뜀
						if (nameSet.contains(filename))
							continue;

						if (filename.contains("__MACOSX")) {
							continue;
						}

						if (StringUtils.isImageFileExt(filename)) {
							// 이제 파일을 열고 바이트 배열을 타겟에 씀
							File cachedFile = new File(cacheDir, ze.getName());
							FileOutputStream fos = new FileOutputStream(cachedFile);
							fos.write(bytes);
							fos.close();

							Options opts = new Options();
							opts.inSampleSize = 8;
							Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

							addPageWithBitmap(bitmap, cachedFile);
						}
					}

				} catch (DropboxException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			private void addPageWithBitmap(Bitmap bitmap, File cachedFile) {
				if (getFileInfo().getMeta().pagesPerScan == 2) {
					if (bitmap.getWidth() < bitmap.getHeight()) {
						if (getReadDirection() == ReadDirection.RTL)
							addPageInfo(PageBuildType.WHOLE, cachedFile, false);
						else
							addPageInfo(PageBuildType.WHOLE, cachedFile, false);
					} else if (getScanDirection() == ReadDirection.RTL) {
						addPageInfo(PageBuildType.RIGHT, cachedFile, false);
						addPageInfo(PageBuildType.LEFT, cachedFile, false);
					} else {
						addPageInfo(PageBuildType.LEFT, cachedFile, false);
						addPageInfo(PageBuildType.RIGHT, cachedFile, false);
					}
				} else {
					if (getReadDirection() == ReadDirection.RTL)
						addPageInfo(PageBuildType.WHOLE, cachedFile, false);
					else
						addPageInfo(PageBuildType.WHOLE, cachedFile, false);
				}

			}
		};

		runningThread.start();
	}

	@Override
	protected void onStop() {
		//
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

}
