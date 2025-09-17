package com.xapps.media.xmusic;

import android.animation.*;
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
import com.google.android.material.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.search.*;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.databinding.MainBinding;
import com.xapps.media.xmusic.settingsFragment;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;
import org.json.*;

public class MusicListFragmentActivity extends Fragment {
	
	private MusicListFragmentBinding binding;
    private int oldPos = -1;
	public final Fragment f = this;
	private static final Handler handler = new Handler(Looper.getMainLooper());
	private String Title = "";
	private String Artitst = "";
	private double Duration = 0;
	private String coverUri = "";
	public SearchBar searchBar;
	public AppBarLayout appbar;
	private boolean IsScrolledDown = false;
	public static Typeface cachedTypeface;
	public static int size;
	private String path = "";
	private MainBinding activity;
	private boolean IgnoreClicks = false;
    private MainActivity a;
    private SongsListAdapter songsAdapter;
    private boolean isPlaying, fabWasHidden = false;
	
	private ArrayList<String> SongsList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
    
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
		binding = MusicListFragmentBinding.inflate(_inflater, _container, false);
		initializeLogic();
        setUpListeners(); 
		return binding.getRoot();
	}
	
	private void initializeLogic() {
        a = (MainActivity) getActivity();
		activity = a.getBinding();
		Context context = getActivity();
		int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        binding.collapsingToolbar.setPadding(0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
        
        /*binding.songsList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = activity.miniPlayerDetailsLayout.getHeight()*2 + activity.bottomNavigation.getHeight();
                }
            }
        });*/

        executor.execute(() -> {
            try {
                if (getActivity() != null) {
                    MainActivity act = (MainActivity) getActivity();
                    SongsMap = act.getSongsMap();
                    songsAdapter = new SongsListAdapter(SongsMap);
                }
                
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.updateSongs(SongsMap);
                act.Start();
        
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        size = SongsMap.size();
                        if (size == 0) {
                            binding.songsList.setVisibility(View.INVISIBLE);
                            mainHandler.postDelayed(this, 25);
                        } else {
                            binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
                            //View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_view, binding.songsList, false);
                            binding.searchBar.post(() -> {
                                HeaderAdapter headerAdapter = new HeaderAdapter();
                                ConcatAdapter concatAdapter = new ConcatAdapter(headerAdapter, songsAdapter);
                                binding.songsList.setAdapter(concatAdapter);
                                binding.songsList.animate().alpha(1f).translationY(0f).setDuration(300).start();
                                binding.emptyLayout.setVisibility(View.GONE);
                            });
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("CoreExecutor", "Error loading songs", e);
            }
        });
        binding.searchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                a.HideBNV(true, fabWasHidden);
            } else if (newState == SearchView.TransitionState.HIDING) {
                a.HideBNV(false, fabWasHidden);
            }
        });
        
        binding.settingsIcon.setOnClickListener( v -> {
            
        });
		
	}
    
    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(myReceiver);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("ACTION_STOP_FRAGMENT");
        requireContext().registerReceiver(myReceiver, filter, Context.RECEIVER_EXPORTED);
    }
	
	public ArrayList<HashMap<String, Object>> getSongsMap() {
			return SongsMap;
	}
    
    public void setUpListeners() {
        binding.settingsIcon.setOnClickListener(v -> {
            Fragment f = new settingsFragment();
            a.addFragmentWithTransition(f);
        });
        
        binding.songsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int _scrollState) {
				super.onScrollStateChanged(recyclerView, _scrollState);
				
			}
			
			@Override
			public void onScrolled(RecyclerView recyclerView, int _offsetX, int _offsetY) {
				super.onScrolled(recyclerView, _offsetX, _offsetY);
				int firstVisibleItem = ((LinearLayoutManager) binding.songsList.getLayoutManager()).findFirstVisibleItemPosition();
				final boolean IsInTop = firstVisibleItem == 0 && binding.songsList.computeVerticalScrollOffset() == 0;
				
				if ((_offsetY > 20) && !IsScrolledDown) {
                    activity.Fab.hide();
					IsScrolledDown = true;
                    fabWasHidden = true;
				}
				if (((_offsetY < -20) && IsScrolledDown) || IsInTop) {
                    activity.Fab.show();
					IsScrolledDown = false;
                    fabWasHidden = false;
				}
			}
		});
    }
	
	public class SongsListAdapter extends RecyclerView.Adapter<SongsListAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public SongsListAdapter(ArrayList<HashMap<String, Object>> _arr) {
            setHasStableIds(true);
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.songs_list_view, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
            
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			SongsListViewBinding binding = SongsListViewBinding.bind(_view);
            boolean isLast = _position == getItemCount() - 1;
            XUtils.setMargins(binding.item, 0, 0, 0, isLast? XUtils.convertToPx(getActivity(), 10f) + activity.miniPlayerDetailsLayout.getHeight()*2 + activity.bottomNavigation.getHeight() : 0);

            
			if (_position == oldPos && isPlaying) {
                binding.item.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rv_selected_ripple));
            } else {
                binding.item.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.rv_ripple));
            }
            
            int spacing = XUtils.convertToPx(getContext(), 5f);
            if (_position == 0) {
                binding.decoView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.top_round_corners));
            } else if (_position == SongsMap.size() - 1) {
                binding.decoView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.bottom_round_corners));
            } else {
                binding.decoView.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.no_corners));
            }
            
			try {
				coverUri = _data.get(_position).get("thumbnail").toString();
			} catch (Exception e) {
				 
			}
			Title = _data.get(_position).get("title").toString();
			Artitst = _data.get(_position).get("author").toString();
			Glide.with(f)
			.load(Uri.parse("file://"+coverUri))
			.centerCrop()
            .override(200, 200)
			.into(binding.songCover);
			if (Title == null || Title.equals("")) {
				binding.SongTitle.setText("Unknown");
			} else {
				binding.SongTitle.setText(Title);
				binding.SongArtist.setText(Artitst);
			}
			binding.item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
                    isPlaying = true;
				    int pos = _holder.getAdapterPosition();
					if (!IgnoreClicks) {
						IgnoreClicks = true;
						path = SongsMap.get(pos).get("path").toString();
						Uri fileUri = Uri.parse("file://" + path);
						MainActivity act = (MainActivity) getActivity();
                        String pth = _data.get(pos).get("thumbnail").toString();
						act._setSong(pos, pth, fileUri);
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
								IgnoreClicks = false;
						
                        }, 50);
				    }
                    notifyItemChanged(oldPos, "color");
                    oldPos = pos;
                    notifyItemChanged(pos, "color");
				}
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
}
