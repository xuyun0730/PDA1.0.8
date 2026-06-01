package cn.starhelix.material;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import cn.starhelix.material.adapter.BatchListAdapter;
import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.entity.BatchItem;
import cn.starhelix.material.erp.ErpErrorUtil;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BatchListActivity extends LoadingWidgetActivity {
    private static final String TAG = "BatchListActivity";
    private static final int PAGE_SIZE = 15;

    private EditText searchView;
    private TextView searchBtn;
    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    private List<BatchItem> itemList;
    private BatchListAdapter adapter;
    private String type;
    private int currentPage = 1;

    private ErpGateway erpGateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_list);
        type = getIntent().getStringExtra("type");

        searchView = findViewById(R.id.searchView);
        searchBtn = findViewById(R.id.searchBtn);
        recyclerView = findViewById(R.id.recyclerView);
        refreshLayout = findViewById(R.id.refreshLayout);

        erpGateway = ErpGatewayProvider.getGateway();

        initBtn();
        initRecyclerView();
        initRefreshLayout();

        if (itemList.isEmpty()) {
            refreshLayout.autoRefresh();
        }
    }

    private void initBtn() {
        searchBtn.setOnClickListener(view -> {
            String keyword = searchView.getText().toString().trim();
            if (StrUtil.isEmpty(keyword)) {
                Toast.makeText(this, "没有输入关键字", Toast.LENGTH_LONG).show();
            }
            requestBatchListWithKeyword(keyword);
        });
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        itemList = new ArrayList<>();
        adapter = new BatchListAdapter(itemList, this);
        adapter.setOnItemClickListener((pos, view) -> {
            if ("overallFeed".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, MaterialListActivity.class);
                intent.putExtra("batch_id", itemList.get(pos).id);
                intent.putExtra("batch_name", itemList.get(pos).batch);
                startActivity(intent);
            } else if ("directFeed".equalsIgnoreCase(type) || "materialWeigh".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, DirectFeedActivity.class);
                intent.putExtra("batch_id", itemList.get(pos).id);
                intent.putExtra("batch_name", itemList.get(pos).batch);
                startActivity(intent);
            } else if ("premixFeed".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, PremixListActivity.class);
                intent.putExtra("batch_id", itemList.get(pos).id);
                intent.putExtra("batch_name", itemList.get(pos).batch);
                startActivity(intent);
            } else {
                Toast.makeText(this, "无效的操作类型：" + type, Toast.LENGTH_LONG).show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void initRefreshLayout() {
        refreshLayout.setEnableLoadMore(true);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                searchView.setText("");
                loadList(true);
            }
        });

        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                loadList(false);
            }
        });
    }

    private void requestBatchListWithKeyword(String keyword) {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        erpGateway.getBatchList(token, null, keyword, PAGE_SIZE, 1, type, null, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BatchInfo>>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                    }

                    @Override
                    public void onSuccess(List<BatchInfo> batchInfos) {
                        hideLoading();
                        itemList.clear();
                        addBatchItemToList(batchInfos);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        hideLoading();
                        Log.e(TAG, "query batch list failed", throwable);
                        Toast.makeText(BatchListActivity.this, "ERP批次查询失败：" + ErpErrorUtil.resolveMessage(throwable, "未知错误"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void addBatchItemToList(List<BatchInfo> batches) {
        if (batches == null) {
            return;
        }

        for (BatchInfo content : batches) {
            BatchItem batchItem = new BatchItem();
            batchItem.id = content.id;
            batchItem.batch = content.batch;
            if (content.outputProduct != null) {
                batchItem.outProductName = content.outputProduct.name;
            }
            itemList.add(batchItem);
        }
    }

    private void loadList(boolean isRefresh) {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            finishRefreshState(isRefresh, true);
            return;
        }

        String keyword = null;
        if (isRefresh) {
            currentPage = 1;
        } else {
            keyword = searchView.getText().toString().trim();
            if (StrUtil.isEmpty(keyword)) {
                keyword = null;
            }
            currentPage += 1;
        }

        Log.d(TAG, "query page number: " + currentPage);
        erpGateway.getBatchList(token, null, keyword, PAGE_SIZE, currentPage, type, null, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BatchInfo>>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                    }

                    @Override
                    public void onSuccess(List<BatchInfo> batchInfos) {
                        if (isRefresh) {
                            refreshLayout.finishRefresh();
                            itemList.clear();
                            if (batchInfos == null || batchInfos.isEmpty()) {
                                Toast.makeText(BatchListActivity.this, "没有查询到数据", Toast.LENGTH_LONG).show();
                            } else {
                                addBatchItemToList(batchInfos);
                            }
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        if (batchInfos == null || batchInfos.isEmpty()) {
                            refreshLayout.finishLoadMoreWithNoMoreData();
                            rollbackPage();
                            return;
                        }

                        int lastSize = itemList.size();
                        addBatchItemToList(batchInfos);
                        adapter.notifyItemRangeInserted(lastSize, batchInfos.size());
                        if (batchInfos.size() < PAGE_SIZE) {
                            refreshLayout.finishLoadMoreWithNoMoreData();
                        } else {
                            refreshLayout.finishLoadMore();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "load batch list failed", throwable);
                        Toast.makeText(BatchListActivity.this, "ERP批次查询失败：" + ErpErrorUtil.resolveMessage(throwable, "未知错误"), Toast.LENGTH_LONG).show();
                        finishRefreshState(isRefresh, false);
                    }
                });
    }

    private void finishRefreshState(boolean isRefresh, boolean noRollback) {
        if (isRefresh) {
            refreshLayout.finishRefresh();
            return;
        }

        refreshLayout.finishLoadMore();
        if (!noRollback) {
            rollbackPage();
        }
    }

    private void rollbackPage() {
        currentPage -= 1;
        if (currentPage < 1) {
            currentPage = 1;
        }
    }

    public void finishActivity(View v) {
        finish();
    }
}
