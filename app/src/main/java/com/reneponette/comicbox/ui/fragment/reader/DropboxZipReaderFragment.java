package com.reneponette.comicbox.ui.fragment.reader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.controller.DropboxZipPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class DropboxZipReaderFragment extends BasePagerReaderFragment {

	public static DropboxZipReaderFragment newInstance(String dropboxPath) {
		DropboxZipReaderFragment fragment = new DropboxZipReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public DropboxZipReaderFragment() {
	}



	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(PATH, pageBuilder.getFileInfo().getPath());
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Entry entry = new Entry();
		if(savedInstanceState == null) {
			entry.path = getArguments().getString(PATH);
		} else {
			entry.path = savedInstanceState.getString(PATH);
		}
		
		pageBuilder.prepare(entry);		
	}

	
	@Override
	protected void onGoNextFile() {
		
		// 드롭박스는 다음권 자동넘김 당장은 지원 안함...
	}

	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {
		PageBuilder builder = new DropboxZipPageBuilder();
		builder.setOnDataBuildListener(new OnPageBuildListener() {
			
			@Override
			public void onStartBuild() {
				pagerAdapter.notifyDataSetChanged();
				showWaitingDialog();
			}

			@Override
			public void onFailBuild(String errStr) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAddPageInfo(PageInfo pageInfo) {
				pagerAdapter.notifyDataSetChanged();

				seekBar.setMax(pageBuilder.pageSize() - 1);
				((TextView) getView().findViewById(R.id.pageLeft)).setText("1");
				((TextView) getView().findViewById(R.id.pageRight)).setText(pageBuilder.pageSize() + "");

				hideWaitingDialog();
			}

			@Override
			public void onFinishBuild() {
				// TODO Auto-generated method stub

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
		if(pi.getFile() == null)
			return null;
		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), false, true);		
	}

}