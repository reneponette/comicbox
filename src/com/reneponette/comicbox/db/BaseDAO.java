package com.reneponette.comicbox.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

public abstract class BaseDAO<T> {

	protected String tableName;

	public BaseDAO(String tableName) {
		this.tableName = tableName;
	}

	public SQLiteDatabase getDB() {
		return DBOpenHelper.getInstance().getDB();
	}

	public abstract String getPrimaryColumnName();

	public abstract T populateObject(Cursor cursor) throws Exception;

	public String getPrimaryCondition(DatabaseStorable<T> t) {
		return getPrimaryColumnName() + "=" + t.getPrimaryKey();
	}

	// insert
	public void insertOrUpdate(DatabaseStorable<T> t) {
		ContentValues values = t.toContentValues();
		try {
			insertOrThrow(values);
		} catch (SQLiteConstraintException e) {
			update(t);
		}
	}

	public long insertOrThrow(ContentValues values) {
		return getDB().insertOrThrow(tableName, null, values);
	}

	public long insert(DatabaseStorable<T> t) {
		return insert(t.toContentValues());
	}

	public long insert(ContentValues values) {
		return getDB().insert(tableName, null, values);
	}

	// update
	public int update(long id, ContentValues contentValues) {
		String condition = getPrimaryColumnName() + "=" + id;
		return getDB().update(tableName, contentValues, condition, null);
	}

	public int update(DatabaseStorable<T> t) {
		String condition = getPrimaryColumnName() + "=" + t.getPrimaryKey();
		ContentValues values = t.toContentValues();
		return getDB().update(tableName, values, condition, null);
	}

	// delete
	public int delete(long id) {
		String condition = getPrimaryColumnName() + "=" + id;
		return getDB().delete(tableName, condition, null);
	}

	// get
	public Cursor getAll(String[] columns) {
		Cursor cursor = getDB().query(tableName, columns, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}
	
	public Cursor getAll(String[] columns, String orderBy) {
		Cursor cursor = getDB().query(tableName, columns, null, null, null, null, orderBy);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}
	

	public Cursor get(String[] columns, long id) {
		String condition = getPrimaryColumnName() + "=" + id;
		Cursor cursor = getDB().query(true, tableName, columns, condition, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}

	public Cursor get(String[] columns, String condition) {
		Cursor cursor = getDB().query(true, tableName, columns, condition, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}
	
	public Cursor get(String[] columns, String condition, String orderBy) {
		Cursor cursor = getDB().query(true, tableName, columns, condition, null, null, null, orderBy, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		return cursor;
	}
	

	// replace
	public long replace(ContentValues values) {
		return getDB().replace(tableName, null, values);
	}

}
