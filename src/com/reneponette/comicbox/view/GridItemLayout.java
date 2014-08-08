package com.reneponette.comicbox.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.reneponette.comicbox.utils.MetricUtils;

public class GridItemLayout extends RelativeLayout {

	public GridItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, widthMeasureSpec + MetricUtils.dpToPixel(getContext(), 50));
	}

}
