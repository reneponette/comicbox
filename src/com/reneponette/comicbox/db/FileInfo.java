package com.reneponette.comicbox.db;

import java.io.File;

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
		UNKNOWN, LOCAL, DROPBOX;
	}

	// 디비에 저장되는 정보
	private String key;
	private String path;
	private LocationType location;
	private FileMeta meta = new FileMeta();

	// 디비에 저장되지 않는정보
	private File file;
	private Entry entry;
	public String focusName;
	public int indexInParent;

	public FileInfo(LocationType location) {
		this.location = location;
	}

	private FileInfo() {

	}

	private String toKey() {
		return StringUtils.getMD5(getLocation() + "/" + getPath());
	}

	// //////////////////////////////////////////////////////

	public static final String COL_KEY = "key";
	public static final String COL_LOCATION = "type";
	public static final String COL_PATH = "path";
	public static final String COL_META = "meta";

	@Override
	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		contentValues.put(COL_KEY, getKey());
		contentValues.put(COL_LOCATION, getLocation().name());
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
		switch (location) {
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
		this.location = LocationType.LOCAL;
		this.path = file.getAbsolutePath();
		this.key = toKey();

		if (file.isDirectory()) {
			meta.type = FileType.DIRECTORY;
			return true;
		} else {
			return setMetaTypeFromFilename(file.getName(), false);
		}
	}

	public Entry getEntry() {
		return entry;
	}

	public boolean setEntry(Entry entry) {
		this.entry = entry;
		this.location = LocationType.DROPBOX;
		this.path = entry.path;
		this.key = toKey();

		if (entry.isDir) {
			meta.type = FileType.DIRECTORY;
			return true;
		} else {
			return setMetaTypeFromFilename(entry.fileName(), true);
		}

	}

	private boolean setMetaTypeFromFilename(String filename, boolean isStreaming) {
		String extension = StringUtils.getExtension(filename);
		if (extension.equalsIgnoreCase("zip")) {
			meta.type = FileType.ZIP;
			return true;
		}
		if (extension.equalsIgnoreCase("pdf") && isStreaming == false) {
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

	public LocationType getLocation() {
		return location;
	}

	public void setLocation(LocationType type) {
		this.location = type;
	}

	public FileMeta getMeta() {
		return meta;
	}

	public void setMeta(FileMeta meta) {
		this.meta = meta;
	}

	public int getChildCount() {
		if (location == LocationType.LOCAL && file != null) {
			if (file.list() != null)
				return file.list().length;
		}
		if (location == LocationType.DROPBOX && entry != null) {
			if (entry.contents != null)
				return entry.contents.size();
		}
		return 0;
	}

	// //////////////////////////////////////////////////////////////

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(path);
		dest.writeSerializable(location);
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
			obj.location = (LocationType) source.readSerializable();
			obj.meta = FileMeta.createFromJSONString(source.readString());

			obj.focusName = source.readString();
			obj.indexInParent = source.readInt();

			if (obj.location == LocationType.LOCAL) {
				obj.setFile(new File(obj.path));
			} else {
				Entry entry = new Entry();
				entry.path = obj.path;
				obj.setEntry(entry);
			}

			return obj;
		}

		@Override
		public FileInfo[] newArray(int size) {
			return new FileInfo[size];
		}

	};

}
