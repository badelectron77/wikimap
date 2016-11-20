package com.medeozz.wikimap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class ClickableFrameLayout extends FrameLayout {

    View.OnClickListener mOnClickListener;

    public ClickableFrameLayout(Context context) {
        super(context);
    }

    public ClickableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public ClickableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        mOnClickListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mOnClickListener != null;
    }
}