package com.darktile.openhubx.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.darktile.openhubx.R;
import com.darktile.openhubx.mvp.model.CodeSearchItem;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.ui.fragment.base.BaseFragment;

import javax.inject.Inject;

/** Code search results: file path + repository. */
public class CodeSearchAdapter extends BaseAdapter<CodeSearchAdapter.ViewHolder, CodeSearchItem> {

    @Inject
    public CodeSearchAdapter(Context context, BaseFragment fragment) {
        super(context, fragment);
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_code_search;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        CodeSearchItem model = data.get(position);
        holder.fileName.setText(model.getName());
        holder.filePath.setText(model.getPath());
        holder.repoName.setText(model.getRepository() == null ? "" : model.getRepository().getFullName());
    }

    class ViewHolder extends BaseViewHolder {

        TextView fileName;
        TextView filePath;
        TextView repoName;

        @Override
        protected void bindViews(View root) {
            super.bindViews(root);
            fileName = root.findViewById(R.id.file_name);
            filePath = root.findViewById(R.id.file_path);
            repoName = root.findViewById(R.id.repo_name);
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
