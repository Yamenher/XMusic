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
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
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
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.databinding.MainBinding;
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
import me.zhanghai.android.fastscroll.FastScroller;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import org.json.*;

public class MusicListFragment extends BaseFragment {
	
	public MusicListFragmentBinding binding;
    private int oldPos = -1;
	public final Fragment f = this;
	private String Title = "";
	private String Artitst = "";
	private String coverUri = "";
	public SearchBar searchBar;
	public AppBarLayout appbar;
	private boolean IsScrolledDown = false;
    private static boolean bgSet = false;
	public int imageSize, size;
	private String path = "";
	private MainBinding activity;
	private boolean IgnoreClicks = false;
    private MainActivity a;
    private SongsListAdapter songsAdapter;
    private boolean isPlaying = false;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Handler mainHandler;
    private Drawable placeholder;
    private long lastClickTime = 0;
    private static final long DEBOUNCE_MS = 120;
    
    public static FloatingActionButton fab;
	
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
    
    private int lastSpacing;
    private int defaultFabMargin;
    
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isPlaying = false;
            songsAdapter.notifyItemChanged(oldPos, "color");
        }
    };
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        setRetainInstance(true);
        a = (MainActivity) getActivity();
		binding = MusicListFragmentBinding.inflate(_inflater, _container, false);
        mainHandler = new Handler(Looper.getMainLooper());
        
		initializeLogic();
        setUpListeners(); 
		return binding.getRoot();
	}
	
	private void initializeLogic() {
        a = (MainActivity) getActivity();
        activity = a.getBinding();
        placeholder = ContextCompat.getDrawable(getActivity(), R.drawable.placeholder_small);
        imageSize = XUtils.convertToPx(getActivity(), 45f);
        activity.bottomNavigation.post(() -> {
            lastSpacing = XUtils.convertToPx(getActivity(), 5f) + activity.miniPlayerDetailsLayout.getHeight()*2 + activity.bottomNavigation.getHeight();
            binding.songsList.addItemDecoration(new BottomSpacingDecoration(lastSpacing));
        });
        binding.collapsingToolbar.setPadding(0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
        binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
        activity.bottomNavigation.post(() -> {
            new FastScrollerBuilder(binding.songsList).useMd2Style().setPadding(0, 0, 0, activity.bottomNavigation.getHeight()+XUtils.getNavigationBarHeight(getActivity())).build();
        });
        
        binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
        executor.execute(() -> {
            SongMetadataHelper.getAllSongs(getActivity(), new SongLoadListener() {
                @Override
                public void onComplete(ArrayList<HashMap<String, Object>> map) {
                    SongsMap = map;
                    size = SongsMap.size();
                    songsAdapter = new SongsListAdapter(SongsMap);
                    MainActivity act = (MainActivity) getActivity();
                    HeaderAdapter headerAdapter = new HeaderAdapter();
                    ConcatAdapter concatAdapter = new ConcatAdapter(headerAdapter, songsAdapter);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.songsList.setAdapter(concatAdapter);
                            binding.emptyLayout.setVisibility(View.GONE);
                            a.Start(); 
                        }
                    });
                }
                    
                @Override
                public void onProgress(ArrayList<HashMap<String, Object>> map, int count) {
                        
                }
            });
        });
        
	}
    
    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter("ACTION_STOP_FRAGMENT");
        requireContext().registerReceiver(myReceiver, filter, Context.RECEIVER_EXPORTED);
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(myReceiver);
        } catch (Exception e) {
            
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        fab = null;
    }
	
	public ArrayList<HashMap<String, Object>> getSongsMap() {
			return SongsMap;
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
		
		ArrayList<HashMap<String, Object>> _data;
        int c1 = MaterialColorUtils.colorPrimary;
        int c2 = MaterialColorUtils.colorSecondary;
        int c3 = MaterialColorUtils.colorOnSurface;
        int c4 = MaterialColorUtils.colorOutline;
        
		public SongsListAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.songs_list_view, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
        
        public Drawable top = ContextCompat.getDrawable(getActivity(), R.drawable.rv_ripple_top);
		public Drawable bottom = ContextCompat.getDrawable(getActivity(), R.drawable.rv_ripple_bottom);
        public Drawable middle = ContextCompat.getDrawable(getActivity(), R.drawable.rv_ripple);
        
        private int spacing = XUtils.convertToPx(getContext(), 5f);
        private int resId = R.drawable.placeholder;
        private Uri uri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + resId);
        private String placeholderUri = uri.toString();
        
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			SongsListViewBinding binding = SongsListViewBinding.bind(_view);
            if (_position == 0) {
                binding.item.setBackground(top.getConstantState().newDrawable().mutate());
            } else if (_position == SongsMap.size() - 1) {
                binding.item.setBackground(bottom.getConstantState().newDrawable().mutate());
            } else {
                binding.item.setBackground(middle.getConstantState().newDrawable().mutate());
            }
            boolean isLast = _position == getItemCount() - 1;
            if (_position == oldPos && isPlaying) {
                binding.item.setChecked(true);
                binding.SongTitle.setTextColor(c1);
                binding.SongArtist.setTextColor(c2);
                binding.songBars.setVisibility(View.VISIBLE);
            } else {
                binding.item.setChecked(false);
                binding.SongTitle.setTextColor(c3);
                binding.SongArtist.setTextColor(c4);
                binding.songBars.setVisibility(View.INVISIBLE);
            }
			coverUri = _data.get(_position).get("thumbnail") == null? "invalid" : _data.get(_position).get("thumbnail").toString();
			Title = _data.get(_position).get("title").toString();
			Artitst = _data.get(_position).get("author").toString();
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
                if (a.bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_SETTLING || a.bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_DRAGGING) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < DEBOUNCE_MS) {
                    return;
                }
                lastClickTime = currentTime;
                activity.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rounded_corners_bottom_sheet));
                isPlaying = true;
                int pos = _position;
                path = SongsMap.get(pos).get("path").toString();
                Uri fileUri = Uri.parse("file://" + path);
                MainActivity act = (MainActivity) getActivity();
                String pth = _data.get(pos).get("thumbnail") == null ? placeholderUri : _data.get(pos).get("thumbnail").toString();
                if (_position != PlayerService.currentPosition) {
                    notifyItemChanged(oldPos, "color");
                    oldPos = pos;
                    notifyItemChanged(oldPos, "color");
                }
                act._setSong(pos, pth, fileUri);
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
			return _data.size();
		}
        
        @Override
        public long getItemId(int position) {
            String path = SongsMap.get(position).get("path").toString();
            return path.hashCode();
        }
        
        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            Glide.with(holder.itemView.getContext()).clear((View)holder.itemView.findViewById(R.id.songCover));
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
            if (isPlaying && songsAdapter != null && oldPos != PlayerService.currentPosition) {
                songsAdapter.notifyItemChanged(oldPos, "color");
                oldPos = PlayerService.currentPosition;
                songsAdapter.notifyItemChanged(oldPos, "color");
            } else {
                int i = -1;
                songsAdapter.notifyItemChanged(i, "color");
            }
    }

    public class BottomSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;
        private int spacing;
        public BottomSpacingDecoration(int bottomSpacing) {
            this.bottomSpacing = bottomSpacing;
            spacing = XUtils.convertToPx(getActivity(), 2f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;

            if (position == state.getItemCount() -1 ) {
                outRect.set(0, 0, 0, lastSpacing);
            } else {
                outRect.set(0, 0, 0, spacing);
            }
        }
    }

    public void shuffle() {
        Uri uri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + R.drawable.placeholder);
            String placeholderUri = uri.toString();
            int r = new Random().nextInt((SongsMap.size()-1 - 0) + 1) + 0;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < DEBOUNCE_MS) {
                return;
            }
            lastClickTime = currentTime;
            activity.miniPlayerBottomSheet.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rounded_corners_bottom_sheet));
            isPlaying = true;
            int pos = r;
            path = SongsMap.get(pos).get("path").toString();
            Uri fileUri = Uri.parse("file://" + path);
            MainActivity act = (MainActivity) getActivity();
            String pth = SongsMap.get(pos).get("thumbnail") == null ? placeholderUri : SongsMap.get(pos).get("thumbnail").toString();
            act._setSong(pos, pth, fileUri);
            songsAdapter.notifyItemChanged(oldPos, "color");
            oldPos = pos;
            songsAdapter.notifyItemChanged(pos, "color");
    }
}
