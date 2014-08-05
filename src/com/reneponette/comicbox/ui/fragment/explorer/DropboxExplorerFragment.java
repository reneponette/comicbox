package com.reneponette.comicbox.ui.fragment.explorer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.R;
import com.reneponette.comicbox.cache.BitmapCache;
import com.reneponette.comicbox.cache.DropboxThumbBitmapLoader;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.manager.DropBoxManager;
import com.reneponette.comicbox.manager.FavoriteManager;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.ui.MainActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.MessageUtils;
import com.reneponette.comicbox.utils.MetricUtils;
import com.reneponette.comicbox.utils.StringUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class DropboxExplorerFragment extends BaseExplorerFragment {

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String PATH = "path";
	private static final String TAG = "DropboxViewFragment";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static DropboxExplorerFragment newInstance(String dropboxPath) {
		DropboxExplorerFragment fragment = new DropboxExplorerFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public DropboxExplorerFragment() {
		//
	}

	private FileInfo curInfo;
	private List<FileInfo> infoList;
	private FolderViewAdapter adapter;
	private GridView gridView;

	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn;
	private String dropboxPath;
	private int numOfColumn;
	private Thread runningThread;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		dropboxPath = getArguments().getString(PATH);
		Entry entry = new Entry();
		entry.path = dropboxPath;
		curInfo = FileInfoDAO.instance().getFileInfo(entry);

		((MainActivity) activity).onSectionAttached(curInfo.getName());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		infoList = new ArrayList<FileInfo>();
		adapter = new FolderViewAdapter(infoList);

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = DropBoxManager.INSTANCE.buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		// Display the proper UI state if logged in or not
		setLoggedIn(mApi.getSession().isLinked());
		if (!mLoggedIn) {
			mApi.getSession().startOAuth2Authentication(getActivity());
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_explorer, container, false);

		gridView = (GridView) rootView.findViewById(R.id.gridView1);
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final FileInfo info = (FileInfo) parent.getItemAtPosition(position);

				// 폴더 클릭
				if (getActivity() instanceof FolderViewFragmentListener) {
					((FolderViewFragmentListener) getActivity()).onEntryClicked(info);
				}
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		((MainActivity) getActivity()).onSectionAttached(curInfo.getName());

		AndroidAuthSession session = mApi.getSession();
		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				DropBoxManager.INSTANCE.storeAuth(session);
				setLoggedIn(true);

			} catch (IllegalStateException e) {
				MessageUtils.toast(getActivity(), "Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}

		numOfColumn = 2;
		// numOfColumn = getResources().getConfiguration().orientation ==
		// Configuration.ORIENTATION_PORTRAIT ? 2 : 4;
		try {
			numOfColumn = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("explorer_num_of_column",
					2);
		} catch (NumberFormatException e) {

		}
		gridView.setNumColumns(numOfColumn);

		enumerate();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isDrawerOpen()) {
			super.onCreateOptionsMenu(menu, inflater);
			return;
		}

		// 같은 메뉴가 중복으로 들어가지 않도록..
		if (menu.findItem(R.id.action_read_direction_setting) == null) {
			inflater.inflate(R.menu.folder, menu);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// 현재 보고있는 fragment만 처리하도록
		FragmentManager fm = getActivity().getFragmentManager();
		if (fm.getBackStackEntryCount() > 0) {
			BackStackEntry be = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1);
			if (curInfo.getPath().equals(be.getName()) == false)
				return false;
		}

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

	private void logOut() {
		// Remove credentials from the session
		mApi.getSession().unlink();

		// Clear our stored keys
		DropBoxManager.INSTANCE.clearKeys();
		// Change UI state to display logged out version
		setLoggedIn(false);
	}

	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
		} else {
//			MessageUtils.toast(getActivity(), "드롭박스 접속안됨");
		}
	}
	
	
	private boolean goParentDirectory() {
//		File parentFile = curInfo.getFile().getParentFile();
//		if (parentFile != null) {
//			FileInfo info = new FileInfo(LocationType.LOCAL);
//			info.setFile(parentFile);
//			info.focusName = curInfo.getName();
//			if (getActivity() instanceof FolderViewFragmentListener) {
//				((FolderViewFragmentListener) getActivity()).onFileClicked(info);
//			}
//			return true;
//		}
		
		// 상위 폴더 넣기
		if (StringUtils.isBlank(curInfo.getEntry().parentPath()) == false) {
			FileInfo parentInfo;
			Entry parentEntry = new Entry();
			parentEntry.isDir = true;
			parentEntry.path = curInfo.getEntry().parentPath();
			parentInfo = new FileInfo(LocationType.DROPBOX);
			parentInfo.setEntry(parentEntry);
			infoList.add(parentInfo);
			
			FileInfo info = new FileInfo(LocationType.DROPBOX);
			info.setEntry(parentEntry);
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onEntryClicked(info);;
			}
			return true;			
		}		
		
		return false;
	}

	private void enumerate() {

		if (runningThread != null)
			runningThread.interrupt();

		showWaitingDialog();
		infoList.clear();
		gridView.setAdapter(null);

		runningThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (getActivity() == null)
						return;

					final Entry entry = mApi.metadata(dropboxPath, 1000, null, true, null);
					if (!entry.isDir || entry.contents == null) {
						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Log.e(TAG, "File or empty directory");
								hideWaitingDialog();
							}
						});
						return;
					}

					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {

							curInfo.setEntry(entry);

							String name = curInfo.getName();
							if (StringUtils.isBlank(name))
								name = "/";
							((MainActivity) getActivity()).onSectionAttached(name);

							for (Entry ent : entry.contents) {
								FileInfo info = FileInfoDAO.instance().getFileInfo(ent);
								if (info.getMeta().type != FileType.UNKNOWN)
									infoList.add(info);
							}
							gridView.setAdapter(adapter);
							hideWaitingDialog();
						}
					});
				} catch (DropboxException e) {
					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							hideWaitingDialog();
						}
					});
					e.printStackTrace();
				}
				runningThread = null;
			}
		});
		runningThread.start();

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
				holder.itemMenuBtn = (ImageView) v.findViewById(R.id.itemMenuBtn);
				v.setTag(holder);
			}

			final Holder holder = (Holder) v.getTag();
			final FileInfo info = (FileInfo) getItem(position);
			
			// name
			holder.itemName.setText(info.getEntry().fileName());
			// image
			holder.itemImage.setScaleType(ScaleType.CENTER_CROP);
			new DropboxThumbBitmapLoader(info, mApi, holder.itemImage).run();
			
			// child count
			int itemCount = 0;
			if (info.getEntry().contents != null) {
				itemCount = info.getEntry().contents.size();
			}
			holder.itemCount.setText(itemCount == 0 ? "" : itemCount + "");
			
			// progress
			holder.itemProgress.setText("");
			
			holder.itemMenuBtn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					PopupMenu popupMenu = new PopupMenu(getActivity(), holder.itemMenuBtn);
					popupMenu.inflate(R.menu.folder);
					if(info.getMeta().type != FileType.DIRECTORY) {
						popupMenu.getMenu().removeItem(R.id.action_add_to_favorite);
						popupMenu.getMenu().removeItem(R.id.action_remove_from_favorite);
					}
					
					if(FavoriteManager.INSTANCE.contains(info)) {
						popupMenu.getMenu().removeItem(R.id.action_add_to_favorite);						
					} else {
						popupMenu.getMenu().removeItem(R.id.action_remove_from_favorite);						
					}
					
					popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							
							if(item.getItemId() == R.id.action_add_to_favorite) {
								FavoriteManager.INSTANCE.add(info);
								return true;
							}
							if(item.getItemId() == R.id.action_remove_from_favorite) {
								FavoriteManager.INSTANCE.remove(info);;
								return true;
							}
							if(item.getItemId() == R.id.action_read_direction_setting) {
								setReadDirection(info);
								return true;
							}
							if(item.getItemId() == R.id.action_recreate_thumbnail) {
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
			public TextView itemProgress;
			public TextView itemCount;			
			public ImageView itemMenuBtn;
		}
	}
}
