package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.artifex.mupdfdemo.FilePicker;
import com.reneponette.comicbox.cache.PageBitmapLoader;
import com.reneponette.comicbox.controller.LocalPdfPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

@SuppressWarnings("deprecation")
public class LocalPdfReaderFragment extends BasePagerReaderFragment implements FilePicker.FilePickerSupport {

	public static LocalPdfReaderFragment newInstance(String folderPath) {
		LocalPdfReaderFragment fragment = new LocalPdfReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, folderPath);
		fragment.setArguments(args);
		return fragment;
	}

	public LocalPdfReaderFragment() {
	}

	File curFile;
	FileInfo fileInfo;

	/*---------------------------------------------------------------------------*/

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
		} else {
			curFile = new File(savedInstanceState.getString(PATH));
		}

		pageBuilder.prepare(curFile);
	}

	
	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {
		PageBuilder builder = new LocalPdfPageBuilder(); 
		builder.setOnDataBuildListener(new OnPageBuildListener() {
			
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
		});
		return builder;
	}

	/*---------------------------------------------------------------------------*/


	@Override
	public void performPickFor(FilePicker arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
		new PageBitmapLoader(pi, iv, isAutocrop(), false).run();
		return null;
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
		if (pi.getPdfCore() == null)
			return null;

		return ImageUtils.getBitmap(pi.getPdfCore(), pi.getPdfIndex(), pi.getBuildType(), false, true);
	}

}