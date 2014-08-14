package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.controller.DataController.OnDataBuildListener;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.StringUtils;

public class ZipStreamReaderFragment extends BasePagerReaderFragment implements OnDataBuildListener {

	public static ZipStreamReaderFragment newInstance(String dropboxPath) {
		ZipStreamReaderFragment fragment = new ZipStreamReaderFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public ZipStreamReaderFragment() {
	}

	/*---------------------------------------------------------------------------*/
	DropboxAPI<AndroidAuthSession> api;

	private Entry curEntry; // dropbox Entry
	private File cacheDir;

	/*---------------------------------------------------------------------------*/

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Entry entry = new Entry();
		entry.path = getArguments().getString(PATH);
		curEntry = entry;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataController.setOnDataBuildListener(this);
		dataController.prepare(curEntry);

		cacheDir = new File(GlobalApplication.instance().getCacheDir(), "comics/"
				+ StringUtils.getMD5(dataController.getFileInfo().getName()));
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		removeOtherCacheDir();

		// dropbox
		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		api = new DropboxAPI<AndroidAuthSession>(session);
	}

	private void removeOtherCacheDir() {
		for (File f : cacheDir.getParentFile().listFiles()) {
			if (f.isHidden())
				continue;
			if (f.isFile())
				continue;
			if (f.getName().equals(cacheDir.getName()))
				continue;
			boolean success = f.delete();
			if (!success) {
				for (File imageFile : f.listFiles()) {
					if (imageFile.isDirectory())
						continue;
					success = imageFile.delete();
				}
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dataController.build(api, cacheDir);
	}
	
	@Override
	protected void onGoNextFile() {
		
		// do nothing
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

		seekBar.setMax(dataController.pageSize() - 1);
		((TextView) getView().findViewById(R.id.pageLeft)).setText("1");
		((TextView) getView().findViewById(R.id.pageRight)).setText(dataController.pageSize() + "");

		hideWaitingDialog();
	}

	@Override
	public void onFinishBuild() {
		// TODO Auto-generated method stub

	}

	@Override
	protected Bitmap getPageBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), isAutocrop(), false);
	}

	@Override
	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		PageInfo pi = dataController.getPageInfo(position);
//		new PageBitmapLoader(pi, iv, isAutocrop(), true).run();
//		return null;
		
		if(pi.getFile() == null)
			return null;
		
		return ImageUtils.getBitmap(pi.getFile(), pi.getBuildType(), false, true);		
	}

}