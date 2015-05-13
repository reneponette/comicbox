package com.reneponette.comicbox.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import com.reneponette.comicbox.utils.StringUtils;

public class FileMeta implements Parcelable {

	public static enum FileType {
		ZIP, PDF, JPG, DIRECTORY, AD, UNKNOWN;
	}

	public static enum ReadDirection {
		LTR, RTL, NOTSET;
	}
	
	public FileType type = FileType.UNKNOWN;
	public int pagesPerScan = 0; //0은 한번도 읽은적 없는 경우
	public int lastPagesPerScan = 0;
	public String cachePath;
	public String coverPath;
	public int coverColor = -1;
	public int lastTotalPageCount;
	public int lastReadPageIndex = -1; // -1은 한번도 읽은적 없는 경우
	public ReadDirection lastReadDirection = ReadDirection.NOTSET;
	public ReadDirection readDirection = ReadDirection.NOTSET;
	public boolean autocrop;
	public int childCount;

	public static FileMeta createFromJSONString(String jsonString) {
		if (StringUtils.isBlank(jsonString))
			return null;

		try {
			JSONObject obj = new JSONObject(jsonString);
			return createFromJSON(obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static FileMeta createFromJSON(JSONObject obj) {

		if (obj == null)
			return null;

		FileMeta result = new FileMeta();
		result.type = FileType.valueOf(obj.optString("type", FileType.UNKNOWN.name()));
		result.pagesPerScan = obj.optInt("pagesPerScan");
		result.lastPagesPerScan = obj.optInt("lastPagesPerScan");
		result.cachePath = obj.optString("cachePath");
		result.coverPath = obj.optString("coverPath");
		result.coverColor = obj.optInt("coverColor");
		result.lastTotalPageCount = obj.optInt("lastTotalPageCount");
		result.lastReadPageIndex = obj.optInt("lastReadPageIndex", -1);
		result.lastReadDirection = ReadDirection
				.valueOf(obj.optString("lastReadDirection", ReadDirection.NOTSET.name()));
		result.readDirection = ReadDirection.valueOf(obj.optString("readDirection", ReadDirection.NOTSET.name()));
		result.autocrop = obj.optBoolean("autocrop");
		result.childCount = obj.optInt("childCount");

		return result;
	}

	public String toJSONString() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", type.name());
			obj.put("pagesPerScan", pagesPerScan);
			obj.put("lastPagesPerScan", lastPagesPerScan);
			if(cachePath != null)
				obj.put("cachePath", cachePath);
			if(coverPath != null)
				obj.put("coverPath", coverPath);
			obj.put("coverColor", coverColor);
			obj.put("lastTotalPageCount", lastTotalPageCount);
			obj.put("lastReadPageIndex", lastReadPageIndex);
			obj.put("lastReadDirection", lastReadDirection.name());
			obj.put("readDirection", readDirection.name());
			obj.put("autocrop", autocrop);
			obj.put("childCount", childCount);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return obj.toString();
	}

	// //////////////parcelable///////////////////

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(type.name());
		dest.writeInt(pagesPerScan);
		dest.writeInt(lastPagesPerScan);
		dest.writeString(coverPath);
		dest.writeInt(coverColor);
		dest.writeInt(lastTotalPageCount);
		dest.writeInt(lastReadPageIndex);
		dest.writeString(lastReadDirection.name());
		dest.writeString(readDirection.name());
		dest.writeString(Boolean.toString(autocrop));
		dest.writeInt(childCount);
	}

	public static final Parcelable.Creator<FileMeta> CREATOR = new Parcelable.Creator<FileMeta>() {
		public FileMeta createFromParcel(Parcel source) {
			FileMeta obj = new FileMeta();
			obj.type = FileType.valueOf(source.readString());
			obj.pagesPerScan = source.readInt();
			obj.lastPagesPerScan = source.readInt();
			obj.coverPath = source.readString();
			obj.coverColor = source.readInt();
			obj.lastTotalPageCount = source.readInt();
			obj.lastReadPageIndex = source.readInt();
			obj.lastReadDirection = ReadDirection.valueOf(source.readString());
			obj.readDirection = ReadDirection.valueOf(source.readString());
			obj.autocrop = Boolean.parseBoolean(source.readString());
			obj.childCount = source.readInt();
			return obj;
		}

		public FileMeta[] newArray(int size) {
			return new FileMeta[size];
		}
	};

}
