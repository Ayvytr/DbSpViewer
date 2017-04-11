package com.ayvytr.dbspviewer.view.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.utils.Root;
import com.ayvytr.easyandroid.Easy;
import com.ayvytr.easyandroid.bean.AppInfo;
import com.ayvytr.easyandroid.tools.FileTool;
import com.ayvytr.easyandroid.tools.withcontext.Packages;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.chrisplus.rootmanager.RootManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRA_DB_FILEPATH = "extra_db_filepath";
    public static final String EXTRA_SP_APP_INFO = "extra_sp_app_info";

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
    private AppAdapter appAdapter;
    private String spPath;

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

        initView();
    }

    private void initView()
    {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        appAdapter = new AppAdapter();
        recyclerView.setAdapter(appAdapter);
        appAdapter.update();
        refreshLayout.setColorSchemeColors(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE,
                Color.BLACK);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                appAdapter.update();
            }
        });
    }

    public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Vh>
    {
        private List<AppInfo> list = new ArrayList<>();

        @Override
        public Vh onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new Vh(LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.item_apps, parent, false));
        }

        @Override
        public void onBindViewHolder(Vh holder, int position)
        {
            holder.update(position);
        }

        @Override
        public int getItemCount()
        {
            return list.size();
        }

        public void update()
        {
            Observable.create(new ObservableOnSubscribe<List<AppInfo>>()
            {
                @Override
                public void subscribe(ObservableEmitter<List<AppInfo>> e) throws Exception
                {
                    e.onNext(Packages.getInstalledAppsInfo());
                    e.onComplete();
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                      .subscribe(new Observer<List<AppInfo>>()
                      {
                          @Override
                          public void onSubscribe(Disposable d)
                          {
                              refreshLayout.setRefreshing(true);
                              if(!list.isEmpty())
                              {
                                  list.clear();
                                  notifyDataSetChanged();
                              }
                          }

                          @Override
                          public void onNext(List<AppInfo> value)
                          {
                              list.addAll(value);
                              Collections.sort(list, new Comparator<AppInfo>()
                              {
                                  @Override
                                  public int compare(AppInfo o1, AppInfo o2)
                                  {
                                      return o1.label.compareTo(o2.label);
                                  }
                              });
                          }

                          @Override
                          public void onError(Throwable e)
                          {

                          }

                          @Override
                          public void onComplete()
                          {
                              notifyDataSetChanged();
                              refreshLayout.setRefreshing(false);
                          }
                      });
        }

        public class Vh extends RecyclerView.ViewHolder
        {
            @BindView(R.id.iv)
            ImageView iv;
            @BindView(R.id.tvTitle)
            TextView tvTitle;
            @BindView(R.id.tvPackageName)
            TextView tvPackageName;
            private View view;

            public Vh(View view)
            {
                super(view);
                this.view = view;
                ButterKnife.bind(this, view);
            }

            public void update(int position)
            {
                final AppInfo appInfo = list.get(position);
                tvTitle.setText(appInfo.label);
                iv.setImageBitmap(appInfo.icon);
                tvPackageName.setText(appInfo.packageName);

                view.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        showSwitchBrowseDialog(appInfo);
                    }
                });
            }
        }
    }

    private void showSwitchBrowseDialog(final AppInfo appInfo)
    {
        new MaterialDialog.Builder(MainActivity.this)
                .items(R.array.select_which)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0,
                        new MaterialDialog.ListCallbackSingleChoice()
                        {
                            @Override
                            public boolean onSelection(MaterialDialog dialog,
                                                       View itemView, int which,
                                                       CharSequence text)
                            {
                                switch(which)
                                {
                                    case 0:
                                        goToSpActivity(appInfo);
                                        break;
                                    case 1:
                                        showSelectDbDialog(appInfo);
                                        break;
                                }
                                return true;
                            }
                        }).show();
    }

    private void goToSpActivity(AppInfo appInfo)
    {
        Intent intent = new Intent(MainActivity.this, SpActivity.class);
        intent.putExtra(EXTRA_SP_APP_INFO, appInfo);
        startActivity(intent);
    }


    private void showSelectDbDialog(AppInfo appInfo)
    {
        String dbPath = getDbPath(appInfo.packageName);
        Root.requestReadPermission(dbPath);
        final File[] files = FileTool.listFilesDislikeNamesNoCase(dbPath, "-journal");
        if(files == null || files.length == 0)
        {
            ToastTool.show(R.string.no_databases);
            return;
        }

        final String[] names = FileTool.toFileNames(files);
        Arrays.sort(files);
        Arrays.sort(names);
        new MaterialDialog.Builder(this)
                .title(R.string.select_db)
                .items(names)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice()
                {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        Intent intent = new Intent(MainActivity.this, DatabaseActivity.class);
                        String path = files[which].getAbsolutePath();
                        intent.putExtra(EXTRA_DB_FILEPATH, path);
                        startActivity(intent);
                        return true;
                    }
                }).show();
    }

    private String getDbPath(String packageName)
    {
        return "/data/data/" + packageName + "/databases";
    }
}
