package com.reneponette.comicbox.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.TextView;

public class ProgressTextView extends TextView {

	public ProgressTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	float progress;
	
	public void setProgress(float progress) {
		this.progress = progress;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		Paint paint = new Paint(getPaint());
		RectF rect = new RectF(-getWidth()/2, -getHeight()/2, (float)(1.5*getWidth()), (float)(1.5*getHeight()));
		paint.setColor(0x88000000);
		canvas.drawArc(rect, -90 + 360*progress, 360*(1-progress), true, paint);
//		canvas.drawCircle(centerX, centerY, getWidth() /2, paint);
		
		
		super.onDraw(canvas);
	}
	

}
