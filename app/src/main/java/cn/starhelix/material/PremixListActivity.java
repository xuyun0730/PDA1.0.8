package cn.starhelix.material;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import cn.starhelix.material.adapter.PremixListAdapter;
import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.entity.MaterialItem;
import cn.starhelix.material.erp.ErpErrorUtil;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.erp.model.BatchInfo;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PremixListActivity extends AppCompatActivity {
    private static final String TAG = "PremixListActivity";

    private String batchId;
    private String batchName;

    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    private List<MaterialItem> itemList;
    private PremixListAdapter adapter;
    private ErpGateway erpGateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premix_list);

        batchId = getIntent().getStringExtra("batch_id");
        Log.i(TAG, "batch id: " + batchId);
        batchName = getIntent().getStringExtra("batch_name");
        Log.i(TAG, "batch name: " + batchName);

        recyclerView = findViewById(R.id.recyclerView);
        refreshLayout = findViewById(R.id.refreshLayout);

        erpGateway = ErpGatewayProvider.getGateway();

        initRecyclerView();
        initRefreshLayout();

        if (itemList.isEmpty()) {
            refreshLayout.autoRefresh();
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        itemList = new ArrayList<>();
        adapter = new PremixListAdapter(itemList, this);
        adapter.setOnItemClickListener((pos, view) -> {
            Intent intent = new Intent(this, PremixSubListActivity.class);
            intent.putExtra("batch_id", batchId);
            intent.putExtra("batch_name", batchName);
            intent.putExtra("premix_id", itemList.get(pos).id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void initRefreshLayout() {
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@androidx.annotation.NonNull RefreshLayout refreshLayout) {
                requestPremixList();
            }
        });
    }

    private void requestPremixList() {
        String token = CacheData.getInstance().getToken();
        if (StrUtil.isEmpty(token)) {
            Toast.makeText(this, "请重新登录", Toast.LENGTH_LONG).show();
            refreshLayout.finishRefresh();
            return;
        }

        erpGateway.getBatchList(token, batchId, batchName, 1, 1, "premixFeed", null, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BatchInfo>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull List<BatchInfo> batchInfos) {
                        itemList.clear();
                        if (batchInfos.isEmpty() || batchInfos.get(0).materialList == null || batchInfos.get(0).materialList.isEmpty()) {
                            Toast.makeText(PremixListActivity.this, "列表返回空数据", Toast.LENGTH_LONG).show();
                            adapter.notifyDataSetChanged();
                            refreshLayout.finishRefresh();
                            return;
                        }

                        for (MaterialItem item : batchInfos.get(0).materialList) {
                            if (item.isPremix) {
                                itemList.add(item);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        refreshLayout.finishRefresh();
                        if (itemList.isEmpty()) {
                            Toast.makeText(PremixListActivity.this, "当前批次没有预混料", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, "load premix list failed", e);
                        Toast.makeText(PremixListActivity.this, "ERP预混列表查询失败：" + ErpErrorUtil.resolveMessage(e, "未知错误"), Toast.LENGTH_LONG).show();
                        refreshLayout.finishRefresh();
                    }
                });
    }

    public void finishActivity(View v) {
        finish();
    }
}
