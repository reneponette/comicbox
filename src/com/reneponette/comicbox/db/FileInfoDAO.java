package com.reneponette.comicbox.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.util.Log;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.db.FileInfo.LocationType;
import com.reneponette.comicbox.model.FileMeta;

public class FileInfoDAO extends BaseDAO<FileInfo> {

	public static final String TABLE_NAME = "file_info";

	private static FileInfoDAO instance;

	public static FileInfoDAO instance() {
		if (instance == null) {
			synchronized (FileInfoDAO.class) {
				if (instance == null) {
					instance = new FileInfoDAO();
				}
			}
		}
		return instance;
	}

	private FileInfoDAO() {
		super(TABLE_NAME);
	}

	@Override
	public String getPrimaryColumnName() {
		return FileInfo.COL_KEY;
	}

	@Override
	public FileInfo populateObject(Cursor cursor) {
		FileInfo info = new FileInfo(
				LocationType.valueOf(cursor.getString(cursor.getColumnIndex(FileInfo.COL_LOCATION))));
		info.setKey(cursor.getString(cursor.getColumnIndex(FileInfo.COL_KEY)));
		info.setPath(cursor.getString(cursor.getColumnIndex(FileInfo.COL_PATH)));
		info.setMeta(FileMeta.createFromJSONString(cursor.getString(cursor.getColumnIndex(FileInfo.COL_META))));
		return info;
	}

	public FileInfo mergeObject(Cursor cursor, FileInfo info) {
		info.setLocation(LocationType.valueOf(cursor.getString(cursor.getColumnIndex(FileInfo.COL_LOCATION))));
		info.setKey(cursor.getString(cursor.getColumnIndex(FileInfo.COL_KEY)));
		info.setPath(cursor.getString(cursor.getColumnIndex(FileInfo.COL_PATH)));
		info.setMeta(FileMeta.createFromJSONString(cursor.getString(cursor.getColumnIndex(FileInfo.COL_META))));
		return info;
	}

	// /////////////////////////////////////////////

	private FileInfo newFileInfo(Object obj) {
		FileInfo info;

		if (obj instanceof File) {
			info = new FileInfo(LocationType.LOCAL);
			info.setFile((File) obj);
		} else if (obj instanceof Entry) {
			info = new FileInfo(LocationType.DROPBOX);
			info.setEntry((Entry) obj);
		} else {
			info = new FileInfo(LocationType.UNKNOWN);
		}

		return info;
	}

	public FileInfo getFileInfo(String key) {

		Cursor cursor = getDB().query(tableName, null, getPrimaryColumnName() + "=\"" + key + "\"", null, null, null, null);
		if (cursor == null) {
			// 기본 객체 반환
			return null;
		}

		cursor.moveToFirst();

		if (cursor.getCount() == 0) {
			if (!cursor.isClosed()) {
				cursor.close();
			}
			return null;
		}

		FileInfo info = populateObject(cursor);
		
		if (!cursor.isClosed()) {
			cursor.close();
		}		

		if (info.getLocation() == LocationType.DROPBOX) {
			Entry entry = new Entry();
			entry.path = info.getPath();
			entry.isDir = true;
			info.setEntry(entry);
		} else if (info.getLocation() == LocationType.LOCAL) {
			info.setFile(new File(info.getPath()));
		} else {
			//LocationType.UNKNOWN
			return null;
		}

		return info;
	}

	public FileInfo getFileInfo(Object obj) {
		FileInfo info = newFileInfo(obj);

		Cursor cursor = getDB().query(tableName, null, getPrimaryCondition(info), null, null, null, null);
		if (cursor == null) {
			// 기본 객체 반환
			return info;
		}

		cursor.moveToFirst();

		if (cursor.getCount() == 0) {
			if (!cursor.isClosed())
				cursor.close();
			// 기본 객체 반환
			return info;
		}

		mergeObject(cursor, info);

		if (!cursor.isClosed()) {
			cursor.close();
		}

		return info;
	}

	// public List<FileInfo> getFileInfos() {
	// List<FileInfo> fileInfos = new ArrayList<FileInfo>();
	// Cursor cursor = get(null, null);
	//
	// if (cursor == null || cursor.getCount() == 0) {
	// return fileInfos;
	// }
	//
	// do {
	// FileInfo info;
	// try {
	// info = populateObject(cursor);
	// fileInfos.add(info);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// } while (cursor.moveToNext());
	//
	// return fileInfos;
	// }
}
