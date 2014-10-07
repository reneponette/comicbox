package com.reneponette.comicbox.ui.fragment.explorer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.reneponette.comicbox.cache.LocalThumbBitmapLoader;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileLocation;
import com.reneponette.comicbox.model.FileMeta.FileType;

/**
 * A placeholder fragment containing a simple view.
 */
public class LocalExplorerFragment extends BaseExplorerFragment {

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String FILE_INFO = "file_info";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static LocalExplorerFragment newInstance(FileInfo fileInfo) {
		LocalExplorerFragment fragment = new LocalExplorerFragment();
		Bundle args = new Bundle();
		args.putParcelable(FILE_INFO, fileInfo);
		fragment.setArguments(args);
		return fragment;
	}

	public LocalExplorerFragment() {
		//
	}

	
	@Override
	protected FileInfo onGetFileInfo() {
		return getArguments().getParcelable(FILE_INFO);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	
	@Override
	public void onResume() {
		super.onResume();
		
		enumerate();
//		handler.postDelayed(new Runnable() {
//			
//			@Override
//			public void run() {
//			}
//		}, 300);
	}
	

	private boolean goParentDirectory() {
		File parentFile = new File(curInfo.getPath()).getParentFile();
		if (parentFile != null) {
			FileInfo info = new FileInfo(FileLocation.LOCAL);
			info.fill(parentFile);
			info.focusName = curInfo.getName();
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onFileInfoClicked(info);
			}
			return true;
		}
		return false;
	}

	private void enumerate() {
		infoList.clear();

		int i = 0;
		FileInfo info;

		int indexInParent = 0;
		int indexOfFocus = 0;

		List<File> childFileList = Arrays.asList(new File(curInfo.getPath()).listFiles());
		Collections.sort(childFileList);

		for (File f : childFileList) {

			if (f.isHidden())
				continue;

			info = FileInfoDAO.instance().getFileInfo(f);

			if (info.getMeta().type != FileType.UNKNOWN) {
				infoList.add(info);

				if (info.getMeta().type == FileType.JPG) {
					info.indexInParent = indexInParent;
					indexInParent++;
				}

				if (f.getName().equals(curInfo.focusName))
					indexOfFocus = i;

				i++;
			}
		}
		adapter.notifyDataSetChanged();
		gridView.setSelection(indexOfFocus);
	}

	@Override
	public boolean onBackPressed() {
		if (C.LOCAL_ROOT_PATH.equals(curInfo.getPath())) {
			return false;
		}
		return goParentDirectory();
	}
	
	@Override
	protected Bitmap getThumbnailBitmap(FileInfo info, ImageView thumbnailIv) {
		new LocalThumbBitmapLoader(info, thumbnailIv).run();
		return null;
	}

}
