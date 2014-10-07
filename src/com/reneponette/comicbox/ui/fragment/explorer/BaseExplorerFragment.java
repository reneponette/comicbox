package com.reneponette.comicbox.ui.fragment.explorer;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.cache.BitmapCache;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.FavoriteManager;
import com.reneponette.comicbox.model.FileLocation;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.ui.MainActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.ImageUtils;
import com.reneponette.comicbox.utils.MetricUtils;
import com.reneponette.comicbox.view.ProgressTextView;

public class BaseExplorerFragment extends Fragment {
	public interface FolderViewFragmentListener {
		public void onFileInfoClicked(FileInfo info);
	}

	FileInfo curInfo;
	List<FileInfo> infoList;
	FolderViewAdapter adapter;
	GridView gridView;

	Handler handler;

	private ProgressDialog mProgressDlg;
	int numOfColumn;

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("file_info", curInfo);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState == null) {
			curInfo = onGetFileInfo();
		} else {
			curInfo = savedInstanceState.getParcelable("file_info");
		}
		
		handler = GlobalApplication.instance().getHandler();
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
					((FolderViewFragmentListener) getActivity()).onFileInfoClicked(info);
				}
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		((MainActivity) getActivity()).onSectionAttached(curInfo.getName());
	}

	@Override
	public void onResume() {
		super.onResume();

		((MainActivity) getActivity()).onSectionAttached(curInfo.getName());

		
		numOfColumn = MetricUtils.getDisplayWidth() / C.COVER_W;
		gridView.setNumColumns(numOfColumn);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isDrawerOpen()) {
			super.onCreateOptionsMenu(menu, inflater);
			return;
		}

		inflater.inflate(R.menu.folder, menu);

		if (FavoriteManager.INSTANCE.contains(curInfo)) {
			menu.removeItem(R.id.action_add_to_favorite);
		} else {
			menu.removeItem(R.id.action_remove_from_favorite);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();

		if (id == R.id.action_read_direction_setting) {
			setReadDirection(curInfo);
			return true;
		}

		if (id == R.id.action_recreate_thumbnail) {
			for (FileInfo info : infoList) {
				removeCover(info);
			}
			adapter.notifyDataSetInvalidated();
			return true;
		}

		if (item.getItemId() == R.id.action_add_to_favorite) {
			FavoriteManager.INSTANCE.add(curInfo);
			return true;
		}
		if (item.getItemId() == R.id.action_remove_from_favorite) {
			FavoriteManager.INSTANCE.remove(curInfo);
			;
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/*---------------------------------------------------------------------*/
	
	public FileInfo getCurrentInfo() {
		return curInfo;
	}
	
	
	private void setInfoBackgroundColor(View v, Bitmap bm, int color) {
		// 텍스뷰 배경 색깔 변경
		int avgColor = color == -1 ? ImageUtils.getAverageColor(bm, 200, false) : color;
		if (v.getBackground() instanceof LayerDrawable) {
			LayerDrawable ld = (LayerDrawable) v.getBackground();
			GradientDrawable drawable = (GradientDrawable) ld.findDrawableByLayerId(R.id.folder_info_bg);
			drawable.setColor(avgColor);
		} else {
			v.setBackgroundColor(avgColor);
		}
	}
	
	public void showWaitingDialog() {
		if (mProgressDlg != null)
			return;
		mProgressDlg = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
		mProgressDlg.setMessage(getResources().getString(R.string.progress_loading));
		mProgressDlg.setCanceledOnTouchOutside(false);
		mProgressDlg.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mProgressDlg = null;
			}
		});
		mProgressDlg.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface arg0) {
				mProgressDlg = null;
			}
		});
		mProgressDlg.show();
	}

	public void hideWaitingDialog() {
		if (mProgressDlg != null) {
			mProgressDlg.dismiss();
		}
	}

	public boolean onBackPressed() {
		return false;
	}

	public void removeCover(FileInfo info) {
		info.getMeta().coverPath = "";
		FileInfoDAO.instance().insertOrUpdate(info);
		BitmapCache.INSTANCE.removeBitmapFromMemCache(info);
	}

	public void setReadDirection(final FileInfo info) {
		DialogHelper.showReadDirectionSelectDialog(getActivity(), info.getMeta(), new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				FileInfoDAO.instance().insertOrUpdate(info);
			}
		});
	}

	protected FileInfo onGetFileInfo() {
		throw new RuntimeException("should implement in subclass");
	}

	protected Bitmap getThumbnailBitmap(FileInfo info, ImageView thumbnailIv) {
		return null;
	}

	
	/*---------------------------------------------------------------------*/

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
				holder.itemProgress = (ProgressTextView) v.findViewById(R.id.itemProgress);
				holder.itemCount = (TextView) v.findViewById(R.id.itemCount);
				holder.itemMenuBtn = (ImageView) v.findViewById(R.id.itemMenuBtn);
				v.setTag(holder);
			}

			final FileInfo info = (FileInfo) getItem(position);
			FileMeta meta = info.getMeta();
			final Holder holder = (Holder) v.getTag();

			// name
			holder.itemName.setText(info.getName());

			// image
			holder.itemImage.setScaleType(ScaleType.CENTER_CROP);
			Bitmap thumbnail = getThumbnailBitmap(info, holder.itemImage);
			if (thumbnail != null)
				holder.itemImage.setImageBitmap(thumbnail);

			// child count
			int itemCount = meta.childCount;
			holder.itemCount.setText(itemCount == 0 ? "" : itemCount + "");

			if (meta.type != FileType.DIRECTORY) {
				holder.itemMenuBtn.setVisibility(View.GONE);
			} else {
				holder.itemMenuBtn.setVisibility(View.VISIBLE);
			}

			// progress
			if ((meta.type == FileType.ZIP || meta.type == FileType.PDF) && meta.lastReadPageIndex != -1) {
				int readPage = meta.lastReadDirection == ReadDirection.RTL ? meta.lastTotalPageCount
						- meta.lastReadPageIndex : meta.lastReadPageIndex + 1;

				holder.itemProgress.setText(readPage + "/" + meta.lastTotalPageCount);
				holder.itemProgress.setProgress(((float) readPage) / meta.lastTotalPageCount);
				holder.itemProgress.setVisibility(View.VISIBLE);
				;
			} else {
				holder.itemProgress.setVisibility(View.GONE);
				;
			}

			holder.itemMenuBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					PopupMenu popupMenu = new PopupMenu(getActivity(), holder.itemMenuBtn);
					popupMenu.inflate(R.menu.folder);
					if (info.getMeta().type != FileType.DIRECTORY) {
						popupMenu.getMenu().removeItem(R.id.action_add_to_favorite);
						popupMenu.getMenu().removeItem(R.id.action_remove_from_favorite);
					}

					if (FavoriteManager.INSTANCE.contains(info)) {
						popupMenu.getMenu().removeItem(R.id.action_add_to_favorite);
					} else {
						popupMenu.getMenu().removeItem(R.id.action_remove_from_favorite);
					}

					popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {

							if (item.getItemId() == R.id.action_add_to_favorite) {
								FavoriteManager.INSTANCE.add(info);
								return true;
							}
							if (item.getItemId() == R.id.action_remove_from_favorite) {
								FavoriteManager.INSTANCE.remove(info);
								;
								return true;
							}
							if (item.getItemId() == R.id.action_read_direction_setting) {
								setReadDirection(info);
								return true;
							}
							if (item.getItemId() == R.id.action_recreate_thumbnail) {
								removeCover(info);
								adapter.notifyDataSetInvalidated();

								return true;
							}

							return false;
						}
					});
					popupMenu.show();
				}
			});

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
			public ImageView itemImage;
			public TextView itemName;
			public ProgressTextView itemProgress;
			public TextView itemCount;
			public ImageView itemMenuBtn;
		}

	}
}
