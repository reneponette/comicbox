package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.controller.DataController;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.utils.Logger;

public class BaseReaderFragment extends Fragment {
	public static final String PATH = "path";
	public static final int REQ_SETTINGS = 0;


	protected DataController dataController;	
	private boolean autocrop;

	private ProgressDialog mProgressDlg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dataController = new DataController();
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		autocrop = pref.getBoolean("viewer_use_autocrop", true);
	}

	
	/*-----------------------------------------------------------------------------------*/
	
	public boolean isAutocrop() {
		return autocrop;
	}

	public void setAutocrop(boolean autocrop) {
		this.autocrop = autocrop;
	}
	
	
	protected void onMoveToLeftPage() {
		
	}
	public void moveToLeftPage() {
		onMoveToLeftPage();
	}
	
	protected void onMoveToRightPage() {
		
	}
	public void moveToRightPage() {
		onMoveToRightPage();
	}
	
	public boolean onBackPressed() {
		return false;
	}

	/*-----------------------------------------------------------------------------------*/
	
	public void showWaitingDialog() {
		
		Logger.e(this, "showWaitingDialog(), getActivity() = " + getActivity());
		
		if(mProgressDlg != null)
			return;
		
		mProgressDlg = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
		mProgressDlg.setMessage(getResources().getString(R.string.progress_loading));
		mProgressDlg.setCanceledOnTouchOutside(false);
		mProgressDlg.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				mProgressDlg = null;
			}
		});
		mProgressDlg.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface arg0) {
				mProgressDlg = null;
			}
		});
		mProgressDlg.show();
	}

	public void hideWaitingDialog() {
		if (mProgressDlg != null) {
			mProgressDlg.dismiss();
		}
	}
	
	
	public File findNextFile() {
		FileInfo info = dataController.getFileInfo();
		File curFile = info.getFile();
		boolean curFound = false;

		// 정렬 해야함
		List<File> fileList = Arrays.asList(curFile.getParentFile().listFiles());
		Collections.sort(fileList);		
		
		for (File child : fileList) {
			if (child.getAbsoluteFile().equals(curFile.getAbsoluteFile())) {
				curFound = true;
				continue;
			}

			if (curFound) {
				return child;
			}
		}
		return null;
	}
	
	/*-----------------------------------------------------------------------------------*/

	public void adjustScreenBrightness(boolean isUpDirection) {
		// 화면 밝기 조절
		try {
			ContentResolver cr = getActivity().getContentResolver();
			int brightNow = android.provider.Settings.System.getInt(cr,
					android.provider.Settings.System.SCREEN_BRIGHTNESS);

			if (android.provider.Settings.System.getInt(cr, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE) == 1) {
				android.provider.Settings.System.putInt(cr, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
			}

			if (isUpDirection) {
				brightNow = brightNow + 3;
				if (brightNow > 100)
					brightNow = 100;
			} else {
				brightNow = brightNow - 3;
				if (brightNow < 1)
					brightNow = 1;
			}

			WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
			params.screenBrightness = (float) brightNow / 100;
			getActivity().getWindow().setAttributes(params);
			android.provider.Settings.System.putInt(cr, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightNow);

		} catch (Exception e) {
			Log.e("Exception e " + e.getMessage(), null);
		}
	}

}
