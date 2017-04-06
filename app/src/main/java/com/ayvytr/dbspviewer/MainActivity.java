package com.ayvytr.dbspviewer;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.ayvytr.easyandroidlibrary.Easy;
import com.ayvytr.easyandroidlibrary.tools.withcontext.ToastTool;
import com.chrisplus.rootmanager.RootManager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
{
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init()
    {
        Easy.getDefault().init(this);
        if(!RootManager.getInstance().hasRooted())
        {
            ToastTool.show(R.string.no_root_permission);
            return;
        }

        boolean hasPermission = RootManager.getInstance().obtainPermission();
        if(!hasPermission)
        {
            ToastTool.show(R.string.get_root_permission_failed);
        }

    }

}
