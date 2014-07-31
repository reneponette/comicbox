package com.reneponette.comicbox.ui;

import java.io.File;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.cache.DropboxComicsDownloader;
import com.reneponette.comicbox.cache.DropboxComicsDownloader.OnLoadComicsListener;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.ui.fragment.NavigationDrawerFragment;
import com.reneponette.comicbox.ui.fragment.explorer.BaseExplorerFragment.FolderViewFragmentListener;
import com.reneponette.comicbox.ui.fragment.explorer.DropboxExplorerFragment;
import com.reneponette.comicbox.ui.fragment.explorer.LocalExplorerFragment;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.MessageUtils;

public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
		FolderViewFragmentListener {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */

	private CharSequence mTitle;
	private File curDir;
	private Entry curEntry;

	private boolean closeFlag = false;
	private Handler closeHandler = new Handler() {
		public void handleMessage(Message msg) {
			closeFlag = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		curDir = getStartLocalDirectory();
		curEntry = getStartDropboxDirectory();

		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(
				R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
	}

	@Override
	protected void onStop() {
		super.onStop();
		closeHandler.removeMessages(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		saveLastDirectory();
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {

		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		if (position == 0) {
			fragmentManager
					.beginTransaction()
					.replace(R.id.container,
							LocalExplorerFragment.newInstance(FileInfoDAO.instance().getFileInfo(curDir)),
							curDir.getAbsolutePath()).commit();
		} else {
			fragmentManager.beginTransaction()
					.replace(R.id.container, DropboxExplorerFragment.newInstance(curEntry.path), curEntry.path)
					.commit();
		}

	}

	public void onSectionAttached(String name) {
		mTitle = name;
		restoreActionBar();
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_settings) {
			Intent intent = new Intent();
			intent.setClass(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (closeFlag == false) {
			// 안내 메세지를 토스트로 출력한다.
			MessageUtils.toast(this, getString(R.string.press_back_key_again));

			// 상태값 변경
			closeFlag = true;
			closeHandler.sendEmptyMessageDelayed(0, 3000);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onFileClicked(FileInfo info) {

		if (info.getMeta().type == FileType.DIRECTORY) {
			curDir = info.getFile();
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.container, LocalExplorerFragment.newInstance(info))
					.commit();
		}

		if (info.getMeta().type == FileType.ZIP) {
			startActivity(ReaderActivity.newIntent(this, info));
		}

		if (info.getMeta().type == FileType.PDF) {
			startActivity(ReaderActivity.newIntent(this, info));
		}

		if (info.getMeta().type == FileType.JPG) {
			FileInfo parentInfo = FileInfoDAO.instance().getFileInfo(info.getFile().getParentFile());
			startActivity(ReaderActivity.newIntent(this, parentInfo, info.indexInParent));
		}
	}

	@Override
	public void onEntryClicked(final FileInfo info) {
		Log.e("rene", "onEntryClicked = " + info.getPath());

		if (info.getMeta().type == FileType.DIRECTORY) {
			curEntry = info.getEntry();
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.container, DropboxExplorerFragment.newInstance(info.getPath())).commit();
		} else {

			ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();

			if (wifi == State.CONNECTED) {
				if (info.getMeta().type == FileType.ZIP) {

					DialogHelper.showSelectDownloadOrStreaming(MainActivity.this, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							startActivity(ReaderActivity.newIntent(MainActivity.this, info));
						}
					}, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							downloadAndShow(info);
						}
					});
				} else if (info.getMeta().type == FileType.PDF) {
					downloadAndShow(info);
				}
			} else {
				DialogHelper.showDataDownloadWarningDialog(this, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						if (info.getMeta().type == FileType.ZIP) {

							DialogHelper.showSelectDownloadOrStreaming(MainActivity.this, new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									startActivity(ReaderActivity.newIntent(MainActivity.this, info));
								}
							}, new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									downloadAndShow(info);
								}
							});
						} else if (info.getMeta().type == FileType.PDF) {
							downloadAndShow(info);
						}

					}
				});
			}
		}

	}

	private void downloadAndShow(final FileInfo info) {
		final ProgressDialog dialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage("다운로드 중...");
		dialog.setCanceledOnTouchOutside(false);

		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		DropboxAPI<AndroidAuthSession> api = new DropboxAPI<AndroidAuthSession>(session);

		final DropboxComicsDownloader downloader = new DropboxComicsDownloader(info, api, new OnLoadComicsListener() {

			@Override
			public void onProgress(long bytes, long total) {
				dialog.setMax((int) total);
				dialog.setProgress((int) bytes);
			}

			@Override
			public void onLoadComics(File comics) {
				dialog.dismiss();
				info.setFile(comics);
				startActivity(ReaderActivity.newIntent(MainActivity.this, info));
			}
		});

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d("asdf", "canceled");
				downloader.stop();
			}
		});

		dialog.show();
		downloader.run();

		dialog.setMax(0);
		dialog.setProgress(0);
	}

	public boolean isDrawerOpen() {
		return mNavigationDrawerFragment.isDrawerOpen();
	}

	private File getStartLocalDirectory() {
		File startDir = null;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String lastPath = pref.getString(C.LAST_LOCAL_PATH, null);
		if (lastPath != null) {
			startDir = new File(lastPath);
			if (!startDir.isDirectory() || !startDir.exists())
				startDir = null;
		}

		if (startDir == null) {
			startDir = new File(C.DEFAULT_LOCAL_PATH);
			if (startDir.exists() == false) {
				startDir.mkdir();
			}
		}
		return startDir;
	}

	private Entry getStartDropboxDirectory() {
		Entry startEntry = new Entry();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String lastPath = pref.getString(C.LAST_DROPBOX_PATH, null);
		if (lastPath != null)
			startEntry.path = lastPath;
		else
			startEntry.path = C.DEFAULT_DROPBOX_PATH;

		return startEntry;
	}

	private void saveLastDirectory() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		pref.edit().putString(C.LAST_LOCAL_PATH, curDir.getAbsolutePath())
				.putString(C.LAST_DROPBOX_PATH, curEntry.path).commit();
	}
}
