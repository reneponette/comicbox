package com.reneponette.comicbox.model;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.artifex.mupdfdemo.MuPDFCore;
import com.reneponette.comicbox.utils.StringUtils;

public class PageInfo {
	public static enum PageType {
		IMG_ZIP, IMG_PDF, IMG_FILE, END, AD
	}
	public static enum PageBuildType {
		WHOLE, LEFT, RIGHT
	}
	
	
	PageType type;
	PageBuildType buildType;
	
	//ZIP
	ZipFile zipFile;
	ZipEntry zipEntry;
	
	//Local Cache
	File file;
	
	//PDF
	String pdfName;
	MuPDFCore pdfCore;
	int pdfIndex;
	
	public PageInfo() {
		
	}
	public PageInfo(ZipFile zipFile, ZipEntry zipEntry) {
		this.type = PageType.IMG_ZIP;
		this.zipFile = zipFile;
		this.zipEntry = zipEntry;
	}
	public PageInfo(String pdfName, MuPDFCore core, int index) {
		this.type = PageType.IMG_PDF;
		this.pdfName = pdfName;
		this.pdfCore = core;
		this.pdfIndex = index;
	}
	public PageInfo(File file) {
		this.type = PageType.IMG_FILE;
		this.file = file;
	}
	
	public String getName() {
		String name;
		switch (type) {
		case IMG_ZIP:
			name = zipEntry.getName();
			break;
		case IMG_PDF:
			name = pdfName + "/" + pdfIndex;
			break;
		case IMG_FILE:
			name = file.getName();
			break;
		default:
			name = "";
			break;
		}
		return name;
	}
	
	@Override
	public String toString() {
		return StringUtils.isBlank(getName()) ? "" : getName() + "/" + buildType.toString();
	}
	
	public PageBuildType getBuildType() {
		return buildType;
	}
	public void setBuildType(PageBuildType type) {
		this.buildType = type;
	}
	
	public PageType getType() {
		return type;
	}
	public void setType(PageType type) {
		this.type = type;
	}
	
	public ZipFile getZipFile() {
		return zipFile;
	}	
	public ZipEntry getZipEntry() {
		return zipEntry;
	}
	public File getFile() {
		return file;
	}
	public MuPDFCore getPdfCore() {
		return pdfCore;
	}
	public int getPdfIndex() {
		return pdfIndex;
	}
}
