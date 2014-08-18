package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.reneponette.comicbox.cache.PageBitmapLoader;
import com.reneponette.comicbox.controller.DataController.OnDataBuildListener;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class DropboxFolderReaderFragment extends BasePagerReaderFragment implements OnDataBuildListener {
	
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

	private File curFile;
	private int startIndex;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		curFile = new File(getArguments().getString(PATH));
		startIndex = getArguments().getInt(START_INDEX, -1);		
		dataController.setOnDataBuildListener(this);
		dataController.prepare(curFile);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dataController.buildFolder();
	}

	/*---------------------------------------------------------------------------*/

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
		if(startIndex > -1 ) {
			if(dataController.getReadDirection() == ReadDirection.RTL)
				startPageIndex = dataController.pageSize() - 1 - startIndex;
			else
				startPageIndex = startIndex;
			viewPager.setCurrentItem(startPageIndex, false);
			seekBar.setMax(dataController.pageSize());
			seekBar.setProgress(startPageIndex);
			updateSeekBarLabel();
		} else {
			initUI();
		}
		
		hideWaitingDialog();
	}

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), isAutocrop(), false);
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;
	}

}