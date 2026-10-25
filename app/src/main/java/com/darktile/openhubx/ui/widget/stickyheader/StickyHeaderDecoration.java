package com.darktile.openhubx.ui.widget.stickyheader;

import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws sticky group headers over a RecyclerView, reserving top space for the first item of
 * each group and pinning the current group's header to the top (pushed up by the next group).
 * Self-contained replacement for the archived com.timehop.stickyheadersrecyclerview.
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    public static final long NO_HEADER_ID = -1L;

    @SuppressWarnings("rawtypes")
    private final StickyHeaderAdapter adapter;
    private final Map<Long, RecyclerView.ViewHolder> headerCache = new HashMap<>();

    public StickyHeaderDecoration(@SuppressWarnings("rawtypes") StickyHeaderAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int top = 0;
        if (position != RecyclerView.NO_POSITION && hasHeader(position)) {
            top = getHeader(parent, position).itemView.getHeight();
        }
        outRect.set(0, top, 0, 0);
    }

    private boolean hasHeader(int position) {
        long cur = adapter.getHeaderId(position);
        if (cur == NO_HEADER_ID) {
            return false;
        }
        return position == 0 || cur != adapter.getHeaderId(position - 1);
    }

    @SuppressWarnings("unchecked")
    private RecyclerView.ViewHolder getHeader(RecyclerView parent, int position) {
        long headerId = adapter.getHeaderId(position);
        RecyclerView.ViewHolder holder = headerCache.get(headerId);
        if (holder == null) {
            holder = adapter.onCreateHeaderViewHolder(parent);
            View header = holder.itemView;
            adapter.onBindHeaderViewHolder(holder, position);
            measureHeader(parent, header);
            headerCache.put(headerId, holder);
        }
        return holder;
    }

    private void measureHeader(RecyclerView parent, View header) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
        int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
        int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
        header.measure(childWidth, childHeight);
        header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();
        if (childCount == 0) {
            return;
        }
        int firstPos = parent.getChildAdapterPosition(parent.getChildAt(0));
        if (firstPos == RecyclerView.NO_POSITION) {
            return;
        }
        long headerId = adapter.getHeaderId(firstPos);
        if (headerId == NO_HEADER_ID) {
            return;
        }
        View header = getHeader(parent, firstPos).itemView;
        int headerHeight = header.getHeight();
        float top = 0;
        for (int i = 1; i < childCount; i++) {
            int pos = parent.getChildAdapterPosition(parent.getChildAt(i));
            if (pos == RecyclerView.NO_POSITION) {
                continue;
            }
            if (adapter.getHeaderId(pos) != headerId) {
                float nextTop = parent.getChildAt(i).getY();
                if (nextTop < headerHeight) {
                    top = nextTop - headerHeight;
                }
                break;
            }
        }
        c.save();
        c.translate(0, top);
        header.draw(c);
        c.restore();
    }

    /** Clear cached header views (call when the dataset changes shape). */
    public void clearHeaderCache() {
        headerCache.clear();
    }
}
