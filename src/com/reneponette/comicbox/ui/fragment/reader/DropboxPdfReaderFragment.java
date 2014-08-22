package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.artifex.mupdfdemo.FilePicker;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.artifex.mupdfdemo.OutlineActivityData;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.PageInfo;

@SuppressWarnings("deprecation")
public class DropboxPdfReaderFragment extends BaseReaderFragment implements OnPageBuildListener,
		FilePicker.FilePickerSupport {

	public static DropboxPdfReaderFragment newInstance(String folderPath) {
		DropboxPdfReaderFragment fragment = new DropboxPdfReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, folderPath);
		fragment.setArguments(args);
		return fragment;
	}

	public DropboxPdfReaderFragment() {
	}

	File curFile;
	FileInfo fileInfo;

	MuPDFCore core;
	MuPDFReaderView mDocView;
	EditText mPasswordView;

	View menuContainer;
	SeekBar seekBar;

	// //////////////////////////////////////////////////

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		curFile = new File(getArguments().getString(PATH));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pageBuilder.setOnDataBuildListener(this);
		pageBuilder.prepare(curFile);

		fileInfo = pageBuilder.getFileInfo();

		if (core == null) {
			core = openFile(fileInfo.getPath());

			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0) {
				core = null;
			}
		}
		if (core == null) {
			AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
			alert.setTitle("문서를 열 수 없음");
			alert.setButton(AlertDialog.BUTTON_POSITIVE, "dismiss", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getActivity().finish();
				}
			});
			alert.show();
			return;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (core == null)
			return null;

		mDocView = new MuPDFReaderView(getActivity()) {
			@Override
			protected void onTapMainDocArea() {
				if (menuContainer.getVisibility() == View.GONE)
					menuContainer.setVisibility(View.VISIBLE);
				else
					menuContainer.setVisibility(View.GONE);
			}

			@Override
			protected void onMoveToChild(int i) {
				super.onMoveToChild(i);

			}
		};
		mDocView.setAdapter(new MuPDFPageAdapter(getActivity(), this, core));
		mDocView.setBackgroundColor(Color.BLACK);
		mDocView.setDisplayedViewIndex(fileInfo.getMeta().lastReadPageIndex);

		RelativeLayout rootView = new RelativeLayout(getActivity());
		rootView.addView(mDocView);

		View view = inflater.inflate(R.layout.fragment_reader_menu, container, false);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rootView.addView(view, lp);

		menuContainer = rootView.findViewById(R.id.menu_container);
		menuContainer.setVisibility(View.GONE);

		rootView.findViewById(R.id.top_box).setVisibility(View.GONE);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar1);
		seekBar.setMax(core.countPages() - 1);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateSeekBarLabel();
	}

	@Override
	public void onDestroy() {
		pageBuilder.saveReadState(mDocView.getDisplayedViewIndex());
		super.onDestroy();
	}

	@Override
	public void onStartBuild() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFailBuild(String errStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddPageInfo(PageInfo pageInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFinishBuild() {
		// TODO Auto-generated method stub

	}

	private MuPDFCore openFile(String path) {
		System.out.println("Trying to open " + path);
		try {
			core = new MuPDFCore(getActivity(), path);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
		return core;
	}

	public void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(getActivity());
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
		alert.setTitle("enter_password");
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (core.authenticatePassword(mPasswordView.getText().toString())) {
							// createUI(savedInstanceState);
						} else {
							requestPassword(savedInstanceState);
						}
					}
				});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						getActivity().finish();
					}
				});
		alert.show();
	}

	@Override
	public void performPickFor(FilePicker arg0) {
		// TODO Auto-generated method stub

	}

	protected void updateSeekBarLabel() {
		((TextView) getView().findViewById(R.id.pageLeft)).setText("1");
		((TextView) getView().findViewById(R.id.pageRight)).setText(core.countPages() + "");
	}

	@Override
	public boolean onBackPressed() {
		if (menuContainer.getVisibility() == View.VISIBLE) {
			menuContainer.setVisibility(View.GONE);
			return true;
		}
		return false;
	}

}