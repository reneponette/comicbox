package com.reneponette.comicbox.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.ui.fragment.reader.BaseReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.DropboxFolderReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.LocalFolderReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.DropboxPdfReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.LocalPdfReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.LocalZipReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.DropboxZipReaderFragment;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.StringUtils;

public class ReaderActivity extends Activity {

	private static final String FILE_INFO = "file_info";
	private static final String TAG_FRAGMENT = "reader_fragment";


	public static Intent newIntent(Context context, FileInfo info) {
		Intent intent = new Intent();
		intent.setClass(context, ReaderActivity.class);
		intent.putExtra(FILE_INFO, info);

		return intent;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Logger.i(this, "onSaveInstanceState() - " + this);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e(getClass().getName(), "onCreate(), savedInstanceState = " + savedInstanceState + " - " + this);		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);
		
		getActionBar().hide();

		if(savedInstanceState != null)
			return;
		
		FileInfo info = getIntent().getExtras().getParcelable(FILE_INFO);

		Fragment f;
		switch (info.getLocation()) {
		case LOCAL:
			switch (info.getMeta().type) {
			case PDF:
				f = LocalPdfReaderFragment.newInstance(info.getPath());
				break;
			case ZIP:
				f = LocalZipReaderFragment.newInstance(info.getPath());
				break;
			case JPG:
				String parentPath = StringUtils.getParentPath(info.getPath());
				f = LocalFolderReaderFragment.newInstance(parentPath, info.indexInParent);
				break;
			default:
				f = null;
				break;
			}
			break;
		case DROPBOX:
			switch (info.getMeta().type) {
			case PDF:
				f = DropboxPdfReaderFragment.newInstance(info.getPath());
				break;
			case JPG:
				String parentPath = StringUtils.getParentPath(info.getPath());
				f = DropboxFolderReaderFragment.newInstance(parentPath, info.indexInParent);
				break;
			default:
				f = DropboxZipReaderFragment.newInstance(info.getPath());
				break;
			}
			break;
		default:
			f = null;
			break;
		}
		
//		new Thread() {
//		public void run() {
//			AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
//			DropboxAPI<AndroidAuthSession> api = new DropboxAPI<AndroidAuthSession>(session);
//			try {
//				Entry parentEntry = api.metadata(info.getEntry().parentPath(), 10000, null, true, null);
//				FileInfo parentInfo = FileInfoDAO.instance().getFileInfo(parentEntry);
//				
//				
//				startActivity(ReaderActivity.newIntent(MainActivity.this, parentInfo, info.indexInParent));
//			} catch (DropboxException e) {
//				e.printStackTrace();
//			}
//		};
//	}.start();		

		if (f != null) {
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.container, f, TAG_FRAGMENT).commit();
		}
	}
	
	@Override
	protected void onResume() {
		Logger.i(this, "onResume() - " + this);
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		Logger.e(this, "onDestroy() - " + this);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		BaseReaderFragment f = (BaseReaderFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT);
		if (!f.onBackPressed())
			super.onBackPressed();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			boolean useVolumeKey = settings.getBoolean("viewer_use_volume_key", false);
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && useVolumeKey) {
				BaseReaderFragment f = (BaseReaderFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT);
				if (f != null)
					f.moveToRightPage();
				return true;

			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && useVolumeKey) {
				BaseReaderFragment f = (BaseReaderFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT);
				if (f != null)
					f.moveToLeftPage();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
