package com.reneponette.comicbox.db;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.dropbox.client2.DropboxAPI.Entry;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.model.FileLocation;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.FileType;
import com.reneponette.comicbox.utils.StringUtils;

import java.io.File;

public class FileInfo implements DatabaseStorable<FileInfo>, Parcelable {

    // 디비에 저장되는 정보
    private String key;
    private FileLocation location;
    private String path;
    private FileMeta meta = new FileMeta();

    // 디비에 저장되지 않는정보
    public String focusName;
    public int indexInParent;

    public FileInfo(FileLocation location) {
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
        contentValues.put(COL_LOCATION, getLocation().toString());
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
        return StringUtils.getName(path);
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

    public boolean fill(File file) {
        this.location = FileLocation.LOCAL;
        this.path = file.getAbsolutePath();
        this.key = toKey();

        if (file.isDirectory()) {
            meta.type = FileType.DIRECTORY;
            meta.childCount = file.list() != null ? file.list().length : 0;
            return true;
        } else {
            return setMetaTypeFromFilename(file.getName(), false);
        }
    }

    public boolean fill(Entry entry) {
        this.location = FileLocation.DROPBOX;
        this.path = entry.path;
        this.key = toKey();

        if (entry.isDir) {
            meta.type = FileType.DIRECTORY;
            meta.childCount = entry.contents != null ? entry.contents.size() : 0;
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

    public FileLocation getLocation() {
        return location;
    }

    public void setLocation(FileLocation type) {
        this.location = type;
    }

    public FileMeta getMeta() {
        return meta;
    }

    public void setMeta(FileMeta meta) {
        this.meta = meta;
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
        dest.writeString(location.toString());
        dest.writeParcelable(meta, PARCELABLE_WRITE_RETURN_VALUE);


        dest.writeString(focusName);
        dest.writeInt(indexInParent);
    }

    public static final Parcelable.Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel source) {
            FileInfo obj = new FileInfo();
            obj.key = source.readString();
            obj.path = source.readString();
            obj.location = FileLocation.toFileLocation(source.readString());
            obj.meta = source.readParcelable(FileMeta.class.getClassLoader());

            obj.focusName = source.readString();
            obj.indexInParent = source.readInt();

            if (obj.location == FileLocation.LOCAL) {
                obj.fill(new File(obj.path));
            } else {
                Entry entry = new Entry();
                entry.path = obj.path;
                entry.isDir = obj.meta.type == FileType.DIRECTORY;
                obj.fill(entry);
            }

            return obj;
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[size];
        }

    };

}
