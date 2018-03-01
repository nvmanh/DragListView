package com.woxthebox.draglistview;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by FRAMGIA\nguyen.viet.manh on 2/27/18.
 */

public class KanbanBoard extends BoardView {
    public KanbanBoard(Context context) {
        super(context);
    }

    public KanbanBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KanbanBoard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DragItemRecyclerView addColumnLast(DragItemAdapter adapter, View header) {
        final DragItemRecyclerView recyclerView = (DragItemRecyclerView) LayoutInflater.from(getContext()).inflate(R.layout.drag_item_recycler_view, this, false);
        recyclerView.setHorizontalScrollBarEnabled(false);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setMotionEventSplittingEnabled(false);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(mColumnWidth, LayoutParams.MATCH_PARENT));
        if (header != null) {
            layout.addView(header);
            mHeaders.put(mLists.size(), header);
        }
        layout.addView(recyclerView);

        mLists.add(recyclerView);
        mColumnLayout.addView(layout);
        return recyclerView;
    }
}
