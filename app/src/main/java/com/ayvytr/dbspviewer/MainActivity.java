package com.ayvytr.dbspviewer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.easyandroidlibrary.Easy;
import com.ayvytr.easyandroidlibrary.bean.AppInfo;
import com.ayvytr.easyandroidlibrary.tools.withcontext.Packages;
import com.ayvytr.easyandroidlibrary.tools.withcontext.ToastTool;
import com.ayvytr.logger.L;
import com.chrisplus.rootmanager.RootManager;

import java.util.ArrayList;
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
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
    private AppAdapter appAdapter;

    public static final String EXTRA_BROWSE_DB = "browse_db";

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
                              L.e(Thread.currentThread().getName());
                              list.addAll(value);
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
                iv.setImageDrawable(appInfo.icon);
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
                                        ToastTool.show(R.string.sp);
                                        break;
                                    case 1:
                                        showSelectDbDialog(appInfo);
                                        break;
                                }
                                return true;
                            }
                        }).show();
    }

    private void showSelectDbDialog(AppInfo appInfo)
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
                      }

                      @Override
                      public void onNext(List<AppInfo> value)
                      {
                      }

                      @Override
                      public void onError(Throwable e)
                      {

                      }

                      @Override
                      public void onComplete()
                      {
                      }
                  });
        new MaterialDialog.Builder(this)
                .title(R.string.select_db)
                .items()
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        return true;
                    }
                }).show();
    }
}
