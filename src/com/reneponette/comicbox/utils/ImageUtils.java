package com.reneponette.comicbox.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.artifex.mupdfdemo.MuPDFCore;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.reneponette.comicbox.application.GlobalApplication;
import com.reneponette.comicbox.cache.BitmapCache;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageBuildType;

/**
 * @author rene
 * 
 */
public class ImageUtils {

	/*--------------페이지/프리뷰 비트맵 가져오기--------------------------------------------*/

	/**
	 * 드롭박스 스트리밍시 캐시에서 JPG파일별로 읽어오는 경우 호출됨
	 * 
	 * @param pageInfo
	 * @param preview
	 * @return
	 */
	public static Bitmap getBitmap(File file, PageBuildType buildType, boolean autocrop, boolean preview) {

		if (file == null)
			return null;

		Bitmap bm = null;
		if (preview) {
			BitmapFactory.Options opts = new Options();
			opts.inSampleSize = 4;
			bm = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		} else {
			bm = BitmapFactory.decodeFile(file.getAbsolutePath());
		}

		if (bm == null)
			return null;

		return getProcessedBitmap(bm, buildType, autocrop);
	}

	/**
	 * Zip 파일 내에서 이미지 가져오기
	 * 
	 * @param zipFile
	 * @param entry
	 * @param buildType
	 * @param preview
	 * @return
	 */
	public static Bitmap getBitmap(ZipFile zipFile, ZipEntry entry, PageBuildType buildType, boolean autocrop,
			boolean preview) {
		Bitmap bm = null;
		try {
			InputStream is = zipFile.getInputStream(entry);
			BitmapFactory.Options opts = new Options();
			if (preview) {
				opts.inSampleSize = 4;
			}
			bm = BitmapFactory.decodeStream(is, null, opts);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (bm == null)
			return null;

		return getProcessedBitmap(bm, buildType, autocrop);
	}
	
	
	/**
	 * PDF 파일에서 이미지 가져오기
	 * @param core
	 * @param pi
	 * @param autocrop
	 * @param preview
	 * @return
	 */
	public static Bitmap getBitmap(MuPDFCore core, int pdfIndex, PageBuildType buildType, boolean autocrop,
			boolean preview) {
		Bitmap bm = null;
		
		PointF point = core.getPageSize(pdfIndex);
		int w = (int) point.x;
		int h = (int) point.y;
		
		bm = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		core.drawPage(bm, pdfIndex, w, h, 0, 0, w, h);
		
		if (bm == null)
			return null;
		
		if(preview) {
			bm = Bitmap.createScaledBitmap(bm, 200, 300, false);
		}
		
		return getProcessedBitmap(bm, buildType, autocrop);
	}

	/**
	 * @param bitmap
	 * @param buildType
	 * @return
	 */
	private static Bitmap getProcessedBitmap(Bitmap bitmap, PageBuildType buildType, boolean autocrop) {

		if (bitmap.getWidth() < bitmap.getHeight())
			buildType = PageBuildType.WHOLE;

		Bitmap resultBitmap = null;
		switch (buildType) {
		case LEFT:
			resultBitmap = cutBitmapInHalf(bitmap, false);
			bitmap.recycle();
			break;
		case RIGHT:
			resultBitmap = cutBitmapInHalf(bitmap, true);
			bitmap.recycle();
			break;

		default:
			resultBitmap = bitmap;
			break;
		}

		if (autocrop)
			resultBitmap = removeMargins(resultBitmap, 350, 200, 350, 200);
		return resultBitmap;
	}

	/*------------------페이지퍼스캔--------------------------------------------*/

	/**
	 * @param zipFile
	 * @param zipEntry
	 * @return
	 */
	public static int pagesPerScan(ZipFile zipFile, ZipArchiveEntry zipEntry) {
		try {
			InputStream is = zipFile.getInputStream(zipEntry);
			BitmapFactory.Options opt = new Options();
			opt.inSampleSize = 16;
			Bitmap bm = BitmapFactory.decodeStream(is, null, opt);

			if (bm.getWidth() > bm.getHeight()) {
				return 2;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/**
	 * 로컬 Zip파일을 볼 경우, 한장에 몇 페이지가 스캔되었는지 자동결정을 위한 함수
	 * 
	 * @param src
	 * @return
	 */
	public static int pagesPerScan(File src) {
		try {

			ZipFile zipFile = new ZipFile(src);
			int count = 6;
			int i = 0;
			int twoPageCount = 0;
			for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
				if (i >= count)
					break;

				ZipEntry zipEntry = e.nextElement();
				String name = zipEntry.getName();

				if (name.indexOf(".") == -1)
					continue;
				if (name.contains("__MACOSX"))
					continue;

				if ("jpg".equalsIgnoreCase(StringUtils.getExtension(name))) {
					InputStream is = zipFile.getInputStream(zipEntry);
					BitmapFactory.Options opt = new Options();
					opt.inSampleSize = 4;
					Bitmap bm = BitmapFactory.decodeStream(is, null, opt);
					if (bm.getWidth() > bm.getHeight()) {
						twoPageCount++;
					}
					i++;
				}
			}
			zipFile.close();

			return (float) twoPageCount / count > 0.5 ? 2 : 1;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/*------------------커버 가져오기--------------------------------------------*/

	/**
	 * @param src
	 * @return
	 */
	public static Bitmap extractCoverFromJpg(File src) {
		BitmapFactory.Options opts = new Options();
		opts.inSampleSize = 4;
		Bitmap bitmap = BitmapFactory.decodeFile(src.getAbsolutePath(), opts);
		return removeMargins(bitmap, 350, 200, 350, 200);
	}

	/**
	 * @param c
	 * @param src
	 * @return
	 */
	public static Bitmap extractCoverFromPdf(Context c, File src) {
		try {
			MuPDFCore core = new MuPDFCore(c, src.getAbsolutePath());
			Bitmap bm = Bitmap.createBitmap(400, 600, Config.ARGB_8888);
			core.countPages();
			core.drawPage(bm, 0, 400, 600, 0, 0, 400, 600);

			return removeMargins(bm, 350, 200, 350, 200);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Zip 파일 내용을 이름순으로 정렬 후, 첫번째 이미지를 썸네일로 만들고 반환함
	 * 
	 * @param src
	 * @return
	 */
	public static Bitmap extractCoverFromZip(File src) {

		try {
			ZipFile zipFile;
			zipFile = new ZipFile(src);

			List<ZipEntry> entries = ZipUtils.enumerateAndSortZipEntries(zipFile, 0);

			for (ZipEntry zipEntry : entries) {

				String zipEntryName = zipEntry.getName();
				if (zipEntryName.indexOf(".") == -1)
					continue;

				String extension = zipEntryName.substring(zipEntryName.indexOf(".") + 1);

				if (extension.equalsIgnoreCase("jpg")) {
					InputStream is = zipFile.getInputStream(zipEntry);
					Bitmap bm = BitmapFactory.decodeStream(is);
					if (bm.getWidth() > bm.getHeight()) {
						// 두장 스캔본은 첫장의 왼쪽 이미지를 뽑음 (일본판 제본이라고 가정하고....)
						bm = cutBitmapInHalf(bm, false);
						bm = Bitmap.createScaledBitmap(bm, 400, 600, false);
					}
					return removeMargins(bm, 350, 200, 350, 200);
				}
			}
			zipFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 스트림에서 읽어오니까 정렬은 못하고 젤 처음 매칭되는 이미지로 썸네일 만들기
	 * 
	 * @param is
	 * @return
	 */
	public static Bitmap extractCoverFromZip(InputStream is) {
		ZipArchiveEntry ze;
		ZipArchiveInputStream zis;
		try {
			zis = new ZipArchiveInputStream(is, "utf-8");
			while ((ze = zis.getNextZipEntry()) != null) {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int count;
				while ((count = zis.read(buffer)) != -1) {
					baos.write(buffer, 0, count);
				}

				String filename = ze.getName();
				byte[] bytes = baos.toByteArray();

				baos.close();

				if (filename.indexOf(".") == -1) {
					continue;
				}

				String extension = filename.substring(filename.indexOf(".") + 1);
				if (extension.equalsIgnoreCase("jpg")) {
					Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
					zis.close();

					if (bm.getWidth() > bm.getHeight()) {
						// 두장 스캔본은 첫장의 왼쪽 이미지를 뽑음 (일본판 제본이라고 가정하고....)
						bm = cutBitmapInHalf(bm, false);
						bm = Bitmap.createScaledBitmap(bm, 400, 600, false);
					}
					return removeMargins(bm, 350, 200, 350, 200);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap extractCoverFromFolder(File src) {
		if (src == null)
			return null;

		if (src.isDirectory() == false)
			return null;

		if (src.listFiles() == null)
			return null;
		
		
		Bitmap cover = null;
		
		int length = src.listFiles().length;
		if(length > 0) {
			int index = new Random().nextInt(length);
			File f = src.listFiles()[index];
			
			String ext = StringUtils.getExtension(f.getName());
			if ("zip".equalsIgnoreCase(ext))
				cover = extractCoverFromZip(f);
			else if ("pdf".equalsIgnoreCase(ext))
				cover = extractCoverFromPdf(GlobalApplication.instance(), f);
			else if ("jpg".equalsIgnoreCase(ext))
				cover = extractCoverFromJpg(f);
		}
		
		return cover;
		

//		Bitmap dst = null;
//		Canvas comboImage = null;
//		int count = 0;
//		int jump = src.listFiles().length / 4;
//		for (int i = 0; i < src.listFiles().length; i++) {
//			if (i != jump * count)
//				continue;
//
//			File f = src.listFiles()[i];
//			if (f.isHidden())
//				continue;
//			if (f.isDirectory())
//				continue;
//
//			Bitmap cover = null;
//			String ext = StringUtils.getExtension(f.getName());
//			if ("zip".equalsIgnoreCase(ext))
//				cover = extractCoverFromZip(f);
//			else if ("pdf".equalsIgnoreCase(ext))
//				cover = extractCoverFromPdf(GlobalApplication.instance(), f);
//			else if ("jpg".equalsIgnoreCase(ext))
//				cover = extractCoverFromJpg(f);
//
//			if (cover != null) {
//				count++;
//
//				if (dst == null) {
//					dst = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888);
//					comboImage = new Canvas(dst);
//				}
//
//				Rect rect = new Rect();
//				if (count == 1) {
//					rect.left = rect.top = 0;
//					rect.right = 200;
//					rect.bottom = 300;
//				} else if (count == 2) {
//					rect.left = 200;
//					rect.top = 0;
//					rect.right = 400;
//					rect.bottom = 300;
//				} else if (count == 3) {
//					rect.left = 0;
//					rect.top = 300;
//					rect.right = 200;
//					rect.bottom = 600;
//				} else if (count == 4) {
//					rect.left = 200;
//					rect.top = 300;
//					rect.right = 400;
//					rect.bottom = 600;
//				} else
//					break;
//
//				comboImage.drawBitmap(cover, null, rect, null);
//			}
//		}
//		return dst;
	}

	public static Bitmap extractCoverFromFolder(DropboxAPI<AndroidAuthSession> api, Entry entry) {

		if (entry.isDir == false)
			return null;

		Bitmap dst = null;
		Canvas comboImage = null;
		try {
			entry = api.metadata(entry.path, 1000, null, true, null);

			int count = 0;
			int jump = entry.contents.size() / 4;
			for (int i = 0; i < entry.contents.size(); i++) {
				if (i != jump * count)
					continue;

				Entry ent = entry.contents.get(i);
				if (ent.isDir)
					continue;

				Bitmap cover = null;

				String ext = StringUtils.getExtension(ent.fileName());
				if ("zip".equalsIgnoreCase(ext)) {
					FileInfo info = FileInfoDAO.instance().getFileInfo(ent);
					cover = BitmapCache.INSTANCE.getBitmapFromMemCache(info);
					if (cover == null && StringUtils.isBlank(info.getMeta().coverPath) == false) {
						cover = BitmapFactory.decodeFile(info.getMeta().coverPath);
						if (cover != null)
							BitmapCache.INSTANCE.addBitmapToMemoryCache(info, cover);
					}

					if (cover == null) {
						cover = extractCoverFromZip(api.getFileStream(ent.path, null));
					}
				} else
					continue;

				if (cover != null) {
					count++;

					if (dst == null) {
						dst = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888);
						comboImage = new Canvas(dst);
					}

					Rect rect = new Rect();
					if (count == 1) {
						rect.left = rect.top = 0;
						rect.right = 200;
						rect.bottom = 300;
					} else if (count == 2) {
						rect.left = 200;
						rect.top = 0;
						rect.right = 400;
						rect.bottom = 300;
					} else if (count == 3) {
						rect.left = 0;
						rect.top = 300;
						rect.right = 200;
						rect.bottom = 600;
					} else if (count == 4) {
						rect.left = 200;
						rect.top = 300;
						rect.right = 400;
						rect.bottom = 600;
					} else
						break;

					comboImage.drawBitmap(cover, null, rect, null);
				}
			}
		} catch (DropboxException e) {
			e.printStackTrace();
		}
		return dst;
	}

	public static Bitmap cutBitmapInHalf(Bitmap src, boolean isRightSide) {

		Bitmap dst = Bitmap.createBitmap(src.getWidth() / 2, src.getHeight(), Bitmap.Config.ARGB_8888);

		Rect srcRect = new Rect();
		if (isRightSide) {
			srcRect.left = src.getWidth() / 2;
			srcRect.top = 0;
			srcRect.right = src.getWidth();
			srcRect.bottom = src.getHeight();
		} else {
			srcRect.left = srcRect.top = 0;
			srcRect.right = src.getWidth() / 2;
			srcRect.bottom = src.getHeight();
		}
		Canvas canvas = new Canvas(dst);
		canvas.drawBitmap(src, srcRect, new Rect(0, 0, dst.getWidth(), dst.getHeight()), null);

		return dst;
	}

	public static int getAverageColor(Bitmap bmp, int alpha, boolean adjust) {
		if (bmp == null)
			return 0;

		int[] bmpIn = new int[bmp.getWidth() * bmp.getHeight()];

		bmp.getPixels(bmpIn, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

		int len = bmpIn.length;
		long r = 0;
		long g = 0;
		long b = 0;
		for (int pixel : bmpIn) {
			r += Color.red(pixel);
			g += Color.green(pixel);
			b += Color.blue(pixel);
		}

		int avgR = (int) (r / len);
		int avgG = (int) (g / len);
		int avgB = (int) (b / len);

		if (adjust) {
			if ((avgR + avgG + avgB) > 255 * 3 / 2) {
				// 밝은 색이면
				avgR /= 2;
				avgG /= 2;
				avgB /= 2;
			} else {
				avgR = avgR < 128 ? avgR * 2 : 255;
				avgG = avgG < 128 ? avgG * 2 : 255;
				avgB = avgB < 128 ? avgB * 2 : 255;
			}
		}

		return Color.argb(alpha, avgR, avgG, avgB);
	}

	public static int getComplementColor(int color, int alpha) {
		return Color.argb(alpha, 255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
	}

	public static Bitmap removeMargins(Bitmap bmp, int dT, int dL, int dB, int dR) {

		int color = bmp.getPixel(0, 0);
		long dtMili = System.currentTimeMillis();
		int MTop = 0, MBot = 0, MLeft = 0, MRight = 0;
		boolean found = false;

		int[] bmpIn = new int[bmp.getWidth() * bmp.getHeight()];
		int[][] bmpInt = new int[bmp.getWidth()][bmp.getHeight()];

		bmp.getPixels(bmpIn, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

		int avg1 = 0;

		// 비트맵을 2차원 배열로 만든다.
		for (int ii = 0, contX = 0, contY = 0; ii < bmpIn.length; ii++) {
			bmpInt[contX][contY] = bmpIn[ii];
			contX++;
			if (contX >= bmp.getWidth()) {
				contX = 0;
				contY++;
				if (contY >= bmp.getHeight()) {
					break;
				}
			}
		}

		for (int hP = 0; hP < bmpInt[0].length && !found; hP++) {
			// looking for MTop
			long total = 0;
			for (int wP = 0; wP < bmpInt.length && !found; wP++)
				total += bmpInt[wP][hP];
			int avgColor = (int) (total / bmpInt.length);

			// 첫줄과 둘째줄의 평균색이 다르면 마진을 제거하지 않는다.
			if (hP == 0)
				avg1 = avgColor;
			if (hP == 1 && avg1 != avgColor)
				break;

			if (getColorDelta(color, avgColor) > dT) {
				MTop = hP;
				found = true;
				break;
			}
		}
		found = false;

		for (int hP = bmpInt[0].length - 1; hP >= 0 && !found; hP--) {
			// looking for MBot
			long total = 0;
			for (int wP = 0; wP < bmpInt.length && !found; wP++)
				total += bmpInt[wP][hP];
			int avgColor = (int) (total / bmpInt.length);

			// 첫줄과 둘째줄의 평균색이 다르면 마진을 제거하지 않는다.
			if (hP == bmpInt[0].length - 1)
				avg1 = avgColor;
			if (hP == bmpInt[0].length - 2 && avg1 != avgColor)
				break;

			if (getColorDelta(color, avgColor) > dB) {
				MBot = bmp.getHeight() - hP;
				found = true;
				break;
			}
		}
		found = false;

		for (int wP = 0; wP < bmpInt.length && !found; wP++) {
			// looking for MLeft
			long total = 0;
			for (int hP = 0; hP < bmpInt[0].length && !found; hP++)
				total += bmpInt[wP][hP];
			int avgColor = (int) (total / bmpInt.length);

			// 첫줄과 둘째줄의 평균색이 다르면 마진을 제거하지 않는다.
			if (wP == 0)
				avg1 = avgColor;
			if (wP == 1 && avg1 != avgColor)
				break;

			if (getColorDelta(color, avgColor) > dL) {
				MLeft = wP;
				found = true;
				break;
			}
		}
		found = false;

		for (int wP = bmpInt.length - 1; wP >= 0 && !found; wP--) {
			// looking for MRight
			long total = 0;
			for (int hP = 0; hP < bmpInt[0].length && !found; hP++)
				total += bmpInt[wP][hP];
			int avgColor = (int) (total / bmpInt.length);

			// 첫줄과 둘째줄의 평균색이 다르면 마진을 제거하지 않는다.
			if (wP == bmpInt.length - 1)
				avg1 = avgColor;
			if (wP == bmpInt.length - 2 && avg1 != avgColor)
				break;

			if (getColorDelta(color, avgColor) > dR) {
				MRight = bmp.getWidth() - wP;
				found = true;
				break;
			}

		}
		found = false;

		int sizeY = bmp.getHeight() - MBot - MTop;
		int sizeX = bmp.getWidth() - MRight - MLeft;

		if (sizeX > 0 && sizeY > 0) {
			bmp = Bitmap.createBitmap(bmp, MLeft, MTop, sizeX, sizeY);
			dtMili = (System.currentTimeMillis() - dtMili);
			// Log.e("Margin   2", "Time needed " + dtMili + "mSec\nMTop:" +
			// MTop + "\nMLeft:" + MLeft + "\nMBot:" + MBot
			// + "\nmRight:" + MRight);
			return bmp;
		}
		return bmp;
	}

	private static int getColorDelta(int color1, int color2) {
		int colorDelta = 0;
		colorDelta += Math.abs(Color.red(color1) - Color.red(color2));
		colorDelta += Math.abs(Color.green(color1) - Color.green(color2));
		colorDelta += Math.abs(Color.blue(color1) - Color.blue(color2));
		return colorDelta;
	}

	public static Bitmap fastblur(Bitmap sentBitmap, int radius) {

		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012

		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);

		return (bitmap);
	}

}