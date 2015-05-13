package com.reneponette.comicbox.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {
	public static String getExtension(String filename) {
		String filenameArray[] = filename.split("\\.");

		String extension = filenameArray[filenameArray.length - 1];
		return extension;
	}

	public static String getName(String path) {
		if(isBlank(path))
			return "";
		
		if("/".equals(path))
			return path;
		
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex == -1)
			return "";
		
		return path.substring(lastIndex+1);
	}

	public static boolean isBlank(String str) {
		if (str == null || str.isEmpty())
			return true;

		return false;
	}

	public static String getMD5(String str) {
		String MD5 = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte byteData[] = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}
			MD5 = sb.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			MD5 = null;
		}
		return MD5;
	}

	public static boolean isImageFileExt(String filename) {
		return "jpg".equalsIgnoreCase(getExtension(filename)) || "jpeg".equalsIgnoreCase(getExtension(filename))
				|| "gif".equalsIgnoreCase(getExtension(filename)) || "png".equalsIgnoreCase(getExtension(filename));
	}

	public static String getParentPath(String path) {
		if (isBlank(path))
			return null;

		// 끝이 /로 끝나면 제거
		if (path.lastIndexOf('/') == path.length() - 1) {
			path = path.substring(0, path.length() - 1);
		}

		int lastIndex = path.lastIndexOf('/');
		if (lastIndex != -1) {
			path = path.substring(0, lastIndex);
		}

		return path;
	}
}
