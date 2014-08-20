package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.artifex.mupdfdemo.PageView;
import com.reneponette.comicbox.controller.LocalZipPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.MessageUtils;

public class ZipFileReaderFragment extends BasePagerReaderFragment {

	public static ZipFileReaderFragment newInstance(String folderPath) {
		ZipFileReaderFragment fragment = new ZipFileReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, folderPath);
		fragment.setArguments(args);
		return fragment;
	}

	public ZipFileReaderFragment() {
	}

	private File curFile;

	/*---------------------------------------------------------------------------*/

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(PATH, curFile.getAbsolutePath());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(savedInstanceState == null) {
			curFile = new File(getArguments().getString(PATH));
		} else {
			curFile = new File(savedInstanceState.getString(PATH));
		}
		
		pageBuilder.prepare(curFile);		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	
	/*---------------------------------------------------------------------------*/
	
	@Override
	protected PageBuilder onCreatePageBuilder() {
		PageBuilder builder = new LocalZipPageBuilder(); 
		builder.setOnDataBuildListener(new OnPageBuildListener() {
			
			@Override
			public void onStartBuild() {
				showWaitingDialog();
				viewPager.setAdapter(null);
			}

			@Override
			public void onFailBuild(String errStr) {
				hideWaitingDialog();
				MessageUtils.toast(getActivity(), errStr);
				getActivity().finish();
			}

			@Override
			public void onAddPageInfo(PageInfo pageInfo) {
				//
			}

			@Override
			public void onFinishBuild() {
				viewPager.setAdapter(pagerAdapter);

				initUI();

				hideWaitingDialog();
			}
		});
		return builder;
	}
	

	/*---------------------------------------------------------------------------*/


	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
		return ImageUtils.getBitmap(pi.getZipFile(), pi.getZipEntry(), pi.getBuildType(), isAutocrop(),
				false);
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
//		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
//		return null;
		
		if(pi.getZipFile() == null)
			return null;
		
		return ImageUtils.getBitmap(pi.getZipFile(), pi.getZipEntry(), pi.getBuildType(), false, true);
	}

}