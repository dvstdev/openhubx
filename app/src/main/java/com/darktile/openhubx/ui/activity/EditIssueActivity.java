

package com.darktile.openhubx.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.AppData;
import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerActivityComponent;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.mvp.contract.IEditIssueContract;
import com.darktile.openhubx.mvp.model.Issue;
import com.darktile.openhubx.mvp.model.Label;
import com.darktile.openhubx.mvp.presenter.EditIssuePresenter;
import com.darktile.openhubx.ui.activity.base.BaseActivity;
import com.darktile.openhubx.ui.widget.ChooseLabelsDialog;
import com.darktile.openhubx.util.BundleHelper;
import com.darktile.openhubx.util.StringUtils;
import com.darktile.openhubx.util.ViewUtils;

import java.util.ArrayList;
/**
 * Created by ThirtyDegreesRay on 2017/10/13 17:37:35
 */

public class EditIssueActivity extends BaseActivity<EditIssuePresenter>
        implements IEditIssueContract.View, ChooseLabelsDialog.ChooseLabelsListener {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            titleEdit = root.findViewById(R.id.title_edit);
            titleLayout = root.findViewById(R.id.title_layout);
            commentEdit = root.findViewById(R.id.comment_edit);
            commentLayout = root.findViewById(R.id.comment_layout);
            labelsText = root.findViewById(R.id.labels_text);
            editLabelsLay = root.findViewById(R.id.edit_labels);
            root.findViewById(R.id.markdown_editor_bn).setOnClickListener(v -> onViewClicked(v));
            root.findViewById(R.id.edit_labels).setOnClickListener(v -> onViewClicked(v));
        }

     TextInputEditText titleEdit;
     TextInputLayout titleLayout;
     TextInputEditText commentEdit;
     TextInputLayout commentLayout;
     TextView labelsText;
     LinearLayout editLabelsLay;

    public static void showForAdd(@NonNull Activity activity, @NonNull String userId,
                                  @NonNull String repoName, int requestCode) {
        Intent intent = new Intent(activity, EditIssueActivity.class);
        intent.putExtras(BundleHelper.builder().put("addMode", true).put("userId", userId)
                .put("repoName", repoName).build());
        activity.startActivityForResult(intent, requestCode);
    }

    public static void showForEdit(@NonNull Activity activity, @NonNull Issue issue, int requestCode) {
        Intent intent = new Intent(activity, EditIssueActivity.class);
        intent.putExtras(BundleHelper.builder().put("addMode", false).put("issue", issue).build());
        activity.startActivityForResult(intent, requestCode);
    }

    private final int MARKDOWN_EDITOR_REQUEST_CODE= 100;
    @AutoAccess String titleEditStr;
    @AutoAccess String commentEditStr;


    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_edit_issue;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_commit){
            if(checkForCommit())
                mPresenter.commitIssue(titleEdit.getText().toString(), commentEdit.getText().toString());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();
        setToolbarTitle(mPresenter.isAddMode() ? getString(R.string.create_issue) : getString(R.string.edit_issue));
        titleEdit.setText(mPresenter.getIssueTitle());
        commentEdit.setText(mPresenter.getIssueComment());
        titleEdit.setSelection(titleEdit.length());
        commentEdit.setSelection(commentEdit.length());

        boolean isOwner = AppData.INSTANCE.getLoggedUser().getLogin().equals(mPresenter.getRepoOwner());
        if(isOwner){
            editLabelsLay.setVisibility(View.VISIBLE);
            setLabelsText();
        } else {
            editLabelsLay.setVisibility(View.GONE);
        }
    }

    @Override
    public void showNewIssue(@NonNull Issue issue) {
        showSuccessToast(mPresenter.isAddMode() ? getString(R.string.create_issue_success) :
                getString(R.string.edit_issue_success));
        Intent data = new Intent();
        data.putExtra("issue", issue);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onLoadLabelsComplete(ArrayList<Label> labels) {
        new ChooseLabelsDialog(getActivity(), labels, this).show();
    }

    public void onViewClicked(View view) {
        int viewId = view.getId();
        if (viewId == R.id.markdown_editor_bn) {
            MarkdownEditorActivity.show(getActivity(), R.string.edit,
                    MARKDOWN_EDITOR_REQUEST_CODE, commentEdit.getText().toString());
        } else if (viewId == R.id.edit_labels) {
            mPresenter.loadLabels();
        }
    }

    private boolean checkForCommit(){
        if(StringUtils.isBlank(titleEdit.getText().toString())){
            titleLayout.setError(getString(R.string.issue_title_null_tip));
            return false;
        }else {
            titleLayout.setErrorEnabled(false);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;
        if(requestCode == MARKDOWN_EDITOR_REQUEST_CODE){
            String text = data.getStringExtra("text");
            commentEdit.setText(text);
            commentEdit.requestFocus();
            commentEdit.setSelection(text.length());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        titleEditStr = titleEdit.getText().toString();
        commentEditStr = commentEdit.getText().toString();
    }

    private void setLabelsText(){
        labelsText.setText(ViewUtils.getLabelsSpan(getActivity(), mPresenter.getLabels()));
    }

    @Override
    public void onChooseLabelsComplete(@NonNull ArrayList<Label> labels) {
        mPresenter.getIssue().setLabels(labels);
        setLabelsText();
    }

    @Override
    public void onShowManageLabels() {
        LabelManageActivity.show(getActivity(), mPresenter.getRepoOwner(), mPresenter.getRepoName());
        mPresenter.clearAllLabelsData();
    }
}
