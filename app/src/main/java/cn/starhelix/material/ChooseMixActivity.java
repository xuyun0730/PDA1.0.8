package cn.starhelix.material;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

// 页面调整，当前代码只是备份
public class ChooseMixActivity extends AppCompatActivity {
    public static final String TAG = "ChooseMixActivity";

    public String batchId;
    public String batchName;
    public int maxSerialNum;
    public TextView totalMixBtn;
    public TextView premixBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_mix);

        totalMixBtn = findViewById(R.id.totalMixBtn);
        premixBtn = findViewById(R.id.premixBtn);

        batchId = getIntent().getStringExtra("batch_id");
        Log.i(TAG, "batch id: " + batchId);
        batchName = getIntent().getStringExtra("batch_name");
        Log.i(TAG, "batch name: " + batchName);
        maxSerialNum = getIntent().getIntExtra("max_serial_num", 0);
        Log.i(TAG, "max serial number: " + maxSerialNum);

        totalMixBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MaterialListActivity.class);
            intent.putExtra("batch_id", batchId);
            intent.putExtra("batch_name", batchName);
            intent.putExtra("max_serial_num", maxSerialNum);
            startActivity(intent);
            // 必须finish 确保下次max serial num会得到更新
            finish();
        });

        premixBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, PremixListActivity.class);
            intent.putExtra("batch_id", batchId);
            intent.putExtra("batch_name", batchName);
            startActivity(intent);
            finish();
        });
    }

    public void finishActivity(View v) {
        finish();
    }
}