package com.xapps.media.xmusic.adapters;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.HashMap;
import com.xapps.media.xmusic.R;

public class CustomPagerAdapter extends RecyclerView.Adapter<CustomPagerAdapter.ViewHolder> {

        private Context context;
        private ArrayList<HashMap<String, Object>> data;

        public CustomPagerAdapter(Context context, ArrayList<HashMap<String, Object>> data) {
            this.context = context;
            this.data = data;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView thumbnail;

            public ViewHolder(View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.miniPlayerThumbnail);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.song_cover_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object thumb = data.get(position).get("thumbnail");
            if (thumb != null && !thumb.toString().trim().isEmpty() && !thumb.toString().equalsIgnoreCase("null")) {
                String path = "file://" + thumb.toString();
                Glide.with(context)
                    .load(Uri.parse(path))
                    .apply(new RequestOptions()
                    .centerCrop()
                    .override(800, 800)
                    .skipMemoryCache(false))
                    .thumbnail(1f)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(holder.thumbnail);
            } else {
                holder.thumbnail.setImageResource(R.drawable.placeholder);
                Log.i("warning", "No cover at pos: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }    
