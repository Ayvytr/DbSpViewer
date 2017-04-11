package com.ayvytr.dbspviewer.view.activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.bean.DbItem;
import com.ayvytr.dbspviewer.utils.Root;
import com.ayvytr.dbspviewer.view.custom.CustomHeaderTextView;
import com.ayvytr.dbspviewer.view.custom.CustomTextView;
import com.ayvytr.easyandroid.tools.Convert;
import com.ayvytr.easyandroid.tools.withcontext.DensityTool;
import com.ayvytr.easyandroid.tools.withcontext.ResTool;
import com.ayvytr.easyandroid.tools.withcontext.ScreenTool;
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
    private String[] columnNames;
    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;

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
        initView();
        path = getIntent().getStringExtra(EXTRA_DB_FILEPATH);
        getTables();
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
        recyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                dbItemAdapter.update();
            }
        });
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        Root.cancelReadDbPermission(path);
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
        if(cursor.getCount() == 0)
        {
            return;
        }

        list.clear();

        int columnCount = cursor.getColumnCount();
        columnNames = cursor.getColumnNames();
        itemsWidth = new int[columnCount];

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

        checkIfNeedGravity();
    }

    private void showItemCount()
    {
        snackbar.setText(Convert.toString(list.size()) + "条数据");
        snackbar.show();
    }

    private void createSnackBar()
    {
        snackbar = Snackbar
                .make(getWindow().getDecorView(), Convert.toString(list.size()) + "条数据",
                        Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.close, new View.OnClickListener()
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
                              notifyDataSetChanged();
                              refreshLayout.setRefreshing(false);
                          }

                          @Override
                          public void onError(Throwable e)
                          {
                          }

                          @Override
                          public void onComplete()
                          {
                              showItemCount();
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
                tvIndex.setText(Convert.toString(position + 1));
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

                for(int i = 0; i < columnNames.length; i++)
                {
                    CustomHeaderTextView tv = new CustomHeaderTextView(view.getContext());
                    tv.setText(columnNames[i]);
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
}
