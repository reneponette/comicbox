package com.reneponette.comicbox.utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ZipUtils {
	
	
	public static List<ZipEntry> enumerateAndSortZipEntries(ZipFile zipFile, int limit) {
		int count = 0;
		List<ZipEntry> entries = new ArrayList<ZipEntry>();
		
		for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
			if(limit != 0 && count > limit)
				break;
			entries.add(e.nextElement());
			count++;
		}

		Collections.sort(entries, new Comparator<ZipEntry>() {
			private final Collator collator = Collator.getInstance();

			@Override
			public int compare(ZipEntry lhs, ZipEntry rhs) {
				return collator.compare(lhs.getName(), rhs.getName());
			}
		});
		
		return entries;
	}
}
