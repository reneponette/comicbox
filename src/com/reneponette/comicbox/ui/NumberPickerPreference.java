package com.reneponette.comicbox.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import com.reneponette.comicbox.R;

public class NumberPickerPreference extends DialogPreference {
	
	NumberPicker picker;
	int number;
	

	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		setDialogLayoutResource(R.layout.dialog_number_picker);
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		picker = (NumberPicker) view.findViewById(R.id.number_picker);
		picker.setMaxValue(5);
		picker.setMinValue(1);
		picker.setValue(number);
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if(restorePersistedValue) {
			number = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(getKey(), 0);
		} else {
			if(defaultValue != null)
				number = Integer.parseInt(defaultValue.toString());
		}
		
		
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		Log.e(this.getClass().getName(), "which = " + which);
		if(which == DialogInterface.BUTTON_POSITIVE) {
			getEditor().putInt(getKey(), picker.getValue()).commit();
		}
	}
	
	
	


}
