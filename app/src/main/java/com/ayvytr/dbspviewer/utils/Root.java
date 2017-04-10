package com.ayvytr.dbspviewer.utils;

import com.chrisplus.rootmanager.RootManager;

/**
 * Desc:
 * Date: 2017/4/10
 *
 * @author davidwang
 */

public class Root
{
    public static void requestReadPermission(String path)
    {
        RootManager.getInstance().runCommand("chmod o+r " + path);
    }

    public static void cancelReadPermission(String path)
    {
        RootManager.getInstance().runCommand("chmod o-r " + path);
    }

    public static void requestWritePermission(String path)
    {
        RootManager.getInstance().runCommand("chmod o+rw " + path);
    }

    public static void cancelWritePermission(String path)
    {
        RootManager.getInstance().runCommand("chmod o-rw " + path);
    }

    public static void requestReadDbPermission(String path)
    {
        //读取数据库需要把数据表和-journal数据表一起加上读权限
        RootManager.getInstance().runCommand("chmod o+r " + path);
        RootManager.getInstance().runCommand("chmod o+r " + path + "-journal");
    }

    public static void cancelReadDbPermission(String path)
    {
        RootManager.getInstance().runCommand("chmod o-r " + path);
        RootManager.getInstance().runCommand("chmod o-r " + path + "-journal");
    }
}
