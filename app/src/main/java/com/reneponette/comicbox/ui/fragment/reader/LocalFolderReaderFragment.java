package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.reneponette.comicbox.cache.PageBitmapLoader;
import com.reneponette.comicbox.controller.LocalFolderPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class LocalFolderReaderFragment extends BasePagerReaderFragment {

	private static final String START_INDEX = "start_index";

	public static LocalFolderReaderFragment newInstance(String path, int startIndex) {
		LocalFolderReaderFragment fragment = new LocalFolderReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, path);
		args.putInt(START_INDEX, startIndex);
		fragment.setArguments(args);
		return fragment;
	}

	public LocalFolderReaderFragment() {
	}

	private File curFile;
	private int startIndex;
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(PATH, curFile.getAbsolutePath());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			curFile = new File(getArguments().getString(PATH));
			startIndex = getArguments().getInt(START_INDEX, -1);
		} else {
			curFile = new File(savedInstanceState.getString(PATH));			
		}
		
		pageBuilder.prepare(curFile);
	}


	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {
		PageBuilder builder = new LocalFolderPageBuilder(); 
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
		PageInfo pi = pageBuilder.getPageInfo(position);
		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), isAutocrop(), false);
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;
	}

}