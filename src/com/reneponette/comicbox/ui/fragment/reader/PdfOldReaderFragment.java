package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
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
import com.reneponette.comicbox.controller.DataController.OnDataBuildListener;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.PageInfo;

@SuppressWarnings("deprecation")
public class PdfOldReaderFragment extends BaseReaderFragment implements OnDataBuildListener, FilePicker.FilePickerSupport {

	public static PdfOldReaderFragment newInstance(String folderPath) {
		PdfOldReaderFragment fragment = new PdfOldReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, folderPath);
		fragment.setArguments(args);
		return fragment;
	}

	public PdfOldReaderFragment() {
	}

	File curFile;
	FileInfo fileInfo;
	
	MuPDFCore core;
	MuPDFReaderView mDocView;
	EditText mPasswordView;

	Gallery previewGallery;
	PreviewAdapter previewAdapter;
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
		dataController.setOnDataBuildListener(this);
		dataController.prepare(curFile);
		
		fileInfo = dataController.getFileInfo();

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
		previewAdapter = new PreviewAdapter();
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
				if (previewGallery == null)
					return;
				previewGallery.setSelection(i);
			}
		};
		mDocView.setAdapter(new MuPDFPageAdapter(getActivity(), this, core));
		mDocView.setBackgroundColor(Color.BLACK);
		mDocView.setDisplayedViewIndex(fileInfo.getMeta().lastReadPage);

		RelativeLayout rootView = new RelativeLayout(getActivity());
		rootView.addView(mDocView);

		View view = inflater.inflate(R.layout.fragment_reader_menu, container, false);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rootView.addView(view, lp);

		menuContainer = rootView.findViewById(R.id.menu_container);
		menuContainer.setVisibility(View.GONE);

		rootView.findViewById(R.id.title_box).setVisibility(View.GONE);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar1);
		seekBar.setMax(core.countPages()-1);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				previewGallery.setSelection(progress);
			}
		});

		previewGallery = (Gallery) rootView.findViewById(R.id.preview_selector);
		previewGallery.setAdapter(previewAdapter);
		previewGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mDocView.setDisplayedViewIndex(position);
				seekBar.setProgress(position);
			}
		});
		previewGallery.setSelection(fileInfo.getMeta().lastReadPage);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateSeekBarLabel();
	}
	
	@Override
	public void onDestroy() {
		dataController.saveReadState(mDocView.getDisplayedViewIndex());
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

	// /////////////////////////////////////////////////////////////////////

	private class PreviewAdapter extends BaseAdapter {
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = getActivity().getLayoutInflater().inflate(R.layout.preview_item, previewGallery, false);
				Holder holder = new Holder();
				holder.previewIv = (ImageView) view.findViewById(R.id.previewImage);
				holder.pageNumTv = (TextView) view.findViewById(R.id.pageNumber);
				view.setTag(holder);
			}

			final Holder holder = (Holder) view.getTag();
			holder.pageNumTv.setText(position + 1 + "");
			Bitmap bm = Bitmap.createBitmap(200, 300, Config.ARGB_8888);
			core.drawPage(bm, position, 200, 300, 0, 0, 200, 300);
			holder.previewIv.setImageBitmap(bm);

			return view;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public int getCount() {
			return core.countPages();
		}

		class Holder {
			public ImageView previewIv;
			public TextView pageNumTv;
		}
	}

}