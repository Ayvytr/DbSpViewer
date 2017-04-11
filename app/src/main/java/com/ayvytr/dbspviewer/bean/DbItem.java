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

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < values.length; i++)
        {
            sb.append(values[i]);
            sb.append("\n");
        }

        return sb.toString();
    }

    public String toString(DbItem headerItem)
    {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < headerItem.values.length; i++)
        {
            sb.append(headerItem.values[i]);
            sb.append(":");
            sb.append(values[i]);
            sb.append("\n");
        }

        return sb.toString();
    }
}
