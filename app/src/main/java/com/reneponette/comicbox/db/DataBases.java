package com.reneponette.comicbox.db;


public final class DataBases {
	public static final String _TABLENAME = "file_info";
	public static final String KEY = "key";
	public static final String PATH = "path";
	public static final String TYPE = "type";
	public static final String META = "meta";
	
	public static final String _CREATE = "create table " + _TABLENAME + "(" +
			KEY + " text primary key, " + 
			PATH + " text," + 
			TYPE + " text not null, " +
			META + " text);";	
}
