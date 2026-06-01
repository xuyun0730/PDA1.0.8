package cn.starhelix.material;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.util.PreferenceUtil;
import cn.starhelix.material.util.StrUtil;

public class MainActivity extends AppCompatActivity {
    public TextView totalMixBtn;
    public TextView directFeedBtn;
    public TextView premixBtn;
    public TextView logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (StrUtil.isEmpty(CacheData.getInstance().getToken())) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        totalMixBtn = findViewById(R.id.totalMixBtn);
        directFeedBtn = findViewById(R.id.directFeedBtn);
        premixBtn = findViewById(R.id.premixBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        totalMixBtn.setOnClickListener(view -> openBatchList("overallFeed"));
        directFeedBtn.setOnClickListener(view -> openBatchList("directFeed"));
        premixBtn.setOnClickListener(view -> openBatchList("premixFeed"));

        logoutBtn.setOnClickListener(view -> {
            PreferenceUtil.getInstance().removeToken(this);
            PreferenceUtil.getInstance().removeAccountInfo(this);
            CacheData.getInstance().clearToken();
            CacheData.getInstance().clearAccountInfo();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void openBatchList(String type) {
        Intent intent = new Intent(this, BatchListActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
    }
}
