package com.reneponette.comicbox.db;

import android.content.ContentValues;

public interface DatabaseStorable<T> {

	public abstract ContentValues toContentValues();
	public abstract String getPrimaryKey();
	
}
