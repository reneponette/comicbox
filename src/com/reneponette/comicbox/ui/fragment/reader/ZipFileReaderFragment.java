package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.reneponette.comicbox.cache.PageBitmapLoader;
import com.reneponette.comicbox.controller.DataController.OnDataBuildListener;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.ui.InterstitialActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.MessageUtils;

public class ZipFileReaderFragment extends BasePagerReaderFragment implements OnDataBuildListener {

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

	// //////////////////////////////////////////////////

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		curFile = new File(getArguments().getString(PATH));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		dataController.build();
	}

	/*---------------------------------------------------------------------------*/

	@Override
	protected void onGoNextFile() {
		// 다음 권으로 넘김
		DialogHelper.showGoNextComicsDialog(getActivity(), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//광고 보여주기
				Intent intent = new Intent();
				intent.setClass(getActivity(), InterstitialActivity.class);
				startActivity(intent);
				
				dataController.saveReadState(0);

				File next = findNextFile();
				if (next != null) {
					dataController.prepare(next);
					dataController.build();
				}
			}
		});
	}


	@Override
	public void onStartBuild() {
		showWaitingDialog();
		viewPager.setAdapter(null);
		previewGallery.setAdapter(null);
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
		previewGallery.setAdapter(previewAdapter);

		initUI();

		hideWaitingDialog();
	}

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		return ImageUtils.getBitmap(pi.getZipFile(), pi.getZipEntry(), pi.getBuildType(), isAutocrop(),
				false);
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;
	}

}