package cn.starhelix.material;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.entity.AccountBasicInfo;
import cn.starhelix.material.erp.ErpErrorUtil;
import cn.starhelix.material.erp.ErpGateway;
import cn.starhelix.material.erp.ErpGatewayProvider;
import cn.starhelix.material.erp.model.LoginResult;
import cn.starhelix.material.util.AppUtil;
import cn.starhelix.material.util.PreferenceUtil;
import cn.starhelix.material.util.StrUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginActivity extends LoadingWidgetActivity {
    private static final String TAG = "LoginActivity";

    private EditText nameEditText;
    private EditText passwordEditText;
    private TextView versionText;
    private TextView configText;
    private TextView switchUserText;

    private ErpGateway erpGateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        nameEditText = findViewById(R.id.nameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        versionText = findViewById(R.id.versionText);
        configText = findViewById(R.id.configText);
        switchUserText = findViewById(R.id.switchUserText);

        erpGateway = ErpGatewayProvider.getGateway();

        // 登录页只允许手动登录；进入这里先清掉旧 token，避免账号切换沿用上一次会话。
        CacheData.getInstance().clearToken();
        PreferenceUtil.getInstance().removeToken(this);

        AccountBasicInfo accountInfo = PreferenceUtil.getInstance().retrieveAccountInfo(this);
        if (accountInfo != null) {
            nameEditText.setText(accountInfo.name);
        }
        passwordEditText.setText("");

        versionText.setText(String.format("v %s", AppUtil.getVerName(this)));
        configText.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, ConfigServerActivity.class));
            finish();
        });
        switchUserText.setOnClickListener(view -> clearCurrentUser());
    }

    public void reqLogin(View v) {
        String name = nameEditText.getText().toString().trim();
        if (StrUtil.isEmpty(name)) {
            Toast.makeText(this, "请输入账号", Toast.LENGTH_LONG).show();
            return;
        }

        String password = passwordEditText.getText().toString().trim();
        if (StrUtil.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        erpGateway.login(name, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LoginResult>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                    }

                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        hideLoading();
                        if (loginResult == null || StrUtil.isEmpty(loginResult.token)) {
                            Toast.makeText(LoginActivity.this, "登录失败：没有返回 token", Toast.LENGTH_LONG).show();
                            return;
                        }

                        CacheData cacheData = CacheData.getInstance();
                        cacheData.setToken(loginResult.token);

                        AccountBasicInfo accountBasicInfo = new AccountBasicInfo();
                        accountBasicInfo.name = name;
                        cacheData.setAccountInfo(accountBasicInfo);

                        PreferenceUtil.getInstance().storeToken(LoginActivity.this, loginResult.token);
                        PreferenceUtil.getInstance().storeAccountInfo(LoginActivity.this, accountBasicInfo);

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        hideLoading();
                        Log.e(TAG, "login failed", throwable);
                        String prefix = ErpErrorUtil.isApiException(throwable) ? "登录失败：" : "登录异常：";
                        Toast.makeText(LoginActivity.this, prefix + ErpErrorUtil.resolveMessage(throwable, "未知错误"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearCurrentUser() {
        CacheData.getInstance().clearToken();
        CacheData.getInstance().clearAccountInfo();
        PreferenceUtil.getInstance().removeToken(this);
        PreferenceUtil.getInstance().removeAccountInfo(this);
        nameEditText.setText("");
        passwordEditText.setText("");
        nameEditText.requestFocus();
    }
}
