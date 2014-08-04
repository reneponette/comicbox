package com.reneponette.comicbox.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;

public enum FavoriteManager {
	INSTANCE;

	Set<String> set;
	List<FileInfo> list;
	SharedPreferences pref;

	private FavoriteManager() {
		list = new ArrayList<FileInfo>();
		set = new HashSet<String>();

		pref = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.instance());
		pref.getStringSet("favorite_set", set);

		refreshList();
	}

	private void refreshList() {
		list.clear();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Log.e(this.getClass().getName(), "key = " + key);
			FileInfo info = FileInfoDAO.instance().getFileInfo(key);
			if (info != null)
				list.add(info);
		}
	}

	public void add(String key) {
		set.add(key);
		pref.edit().putStringSet("favorite_set", set).commit();
		refreshList();
	}

	public void remove(String key) {
		set.remove(key);
		pref.edit().putStringSet("favorite_set", set).commit();		
		refreshList();
	}

	public List<FileInfo> getList() {
		return list;
	}
}
