package com.reneponette.comicbox.ui.fragment.explorer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.utils.ImageUtils;

public class BaseExplorerFragment extends Fragment {
	public interface FolderViewFragmentListener {
		public void onFileClicked(FileInfo info);
		public void onEntryClicked(FileInfo info);
	}

	private ProgressDialog mProgressDlg;
	
	private void setInfoBackgroundColor(View v, Bitmap bm, int color) {
    	//텍스뷰 배경 색깔 변경
		int avgColor = color == -1 ? ImageUtils.getAverageColor(bm, 200, false) : color;
		if(v.getBackground() instanceof LayerDrawable) {
			LayerDrawable ld = (LayerDrawable) v.getBackground();
			GradientDrawable drawable = (GradientDrawable) ld.findDrawableByLayerId(R.id.folder_info_bg);
			drawable.setColor(avgColor);
		} else {
			v.setBackgroundColor(avgColor);
		}
	}
	
	// //////////////////////////////
	public void showWaitingDialog() {
		if(mProgressDlg != null)
			return;
		mProgressDlg = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
//		mProgressDlg.setTitle(R.string.progress_title);
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
	
	public boolean onBackPressed() {
		return false;
	}
}
