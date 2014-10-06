package com.reneponette.comicbox.ui.fragment.explorer;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.cache.DropboxThumbBitmapLoader;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.FileLocation;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.ui.MainActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.StringUtils;
import com.reneponette.comicbox.utils.ToastUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class DropboxExplorerFragment extends BaseExplorerFragment {

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String PATH = "path";
	private static final String TAG = "DropboxExplorerFragment";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static DropboxExplorerFragment newInstance(String dropboxPath) {
		DropboxExplorerFragment fragment = new DropboxExplorerFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public DropboxExplorerFragment() {
		//
	}

	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn;
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

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		// Display the proper UI state if logged in or not
		setLoggedIn(mApi.getSession().isLinked());
		if (!mLoggedIn) {
			mApi.getSession().startOAuth2Authentication(getActivity());
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		enumerate();
	}

	@Override
	public void onResume() {
		AndroidAuthSession session = mApi.getSession();
		if (!mLoggedIn && session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				DropBoxManager.INSTANCE.storeAuth(session);
				setLoggedIn(true);
				enumerate();

			} catch (IllegalStateException e) {
				ToastUtils.toast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}
		super.onResume();
	}

	private boolean goParentDirectory() {

		// 상위 폴더 넣기
		if (StringUtils.isBlank(curInfo.getEntry().parentPath()) == false) {
			FileInfo parentInfo;
			Entry parentEntry = new Entry();
			parentEntry.isDir = true;
			parentEntry.path = curInfo.getEntry().parentPath();
			parentInfo = new FileInfo(FileLocation.DROPBOX);
			parentInfo.setEntry(parentEntry);

			FileInfo info = new FileInfo(FileLocation.DROPBOX);
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
				try {

					final Entry entry = mApi.metadata(getCurrentInfo().getPath(), 1000, null, true, null);
					if (!entry.isDir || entry.contents == null) {

						handler.post(new Runnable() {

							@Override
							public void run() {
								Logger.e(DropboxExplorerFragment.this, "File or empty directory");
								hideWaitingDialog();
							}
						});

						return;
					}

					handler.post(new Runnable() {

						@Override
						public void run() {
							curInfo.setEntry(entry);

							String name = curInfo.getName();
							if (StringUtils.isBlank(name))
								name = "/";
							if (getActivity() != null)
								((MainActivity) getActivity()).onSectionAttached(name);

							int indexInParent = 0;
							for (Entry ent : entry.contents) {
								FileInfo info = FileInfoDAO.instance().getFileInfo(ent);
								if (info.getMeta().type != FileType.UNKNOWN) {
									info.indexInParent = indexInParent;
									infoList.add(info);
									indexInParent++;
								}
							}
							adapter.notifyDataSetChanged();
							hideWaitingDialog();
						}
					});

				} catch (DropboxException e) {
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								hideWaitingDialog();
								if (mLoggedIn) {
									DialogHelper.showRetryDialog(getActivity(), new OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											DropboxExplorerFragment.this.enumerate();
										}
									});
								}
							}
						});
					}
					e.printStackTrace();
				}
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

	private void logOut() {
		// Remove credentials from the session
		mApi.getSession().unlink();

		// Clear our stored keys
		DropBoxManager.INSTANCE.clearKeys();
		// Change UI state to display logged out version
		setLoggedIn(false);
	}

	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
			//
		} else {
			//
		}
	}

}
