package com.reneponette.comicbox.db;

import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.utils.StringUtils;

public class FileInfo implements DatabaseStorable<FileInfo>, Parcelable {

	public enum LocationType {
		LOCAL, DROPBOX;
	}

	//디비에 저장되는 정보
	private String key;
	private String path;
	private LocationType type;
	private FileMeta meta = new FileMeta();

	//디비에 저장되지 않는정보
	private File file;
	private Entry entry;
	private boolean parentDir;
	public String focusName;
	public int indexInParent;


	public FileInfo(LocationType type) {
		this.type = type;
	}
	
	private FileInfo() {
		
	}

	private String toKey() {
		return StringUtils.getMD5(getType() + "/" + getPath());
	}

	// //////////////////////////////////////////////////////

	public static final String COL_KEY = "key";
	public static final String COL_TYPE = "type";
	public static final String COL_PATH = "path";
	public static final String COL_META = "meta";

	@Override
	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		contentValues.put(COL_KEY, getKey());
		contentValues.put(COL_TYPE, getType().name());
		contentValues.put(COL_PATH, getPath());
		if (getMeta() != null)
			contentValues.put(COL_META, getMeta().toJSONString());

		return contentValues;
	}

	@Override
	public String getPrimaryKey() {
		return "\"" + getKey() + "\"";
	}

	// ///////////////////////////////////////////////////////

	public String getName() {
		String name = "";
		switch (type) {
		case LOCAL:
			name = file == null ? "" : file.getName();
			break;
		case DROPBOX:
			name = entry == null ? "" : entry.fileName();
			break;
		default:
			break;
		}
		return name;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}	

	public File getFile() {
		return file;
	}

	public boolean setFile(File file) {
		this.file = file;
		this.type = LocationType.LOCAL;
		this.path = file.getAbsolutePath();
		this.key = toKey();

		if (file.isDirectory()) {
			meta.type = FileType.DIRECTORY;
			return true;
		} else {
			return setMetaTypeFromFilename(file.getName());
		}
	}

	public Entry getEntry() {
		return entry;
	}

	public boolean setEntry(Entry entry) {
		this.entry = entry;
		this.type = LocationType.DROPBOX;
		this.path = entry.path;
		this.key = toKey();

		if (entry.isDir) {
			meta.type = FileType.DIRECTORY;
			return true;
		} else {
			return setMetaTypeFromFilename(entry.fileName());
		}

	}

	private boolean setMetaTypeFromFilename(String filename) {
		String extension = StringUtils.getExtension(filename);
		if (extension.equalsIgnoreCase("zip")) {
			meta.type = FileType.ZIP;
			return true;
		}
		if (extension.equalsIgnoreCase("pdf")) {
			meta.type = FileType.PDF;
			return true;
		}
		if (extension.equalsIgnoreCase("jpg")) {
			meta.type = FileType.JPG;
			return true;
		}		

		meta.type = FileType.UNKNOWN;
		return false;
	}
	
	public File getCacheDir() {
		File comicsCacheDir = new File(C.COMICS_CACHE_ROOT);
		return new File(comicsCacheDir, toKey());
	}
	
	public File getCacheOuputFile() {
		return new File(getCacheDir(), getName());
	}

	public LocationType getType() {
		return type;
	}

	public void setType(LocationType type) {
		this.type = type;
	}

	public FileMeta getMeta() {
		return meta;
	}

	public void setMeta(FileMeta meta) {
		this.meta = meta;
	}
	
	public boolean isParentDir() {
		return parentDir;
	}

	public void setParentDir(boolean parentDir) {
		this.parentDir = parentDir;
	}	
	
	////////////////////////////////////////////////////////////////
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(path);
		dest.writeSerializable(type);
		dest.writeString(meta.toJSONString());
		
		dest.writeString(focusName);
		dest.writeInt(indexInParent);
	}

	public static final Parcelable.Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
		@Override
		public FileInfo createFromParcel(Parcel source) {
			FileInfo obj = new FileInfo();
			obj.key = source.readString();
			obj.path = source.readString();
			obj.type = (LocationType) source.readSerializable();
			obj.meta = FileMeta.createFromJSONString(source.readString());
			
			obj.focusName = source.readString();
			obj.indexInParent = source.readInt();
			
			return obj;
		}

		@Override
		public FileInfo[] newArray(int size) {
			return new FileInfo[size];
		}

	};	

}
