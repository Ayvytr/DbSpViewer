package com.ayvytr.dbspviewer.view.activity;

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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ayvytr.dbspviewer.R;
import com.ayvytr.dbspviewer.bean.SpItem;
import com.ayvytr.dbspviewer.utils.Path;
import com.ayvytr.dbspviewer.view.custom.CustomHeaderTextView;
import com.ayvytr.dbspviewer.view.custom.CustomTextView;
import com.ayvytr.easyandroid.bean.AppInfo;
import com.ayvytr.easyandroid.tools.Convert;
import com.ayvytr.easyandroid.tools.FileTool;
import com.ayvytr.easyandroid.tools.withcontext.ClipboardTool;
import com.ayvytr.easyandroid.tools.withcontext.ToastTool;
import com.ayvytr.prettyitemdecoration.header.StickyHeaderAdapter;
import com.ayvytr.prettyitemdecoration.header.StickyHeaderItemDecoration;
import com.ayvytr.root.Roots;

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

import static com.ayvytr.dbspviewer.view.activity.MainActivity.EXTRA_SP_APP_INFO;

public class SpActivity extends AppCompatActivity
{
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    private AppInfo appInfo;
    private int currentIndex;
    private File currentSp;
    private StickyHeaderItemDecoration stickyRecyclerHeadersDecoration;
    private SpItemAdapter spItemAdapter;

    private List<SpItem> list = new ArrayList<>();
    private Snackbar snackbar;
    private SpItem headerItem;
    private boolean isAddedDecoration;

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
        headerItem = new SpItem(getString(R.string.type), getString(R.string.key),
                getString(R.string.value));
        appInfo = getIntent().getParcelableExtra(EXTRA_SP_APP_INFO);
        initView();
        selectSpFile();
    }

    private void initView()
    {
        setTitle(R.string.sp);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        spItemAdapter = new SpItemAdapter();
        recyclerView.setAdapter(spItemAdapter);
        stickyRecyclerHeadersDecoration = new StickyHeaderItemDecoration(spItemAdapter);
        addItemDecoration();
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

    @Override
    protected void onDestroy()
    {
        if(currentSp != null)
        {
            Roots.THIS.cancelReadPermission(currentSp.getAbsolutePath());
        }
        list.clear();
        super.onDestroy();
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

    private void selectSpFile()
    {
        String spPath = Path.getSpPath(appInfo.packageName);
        Roots.THIS.requestReadPermisson(spPath);

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
                .title(R.string.select_sp)
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
        getMenuInflater().inflate(R.menu.menu_sp, menu);
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
        MainActivity.showSelectDbDialog(this, appInfo);
    }

    private void seekListItem()
    {
        if(list.isEmpty() || list.get(0) == headerItem)
        {
            showNoItemMsg();
            return;
        }
        ArrayList<Integer> items = new ArrayList<>();
        for(int i = 0; i < list.size(); i++)
        {
            items.add(i + 1);
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

    public class SpItemAdapter extends RecyclerView.Adapter<SpItemAdapter.Vh>
            implements StickyHeaderAdapter<SpItemAdapter.SpHeaderVh>
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
        public int getId(int position)
        {
            return 0;
        }

        @Override
        public SpHeaderVh onCreateHeaderViewHolder(RecyclerView parent)
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
            @BindView(R.id.tvIndex)
            CustomTextView tvIndex;
            @BindView(R.id.tvType)
            CustomTextView tvType;
            @BindView(R.id.tvKey)
            CustomTextView tvKey;
            @BindView(R.id.tvValue)
            CustomTextView tvValue;
            private View view;

            public Vh(View view)
            {
                super(view);
                this.view = view;
                ButterKnife.bind(this, view);
            }

            public void bind(final int position)
            {
                final SpItem spItem = list.get(position);
                final boolean isHeader = position == 0 && spItem == headerItem;
                if(isHeader)
                {
                    tvIndex.setText(R.string.index);
                }
                else
                {
                    tvIndex.setText(Convert.toString(position + 1));
                }

                tvType.setText(spItem.type);
                tvKey.setText(spItem.key);
                tvValue.setText(spItem.value);

                stickyRecyclerHeadersDecoration.invalidate();

                view.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onShowItemInfo(spItem, position, isHeader);
                    }
                });
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
                tvType.setText(headerItem.type);
                tvKey.setText(headerItem.key);
                tvValue.setText(headerItem.value);
            }
        }
    }

    private void checkItemDecoration()
    {
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

    private void readSp()
    {
        Roots.THIS.requestReadPermisson(currentSp.getAbsolutePath());

        Document document;
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

    private void onShowItemInfo(final SpItem spItem, int position, boolean isHeader)
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
            content = spItem.toString(headerItem);
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
                        selectFieldCopy(spItem);
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

    private void selectFieldCopy(final SpItem spItem)
    {
        final String[] values = new String[]
                {
                        spItem.type,
                        spItem.key,
                        spItem.value
                };
        new MaterialDialog.Builder(this)
                .title(R.string.copy_item)
                .items(values)
                .alwaysCallSingleChoiceCallback()
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice()
                {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which,
                                               CharSequence text)
                    {
                        ClipboardTool.setText(values[which]);
                        ToastTool.show(R.string.copied_field);
                        return true;
                    }
                }).show();
    }

}
