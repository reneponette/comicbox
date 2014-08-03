package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
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
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.ui.InterstitialActivity;
import com.reneponette.comicbox.ui.ReaderActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.MessageUtils;

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
		dataController.buildPdf();
	}
	
	
	/*---------------------------------------------------------------------------*/
	
	
	@Override
	protected void onGoNextFile() {
		// 다음 권으로 넘김
		DialogHelper.showGoNextComicsDialog(getActivity(), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dataController.saveReadState(0);

				File next = findNextFile();
				if (next != null) {
					FileInfo info = FileInfoDAO.instance().getFileInfo(next);
					Intent i = ReaderActivity.newIntent(getActivity(), info);
					startActivity(i);
					getActivity().finish();
				
//					//광고 보여주기
//					Intent intent = new Intent();
//					intent.setClass(getActivity(), InterstitialActivity.class);
//					startActivity(intent);					
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
		previewGallery.setAdapter(previewAdapter);

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
		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;		
	}

}