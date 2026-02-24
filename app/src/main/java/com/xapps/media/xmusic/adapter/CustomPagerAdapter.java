package com.xapps.media.xmusic.adapter;
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
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.XUtils;
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
            String thumb = data.get(position).get("thumbnail").toString();
            if (!thumb.trim().isEmpty()) {
                String path = "file://" + thumb.toString();
                Glide.with(holder.thumbnail)
                    .load(Uri.parse(path))
                    .apply(new RequestOptions()
                    .centerCrop()
                    .priority(Priority.LOW)
                    .skipMemoryCache(false))
                    .into(holder.thumbnail);
            } else {
                Glide.with(holder.thumbnail)
                    .load(PlayerService.fallbackUri)
                    .apply(new RequestOptions()
                    .centerCrop()
                    .priority(Priority.LOW)
                    .skipMemoryCache(false))
                    .into(holder.thumbnail);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }    
