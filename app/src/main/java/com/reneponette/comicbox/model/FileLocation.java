package com.reneponette.comicbox.model;

import com.reneponette.comicbox.utils.StringUtils;

public class FileLocation {
	public static final FileLocation UNKNOWN = new FileLocation("UNKNOWN");
	public static final FileLocation LOCAL = new FileLocation("LOCAL");
	public static final FileLocation DROPBOX = new FileLocation("DROPBOX");
	public static final FileLocation GOOGLE = new FileLocation("GOOGLE");

	private String location;

	public FileLocation(String location) {
		this.location = location;
	}

	public static FileLocation toFileLocation(String location) {
		if ("LOCAL".equals(location))
			return LOCAL;
		if ("DROPBOX".equals(location))
			return DROPBOX;
		if ("GOOGLE".equals(location))
			return GOOGLE;
		if (!StringUtils.isBlank(location)
				&& isCustomLocation(location)) {
			return new FileLocation(location);
		}
		return UNKNOWN;
	}
	
	public static boolean isCustomLocation(String location) {
		return (location.startsWith("ftp") || location.startsWith("ftps") || location.startsWith("sftp"));
	}

	@Override
	public String toString() {
		return location;
	}
}