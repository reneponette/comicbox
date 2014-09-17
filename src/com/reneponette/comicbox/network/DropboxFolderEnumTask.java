package com.reneponette.comicbox.network;

import java.util.List;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.utils.ToastUtils;

public class DropboxFolderEnumTask extends AsyncTask<String, Void, List<String>> {
	
	DropboxAPI<AndroidAuthSession> mApi;	
	
	public DropboxFolderEnumTask(DropboxAPI<AndroidAuthSession> api) {
		mApi = api;
	}

	@Override
	protected List<String> doInBackground(String... arg0) {
        Entry dirent;
		try {
			dirent = mApi.metadata("/", 1000, null, true, null);
			if (!dirent.isDir || dirent.contents == null) {
				// It's not a directory, or there's nothing in it
				ToastUtils.toast("File or empty directory");
			}
			
			
//			ArrayList<Entry> thumbs = new ArrayList<Entry>();
			for (Entry ent: dirent.contents) {
				if (ent.thumbExists) {
//					thumbs.add(ent);
					Log.e("dropbox", ent.fileName());
				}
			}
			
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(List<String> result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
	}
}
