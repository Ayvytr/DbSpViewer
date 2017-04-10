package com.ayvytr.dbspviewer.bean;

/**
 * Desc:
 * Date: 2017/4/10
 *
 * @author davidwang
 */

public class DbItem
{
    public String[] values;

    public DbItem(String[] values)
    {
        this.values = values;
    }

    public DbItem(int valuesCount)
    {
        values = new String[valuesCount];
    }

}
