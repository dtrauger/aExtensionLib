package com.derektrauger.library.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/*
 * Description:
 * VerticalScrollView extends ScrollView to override the onInterceptTouchEvent method to only intercept 
 * touch events if the motion in the vertical direction is greater than the corresponding touch events 
 * in the horizontal direction.  This control effectively only intercepts events if the user intentionally or 
 * deliberately scrolls in the vertical direction and in that event passes the ACTION_CANCEL flag to its children.
 * 
 * Usage:
 * <com.derektrauger.controls.VerticalScrollView
                         android:id="@+id/VerticalScrollView01"
                         android:layout_width="fill_parent"
                         android:layout_height="wrap_content"></com.derektrauger.controls.VerticalScrollView>
 */

public class VerticalScrollView extends ScrollView {
	private float xDistance, yDistance, lastX, lastY;

	public VerticalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			xDistance = yDistance = 0f;
			lastX = ev.getX();
			lastY = ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			final float curX = ev.getX();
			final float curY = ev.getY();
			xDistance += Math.abs(curX - lastX);
			yDistance += Math.abs(curY - lastY);
			lastX = curX;
			lastY = curY;
			if(xDistance > yDistance)
				return false;
		}

		return super.onInterceptTouchEvent(ev);
	}
}
