package com.ayvytr.dbspviewer.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import com.miguelcatalan.materialsearchview.MaterialSearchView;

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
        implements NavigationView.OnNavigationItemSelectedListener
{
    public static final String EXTRA_DB_FILEPATH = "extra_db_filepath";
    public static final String EXTRA_SP_APP_INFO = "extra_sp_app_info";

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.nav_view)
    NavigationView navView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.search_view)
    MaterialSearchView searchView;
    private AppAdapter appAdapter;

    private int currentFilterType;
    private int currentSortType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NoActionbar);
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

        MenuItem item = menu.findItem(R.id.menu_id_search);
        searchView.setMenuItem(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_id_filter:
                filterApplications();
                break;
            case R.id.menu_id_sort:
                sortApplications();
                break;
            case R.id.menu_id_search:

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
                        seeGitHub();
                    }
                })
                .show();
    }

    private void seeGitHub()
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/ayvytr"));
        startActivity(intent);
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

        setSupportActionBar(toolbar);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toolbar.setNavigationIcon(R.drawable.launcher_transparent_small);
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        navView.setNavigationItemSelectedListener(this);

        searchView.setVoiceSearch(false);
        searchView.setCursorDrawable(R.drawable.color_cursor_white);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                ToastTool.show("搜索");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                fillSuggestions(newText);
                return false;
            }
        });

//        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener()
//        {
//            @Override
//            public void onSearchViewShown()
//            {
//                //Do some magic
//            }
//
//            @Override
//            public void onSearchViewClosed()
//            {
//                //Do some magic
//            }
//        });
    }

    private void fillSuggestions(String text)
    {
        List<AppInfo> list = appAdapter.getList();
        List<String> strs = new ArrayList<>();
        for(AppInfo appInfo : list)
        {
            if(appInfo.label != null && appInfo.label.contains(text))
            {
                strs.add(appInfo.label);
            }
            if(appInfo.className != null && appInfo.className.contains(text))
            {
                strs.add(appInfo.className);
            }
            if(appInfo.packageName != null && appInfo.packageName.contains(text))
            {
                strs.add(appInfo.packageName);
            }
        }

        searchView.setSuggestions(strs.toArray(new String[strs.size()]));
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.nav_help:
                showHelpInfo();
                break;
            case R.id.nav_view:
                seeGitHub();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Vh>
    {
        private List<AppInfo> list = new ArrayList<>();

        public List<AppInfo> getList()
        {
            return list;
        }

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
                      }).subscribe(new Consumer<DiffUtil.DiffResult>()
            {
                @Override
                public void accept(DiffUtil.DiffResult diffResult) throws Exception
                {
                    diffResult.dispatchUpdatesTo(appAdapter);
                    refreshLayout.setRefreshing(false);
                    recyclerView.scrollToPosition(0);
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
