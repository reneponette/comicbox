package com.reneponette.comicbox.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;

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
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;
import com.reneponette.comicbox.utils.ZipUtils;

public class DataController {
	FileInfo fileInfo;
	FileMeta fileMeta;
	List<PageInfo> pageInfoList;
	ReadDirection readDirection;
	ReadDirection scanDirection;
	int pagesPerScan;
	boolean autocrop;

	Thread runningThread;
	Handler handler;

	
	///////////////////
	ZipFile zipFile;
	MuPDFCore core;
	///////////////////
	
	

	OnDataBuildListener listener;

	public interface OnDataBuildListener {
//		public FileInfo onPrepareFileInfo();
		
		public void onStartBuild();

		public void onFailBuild(String errStr);

		public void onAddPageInfo(PageInfo pageInfo);

		public void onFinishBuild();
	}

	public void setOnDataBuildListener(OnDataBuildListener l) {
		listener = l;
	}

	public DataController(OnDataBuildListener l) {
		listener = l;
		init();
	}

	public DataController() {
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
	public DataController prepare(Object obj) {
		
		fileInfo = FileInfoDAO.instance().getFileInfo(obj);
		fileMeta = fileInfo.getMeta();
		
		if(fileMeta.type == FileType.ZIP) {
			if(obj instanceof Entry)
				return prepareForZipStreaming();
			else
				return prepareForZip();
		} else if(fileMeta.type == FileType.PDF) {
			return prepareForZip();
		} else if(fileMeta.type == FileType.DIRECTORY) {
			return prepareForFolder();
		}
		return this;
	}
	
	private DataController prepareForZip() {
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

		return this;		
	}
	
	private DataController prepareForFolder() {
		return prepareForZip();
	}
	
	
	private DataController prepareForZipStreaming() {
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
		
		return this;
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

	/**
	 * zip압축 파일용
	 */
	public void build() {
		pageInfoList.clear();
		if (listener != null)
			listener.onStartBuild();

		try {
			if (zipFile != null) {
				zipFile.close();
			}
			zipFile = new ZipFile(fileInfo.getFile());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (zipFile == null) {
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
				
				fillFinalPagesAndNotify();
			}
		});
		runningThread.start();
	}


	
	/**
	 * 압축 되어있지 않은 폴더용
	 */
	public void buildFolder() {
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

				fillFinalPagesAndNotify();
			}
		});
		runningThread.start();
	}

	
	
	/**
	 * PDF 파일용
	 */
	public void buildPdf() {
		pageInfoList.clear();
		if (listener != null)
			listener.onStartBuild();
		
		runningThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (core == null) {
					core = openFile(fileInfo.getPath());
					if (core != null && core.countPages() == 0) {
						core = null;
					}
				}
				if (core == null) {
					if (listener != null)
						listener.onFailBuild(GlobalApplication.instance().getString(R.string.cannot_read_file));
					return;			
				}		
				
				for(int i=0 ; i<core.countPages() ; i++) {
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
				
				fillFinalPagesAndNotify();
			}
		});
		runningThread.start();
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


	
	
	/**
	 * @param api
	 * @param cacheDir
	 */
	public void build(final DropboxAPI<AndroidAuthSession> api, final File cacheDir) {
		pageInfoList.clear();
		if (listener != null)
			listener.onStartBuild();

		runningThread = new Thread(new Runnable() {

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
					zis = new ZipArchiveInputStream(api.getFileStream(getFileInfo().getPath(), null), "utf-8");

					ZipArchiveEntry ze;
					while ((ze = zis.getNextZipEntry()) != null) {


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

						//이미 디스크에 캐쉬 되어있음 건너뜀
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
		});
		runningThread.start();
	}

	private void addPageInfo(PageBuildType buildType, File file, final boolean prepend) {
		PageInfo info = new PageInfo(file);
		fillPageInfoAndNotify(info, PageType.IMG_FILE, buildType, prepend);
	}
	
	private void addPageInfo(PageBuildType buildType, ZipEntry ze, final boolean prepend) {
		PageInfo info = new PageInfo(zipFile, ze);
		fillPageInfoAndNotify(info, PageType.IMG_ZIP, buildType, prepend);
	}
	
	private void addPageInfo(PageBuildType buildType, int pdfIndex, final boolean prepend) {
		PageInfo info = new PageInfo(fileInfo.getName(), core, pdfIndex);
		fillPageInfoAndNotify(info, PageType.IMG_PDF, buildType, prepend);
	}
	
	private void addAdPageInfo(boolean prepend) {
		PageInfo info = new PageInfo();
		fillPageInfoAndNotify(info, PageType.AD, null, prepend);
	}
	
	private void addEndPageInfo(boolean prepend) {
		PageInfo info = new PageInfo();
		fillPageInfoAndNotify(info, PageType.END, null, prepend);
	}
	
	private void fillPageInfoAndNotify(final PageInfo info, PageType pageType, PageBuildType buildType, final boolean prepend) {
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
	
	private void fillFinalPagesAndNotify() {
		// 끝페이지, 광고 페이지 삽입
//		addAdPageInfo(getReadDirection() == ReadDirection.RTL);
		addEndPageInfo(getReadDirection() == ReadDirection.RTL);
		
		handler.post(new Runnable() {

			@Override
			public void run() {

				if (listener != null) {
					listener.onFinishBuild();;
				}
			}
		});		
	}
	
	
	public void stopBuilding() {
		if (runningThread != null) {
			runningThread.interrupt();
			runningThread = null;
		}

		if (zipFile != null) {
			try {
				zipFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
