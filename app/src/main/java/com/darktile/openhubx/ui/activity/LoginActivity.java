

package com.darktile.openhubx.ui.activity;

import android.content.Intent;
import android.net.Uri;
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
import com.darktile.openhubx.auth.qr.QrLoginLink;
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
import com.darktile.openhubx.util.PrefUtils;
import com.darktile.openhubx.util.StringUtils;
import com.darktile.openhubx.util.ViewUtils;
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
            tokenLayout = root.findViewById(R.id.token_layout);
            tokenEt = root.findViewById(R.id.token_et);
            root.findViewById(R.id.row_browser).setOnClickListener(v -> onOauthLoginClick());
            root.findViewById(R.id.row_qr).setOnClickListener(v -> onQrLoginClick());
            root.findViewById(R.id.row_token).setOnClickListener(v -> toggleTokenPanel(root));
            root.findViewById(R.id.token_login_bn).setOnClickListener(v -> onTokenLoginClick());
            root.findViewById(R.id.row_browse).setOnClickListener(v -> onBrowseAnonymousClick());
        }

        /** The token row opens a panel with its steps, the field and the sign-in button. */
        private void toggleTokenPanel(View root) {
            View panel = root.findViewById(R.id.token_panel);
            boolean show = panel.getVisibility() != View.VISIBLE;
            panel.setVisibility(show ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.token_chevron).setRotation(show ? 90f : 0f);
            if (show) tokenEt.requestFocus();
        }

    private final String TAG = LoginActivity.class.getSimpleName();

//     TextInputEditText userNameEt;
//     TextInputLayout userNameLayout;
//     TextInputEditText passwordEt;
//     TextInputLayout passwordLayout;
     TextInputLayout tokenLayout;
     TextInputEditText tokenEt;

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
        if (isQrLoginLink(intent)) {
            handleQrLoginLink(intent);
        } else {
            mPresenter.handleOauth(intent);
        }
        setIntent(null);
    }

    @Override
    public void onGetTokenSuccess(BasicToken basicToken) {
        mPresenter.getUserInfo(basicToken);
    }

    @Override
    public void onGetTokenError(String errorMsg) {
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
        // Cold start from an openhubx://login?d=... link (system camera / browser).
        if (savedInstanceState == null && isQrLoginLink(getIntent())) {
            handleQrLoginLink(getIntent());
            setIntent(new Intent(getIntent()).setData(null));
        }
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

    /** Enters anonymous mode: no account, lands on Trending. Only public tabs work. */
    public void onBrowseAnonymousClick(){
        PrefUtils.set(PrefUtils.BROWSE_ANONYMOUS, true);
        startActivity(new Intent(getActivity(), MainActivity.class));
        getActivity().finish();
    }

    /**
     * Signs in with a token the user pastes. Builds a {@link BasicToken} and hands it to
     * the presenter, which validates it with {@code GET /user} and stores it on success,
     * exactly like the QR path. The token is never logged.
     */
    public void onTokenLoginClick(){
        String token = tokenEt.getText() == null ? "" : tokenEt.getText().toString().trim();
        if (StringUtils.isBlank(token)) {
            tokenLayout.setError(getString(R.string.token_empty_warning));
            return;
        }
        tokenLayout.setErrorEnabled(false);
        BasicToken basicToken = new BasicToken();
        basicToken.setToken(token);
        basicToken.setScopes(Collections.<String>emptyList());
        basicToken.setTokenType(QrLoginPayload.TT_PAT);
        mPresenter.loginWithToken(basicToken);
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
        // In-app scanner (ZXing CaptureActivity); accepts a raw envelope or an openhubx://login link.
        qrScanLauncher.launch(options);
    }

    /**
     * Handles scanned QR contents or an {@code openhubx://login?d=} deep link: decode +
     * validate the login envelope, then validate the token against GitHub before it is
     * stored. On a valid envelope the token (PAT or GitHub App user token, with its
     * expiry and optional refresh data) is handed to the presenter, which calls
     * {@code GET /user}, persists it on success and routes to the home screen exactly as
     * the OAuth path does. Distinct messages are shown for a bad envelope, an expired
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
        basicToken.setTokenType(payload.getTt());
        basicToken.setExpiresAt(exp);
        if (payload.isRefreshable(System.currentTimeMillis() / 1000L)) {
            basicToken.setRefreshToken(payload.getRt());
            basicToken.setRefreshExpiresAt(payload.getRexp());
            basicToken.setClientId(payload.getCid());
        }
        mPresenter.loginWithToken(basicToken);
    }

    /** True for an {@code openhubx://login?d=...} deep link (vs. the OAuth {@code openhub://} callback). */
    private static boolean isQrLoginLink(Intent intent) {
        Uri uri = intent == null ? null : intent.getData();
        return uri != null && QrLoginLink.SCHEME.equalsIgnoreCase(uri.getScheme())
                && QrLoginLink.HOST.equalsIgnoreCase(uri.getHost());
    }

    /** Routes a login deep link through the scanner's import path. Never logs the URI. */
    private void handleQrLoginLink(Intent intent) {
        onQrScanned(intent.getData().toString());
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
