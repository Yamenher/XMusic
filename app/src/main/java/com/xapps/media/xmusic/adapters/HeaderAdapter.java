package com.xapps.media.xmusic.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.xapps.media.xmusic.databinding.HeaderViewBinding;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {

    private final int size;

    public HeaderAdapter(int size) {
        this.size = size;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HeaderViewBinding binding = HeaderViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new HeaderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        holder.binding.songsCount.setText(size + " Songs");
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final HeaderViewBinding binding;

        public HeaderViewHolder(HeaderViewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
