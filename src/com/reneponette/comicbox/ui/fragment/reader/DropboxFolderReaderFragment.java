package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.controller.DropboxFolderPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.StringUtils;

public class DropboxFolderReaderFragment extends BasePagerReaderFragment {

	private static final String START_INDEX = "start_index";

	public static DropboxFolderReaderFragment newInstance(String path, int startIndex) {
		DropboxFolderReaderFragment fragment = new DropboxFolderReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, path);
		args.putInt(START_INDEX, startIndex);
		fragment.setArguments(args);
		return fragment;
	}

	public DropboxFolderReaderFragment() {
	}

	private int startIndex;

	/*---------------------------------------------------------------------------*/
	DropboxAPI<AndroidAuthSession> api;
	File cacheDir;

	/*---------------------------------------------------------------------------*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startIndex = getArguments().getInt(START_INDEX, -1);

		Entry entry = new Entry();
		entry.path = getArguments().getString(PATH);
		pageBuilder.prepare(entry);
		Logger.d(this, "entry.path = " + entry.path);

		cacheDir = new File(GlobalApplication.instance().getCacheDir(), "comics/"
				+ StringUtils.getMD5(pageBuilder.getFileInfo().getName()));
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		removeOtherCacheDir();
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

	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {

		// dropbox
		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		api = new DropboxAPI<AndroidAuthSession>(session);

		PageBuilder builder = new DropboxFolderPageBuilder(api);
		builder.setOnDataBuildListener(new OnPageBuildListener() {

			@Override
			public void onStartBuild() {
				showWaitingDialog();
				viewPager.setAdapter(null);
			}

			@Override
			public void onFailBuild(String errStr) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onAddPageInfo(PageInfo pageInfo) {

			}

			@Override
			public void onFinishBuild() {
				viewPager.setAdapter(pagerAdapter);

				int startPageIndex;
				if (startIndex > -1) {
					if (pageBuilder.getReadDirection() == ReadDirection.RTL)
						startPageIndex = pageBuilder.pageSize() - 1 - startIndex;
					else
						startPageIndex = startIndex;
					viewPager.setCurrentItem(startPageIndex, false);
					seekBar.setMax(pageBuilder.pageSize());
					seekBar.setProgress(startPageIndex);
					updateSeekBarLabel();
				} else {
					initUI();
				}

				hideWaitingDialog();
			}
		});
		return builder;
	}

	/*---------------------------------------------------------------------------*/

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		loadPageBitmap(iv, position, false);
		// return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(),
		// isAutocrop(), false);
		return null;
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		loadPageBitmap(iv, position, true);
		// PageInfo pi = pageBuilder.getPageInfo(position);
		// new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;
	}

	private void loadPageBitmap(final ImageView iv, int position, final boolean preview) {
		final PageInfo pi = pageBuilder.getPageInfo(position);
		final String filename = pi.getRemotePath().substring(pi.getRemotePath().lastIndexOf('/') + 1);
		final File cachedFile = new File(cacheDir, filename);

		iv.setTag(filename);

		new Thread() {
			public void run() {
				final Bitmap bitmap = ImageUtils.getBitmap(api, pi.getRemotePath(), cachedFile, pi.getBuildType(), isAutocrop(), preview);

				if (getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							if (filename.equals(iv.getTag()))
								iv.setImageBitmap(bitmap);
						}
					});
				}
			};
		}.start();
	}

}