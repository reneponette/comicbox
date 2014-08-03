package com.reneponette.comicbox.ui.fragment.explorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.cache.BitmapCache;
import com.reneponette.comicbox.cache.LocalThumbBitmapLoader;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.ui.MainActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.MetricUtils;

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

	FileInfo curInfo;
	List<FileInfo> infoList;
	FolderViewAdapter adapter;
	GridView gridView;
	int numOfColumn;

	PopupMenu popupMenu;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		curInfo = getArguments().getParcelable(FILE_INFO);
		((MainActivity) activity).onSectionAttached(curInfo.getName());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		infoList = new ArrayList<FileInfo>();
		adapter = new FolderViewAdapter(infoList);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);

		gridView = (GridView) rootView.findViewById(R.id.gridView1);
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (getActivity() instanceof FolderViewFragmentListener) {
					FileInfo info = (FileInfo) parent.getItemAtPosition(position);
					((FolderViewFragmentListener) getActivity()).onFileClicked(info);
				}
			}

		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String title = curInfo.getName();
		getActivity().setTitle(title);
		enumerate();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();

		numOfColumn = 2;
		try {
			numOfColumn = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("explorer_num_of_column",
					2);
		} catch (NumberFormatException e) {

		}
		gridView.setNumColumns(numOfColumn);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isDrawerOpen()) {
			super.onCreateOptionsMenu(menu, inflater);
			return;
		}

		inflater.inflate(R.menu.folder, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();

		if (id == R.id.action_read_direction_setting) {
			DialogHelper.showReadDirectionSelectDialog(getActivity(), curInfo.getMeta(), new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					FileInfoDAO.instance().insertOrUpdate(curInfo);
				}
			});
			return true;
		}

		if (id == R.id.action_recreate_thumbnail) {
			for (FileInfo info : infoList) {
				info.getMeta().coverPath = "";
				FileInfoDAO.instance().insertOrUpdate(info);
				BitmapCache.INSTANCE.removeBitmapFromMemCache(info);
			}
			adapter.notifyDataSetInvalidated();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private boolean goParentDirectory() {
		File parentFile = curInfo.getFile().getParentFile();
		if (parentFile != null) {
			FileInfo info = new FileInfo(LocationType.LOCAL);
			info.setFile(parentFile);
			info.focusName = curInfo.getName();
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onFileClicked(info);
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

		List<File> childFileList = Arrays.asList(curInfo.getFile().listFiles());
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

	public class FolderViewAdapter extends BaseAdapter {

		private List<FileInfo> list;

		public FolderViewAdapter(List<FileInfo> metaList) {
			this.list = metaList;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {

			View v = view;
			if (v == null) {
				v = getActivity().getLayoutInflater().inflate(R.layout.fragment_explorer_item, null);
				Holder holder = new Holder();
				holder.itemImage = (ImageView) v.findViewById(R.id.itemImage);
				holder.itemName = (TextView) v.findViewById(R.id.itemName);
				holder.itemProgress = (TextView) v.findViewById(R.id.itemProgress);
				holder.itemCount = (TextView) v.findViewById(R.id.itemCount);
				holder.itemImage.setTag(holder.itemName);
				v.setTag(holder);
			}

			FileInfo info = (FileInfo) getItem(position);
			Holder holder = (Holder) v.getTag();

			GridView.LayoutParams lp;

			int itemSpacing = MetricUtils.dpToPixel(7);
			int itemWidth = (gridView.getWidth() - itemSpacing * (numOfColumn - 1)) / numOfColumn;
			int itemHeight = (int) (itemWidth * 1.0 + MetricUtils.dpToPixel(50));
			lp = new GridView.LayoutParams(GridView.AUTO_FIT, itemHeight);
			v.setLayoutParams(lp);

			// name
			holder.itemName.setText(info.getFile().getName());
			// image
			holder.itemImage.setScaleType(ScaleType.CENTER_CROP);
			new LocalThumbBitmapLoader(info, holder.itemImage).run();
			// child count
			int itemCount = 0;
			if (info.getFile().list() != null) {
				itemCount = info.getFile().list().length;
			}
			holder.itemCount.setText(itemCount == 0 ? "" : itemCount + "");

			FileMeta meta = info.getMeta();
			if ((meta.type == FileType.ZIP || meta.type == FileType.PDF) && meta.lastReadPageIndex != -1) {
				int readPage = meta.lastReadDirection == ReadDirection.RTL ? meta.lastTotalPageCount
						- meta.lastReadPageIndex : meta.lastReadPageIndex + 1;

				holder.itemProgress.setText(readPage + "/" + meta.lastTotalPageCount);
			} else {
				holder.itemProgress.setText("");
			}

			return v;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		class Holder {
			public FileInfo info;
			public ImageView itemImage;
			public TextView itemName;
			public TextView itemProgress;
			public TextView itemCount;
		}

	}
}
