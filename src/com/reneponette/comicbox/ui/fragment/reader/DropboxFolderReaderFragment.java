package com.reneponette.comicbox.ui.fragment.reader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.controller.DropboxFolderPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.MessageUtils;

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


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startIndex = getArguments().getInt(START_INDEX, -1);
		
		Entry entry = new Entry();
		entry.path = getArguments().getString(PATH);
		pageBuilder.prepare(entry);
	}


	
	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {
		PageBuilder builder = new DropboxFolderPageBuilder();
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
				if(startIndex > -1 ) {
					if(pageBuilder.getReadDirection() == ReadDirection.RTL)
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
				
				MessageUtils.toast(getActivity(), "pageCount = " + pageBuilder.pageSize());
			}
		});
		return builder;
	}
	/*---------------------------------------------------------------------------*/


	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
//		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), isAutocrop(), false);
		return null;
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = pageBuilder.getPageInfo(position);
//		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
		return null;
	}

}