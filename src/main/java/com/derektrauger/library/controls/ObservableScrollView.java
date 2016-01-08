package com.derektrauger.library.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

/*
 * Description:
 * ObservableScrollView extends HorizontalScrollView to enable tracking of changes to the 
 * ScrollView's position via the onScrollChanged event.
 * 
 * Usage:
 * <com.derektrauger.controls.ObservableScrollView
                         android:id="@+id/HorizontalScrollView01"
                         android:layout_width="fill_parent"
                         android:layout_height="wrap_content"></com.derektrauger.controls.ObservableScrollView>
 * implements ScrollViewListener
 * @Override
	public void onScrollChanged(ObservableScrollView scrollView, int x, int y,
			int oldx, int oldy) { }
 */

public class ObservableScrollView extends HorizontalScrollView {

    private ScrollViewListener scrollViewListener = null;

    public ObservableScrollView(Context context) {
        super(context);
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if(scrollViewListener != null) {
            scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }

}

