package cn.starhelix.material;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.net.InetAddresses;

import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.util.HttpRequestUtil;
import cn.starhelix.material.util.PreferenceUtil;
import cn.starhelix.material.util.StrUtil;

public class ConfigServerActivity extends AppCompatActivity {
    private EditText ipView;
    private EditText portView;
    private TextView configBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_server);

        ipView = findViewById(R.id.ipView);
        portView = findViewById(R.id.portView);
        configBtn = findViewById(R.id.configBtn);

        ipView.setText(HttpRequestUtil.getBackendHost());
        portView.setText(String.valueOf(HttpRequestUtil.getBackendPort()));

        configBtn.setOnClickListener(view -> saveServerConfig());
    }

    private void saveServerConfig() {
        String ip = ipView.getText().toString().trim();
        String port = portView.getText().toString().trim();

        if (!InetAddresses.isInetAddress(ip)) {
            Toast.makeText(this, "请输入正确的服务器 IP", Toast.LENGTH_LONG).show();
            return;
        }

        if (!StrUtil.isInteger(port)) {
            Toast.makeText(this, "请输入正确的服务器端口", Toast.LENGTH_LONG).show();
            return;
        }

        int portInt = Integer.parseInt(port);
        if (portInt <= 0 || portInt > 65535) {
            Toast.makeText(this, "服务器端口范围应为 1-65535", Toast.LENGTH_LONG).show();
            return;
        }

        PreferenceUtil.getInstance().storeServerHost(this, ip);
        PreferenceUtil.getInstance().storeServerPort(this, portInt);
        HttpRequestUtil.resetRetrofit();
        ErpGatewayProvider.reset();

        Toast.makeText(this, "服务器配置已保存", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void finishActivity(View v) {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
