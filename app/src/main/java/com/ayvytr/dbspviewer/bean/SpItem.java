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
}
