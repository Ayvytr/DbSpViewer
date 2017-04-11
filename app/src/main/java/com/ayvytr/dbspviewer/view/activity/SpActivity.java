package com.ayvytr.dbspviewer.view.activity;

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

import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.bean.SpItem;
import com.ayvytr.dbspviewer.utils.Root;
import com.ayvytr.dbspviewer.view.custom.CustomHeaderTextView;
import com.ayvytr.dbspviewer.view.custom.CustomTextView;
import com.ayvytr.easyandroid.bean.AppInfo;
import com.ayvytr.easyandroid.tools.Convert;
import com.ayvytr.easyandroid.tools.FileTool;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

public class SpActivity extends AppCompatActivity
{
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    private AppInfo appInfo;
    private int currentIndex;
    private File currentSp;
    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;
    private SpItemAdapter spItemAdapter;

    private List<SpItem> list = new ArrayList<>();
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp);
        ButterKnife.bind(this);
        init();
    }

    private void init()
    {
        appInfo = getIntent().getParcelableExtra(MainActivity.EXTRA_SP_APP_INFO);
        initView();
        selectSpFile();
    }

    private void initView()
    {
        setTitle(R.string.db);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        spItemAdapter = new SpItemAdapter();
        recyclerView.setAdapter(spItemAdapter);
        stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(spItemAdapter);
        recyclerView.addItemDecoration(stickyRecyclerHeadersDecoration);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                spItemAdapter.update();
            }
        });
        createSnackBar();
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

    private void selectSpFile()
    {
        String spPath = getSpPath(appInfo.packageName);
        Root.requestReadPermission(spPath);

        final File[] files = FileTool.listFiles(spPath);
        if(files == null || files.length == 0)
        {
            ToastTool.show(R.string.no_sps);
            return;
        }

        final String[] names = FileTool.toFileNames(files);
        Arrays.sort(files);
        Arrays.sort(names);
        new MaterialDialog.Builder(this)
                .title(R.string.select_db)
                .items(names)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(currentIndex,
                        new MaterialDialog.ListCallbackSingleChoice()
                        {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView,
                                                       int which,
                                                       CharSequence text)
                            {
                                currentIndex = which;
                                currentSp = files[which];
                                setTitle(FileTool.getTitle(currentSp));
                                spItemAdapter.update();
                                return true;
                            }
                        }).show();
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
                selectSpFile();
                return true;
            case R.id.menu_id_show_count:
                showItemCount();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getSpPath(String packageName)
    {
        return "/data/data/" + packageName + "/shared_prefs";
    }

    public class SpItemAdapter extends RecyclerView.Adapter<SpItemAdapter.Vh>
            implements StickyRecyclerHeadersAdapter<SpItemAdapter.SpHeaderVh>
    {
        @Override
        public Vh onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new Vh(LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.item_sp, parent, false));
        }

        @Override
        public void onBindViewHolder(Vh holder, int position)
        {
            holder.bind(position);
        }

        @Override
        public long getHeaderId(int position)
        {
            return 0;
        }

        @Override
        public SpHeaderVh onCreateHeaderViewHolder(ViewGroup parent)
        {
            return new SpHeaderVh(LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.item_sp_header, parent, false));
        }

        @Override
        public void onBindHeaderViewHolder(SpHeaderVh holder, int position)
        {
            holder.bind();
        }


        @Override
        public int getItemCount()
        {
            return list.size();
        }

        public void update()
        {
            Observable.create(new ObservableOnSubscribe<List<SpItem>>()
            {
                @Override
                public void subscribe(ObservableEmitter<List<SpItem>> e) throws Exception
                {
                    readSp();
                    e.onNext(list);
                    e.onComplete();
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                      .subscribe(new Observer<List<SpItem>>()
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
                          public void onNext(List<SpItem> value)
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
            @BindView(R.id.tvIndex)
            CustomTextView tvIndex;
            @BindView(R.id.tvType)
            CustomTextView tvType;
            @BindView(R.id.tvKey)
            CustomTextView tvKey;
            @BindView(R.id.tvValue)
            CustomTextView tvValue;

            public Vh(View view)
            {
                super(view);
                ButterKnife.bind(this, view);
            }

            public void bind(int position)
            {
                SpItem spItem = list.get(position);

                tvIndex.setText(Convert.toString(position + 1));
                tvType.setText(spItem.type);
                tvKey.setText(spItem.key);
                tvValue.setText(spItem.value);

                stickyRecyclerHeadersDecoration.invalidateHeaders();
            }
        }

        public class SpHeaderVh extends RecyclerView.ViewHolder
        {
            @BindView(R.id.tvIndex)
            CustomHeaderTextView tvIndex;
            @BindView(R.id.tvType)
            CustomHeaderTextView tvType;
            @BindView(R.id.tvKey)
            CustomHeaderTextView tvKey;
            @BindView(R.id.tvValue)
            CustomHeaderTextView tvValue;

            public SpHeaderVh(View view)
            {
                super(view);
                ButterKnife.bind(this, view);
            }

            public void bind()
            {
                tvIndex.setText(R.string.index);
                tvType.setText(R.string.type);
                tvKey.setText(R.string.key);
                tvValue.setText(R.string.value);
            }
        }
    }

    private void showItemCount()
    {
        snackbar.setText(Convert.toString(list.size()) + "条数据");
        snackbar.show();
    }

    private void readSp()
    {
        Root.requestReadPermission(currentSp.getAbsolutePath());

        Document document = null;
        try
        {
            document = new SAXReader().read(currentSp);
        } catch(DocumentException e)
        {
            e.printStackTrace();
            ToastTool.show(R.string.read_sp_error);
            return;
        }
        Element rootElement = document.getRootElement();
        List<Element> elements = rootElement.elements();
        for(Element element : elements)
        {
            String type = element.getName();
            String name = element.attributeValue("name");
            String value;

            if(type.equalsIgnoreCase("string"))
            {
                value = element.getStringValue();
            }
            else
            {
                value = element.attributeValue("value");
            }

            list.add(new SpItem(type, name, value));
        }
    }
}
