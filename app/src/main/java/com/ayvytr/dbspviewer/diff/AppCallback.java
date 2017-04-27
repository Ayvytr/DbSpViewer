package com.ayvytr.dbspviewer.diff;

import android.support.v7.util.DiffUtil;

import com.ayvytr.easyandroid.bean.AppInfo;

import java.util.List;

/**
 * Desc:
 * Date: 2017/4/27
 *
 * @author davidwang
 */

public class AppCallback extends DiffUtil.Callback
{
    private List<AppInfo> list;
    private List<AppInfo> value;

    public AppCallback(List<AppInfo> list, List<AppInfo> value)
    {
        this.list = list;
        this.value = value;
    }

    @Override
    public int getOldListSize()
    {
        return list.size();
    }

    @Override
    public int getNewListSize()
    {
        return value.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
    {
        AppInfo appInfo = list.get(oldItemPosition);
        AppInfo newAppInfo = value.get(newItemPosition);
        return appInfo.packageName.equals(newAppInfo.packageName);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
    {
        return list.get(oldItemPosition).equals(value.get(newItemPosition));
    }
}
