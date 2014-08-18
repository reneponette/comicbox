package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.artifex.mupdfdemo.FilePicker;
import com.reneponette.comicbox.cache.PageBitmapLoader;
import com.reneponette.comicbox.controller.DataController.OnDataBuildListener;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

@SuppressWarnings("deprecation")
public class PdfReaderFragment extends BasePagerReaderFragment implements OnDataBuildListener,
		FilePicker.FilePickerSupport {

	public static PdfReaderFragment newInstance(String folderPath) {
		PdfReaderFragment fragment = new PdfReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, folderPath);
		fragment.setArguments(args);
		return fragment;
	}

	public PdfReaderFragment() {
	}

	File curFile;
	FileInfo fileInfo;

	/*---------------------------------------------------------------------------*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		curFile = new File(getArguments().getString(PATH));
		dataController.setOnDataBuildListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dataController.prepare(curFile).buildPdf();
	}
	
	
	/*---------------------------------------------------------------------------*/
		

	@Override
	public void onStartBuild() {
		showWaitingDialog();
		viewPager.setAdapter(null);
	}

	@Override
	public void onFailBuild(String errStr) {
		hideWaitingDialog();
		AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
		alert.setTitle("문서를 열 수 없음");
		alert.setButton(AlertDialog.BUTTON_POSITIVE, "dismiss", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				getActivity().finish();
			}
		});
		alert.show();
	}

	@Override
	public void onAddPageInfo(PageInfo pageInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFinishBuild() {
		viewPager.setAdapter(pagerAdapter);

		initUI();
		hideWaitingDialog();
	}

	@Override
	public void performPickFor(FilePicker arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		new PageBitmapLoader(pi, iv, isAutocrop(), false).run();
		return null;
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
//		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
//		return null;
		
		if(pi.getPdfCore() == null)
			return null;
		
		return ImageUtils.getBitmap(pi.getPdfCore(), pi.getPdfIndex(), pi.getBuildType(), false, true);		
	}

}