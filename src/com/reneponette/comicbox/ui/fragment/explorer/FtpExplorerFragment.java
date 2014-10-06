package com.reneponette.comicbox.ui.fragment.explorer;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileLocation;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.StringUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class FtpExplorerFragment extends BaseExplorerFragment {

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String PATH = "path";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static FtpExplorerFragment newInstance(String dropboxPath) {
		FtpExplorerFragment fragment = new FtpExplorerFragment();
		Bundle args = new Bundle();
		args.putString(PATH, dropboxPath);
		fragment.setArguments(args);
		return fragment;
	}

	public FtpExplorerFragment() {
		//
	}

	private Handler handler;
	private Thread runningThread;

	StandardFileSystemManager manager;
	private String host;
	private int port;
	String userId;
	String password;

	@Override
	protected FileInfo onGetFileInfo() {
		String path = getArguments().getString(PATH);
		Entry entry = new Entry();
		entry.path = path;
		return FileInfoDAO.instance().getFileInfo(entry);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		handler = GlobalApplication.instance().getHandler();

		manager = new StandardFileSystemManager();
		port = 22;
		host = "ross.diskstation.me";
		userId = "rene";
		password = "7797";
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		try {
			// Initializes the file manager
			manager.init();
			enumerate();
		} catch (FileSystemException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private boolean goParentDirectory() {

		if (StringUtils.isBlank(curInfo.getEntry().parentPath()) == false) {
			FileInfo parentInfo;
			Entry parentEntry = new Entry();
			parentEntry.isDir = true;
			parentEntry.path = curInfo.getEntry().parentPath();
			parentInfo = new FileInfo(FileLocation.DROPBOX);
			parentInfo.setEntry(parentEntry);

			FileInfo info = new FileInfo(FileLocation.DROPBOX);
			info.setEntry(parentEntry);
			if (getActivity() instanceof FolderViewFragmentListener) {
				((FolderViewFragmentListener) getActivity()).onEntryClicked(info);
				;
			}
			return true;
		}

		return false;
	}

	private void enumerate() {

		if (runningThread != null)
			runningThread.interrupt();

		infoList.clear();
		adapter.notifyDataSetChanged();
		showWaitingDialog();

		runningThread = new Thread() {
			@Override
			public void run() {
				
				if (isInterrupted())
					return;

				
				FileSystemOptions opts = new FileSystemOptions();
				try {
					SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
					SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);
					SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);
					
					String sftpUri = "sftp://" + userId + ":" + password + "@" + host;
					FileObject fo = manager.resolveFile(sftpUri, opts);
					if (fo.isReadable()) {
						FileObject[] children = fo.getChildren();
						for (int i = 0; i < children.length; i++) {
							Logger.e(this, children[i].getName().getRootURI());
							Logger.e(this, children[i].getName().getPath());
							
							FileInfo info = FileInfoDAO.instance().getFileInfo(fo);
							if(info.getMeta().type != FileType.UNKNOWN) {
								infoList.add(info);
							}
						}						
					}
					fo.close();
					
					
				} catch (FileSystemException e) {
					e.printStackTrace();
				}


				handler.post(new Runnable() {

					@Override
					public void run() {
						adapter.notifyDataSetChanged();
						hideWaitingDialog();
					}
				});

				runningThread = null;
			}
		};
		runningThread.start();

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
		return null;
	}

}
