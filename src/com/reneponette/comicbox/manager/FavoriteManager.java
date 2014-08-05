package com.reneponette.comicbox.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.db.FileInfoDAO;

public enum FavoriteManager {
	INSTANCE;
	
	Set<String> set;
	List<FileInfo> list;
	SharedPreferences pref;
	OnFavoriteChangedListener listener;

	public interface OnFavoriteChangedListener {
		public void onFavoriteChanged();
	}

	public void setOnFavoriteChangedListener(OnFavoriteChangedListener l) {
		listener = l;
	}

	private FavoriteManager() {
		pref = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.instance());

		list = new ArrayList<FileInfo>();
		
		try {
			JSONArray jsonArray = new JSONArray(pref.getString("favorites", "[]"));
			set = new HashSet<String>();
			for(int i=0 ; i<jsonArray.length() ; i++) {
				set.add(jsonArray.getString(i));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		
		refreshList();
	}

	private void refreshList() {
		list.clear();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			FileInfo info = FileInfoDAO.instance().getFileInfo(key);

			if (info.getType() == LocationType.DROPBOX) {
				Entry entry = new Entry();
				entry.path = info.getPath();
				info.setEntry(entry);
			}
			if (info.getType() == LocationType.LOCAL) {
				info.setFile(new File(info.getPath()));
			}

			if (info != null)
				list.add(info);
		}

		if (listener != null)
			listener.onFavoriteChanged();
	}
	
	private void save() {
		
		JSONArray jsonArray = new JSONArray();
		
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			jsonArray.put(key);
		}
		
		pref.edit().putString("favorites", jsonArray.toString()).commit();		
	}

	public void add(FileInfo info) {
		FileInfoDAO.instance().insertOrUpdate(info);
		set.add(info.getKey());
		save();
		refreshList();
	}

	public void remove(FileInfo info) {
		FileInfoDAO.instance().insertOrUpdate(info);
		set.remove(info.getKey());
		save();
		refreshList();
	}
	
	public boolean contains(FileInfo info) {
		return set.contains(info.getKey());
	}
	
	public List<FileInfo> getList() {
		return list;
	}
}
