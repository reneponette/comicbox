package com.reneponette.comicbox.db;

import com.reneponette.comicbox.application.GlobalApplication;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper {
	 
    private static final String DATABASE_NAME = "data.db";
    private static final int DATABASE_VERSION = 1;
    private static volatile DBOpenHelper instance;
    
    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDB;
    private Context mCtx;
 
    //DatabaseAdapter
    private class DatabaseHelper extends SQLiteOpenHelper{
 
        // 생성자
        public DatabaseHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
        }
 
        // 최초 DB를 만들때 한번만 호출된다.
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DataBases._CREATE);
 
        }
 
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            db.execSQL("DROP TABLE IF EXISTS "+DataBases.CreateDB._TABLENAME);
//            onCreate(db);
        }
    }
    
    public static final DBOpenHelper getInstance() {
		if (instance == null || instance.mDB == null || !instance.mDB.isOpen()) {
			synchronized (DBOpenHelper.class) {
				if (instance == null || instance.mDB == null || !instance.mDB.isOpen()) {
					instance = new DBOpenHelper(GlobalApplication.instance());
					instance.open();
				}
			}
		}
		return instance;
    }
    
 
    public DBOpenHelper(Context context){
        this.mCtx = context;
    }
 
    public DBOpenHelper open() throws SQLException{
        mDBHelper = new DatabaseHelper(mCtx, DATABASE_NAME, null, DATABASE_VERSION);
        mDB = mDBHelper.getWritableDatabase();
        return this;
    }
 
    public void close(){
        mDB.close();
    }
    
    public SQLiteDatabase getDB() {
    	return mDB;
    }
 
}