package com.ayvytr.dbspviewer.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.diff.AppCallback;
import com.ayvytr.dbspviewer.utils.Path;
import com.ayvytr.easyandroid.Easy;
import com.ayvytr.easyandroid.bean.AppInfo;
import com.ayvytr.easyandroid.tools.FileTool;
import com.ayvytr.easyandroid.tools.withcontext.Packages;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.ayvytr.root.Roots;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
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

    private int currentFilterType;
    private int currentSortType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init()
    {
        Easy.getDefault().init(this);
        initView();
    }

    private boolean cannotAccess()
    {
        if(!Roots.get().hasRooted())
        {
            ToastTool.show(R.string.no_root_permission);
            return true;
        }

        boolean hasPermission = Roots.get().requestRootPermission();
        if(!hasPermission)
        {
            ToastTool.show(R.string.get_root_permission_failed);
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_id_help:
                showHelpInfo();
                return true;
            case R.id.menu_id_filter:
                filterApplications();
                break;
            case R.id.menu_id_sort:
                sortApplications();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sortApplications()
    {
        new MaterialDialog.Builder(this)
                .title(R.string.sort_applications)
                .items(R.array.sort_items)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(currentSortType,
                        new MaterialDialog.ListCallbackSingleChoice()
                        {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView,
                                                       int which,
                                                       CharSequence text)
                            {
                                currentSortType = which;
                                appAdapter.update();
                                return true;
                            }
                        }).show();
    }

    private void filterApplications()
    {
        new MaterialDialog.Builder(this)
                .title(R.string.filter_applications)
                .items(R.array.filter_items)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(currentFilterType,
                        new MaterialDialog.ListCallbackSingleChoice()
                        {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView,
                                                       int which,
                                                       CharSequence text)
                            {
                                currentFilterType = which;
                                appAdapter.update();
                                return true;
                            }
                        }).show();
    }

    private void showHelpInfo()
    {
        new MaterialDialog.Builder(this)
                .title(R.string.help)
                .content(R.string.help_content)
                .neutralText(R.string.goto_author_s_github)
                .positiveText(R.string.confirm)
                .onNeutral(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                    {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://github.com/ayvytr"));
                        startActivity(intent);
                    }
                })
                .show();
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

    private void onSortApplications(List<AppInfo> list)
    {
        Collections.sort(list, new Comparator<AppInfo>()
        {
            @Override
            public int compare(AppInfo o1, AppInfo o2)
            {
                return currentSortType == 0 ? o1.label
                        .compareTo(o2.label) : o1.packageName
                        .compareTo(o2.packageName);
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
            refreshLayout.setRefreshing(true);
            Observable.fromIterable(Packages.getInstalledAppsInfo())
                      .filter(new Predicate<AppInfo>()
                      {
                          @Override
                          public boolean test(AppInfo appInfo) throws Exception
                          {
                              switch(currentFilterType)
                              {
                                  case 1:
                                      return appInfo.isSystemApp;
                                  case 2:
                                      return !appInfo.isSystemApp;
                                  default:
                                      return true;
                              }
                          }
                      }).observeOn(AndroidSchedulers.mainThread())
                      .subscribeOn(Schedulers.io())
                      .buffer(1000)
                      .flatMap(new Function<List<AppInfo>, Observable<DiffUtil.DiffResult>>()
                               {
                                   @Override
                                   public Observable<DiffUtil.DiffResult> apply(
                                           final List<AppInfo> value) throws Exception
                                   {
                                       return new Observable<DiffUtil.DiffResult>()
                                       {
                                           @Override
                                           protected void subscribeActual(
                                                   Observer<? super DiffUtil.DiffResult> observer)
                                           {
                                               onSortApplications(value);
                                               DiffUtil.DiffResult diffResult = DiffUtil
                                                       .calculateDiff(new AppCallback(list, value), true);
                                               observer.onNext(diffResult);
                                               list = value;
                                               observer.onComplete();
                                           }
                                       };
                                   }
                               }).subscribe(new Consumer<DiffUtil.DiffResult>() {
                @Override
                public void accept(DiffUtil.DiffResult diffResult) throws Exception
                {
                    diffResult.dispatchUpdatesTo(appAdapter);
                    refreshLayout.setRefreshing(false);
                    recyclerView.scrollToPosition(0);
                }
            });
//                      }).subscribe(new Observer<DiffUtil.DiffResult>()
//            {
//                @Override
//                public void onSubscribe(Disposable d)
//                {
//                    refreshLayout.setRefreshing(true);
//                }
//
//                @Override
//                public void onNext(DiffUtil.DiffResult value)
//                {
//                    value.dispatchUpdatesTo(appAdapter);
//                }
//
//                @Override
//                public void onError(Throwable e)
//                {
//                    refreshLayout.setRefreshing(false);
//                }
//
//                @Override
//                public void onComplete()
//                {
//                    refreshLayout.setRefreshing(false);
//                    recyclerView.scrollToPosition(0);
//                }
//            });
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
        if(cannotAccess())
        {
            return;
        }

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
                                        showSelectDbDialog(MainActivity.this, appInfo);
                                        break;
                                }
                                return true;
                            }
                        }).show();
    }

    void goToSpActivity(AppInfo appInfo)
    {
        Intent intent = new Intent(MainActivity.this, SpActivity.class);
        intent.putExtra(EXTRA_SP_APP_INFO, appInfo);
        startActivity(intent);
    }

    public static void showSelectDbDialog(final Activity activity, final AppInfo appInfo)
    {
        String dbPath = Path.getDbPath(appInfo.packageName);
        Roots.get().requestReadPermisson(dbPath);
        final File[] files = FileTool.listFilesDislikeNamesNoCase(dbPath, "-journal");
        if(files == null || files.length == 0)
        {
            ToastTool.show(R.string.no_databases);
            return;
        }

        final String[] names = FileTool.toFileNames(files);
        Arrays.sort(files);
        Arrays.sort(names);
        new MaterialDialog.Builder(activity)
                .title(R.string.select_db)
                .items(names)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice()
                {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        Intent intent = new Intent(activity, DatabaseActivity.class);
                        String path = files[which].getAbsolutePath();
                        intent.putExtra(EXTRA_DB_FILEPATH, path);
                        intent.putExtra(EXTRA_SP_APP_INFO, appInfo);
                        activity.startActivity(intent);
                        return true;
                    }
                }).show();
    }

}
