package com.ayvytr.dbspviewer.bean;

/**
 * Desc:
 * Date: 2017/4/11
 *
 * @author davidwang
 */

public class SpItem
{
    public String type;
    public String key;
    public String value;

    public SpItem(String type, String key, String value)
    {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public SpItem()
    {
    }

    @Override
    public String toString()
    {
        return type + "\n" + key + "\n" + value + "\n";
    }

    public String toString(SpItem headerItem)
    {
        StringBuffer sb = new StringBuffer(headerItem.type);
        sb.append(":");
        sb.append(type);
        sb.append("\n");
        sb.append(headerItem.key);
        sb.append(":");
        sb.append(key);
        sb.append("\n");
        sb.append(headerItem.value);
        sb.append(":");
        sb.append(value);
        sb.append("\n");
        return sb.toString();
    }
}
