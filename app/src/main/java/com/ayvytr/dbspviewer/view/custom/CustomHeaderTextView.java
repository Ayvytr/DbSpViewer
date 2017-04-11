package com.ayvytr.dbspviewer.view.custom;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.ayvytr.dbspviewer.R;
import com.ayvytr.easyandroid.tools.withcontext.ResTool;
import com.ayvytr.easyandroid.view.custom.LeftCenterGravityTextView;

/**
 * Desc:
 * Date: 2017/4/10
 *
 * @author davidwang
 */

public class CustomHeaderTextView extends LeftCenterGravityTextView
{
    public CustomHeaderTextView(Context context)
    {
        this(context, null);
    }

    public CustomHeaderTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public CustomHeaderTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setMaxLines(1);
        setEllipsize(TextUtils.TruncateAt.END);
        setTextColor(ResTool.getColor(R.color.colorAccent));
        setBackgroundResource(R.drawable.tv_bg_header);
        setPadding(20, 0, 0, 0);
    }
}
