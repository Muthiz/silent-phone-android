/*
Copyright (C) 2017, Silent Circle, LLC.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.silentcircle.messaging.views;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import com.silentcircle.common.util.ViewUtil;

public class BoundedCheckableLinearLayout extends CheckableLinearLayout {

    public static final int MAXIMUM_WIDTH_PERCENTAGE = 85;
    public static final int MAXIMUM_WIDTH_GROUP_PERCENTAGE = 75;

    private int mMaximumWidthPercentage = MAXIMUM_WIDTH_PERCENTAGE;
    private Point mSize;

    private int mMaximumWidth;

    public BoundedCheckableLinearLayout(Context context) {
        super(context);
    }

    public BoundedCheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoundedCheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mSize == null) {
            mSize = new Point();
            ViewUtil.getScreenDimensions(getContext(), mSize);
            mMaximumWidth = (mMaximumWidthPercentage * mSize.x * 2 - mSize.x) / 200;
        }
        int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        if (mMaximumWidth > 0 && mMaximumWidth < measuredWidth) {
            int measureMode = View.MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(mMaximumWidth, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setMaximumWidthPercentage(int maximumWidthPercentage) {
        mMaximumWidthPercentage = maximumWidthPercentage;
        requestLayout();
    }

    public void setMaximumWidth(int maximumWidth) {
        mMaximumWidth = maximumWidth;
        requestLayout();
    }
}
