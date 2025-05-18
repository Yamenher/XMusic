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
import android.widget.*;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.*;
import androidx.appcompat.resources.*;
import androidx.core.*;
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
import androidx.security.*;
import androidx.startup.*;
import androidx.transition.*;
import com.appbroker.roundedimageview.*;
import com.bumptech.glide.Glide;
import com.google.android.material.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.search.SearchBar;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.databinding.*;
import com.xapps.media.xmusic.databinding.MainBinding;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

public class MusicListFragmentActivity extends Fragment {
	
	private MusicListFragmentBinding binding;
	public final Fragment f = this;
	private static final Handler handler = new Handler(Looper.getMainLooper());
	private String Title = "";
	private String Artitst = "";
	private double Duration = 0;
	private String coverUri = "";
	public SearchBar searchBar;
	public AppBarLayout appbar;
	private boolean IsScrolledDown = false;
	private DataManager dataManager;
	public static Typeface cachedTypeface;
	public static int size;
	private String path = "";
	private MainBinding activity;
	private boolean IgnoreClicks = false;
	
	private ArrayList<String> SongsList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> SongsMap = new ArrayList<>();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = MusicListFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		
        binding.searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.bar_settings) {
                BasicUtil.showMessage((MainActivity)getActivity(), "Settings");
                return true;
            }
            return false;
        });
		binding.songsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int _scrollState) {
				super.onScrollStateChanged(recyclerView, _scrollState);
				
			}
			
			@Override
			public void onScrolled(RecyclerView recyclerView, int _offsetX, int _offsetY) {
				super.onScrolled(recyclerView, _offsetX, _offsetY);
				final int searchBarHeight= binding.searchBar.getHeight();
				int firstVisibleItem = ((LinearLayoutManager) binding.songsList.getLayoutManager()).findFirstVisibleItemPosition();
				final boolean IsInTop = firstVisibleItem == 0 && binding.songsList.computeVerticalScrollOffset() == 0;
				
				if ((_offsetY > 30) && !IsScrolledDown) {
					binding.searchBar.animate().translationY(-(searchBarHeight+XUtils.getStatusBarHeight(getActivity()))).setDuration(150).start();
					IsScrolledDown = true;
				}
				if (((_offsetY < -30) && IsScrolledDown) || IsInTop) {
					binding.searchBar.animate().translationY(0).setDuration(150).start();
					IsScrolledDown = false;
				}
			}
		});
	}
	
	private void initializeLogic() {
		MainActivity a = (MainActivity) getActivity();
		activity = a.getBinding();
		Context context = getActivity();
        binding.songsList.addItemDecoration(new LastItemMarginDecoration(XUtils.getMargin(activity.Fab, "bottom")));
        binding.searchBar.inflateMenu(R.menu.search_menu);
		TextView textView = new TextView(context);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		textView.setGravity(Gravity.CENTER);
		textView.setLayoutParams(new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT,
		LinearLayout.LayoutParams.WRAP_CONTENT
		));
		if (cachedTypeface == null) {
			cachedTypeface = ResourcesCompat.getFont(context, R.font.product_sans_regular);
		}
		textView.setTypeface(cachedTypeface, Typeface.BOLD);
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
		int primaryColor = typedValue.data;
		SpannableStringBuilder spannable = new SpannableStringBuilder("XMusic");
		spannable.setSpan(new ForegroundColorSpan(primaryColor), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		textView.setText(spannable);
		binding.searchBar.setCenterView(textView);
		binding.searchBar.startOnLoadAnimation();
		binding.searchView.setHint("Search your songs");
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				binding.searchBar.setHint("Search your songs");
		    }
		}, 100);
		XUtils.increaseMargins(binding.searchBar, 0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
		binding.searchBar.post(() -> {
			int searchBarHeight = binding.searchBar.getHeight() + XUtils.getStatusBarHeight(getActivity());
			if (binding.songsList.getItemDecorationCount() == 0) {
				binding.songsList.addItemDecoration(new RecyclerView.ItemDecoration() {
					@Override
					public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
						if (parent.getChildAdapterPosition(view) == 0) {
							outRect.top = searchBarHeight;
						}
					}
			    });
			}
		});
		
		HandlerThread handlerThread = new HandlerThread("BackgroundThread");
		handlerThread.start();
		Handler backgroundHandler = new Handler(handlerThread.getLooper());
		Handler mainHandler = new Handler(Looper.getMainLooper());
		backgroundHandler.post(() -> {
		    try {
				if (getActivity() != null) { 
					SongsMap = SongMetadataHelper.getAllSongs(getActivity());
                                        
				}
				mainHandler.post(() -> {
					if (getActivity() != null && SongsMap != null) {
						size = SongsMap.size();
					    binding.songsList.setLayoutManager(new LinearLayoutManager(getContext()));
					    View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_view, binding.songsList, false);
					    HeaderAdapter headerAdapter = new HeaderAdapter(headerView);
						ConcatAdapter concatAdapter = new ConcatAdapter(headerAdapter, new SongsListAdapter(SongsMap));
						binding.songsList.setAdapter(concatAdapter);
					    int searchBarHeight = binding.searchBar.getHeight() + XUtils.getStatusBarHeight(getActivity());
                        XUtils.increaseMargins(headerView, 0, searchBarHeight /*+ XUtils.getMargin(binding.searchBar, "top")*2*/, 0, 0);
					    binding.songsList.animate().alpha(1f).translationY(0f).setDuration(300).start();
				    } else {
                        BasicUtil.showMessage(getActivity(), "smth is null");
                    }
				});
                MainActivity tmp = (MainActivity) getActivity();
                tmp.updateSongs(SongsMap);
			} catch (Exception e) {
			    Log.e("BackgroundThread", "Error loading songs", e);
			} finally {
			    handlerThread.quitSafely();  
		    }
		});
		
	}
	
	public static class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {
		private final View headerView;
		public HeaderAdapter(View headerView) {
			this.headerView = headerView;
		}
		
	    @Override
		public HeaderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
	
	public ArrayList<HashMap<String, Object>> getSongsMap() {
			return SongsMap;
	}
    
    public class LastItemMarginDecoration extends RecyclerView.ItemDecoration {

        private final int bottomMarginPx;
        public LastItemMarginDecoration(int bottomMarginPx) {
            this.bottomMarginPx = bottomMarginPx;
        }
		
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == parent.getAdapter().getItemCount() - 1) {
                outRect.bottom = bottomMarginPx;
            } else {
                outRect.bottom = 0;
            }
        }
    }
	
	
	public class SongsListAdapter extends RecyclerView.Adapter<SongsListAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public SongsListAdapter(ArrayList<HashMap<String, Object>> _arr) {
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
			
			try {
				coverUri = _data.get((int)_position).get("thumbnail").toString();
			} catch (Exception e) {
				 
			}
			Title = _data.get((int)_position).get("title").toString();
			Artitst = _data.get((int)_position).get("author").toString();
			Glide.with(f)
			.load(Uri.parse("file://"+coverUri))
			.centerCrop()
			.diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
			.skipMemoryCache(false) 
			.into(binding.songCover);
			if (Title == null || Title.equals("")) {
				binding.SongTitle.setText("");
			} else {
				binding.SongTitle.setText(Title);
				binding.SongArtist.setText(Artitst);
			}
			binding.item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (!IgnoreClicks) {
						IgnoreClicks = true;
						path = SongsMap.get(_position).get("path").toString();
						Uri fileUri = Uri.parse("file://" + path);
						MainActivity act = (MainActivity) getActivity();
                        String pth = _data.get((int)_position).get("thumbnail").toString();
						act._setSong(_position, pth, fileUri);
						new Handler(Looper.getMainLooper()).postDelayed(() -> {
								IgnoreClicks = false;
						
                        }, 50);
				    }
				}
			});
			
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
