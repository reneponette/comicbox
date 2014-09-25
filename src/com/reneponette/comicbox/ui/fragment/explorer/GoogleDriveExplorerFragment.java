package com.reneponette.comicbox.ui.fragment.explorer;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI.Entry;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.utils.StringUtils;
import com.reneponette.comicbox.utils.ToastUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class GoogleDriveExplorerFragment extends BaseExplorerFragment implements ConnectionCallbacks,
		OnConnectionFailedListener {

	private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String PATH = "path";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static GoogleDriveExplorerFragment newInstance(String path) {
		GoogleDriveExplorerFragment fragment = new GoogleDriveExplorerFragment();
		Bundle args = new Bundle();
		args.putString(PATH, path);
		fragment.setArguments(args);
		return fragment;
	}

	public GoogleDriveExplorerFragment() {
		//
	}

	private Thread runningThread;
	Handler handler;

	GoogleApiClient mGoogleApiClient;

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

		mGoogleApiClient = new GoogleApiClient.Builder(getActivity()).addApi(Drive.API).addScope(Drive.SCOPE_FILE)
				.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
		mGoogleApiClient.connect();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		enumerate();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private boolean goParentDirectory() {

		// 상위 폴더 넣기
		if (StringUtils.isBlank(curInfo.getEntry().parentPath()) == false) {
			FileInfo parentInfo;
			Entry parentEntry = new Entry();
			parentEntry.isDir = true;
			parentEntry.path = curInfo.getEntry().parentPath();
			parentInfo = new FileInfo(LocationType.GOOGLE);
			parentInfo.setEntry(parentEntry);

			FileInfo info = new FileInfo(LocationType.GOOGLE);
			info.setEntry(parentEntry);
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onEntryClicked(info);
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
				
				if(interrupted())
					return;

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
		// new DropboxThumbBitmapLoader(info, mApi, thumbnailIv).run();
		return null;
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		ToastUtils.toast("onConnected. bundle = " + connectionHint.toString());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		ToastUtils.toast("onConnectionSuspended. cause = " + cause);

	}


	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		ToastUtils.toast("onConnectionFailed. connectionResult = " + connectionResult);
		if (connectionResult.hasResolution()) {
			try {
				connectionResult.startResolutionForResult(getActivity(), RESOLVE_CONNECTION_REQUEST_CODE);
			} catch (IntentSender.SendIntentException e) {
				// Unable to resolve, message user appropriately
			}
		} else {
			GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();
		}

	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		ToastUtils.toast("onActivityResult");
		switch (requestCode) {
		case RESOLVE_CONNECTION_REQUEST_CODE:
			if (resultCode == Activity.RESULT_OK) {
				mGoogleApiClient.connect();
			}
			break;
		}
	}

}
