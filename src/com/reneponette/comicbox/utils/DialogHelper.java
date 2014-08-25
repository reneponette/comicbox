package com.reneponette.comicbox.utils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;

public class DialogHelper {

	public static void showReadDirectionSelectDialog(Context context, final FileMeta meta, DialogInterface.OnDismissListener dismissListener) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setTitle(context.getString(R.string.title_read_direction));
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setOnDismissListener(dismissListener);
		builder.setSingleChoiceItems(R.array.select_read_direction_items, meta.readDirection == ReadDirection.RTL ? 1 : 0,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						meta.readDirection = which == 1 ? ReadDirection.RTL : ReadDirection.LTR;
					}
				});
		builder.create().show();
	}
	
	public static void showGoNextComicsDialog(Context context, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setMessage(context.getString(R.string.will_read_next_comics));
		builder.setPositiveButton(android.R.string.ok, listener);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.create().show();		
	}
	
	
	public static void showDataDownloadWarningDialog(Context context, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setTitle(context.getString(R.string.title_warning));
		builder.setMessage(context.getString(R.string.warning_message_data_download));
		builder.setPositiveButton(android.R.string.ok, listener);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.create().show();
	}
	
	public static void showSelectDownloadOrStreaming(Context context, DialogInterface.OnClickListener streamingListener, DialogInterface.OnClickListener downloadListener) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setMessage(context.getString(R.string.warning_message_use_streaming));
		builder.setPositiveButton("스트리밍", streamingListener);
		builder.setNeutralButton("다운로드", downloadListener);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.create().show();
	}
	
	public static void showRetryDialog(Context context, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new Builder(context);
		builder.setMessage(context.getString(R.string.error_retry));
		builder.setPositiveButton(android.R.string.ok, listener);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.create().show();
	}
}
