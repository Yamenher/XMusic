package com.xapps.media.xmusic.fragment;

import android.animation.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.Spanned;
import android.text.style.*;
import android.text.style.ForegroundColorSpan;
import android.transition.Transition;
import android.util.*;
import android.util.TypedValue;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.*;
import androidx.appcompat.resources.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.*;
import androidx.core.content.*;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.ktx.*;
import androidx.core.splashscreen.*;
import androidx.core.view.ViewKt;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.emoji2.*;
import androidx.emoji2.viewsintegration.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.process.*;
import androidx.lifecycle.runtime.*;
import androidx.lifecycle.viewmodel.*;
import androidx.lifecycle.viewmodel.savedstate.*;
import androidx.media.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.session.MediaController;
import androidx.palette.*;
import androidx.profileinstaller.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.savedstate.*;
import androidx.startup.*;
import androidx.transition.*;
import com.appbroker.roundedimageview.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.material.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.*;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.activity.MainActivity;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.databinding.ActivityMainBinding;
import com.xapps.media.xmusic.fragment.SettingsFragment;
import com.xapps.media.xmusic.service.PlayerService;
import com.xapps.media.xmusic.utils.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;
import kotlin.Unit;
import me.zhanghai.android.fastscroll.FastScroller;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import org.json.*;

public class MusicListFragment extends BaseFragment {
	
	public MusicListFragmentBinding binding;
    private int oldPos = -1;
    private int currentPos= -1;
	public final Fragment f = this;
	private String Title = "";
	private String Artitst = "";
	private String coverUri = "";
	public int imageSize, size;
	private String path = "";
	private ActivityMainBinding activity;
    private MainActivity a;
    private SongsListAdapter songsAdapter;
    private boolean isPlaying = false;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Handler mainHandler;
    private Drawable placeholder;
    private long lastClickTime = 0;
    private final long DEBOUNCE_MS = 300;
    private ConcatAdapter concatAdapter;
    
    public static FloatingActionButton fab;
    
    private int lastSpacing;
    private int defaultFabMargin;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        a = (MainActivity) getActivity();
		binding = MusicListFragmentBinding.inflate(_inflater, _container, false);
        mainHandler = new Handler(Looper.getMainLooper());
        if (getActivity() != null)binding.collapsingToolbar.setPadding(0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
		initializeLogic();
        setUpListeners(); 
		return binding.getRoot();
	}
	
	private void initializeLogic() {
        fab = binding.shuffleButton;
        a = (MainActivity) getActivity();
        activity = a.getBinding();
        placeholder = ContextCompat.getDrawable(getActivity(), R.drawable.placeholder_small);
        imageSize = XUtils.convertToPx(getActivity(), 45f);
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(MaterialColorUtils.colorSurfaceContainerHigh);
        binding.swipeRefreshLayout.setColorSchemeColors(MaterialColorUtils.colorPrimary);
        activity.bottomNavigation.post(() -> {
            if (getActivity() == null) return;
            a.setMusicListFragmentInstance(this);
            lastSpacing = XUtils.convertToPx(getActivity(), 5f) + activity.miniPlayerDetailsLayout.getHeight()*2 + activity.bottomNavigation.getHeight();
            binding.songsList.addItemDecoration(new BottomSpacingDecoration(lastSpacing));
                binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
                activity.bottomNavigation.post(() -> {
                    new FastScrollerBuilder(binding.songsList).useMd2Style().setPadding(0, 0, 0, activity.bottomNavigation.getHeight()+XUtils.getNavigationBarHeight(getActivity())).build();
                });
        });
        
        binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
        executor.execute(() -> {
            SongMetadataHelper.getAllSongs(getActivity(), new SongLoadListener() {
                @Override
                public void onComplete(ArrayList<HashMap<String, Object>> map) {
                    if (getActivity() == null) return;
                    RuntimeData.songsMap = map;
                    size = RuntimeData.songsMap.size();
                    songsAdapter = new SongsListAdapter(getActivity(), RuntimeData.songsMap);
                    MainActivity act = (MainActivity) getActivity();
                    HeaderAdapter headerAdapter = new HeaderAdapter();
                    concatAdapter = new ConcatAdapter(headerAdapter, songsAdapter);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.songsList.setAdapter(concatAdapter);
                            binding.songsList.setItemAnimator(null);
                            binding.emptyLayout.setVisibility(View.GONE);
                            ViewKt.doOnLayout(binding.collapsingToolbar, v -> {
                                binding.shuffleButton.setTranslationY(v.getHeight() / 2f);
                                return Unit.INSTANCE;
                            });
                        }
                    });
                }
                    
                @Override
                public void onProgress(ArrayList<HashMap<String, Object>> map, int count) {
                        
                }
            });
        });
        
        binding.shuffleButton.setOnClickListener(v -> {
            shuffle();
        });
        
	}
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        fab = null;
    }
    
    public void setupSongsList(ArrayList<HashMap<String, Object>> list) {
        
    }
    
    public void setUpListeners() {
        binding.topTitle.setOnClickListener(v -> {
            songsAdapter.notifyDataSetChanged();
            XUtils.showMessage(getActivity(), "done");
        });
    }
	
	public class SongsListAdapter extends RecyclerView.Adapter<SongsListAdapter.ViewHolder> {
		
        int c1 = MaterialColorUtils.colorPrimary;
        int c2 = MaterialColorUtils.colorSecondary;
        int c3 = MaterialColorUtils.colorOnSurface;
        int c4 = MaterialColorUtils.colorOutline;
        
        private ArrayList<HashMap<String, Object>> data = new ArrayList();
        
        private int spacing;
        private int resId = R.drawable.placeholder;
        private Uri uri;
        private String placeholderUri;
        
        private static final int TYPE_TOP = 0;
        private static final int TYPE_MIDDLE = 1;
        private static final int TYPE_BOTTOM = 2;
        
        private SongItemMiddleBinding binding;
        
		public SongsListAdapter(Context c, ArrayList<HashMap<String, Object>> arraylist) {
            spacing = XUtils.convertToPx(c, 5f);
            uri = Uri.parse("android.resource://" + c.getPackageName() + "/" + resId);
            placeholderUri = uri.toString();
            data = arraylist;
        }
		
		@Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout;
            if (viewType == TYPE_TOP) layout = R.layout.song_item_top;
            else if (viewType == TYPE_BOTTOM) layout = R.layout.song_item_bottom;
            else layout = R.layout.song_item_middle;

            return new ViewHolder(inflater.inflate(layout, parent, false));
        }
        
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			View view = holder.itemView;
            binding = SongItemMiddleBinding.bind(view);
            if (position == currentPos) {
                binding.item.setChecked(true);
                binding.vumeterFrame.setVisibility(View.VISIBLE);
                binding.SongTitle.setTextColor(c1);
                binding.SongArtist.setTextColor(c2);
            } else {
                binding.item.setChecked(false);
                binding.vumeterFrame.setVisibility(View.INVISIBLE);
                binding.SongTitle.setTextColor(c3);
                binding.SongArtist.setTextColor(c4);
            }
			coverUri = data.get(position).get("thumbnail") == null? "invalid" : data.get(position).get("thumbnail").toString();
			Title = data.get(position).get("title").toString();
			Artitst = data.get(position).get("author").toString();
			Glide.with(f)
			.load(coverUri.equals("invalid")? placeholder : Uri.parse("file://"+coverUri))
			.centerCrop()
            .fallback(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(placeholder)
            .override(imageSize, imageSize)
			.into(binding.songCover);
			if (Title == null || Title.equals("")) {
				binding.SongTitle.setText("Unknown");
			} else {
				binding.SongTitle.setText(Title);
				binding.SongArtist.setText(Artitst);
			}
			binding.item.setOnClickListener(v -> {
                if (a.getController() == null ) return;
                if (a.bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_SETTLING || a.bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_DRAGGING) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < DEBOUNCE_MS) {
                    return;
                }
                lastClickTime = currentTime;
                activity.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rounded_corners_bottom_sheet));
                path = data.get(position).get("path").toString();
                Uri fileUri = Uri.parse("file://" + path);
                MainActivity act = (MainActivity) getActivity();
                String pth = data.get(position).get("thumbnail") == null ? placeholderUri : data.get(position).get("thumbnail").toString();
                act.setSong(position, pth, fileUri);
                updateActiveItem(position);
            });
            binding.optionsIcon.setOnClickListener(v -> {
                BottomSheetDragHandleView drag = new BottomSheetDragHandleView(getActivity());
                LinearLayout bsl = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.options_bottom_sheet, null);
                bsl.addView(drag, 0);
                BottomSheetDialog bsd = new BottomSheetDialog(getActivity());
                bsd.setContentView(bsl, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                bsd.show();
            });
		}
        
        @Override
        public int getItemCount() {
            return data.size();
        }
        
        @Override
        public long getItemId(int position) {
            String path = data.get(position).get("path").toString();
            return path.hashCode();
        }
        
        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            Glide.with(holder.itemView.getContext()).clear((View)holder.itemView.findViewById(R.id.songCover)); 
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_TOP;
            if (position == getItemCount() - 1) return TYPE_BOTTOM;
            return TYPE_MIDDLE;
        }
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
        
	}

    public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {
	    @Override
	    public HeaderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		    View headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_view, parent, false);
		    return new HeaderViewHolder(headerView);
	    }
		
	    @Override
		public void onBindViewHolder(HeaderViewHolder holder, int position) {
			View view = holder.itemView;
			TextView sg = (TextView) view.findViewById(R.id.songs_count);
			sg.setText("0 Songs".replace("0",String.valueOf(size)));
		}
		
		@Override
		public int getItemCount() {
		    return 1;
		}
		public static class HeaderViewHolder extends RecyclerView.ViewHolder {
			public HeaderViewHolder(View itemView) {
				super(itemView);
			}
		}
	}

    public void adjustUI() {
        if (songsAdapter != null) {
            updateActiveItem(PlayerService.currentPosition);
        }
    }

    public class BottomSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;
        private int spacing;
        private int sideSpacing;
        public BottomSpacingDecoration(int bottomSpacing) {
            sideSpacing = XUtils.convertToPx(getActivity(), 12f);
            this.bottomSpacing = bottomSpacing;
            spacing = XUtils.convertToPx(getActivity(), 2f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;
            if (position == state.getItemCount() -1 ) {
                outRect.set(sideSpacing, 0, sideSpacing, lastSpacing);
            } else {
                outRect.set(sideSpacing, 0, sideSpacing, spacing);
            }
        }
    }

    public void shuffle() {
        Uri uri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + R.drawable.placeholder);
            String placeholderUri = uri.toString();
            int r = new Random().nextInt((RuntimeData.songsMap.size()-1 - 0) + 1) + 0;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < DEBOUNCE_MS) {
                return;
            }
            lastClickTime = currentTime;
            activity.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rounded_corners_bottom_sheet));
            path = RuntimeData.songsMap.get(r).get("path").toString();
            Uri fileUri = Uri.parse("file://" + path);
            MainActivity act = (MainActivity) getActivity();
            String pth = RuntimeData.songsMap.get(r).get("thumbnail") == null ? placeholderUri : RuntimeData.songsMap.get(r).get("thumbnail").toString();
            act.setSong(r, pth, fileUri);
            updateActiveItem(r);
    }
    
    public void updateActiveItem(int i) {
        oldPos = currentPos;
        currentPos = i;
        if (currentPos == oldPos || a.getController() == null) return;
        if (oldPos != -1) songsAdapter.notifyItemChanged(oldPos, "color");
        if (i != -1) songsAdapter.notifyItemChanged(i, "color");
        if (i == -1) oldPos = -1;
    }

}
