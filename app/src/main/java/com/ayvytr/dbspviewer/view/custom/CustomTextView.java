package com.ayvytr.dbspviewer.view.custom;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.ayvytr.dbspviewer.R;
import com.ayvytr.easyandroid.view.custom.LeftCenterGravityTextView;

/**
 * Desc:
 * Date: 2017/4/10
 *
 * @author davidwang
 */

public class CustomTextView extends LeftCenterGravityTextView
{
    public CustomTextView(Context context)
    {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setMaxLines(1);
        setEllipsize(TextUtils.TruncateAt.END);
        setBackgroundResource(R.drawable.tv_bg);
        setPadding(20, 0, 0, 0);
    }
}
