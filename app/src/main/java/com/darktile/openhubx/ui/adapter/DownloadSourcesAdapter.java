package com.darktile.openhubx.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import android.view.View;
import android.widget.TextView;

import com.darktile.openhubx.R;
import com.darktile.openhubx.http.Downloader;
import com.darktile.openhubx.mvp.model.DownloadSource;
import com.darktile.openhubx.ui.adapter.base.BaseAdapter;
import com.darktile.openhubx.ui.adapter.base.BaseViewHolder;
import com.darktile.openhubx.util.AppOpener;
import com.darktile.openhubx.util.StringUtils;
/**
 * Created by ThirtyDegreesRay on 2017/9/16 15:53:46
 */

public class DownloadSourcesAdapter extends BaseAdapter<DownloadSourcesAdapter.ViewHolder, DownloadSource> {

    private String repoName;
    private String tagName;

    public DownloadSourcesAdapter(Context context, String repoName, String tagName) {
        super(context);
        this.repoName = repoName;
        this.tagName = tagName;
    }

    @Override
    protected int getLayoutId(int viewType) {
        return R.layout.layout_item_download_source;
    }

    @Override
    protected ViewHolder getViewHolder(View itemView, int viewType) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        DownloadSource model = data.get(position);
        holder.icon.setImageResource(model.isSourceCode() ? R.drawable.ic_source : R.drawable.ic_collection);
        holder.name.setText(model.getName());
        if(model.getSize() != 0){
            holder.size.setVisibility(View.VISIBLE);
            holder.size.setText(StringUtils.getSizeString(model.getSize()));
        }else{
            holder.size.setVisibility(View.INVISIBLE);
        }
    }

    class ViewHolder extends BaseViewHolder {

        @Override
        protected void bindViews(android.view.View root) {
            super.bindViews(root);
            icon = root.findViewById(R.id.icon);
            name = root.findViewById(R.id.name);
            size = root.findViewById(R.id.size);
            root.findViewById(R.id.download_bn).setOnClickListener(v -> onDownloadClick());
        }

         AppCompatImageView icon;
         TextView name;
         TextView size;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void onDownloadClick(){
            DownloadSource source = data.get(getAdapterPosition());
            String fileName = repoName.concat("-").concat(tagName)
                    .concat("-").concat(source.getName());
            AppOpener.startDownload(context, source.getUrl(), fileName);
        }

    }

}
