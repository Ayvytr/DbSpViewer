package com.ayvytr.dbspviewer.view.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.bean.DbItem;
import com.ayvytr.dbspviewer.utils.Root;
import com.ayvytr.dbspviewer.view.custom.CustomHeaderTextView;
import com.ayvytr.dbspviewer.view.custom.CustomTextView;
import com.ayvytr.easyandroid.bean.AppInfo;
import com.ayvytr.easyandroid.tools.Convert;
import com.ayvytr.easyandroid.tools.withcontext.ClipboardTool;
import com.ayvytr.easyandroid.tools.withcontext.DensityTool;
import com.ayvytr.easyandroid.tools.withcontext.ResTool;
import com.ayvytr.easyandroid.tools.withcontext.ScreenTool;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.util.ArrayList;
import java.util.Collections;
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

import static com.ayvytr.dbspviewer.view.activity.MainActivity.EXTRA_DB_FILEPATH;
import static com.ayvytr.dbspviewer.view.activity.MainActivity.EXTRA_SP_APP_INFO;

public class DatabaseActivity extends AppCompatActivity
{

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    private SQLiteDatabase db;

    private String path;
    private List<String> tables = new ArrayList<>();
    private String currentTable;
    private int currentTableIndex;

    private List<DbItem> list = new ArrayList<>();
    private DbItemAdapter dbItemAdapter;

    private int[] itemsWidth;
    private boolean needGravity;
    private int maxItemWidth;
    private Snackbar snackbar;
    private int screenWidth;
    private AppInfo appInfo;
    private DbItem headerItem;

    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;
    private boolean isAddedDecoration = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        ButterKnife.bind(this);
        init();
    }

    private void init()
    {
        initExtra();
        initView();
        getTables();
    }

    private void initExtra()
    {
        path = getIntent().getStringExtra(EXTRA_DB_FILEPATH);
        appInfo = getIntent().getParcelableExtra(EXTRA_SP_APP_INFO);
    }

    private void initView()
    {
        maxItemWidth = DensityTool.dp2px(200);
        screenWidth = ScreenTool.getScreenWidth();
        setTitle(R.string.db);

        createSnackBar();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbItemAdapter = new DbItemAdapter();
        recyclerView.setAdapter(dbItemAdapter);
        stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(dbItemAdapter);
        addItemDecoration();
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                dbItemAdapter.update();
            }
        });
    }

    private void addItemDecoration()
    {
        if(!isAddedDecoration)
        {
            recyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        }

        isAddedDecoration = true;
    }

    private void removeItemDecoration()
    {
        recyclerView.removeItemDecoration(stickyRecyclerHeadersDecoration);
        isAddedDecoration = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_id_select_table:
                selectTable();
                return true;
            case R.id.menu_id_show_count:
                showItemCount();
                return true;
            case R.id.menu_id_seek:
                seekListItem();
                break;
            case R.id.menu_id_fast_seek:
                fastSeek();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fastSeek()
    {
        Intent intent = new Intent(this, SpActivity.class);
        intent.putExtra(EXTRA_SP_APP_INFO, appInfo);
        startActivity(intent);
    }

    private void seekListItem()
    {
        if(list.isEmpty())
        {
            showNoItemMsg();
            return;
        }
        ArrayList<Integer> items = new ArrayList<>();
        for(int i = 0; i < list.size(); i++)
        {
            items.set(i, i + 1);
        }
        new MaterialDialog.Builder(this)
                .title(R.string.seek_item)
                .items(items)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice()
                {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        recyclerView.scrollToPosition(which);
                        return true;
                    }
                }).show();
    }

    private void showNoItemMsg()
    {
        snackbar.setText(R.string.no_db_items);
        snackbar.show();
    }

    @Override
    protected void onDestroy()
    {
        Root.cancelReadDbPermission(path);
        list.clear();
        super.onDestroy();
    }

    private void getTables()
    {
        Root.requestReadDbPermission(path);

        db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db
                .rawQuery("select name from sqlite_master where type='table' order by name", null);
        while(cursor.moveToNext())
        {
            String name = cursor.getString(0);
            if(!isMetadataTable(name))
            {
                tables.add(name);
            }
        }

        Collections.sort(tables);
        selectTable();
    }

    private void selectTable()
    {
        if(tables.isEmpty())
        {
            snackbar.setText(R.string.empty_table).show();
            return;
        }
        new MaterialDialog.Builder(this)
                .title(R.string.select_table)
                .items(tables)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(currentTableIndex,
                        new MaterialDialog.ListCallbackSingleChoice()
                        {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView,
                                                       int which,
                                                       CharSequence text)
                            {
                                currentTable = tables.get(which);
                                currentTableIndex = which;
                                dbItemAdapter.update();
                                setTitle(currentTable);
                                return true;
                            }
                        }).show();
    }

    private void readTable()
    {
        Cursor cursor = db.query(currentTable, null, null, null, null, null, null);

        list.clear();

        int columnCount = cursor.getColumnCount();
        itemsWidth = new int[columnCount];

        //防止列名显示不全，需要进行文字长度测量
        headerItem = new DbItem(cursor.getColumnNames());
        measureItemWidth(headerItem);

        while(cursor.moveToNext())
        {
            DbItem dbItem = new DbItem(columnCount);
            for(int i = 0; i < columnCount; i++)
            {
                dbItem.values[i] = cursor.getString(i);
            }

            list.add(dbItem);
            measureItemWidth(dbItem);
        }
        cursor.close();

        checkIfNeedGravity();
    }

    private void checkItemDecoration()
    {
        //如果是空表，显示表列名列表
        if(list.isEmpty())
        {
            list.add(headerItem);
            removeItemDecoration();
        }
        else
        {
            addItemDecoration();
        }
    }

    private void showItemCount()
    {
        int count = list.size();
        if(list.size() == 1 && list.get(0) == headerItem)
        {
            count = 0;
        }
        snackbar.setText(Convert.toString(count) + "条数据");
        snackbar.show();
    }

    private void createSnackBar()
    {
        snackbar = Snackbar
                .make(getWindow().getDecorView(), Convert.toString(list.size()) + "条数据",
                        Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.confirm, new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                snackbar.dismiss();
            }
        });
    }

    private void checkIfNeedGravity()
    {
        int totalWidth = 0;
        for(int i : itemsWidth)
        {
            totalWidth += i;
        }

        if(totalWidth < ScreenTool.getScreenWidth())
        {
            needGravity = true;
        }
        else
        {
            needGravity = false;
        }
    }

    private void measureItemWidth(DbItem item)
    {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        for(int i = 0; i < item.values.length; i++)
        {
            tv.setText(item.values[i]);
            tv.measure(0, 0);

            int width = tv.getMeasuredWidth();
            if(width > itemsWidth[i])
            {
                itemsWidth[i] = width;
                if(itemsWidth[i] > maxItemWidth)
                {
                    itemsWidth[i] = maxItemWidth;
                }
            }
        }
    }

    private boolean isMetadataTable(String name)
    {
        return "android_metadata".equals(name);
    }

    public class DbItemAdapter extends RecyclerView.Adapter<DbItemAdapter.Vh>
            implements StickyRecyclerHeadersAdapter<DbItemAdapter.HeaderVh>
    {
        @Override
        public Vh onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new Vh(LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.item_db, parent,
                                                false));
        }

        @Override
        public void onBindViewHolder(Vh holder, int position)
        {
            holder.update(position);
        }

        @Override
        public long getHeaderId(int position)
        {
            return 0;
        }

        @Override
        public HeaderVh onCreateHeaderViewHolder(ViewGroup parent)
        {
            return new HeaderVh(LayoutInflater.from(parent.getContext())
                                              .inflate(R.layout.item_db, parent,
                                                      false));
        }

        @Override
        public void onBindHeaderViewHolder(HeaderVh holder, int position)
        {
            holder.update();
        }

        @Override
        public int getItemCount()
        {
            return list.size();
        }

        public void update()
        {
            Observable.create(new ObservableOnSubscribe<List<DbItem>>()
            {
                @Override
                public void subscribe(ObservableEmitter<List<DbItem>> e) throws Exception
                {
                    readTable();
                    e.onNext(list);
                    e.onComplete();
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                      .subscribe(new Observer<List<DbItem>>()
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
                          public void onNext(List<DbItem> value)
                          {
                              checkItemDecoration();
                              notifyDataSetChanged();
                          }

                          @Override
                          public void onError(Throwable e)
                          {
                          }

                          @Override
                          public void onComplete()
                          {
                              showItemCount();
                              refreshLayout.setRefreshing(false);
                          }
                      });
        }

        public class Vh extends RecyclerView.ViewHolder
        {
            private ViewGroup view;

            public Vh(View view)
            {
                super(view);
                this.view = (ViewGroup) view;
            }

            public void update(final int position)
            {
                final DbItem dbItem = list.get(position);
                view.removeAllViews();
                view.setMinimumWidth(screenWidth);

                CustomTextView tvIndex = new CustomTextView(view.getContext());
                final boolean isHeader = position == 0 && dbItem == headerItem;
                if(isHeader)
                {
                    tvIndex.setText(R.string.index);
                }
                else
                {
                    tvIndex.setText(Convert.toString(position + 1));
                }

                tvIndex.setWidth(DensityTool.dp2px(60));
                tvIndex.setBackgroundColor(Color.WHITE);
                tvIndex.setBackgroundResource(R.drawable.tv_bg);
                tvIndex.setTextColor(ResTool.getColor(R.color.colorAccent));
                view.addView(tvIndex,
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                for(int i = 0; i < dbItem.values.length; i++)
                {
                    CustomTextView tv = new CustomTextView(view.getContext());
                    tv.setText(dbItem.values[i]);
                    tv.setWidth(itemsWidth[i] + DensityTool.dp2px(20));
                    if(needGravity)
                    {
                        view.addView(tv,
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                    }
                    else
                    {
                        view.addView(tv,
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                }

                stickyRecyclerHeadersDecoration.invalidateHeaders();

                view.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onShowItemInfo(dbItem, position, isHeader);
                    }
                });
            }
        }

        public class HeaderVh extends RecyclerView.ViewHolder
        {
            private ViewGroup view;

            public HeaderVh(View view)
            {
                super(view);
                this.view = (ViewGroup) view;
            }

            public void update()
            {
                view.removeAllViews();
                view.setMinimumWidth(screenWidth);

                CustomHeaderTextView tvIndex = new CustomHeaderTextView(view.getContext());
                tvIndex.setText("Index");
                tvIndex.setWidth(DensityTool.dp2px(60));
                view.addView(tvIndex,
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                for(int i = 0; i < headerItem.values.length; i++)
                {
                    CustomHeaderTextView tv = new CustomHeaderTextView(view.getContext());
                    tv.setText(headerItem.values[i]);
                    tv.setWidth(itemsWidth[i] + DensityTool.dp2px(20));
                    if(needGravity)
                    {
                        view.addView(tv,
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
                    }
                    else
                    {
                        view.addView(tv,
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                }
            }
        }
    }

    private void onShowItemInfo(final DbItem dbItem, int position, boolean isHeader)
    {
        String title;
        final String content;
        if(isHeader)
        {
            title = "表头信息";
            content = headerItem.toString();
        }
        else
        {
            title = "第" + Convert.toString(position + 1) + "条条目信息";
            content = dbItem.toString(headerItem);
        }

        new MaterialDialog.Builder(this)
                .title(title)
                .content(content)
                .neutralText(R.string.select_field_copy)
                .negativeText(R.string.copy_item)
                .positiveText(R.string.confirm)
                .onNeutral(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                    {
                        selectFieldCopy(dbItem);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                    {
                        ClipboardTool.setText(content);
                        ToastTool.show(R.string.copied_item);
                    }
                })
                .show();
    }

    private void selectFieldCopy(final DbItem dbItem)
    {
        new MaterialDialog.Builder(this)
                .title(R.string.copy_item)
                .items(dbItem.values)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice()
                {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        ClipboardTool.setText(dbItem.values[which]);
                        ToastTool.show(R.string.copied_field);
                        return true;
                    }
                }).show();
    }

}
