

package com.darktile.openhubx.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.auth.qr.QrLoginParseException;
import com.darktile.openhubx.auth.qr.QrLoginPayload;
import com.darktile.openhubx.auth.qr.QrLoginPayloadParser;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerActivityComponent;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.mvp.contract.ILoginContract;
import com.darktile.openhubx.mvp.model.BasicToken;
import com.darktile.openhubx.mvp.presenter.LoginPresenter;
import com.darktile.openhubx.ui.activity.base.BaseActivity;
import com.darktile.openhubx.util.AppOpener;
import com.darktile.openhubx.util.StringUtils;
import com.darktile.openhubx.util.ViewUtils;
import com.darktile.openhubx.ui.widget.SubmitButton;
import es.dmoral.toasty.Toasty;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created on 2017/7/12.
 *
 * @author ThirtyDegreesRay
 */

public class LoginActivity extends BaseActivity<LoginPresenter>
        implements ILoginContract.View {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            loginBn = root.findViewById(R.id.login_bn);
            root.findViewById(R.id.login_bn).setOnClickListener(v -> onOauthLoginClick());
            root.findViewById(R.id.qr_login_bn).setOnClickListener(v -> onQrLoginClick());
        }

    private final String TAG = LoginActivity.class.getSimpleName();

//     TextInputEditText userNameEt;
//     TextInputLayout userNameLayout;
//     TextInputEditText passwordEt;
//     TextInputLayout passwordLayout;
     SubmitButton loginBn;

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toasty.info(getApplicationContext(),
                            getString(R.string.qr_scan_cancelled)).show();
                } else {
                    onQrScanned(result.getContents());
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchQrScanner();
                } else {
                    Toasty.warning(getApplicationContext(),
                            getString(R.string.qr_camera_permission_denied)).show();
                }
            });

//    private String userName;
//    private String password;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mPresenter.handleOauth(intent);
        setIntent(null);
    }

    @Override
    public void onGetTokenSuccess(BasicToken basicToken) {
        loginBn.doResult(true);
        mPresenter.getUserInfo(basicToken);
    }

    @Override
    public void onGetTokenError(String errorMsg) {
        loginBn.doResult(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loginBn.reset();
                loginBn.setEnabled(true);
            }
        }, 1000);

        Toasty.error(getApplicationContext(), errorMsg).show();
    }

    @Override
    public void onLoginComplete() {
        delayFinish();
        startActivity(new Intent(getActivity(), MainActivity.class));
    }

    /**
     * 依赖注入的入口
     *
     * @param appComponent appComponent
     */
    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    /**
     * 获取ContentView id
     *
     * @return
     */
    @Override
    protected int getContentView() {
        return R.layout.activity_login_new;
    }

    /**
     * 初始化view
     *
     * @param savedInstanceState
     */
    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
//        loginBn.setOnResultEndListener(new SubmitButton.OnResultEndListener() {
//            @Override
//            public void onResultEnd() {
//
//            }
//        });


//        passwordEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if(actionId == EditorInfo.IME_ACTION_SEND
//                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
//                        event.getAction() == KeyEvent.ACTION_DOWN)){
//                    if(loginBn.isEnabled())
//                        ViewUtils.virtualClick(loginBn);
//                }
//                return false;
//            }
//        });

    }


    public void onOauthLoginClick(){
        AppOpener.openInCustomTabsOrBrowser(getActivity(), mPresenter.getOAuth2Url());
    }

    public void onQrLoginClick(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner(){
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.qr_scan_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        // In-app scanner only (ZXing CaptureActivity); no system-camera deep-link.
        qrScanLauncher.launch(options);
    }

    /**
     * Handles scanned QR contents: decode + validate the login envelope, then
     * validate the token against GitHub before it is stored. On a valid envelope
     * the decoded PAT is handed to the presenter, which calls {@code GET /user},
     * persists the token on success and routes to the home screen exactly as the
     * OAuth path does. Distinct messages are shown for a bad envelope, an expired
     * code, or a token GitHub rejects.
     */
    private void onQrScanned(String contents){
        QrLoginPayload payload;
        try {
            // Parse without the parser's wall-clock expiry gate (Long.MIN_VALUE
            // disables it) so we can surface a distinct "expired" message below.
            payload = new QrLoginPayloadParser().parse(contents, Long.MIN_VALUE);
        } catch (QrLoginParseException e) {
            Toasty.error(getApplicationContext(),
                    getString(R.string.qr_error_invalid)).show();
            return;
        }

        Long exp = payload.getExp();
        if (exp != null && exp <= System.currentTimeMillis() / 1000L) {
            Toasty.error(getApplicationContext(),
                    getString(R.string.qr_error_expired)).show();
            return;
        }

        BasicToken basicToken = new BasicToken();
        basicToken.setToken(payload.getTok());
        String scope = payload.getScope();
        basicToken.setScopes(scope == null || scope.isEmpty()
                ? Collections.<String>emptyList()
                : Arrays.asList(scope.split(",")));
        mPresenter.loginWithToken(basicToken);
    }

    @Override
    public void onQrTokenRejected() {
        Toasty.error(getApplicationContext(),
                getString(R.string.qr_token_rejected)).show();
    }

//    
//    public void onLoginClick(){
//        if(loginCheck()){
//            loginBn.setEnabled(false);
//            mPresenter.basicLogin(userName, password);
//        }else{
//            loginBn.reset();
//        }
//    }

//    private boolean loginCheck(){
//        boolean valid = true;
//        userName = userNameEt.getText().toString();
//        password = passwordEt.getText().toString();
//        if(StringUtils.isBlank(userName)){
//            valid = false;
//            userNameLayout.setError(getString(R.string.user_name_warning));
//        }else{
//            userNameLayout.setErrorEnabled(false);
//        }
//        if(StringUtils.isBlank(password)){
//            valid = false;
//            passwordLayout.setError(getString(R.string.password_warning));
//        }else{
//            passwordLayout.setErrorEnabled(false);
//        }
//        return valid;
//    }
}
