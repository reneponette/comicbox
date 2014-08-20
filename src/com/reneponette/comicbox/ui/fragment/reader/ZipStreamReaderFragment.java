package com.reneponette.comicbox.ui.fragment.reader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.controller.DropboxZipPageBuilder;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.controller.PageBuilder.OnPageBuildListener;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class ZipStreamReaderFragment extends BasePagerReaderFragment implements OnPageBuildListener {

	public static ZipStreamReaderFragment newInstance(String dropboxPath) {
		ZipStreamReaderFragment fragment = new ZipStreamReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public ZipStreamReaderFragment() {
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
		pageBuilder.setOnDataBuildListener(this);
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	
	@Override
	protected void onGoNextFile() {
		
		// 드롭박스는 다음권 자동넘김 당장은 지원 안함...
	}

	/*---------------------------------------------------------------------------*/
	@Override
	protected PageBuilder onCreatePageBuilder() {
		return new DropboxZipPageBuilder();
	}
	
	/*---------------------------------------------------------------------------*/

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