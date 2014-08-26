package com.reneponette.comicbox.ui.fragment.explorer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.cache.DropboxThumbBitmapLoader;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.utils.StringUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class FtpExplorerFragment extends BaseExplorerFragment {

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String PATH = "path";
	private static final String TAG = "DropboxViewFragment";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static FtpExplorerFragment newInstance(String dropboxPath) {
		FtpExplorerFragment fragment = new FtpExplorerFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public FtpExplorerFragment() {
		//
	}

	DropboxAPI<AndroidAuthSession> mApi;

	private Thread runningThread;

	Handler handler;

	@Override
	protected FileInfo onGetFileInfo() {
		String dropboxPath = getArguments().getString(PATH);
		Entry entry = new Entry();
		entry.path = dropboxPath;
		return FileInfoDAO.instance().getFileInfo(entry);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		handler = GlobalApplication.instance().getHandler();


	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		enumerate();
	}

	@Override
	public void onResume() {
		super.onResume();
		enumerate();
	}

	private boolean goParentDirectory() {

		// 상위 폴더 넣기
		if (StringUtils.isBlank(curInfo.getEntry().parentPath()) == false) {
			FileInfo parentInfo;
			Entry parentEntry = new Entry();
			parentEntry.isDir = true;
			parentEntry.path = curInfo.getEntry().parentPath();
			parentInfo = new FileInfo(LocationType.DROPBOX);
			parentInfo.setEntry(parentEntry);

			FileInfo info = new FileInfo(LocationType.DROPBOX);
			info.setEntry(parentEntry);
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onEntryClicked(info);
				;
			}
			return true;
		}

		return false;
	}

	private void enumerate() {

		if (runningThread != null)
			runningThread.interrupt();

		infoList.clear();
		adapter.notifyDataSetChanged();
		showWaitingDialog();

		runningThread = new Thread() {
			@Override
			public void run() {
				
				// ftp 콜
				
				if(isInterrupted())
					return;
				
				
				//...
				
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						adapter.notifyDataSetChanged();
						hideWaitingDialog();
					}
				});
				
				
				runningThread = null;
			}
		};
		runningThread.start();

	}

	@Override
	public boolean onBackPressed() {
		if (C.LOCAL_ROOT_PATH.equals(curInfo.getPath())) {
			return false;
		}
		return goParentDirectory();
	}

	@Override
	protected Bitmap getThumbnailBitmap(FileInfo info, ImageView thumbnailIv) {
		new DropboxThumbBitmapLoader(info, mApi, thumbnailIv).run();
		return null;
	}




}
