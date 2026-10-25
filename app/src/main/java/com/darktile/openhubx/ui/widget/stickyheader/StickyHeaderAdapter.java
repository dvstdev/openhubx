package com.darktile.openhubx.ui.widget.stickyheader;

import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Contract for a RecyclerView.Adapter that supplies grouped, date-style sticky headers.
 * Replaces the archived com.timehop.stickyheadersrecyclerview library (no AndroidX build).
 */
public interface StickyHeaderAdapter<VH extends RecyclerView.ViewHolder> {

    /** Stable id for the header group an item belongs to; {@link StickyHeaderDecoration#NO_HEADER_ID} for none. */
    long getHeaderId(int position);

    VH onCreateHeaderViewHolder(ViewGroup parent);

    void onBindHeaderViewHolder(VH holder, int position);
}
