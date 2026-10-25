package com.darktile.openhubx.ui.fragment;

import android.os.Bundle;
import android.widget.TextView;
import android.view.View;

import com.darktile.openhubx.R;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.ui.activity.MarkdownEditorCallback;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;
import com.darktile.openhubx.util.StringUtils;
import com.zzhoujay.richtext.RichText;
/**
 * Created by ThirtyDegreesRay on 2017/9/29 11:52:42
 */
//FIXME click link cause exception
public class MarkdownPreviewFragment extends BaseFragment{

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            previewText = root.findViewById(R.id.preview_text);
        }

    public static MarkdownPreviewFragment create(){
        MarkdownPreviewFragment fragment = new MarkdownPreviewFragment();
        return fragment;
    }

     TextView previewText;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_markdown_preview;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {

    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        previewText.setText(R.string.nothing_to_preview);
    }

    private MarkdownEditorCallback getMarkdownEditorCallback(){
        return (MarkdownEditorCallback) getActivity();
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if(getMarkdownEditorCallback().isTextChanged()){
            if(StringUtils.isBlank(getMarkdownEditorCallback().getText())){
                previewText.setText(R.string.nothing_to_preview);
            }else{
                RichText.fromMarkdown(getMarkdownEditorCallback().getText()).into(previewText);
            }
        }
    }

}
