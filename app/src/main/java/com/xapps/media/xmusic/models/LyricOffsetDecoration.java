package com.xapps.media.xmusic.models;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LyricOffsetDecoration extends RecyclerView.ItemDecoration {

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        int itemCount = state.getItemCount();

        if (itemCount <= 0) return;

        if (position == 0) {
            outRect.top = parent.getHeight() / 3;
        } 
        
        if (position == itemCount - 1) {
            outRect.bottom = (parent.getHeight() * 2) / 3;
        }
    }
}
