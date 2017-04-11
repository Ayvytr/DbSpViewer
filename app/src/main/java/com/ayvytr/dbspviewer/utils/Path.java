package com.ayvytr.dbspviewer.utils;

/**
 * Desc:
 * Date: 2017/4/11
 *
 * @author davidwang
 */

public class Path
{
    public static String getDbPath(String packageName)
    {
        return "/data/data/" + packageName + "/databases";
    }

    public static String getSpPath(String packageName)
    {
        return "/data/data/" + packageName + "/shared_prefs";
    }
}
