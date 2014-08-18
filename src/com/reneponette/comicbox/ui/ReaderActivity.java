package com.reneponette.comicbox.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.ui.fragment.reader.BaseReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.DropboxFolderReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.FolderReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.PdfReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.PdfOldReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.ZipFileReaderFragment;
import com.reneponette.comicbox.ui.fragment.reader.ZipStreamReaderFragment;

public class ReaderActivity extends Activity {

	private static final String FILE_INFO = "file_info";
	private static final String INDEX_IN_PARENT = "index_in_parent";
	private static final String TAG_FRAGMENT = "reader_fragment";

	public static Intent newIntent(Context context, FileInfo info, int indexInParent) {
		Intent intent = new Intent();
		intent.setClass(context, ReaderActivity.class);
		intent.putExtra(FILE_INFO, info);
		intent.putExtra(INDEX_IN_PARENT, indexInParent);

		return intent;
	}

	public static Intent newIntent(Context context, FileInfo info) {
		Intent intent = new Intent();
		intent.setClass(context, ReaderActivity.class);
		intent.putExtra(FILE_INFO, info);

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);

		getActionBar().hide();

		FileInfo info = getIntent().getExtras().getParcelable(FILE_INFO);

		Fragment f;
		switch (info.getLocation()) {
		case LOCAL:
			switch (info.getMeta().type) {
			case PDF:
				f = PdfReaderFragment.newInstance(info.getPath());
				break;
			case ZIP:
				f = ZipFileReaderFragment.newInstance(info.getPath());
				break;
			default:
				f = FolderReaderFragment.newInstance(info.getPath(), getIntent().getExtras()
						.getInt(INDEX_IN_PARENT, -1));
				break;
			}
			break;
		case DROPBOX:
			switch (info.getMeta().type) {
			case PDF:
				f = PdfOldReaderFragment.newInstance(info.getPath());
				break;
			case JPG:
				f = DropboxFolderReaderFragment.newInstance(info.getPath(), 0);
				break;
			default:
				f = ZipStreamReaderFragment.newInstance(info.getPath());
				break;
			}
			break;
		default:
			f = null;
			break;
		}

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.container, f, TAG_FRAGMENT).commit();
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
