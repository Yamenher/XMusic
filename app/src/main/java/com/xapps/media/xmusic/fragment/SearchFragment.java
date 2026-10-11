package com.xapps.media.xmusic.fragment;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;
import com.google.android.material.search.SearchView.TransitionState;
import com.xapps.media.xmusic.R;
import com.xapps.media.xmusic.activity.RootActivity;
import com.xapps.media.xmusic.activity.manager.UIManager;
import com.xapps.media.xmusic.common.SongLoadListener;
import com.xapps.media.xmusic.callback.*;
import com.xapps.media.xmusic.data.RuntimeData;
import com.xapps.media.xmusic.databinding.ActivityRootBinding;
import com.xapps.media.xmusic.databinding.FragmentSearchBinding;
import com.xapps.media.xmusic.databinding.SearchItemMiddleBinding;
import com.xapps.media.xmusic.helper.SongMetadataHelper;
import com.xapps.media.xmusic.helper.SongSearchHelper;
import com.xapps.media.xmusic.models.BottomSheetBehavior;
import com.xapps.media.xmusic.models.Song;
import com.xapps.media.xmusic.utils.MaterialColorUtils;
import com.xapps.media.xmusic.utils.XUtils;

import com.xapps.media.xmusic.widget.ExpressiveSliderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends BaseFragment implements FragmentCallback {

    public FragmentSearchBinding binding;
    private ActivityRootBinding activity;
    private RootActivity a;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int searchGeneration = 0;

    private boolean searchAll = true;
    private boolean searchTitle;
    private boolean searchArtist;
    private boolean searchAlbum;
    private boolean searchAlbumArtist;

    private String lastQuery = "";

    private int currentPos = -1;
    private int oldPos = -1;
    
    private int currentState = UIManager.LAYOUT_STATE_EXPOSE_BNV;
    
    private boolean isCurrentlyPlaying = false;
    
    private String currentSong = "null";
    private String oldSong = "null";

    private int imageSize;
    private long lastClickTime;
    private static final int DEBOUNCE_MS = 200;

    private SearchListAdapter searchAdapter;

    public interface ContextAction {
        void run(@NonNull Context context);
    }

    private final List<ContextAction> pendingContextActions = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        for (ContextAction action : new ArrayList<>(pendingContextActions)) {
            action.run(context);
        }
        pendingContextActions.clear();
    }

    public void runWithContext(@NonNull ContextAction action) {
        Context context = getContext();
        if (context != null && isAdded()) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action.run(context);
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Context ctx = getContext();
                    if (ctx != null && isAdded()) {
                        action.run(ctx);
                    }
                });
            }
        } else {
            pendingContextActions.add(action);
        }
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        a = (RootActivity) getActivity();
        //a.setSearchFragmentInstance(this);
        activity = a.getBinding();

        imageSize = XUtils.convertToPx(getActivity(), 50);

        initializeLogic();
        setupListeners();
        setupInsets();

        return binding.getRoot();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadSongs();
    }

    private void initializeLogic() {
        CallbackInterface.setSrFragCallback(this);
        runWithContext(context -> binding.searchRecycler.setLayoutManager(new LinearLayoutManager(context)));
        /*DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(true);
        animator.setMoveDuration(0);
        animator.setRemoveDuration(0);
        animator.setAddDuration(0);
        
        Until I find a fix for ugly animations*/
        binding.searchRecycler.setItemAnimator(null);
        activity.bottomNavigation.post(() -> {
            runWithContext(context -> {
                if (binding != null) {
                    binding.searchRecycler.addItemDecoration(new BottomSpacingDecoration(XUtils.convertToPx(context, activity.bottomNavigation.getHeight())));
                }
            });
        });
        loadSongs();
    }

    private void setupInsets() {
        XUtils.setMargins(binding.searchBar, 0, XUtils.getStatusBarHeight(getActivity()), 0, 0);
    }

    private void setupListeners() {

        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastQuery = s.toString();
                onSearchPreferencesChanged();
            }
        });

        binding.chipAll.setOnCheckedChangeListener((v, b) -> {
            searchAll = b;
            if (b) {
                binding.chipTitle.setChecked(false);
                binding.chipArtist.setChecked(false);
                binding.chipAlbum.setChecked(false);
                binding.chipAlbumArtist.setChecked(false);
            }
            onSearchPreferencesChanged();
        });

        binding.chipTitle.setOnCheckedChangeListener((v, b) -> {
            searchTitle = b;
            onSearchPreferencesChanged();
        });

        binding.chipArtist.setOnCheckedChangeListener((v, b) -> {
            searchArtist = b;
            onSearchPreferencesChanged();
        });

        binding.chipAlbum.setOnCheckedChangeListener((v, b) -> {
            searchAlbum = b;
            onSearchPreferencesChanged();
        });

        binding.chipAlbumArtist.setOnCheckedChangeListener((v, b) -> {
            searchAlbumArtist = b;
            onSearchPreferencesChanged();
        });
        
        binding.searchView.addTransitionListener(
        (searchView, previousState, newState) -> {
            if (newState == TransitionState.SHOWING) {
                currentState = UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY;
                //a.bottomSheetBehavior.setDraggable(false);
                //a.HideBNV(true);
            } else if (newState == TransitionState.SHOWN) {
                //a.bottomSheetBehavior.setDraggable(true);
            } else if (newState == TransitionState.HIDING) {
                currentState = UIManager.LAYOUT_STATE_EXPOSE_PLAYER_ONLY;
                //a.HideBNV(false);
            }
        });
    }

    private void onSearchPreferencesChanged() {
        final int generation = ++searchGeneration;
        final String query = lastQuery == null ? "" : lastQuery.trim().toLowerCase();

        executor.execute(() -> {
            ArrayList<Song> results =
            SongSearchHelper.search(
                query,
                searchAll || searchTitle,
                searchAll || searchArtist,
                searchAll || searchAlbum,
                searchAll || searchAlbumArtist);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                if (generation != searchGeneration) return;
                if (searchAdapter == null) return;
                searchAdapter.setData(results);
                binding.searchRecycler.scrollToPosition(0);
            });
        });
    }

    public class SearchListAdapter extends RecyclerView.Adapter<SearchListAdapter.ViewHolder> {

        private static final int TYPE_SINGLE = -1;
        private static final int TYPE_TOP = 0;
        private static final int TYPE_MIDDLE = 1;
        private static final int TYPE_BOTTOM = 2;

        private final ArrayList<Song> data = new ArrayList<>();

        private final int c1 = MaterialColorUtils.colorPrimary;
        private final int c2 = MaterialColorUtils.colorSecondary;
        private final int c3 = MaterialColorUtils.colorOnSurface;
        private final int c4 = MaterialColorUtils.colorOutline;

        private final String placeholderUri;

        SearchListAdapter(Context c, ArrayList<Song> list) {
            setHasStableIds(true);
            placeholderUri = Uri.parse("android.resource://" + c.getPackageName() + "/" + R.drawable.placeholder).toString();
            data.addAll(list);
        }

        @Override
        public int getItemViewType(int position) {
            int size = getItemCount();

            if (size == 1) {
                return TYPE_SINGLE;
            }

            if (size == 2) {
                return position == 0 ? TYPE_TOP : TYPE_BOTTOM;
            }

            if (position == 0) return TYPE_TOP;
            if (position == size - 1) return TYPE_BOTTOM;
            return TYPE_MIDDLE;
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).path.hashCode();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout;
            if (viewType == TYPE_TOP) layout = R.layout.search_item_top;
            else if (viewType == TYPE_BOTTOM) layout = R.layout.search_item_bottom;
            else if (viewType == TYPE_SINGLE) layout = R.layout.search_item_single;
            else layout = R.layout.search_item_middle;

            return new ViewHolder(inflater.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = data.get(position);
            SearchItemMiddleBinding b = holder.binding;

            boolean active = song.path.equals(currentSong);

            if (!a.getController().isPlaying()) b.vumeterView.pause(); else b.vumeterView.resume();

            b.item.setChecked(active);
            b.vumeterFrame.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            b.SongTitle.setTextColor(active ? c1 : c3);
            b.SongArtist.setTextColor(active ? c2 : c4);

            b.SongTitle.setText(song.title);
            b.SongArtist.setText(song.artist);

            Uri cover = song.getArtworkUri();

            Glide.with(b.songCover)
                .load(cover)
                .centerCrop()
				.error(R.drawable.placeholder_small)
				.fallback(R.drawable.placeholder_small)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(imageSize, imageSize)
                .into(b.songCover);

            b.item.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                currentPos = pos;
                if (pos == RecyclerView.NO_POSITION) return;
                if (a.getController() == null) return;
                if (activity.miniPlayer.getState() == ExpressiveSliderLayout.STATE_SETTLING || activity.miniPlayer.getState() == ExpressiveSliderLayout.STATE_DRAGGING) return;
                long now = System.currentTimeMillis();
                if (now - lastClickTime < DEBOUNCE_MS) return;
                lastClickTime = now;
                String path = song.path;
                int realIndex = -1;
                for (int i = 0; i < RuntimeData.songs.size(); i++) {
                    if (RuntimeData.songs.get(i).path.toString().equals(path)) {
                        realIndex = i;
                        break;
                    }
                }

                if (realIndex == -1) return;
                a.setSong(realIndex);
                updateActiveItem(realIndex);
            });
            b.optionsIcon.setOnClickListener(v -> {  
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

        void setData(ArrayList<Song> newData) {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new SongDiff(data, newData));
            data.clear();
            data.addAll(newData);
            diff.dispatchUpdatesTo(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final SearchItemMiddleBinding binding;

            ViewHolder(View v) {
                super(v);
                binding = SearchItemMiddleBinding.bind(v);
            }
        }
    }

    static class SongDiff extends DiffUtil.Callback {

        private final ArrayList<Song> oldL;
        private final ArrayList<Song> newL;

        SongDiff(ArrayList<Song> o, ArrayList<Song> n) {
            oldL = o;
            newL = n;
        }

        @Override
        public int getOldListSize() {
            return oldL.size();
        }

        @Override
        public int getNewListSize() {
            return newL.size();
        }

        @Override
        public boolean areItemsTheSame(int o, int n) {
            return oldL.get(o).path.equals(newL.get(n).path);
        }

        @Override
        public boolean areContentsTheSame(int o, int n) {
            return oldL.get(o).equals(newL.get(n));
        }
    }

    public class BottomSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int bottomSpacing;

        BottomSpacingDecoration(int bottomSpacing) {
            this.bottomSpacing = bottomSpacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;
            if (pos == state.getItemCount() - 1) {
                outRect.bottom = bottomSpacing;
            }
        }
    }

    public void updateActiveItem(int pos) {
        currentPos = pos;
        oldSong = currentSong;
        currentSong = pos >= 0? RuntimeData.songs.get(pos).path : "null";
        if (searchAdapter != null) searchAdapter.notifyDataSetChanged();
    }
    
    public void updateVumeter(boolean b) {
        if (a.getController() != null && currentPos == -1) currentPos = a.getController().getCurrentMediaItemIndex();
        SearchListAdapter.ViewHolder v = (SearchListAdapter.ViewHolder) binding.searchRecycler.findViewHolderForAdapterPosition(currentPos);
        if (v != null) {
            if (b) {
                v.binding.vumeterView.resume();
            } else {
                v.binding.vumeterView.pause();
            }
        }
    }

    private void loadSongs() {
        runWithContext(context -> {
            executor.execute(() -> {
                SongMetadataHelper.getAllSongs(context, new SongLoadListener() {
                    @Override
                    public void onComplete(ArrayList<Song> map) {
                        bindInitial();
                    }
                });
            });
        });
    }
    
    private void bindInitial() {
        runWithContext(context -> {
            if (binding == null) return;
            ArrayList<Song> initial = SongSearchHelper.search("", true, true, true, true);
            searchAdapter = new SearchListAdapter(context, initial);
            binding.searchRecycler.setAdapter(searchAdapter);
        });
    }
    
    @Override
    public TransitionState getSearchViewState() {
        return binding.searchView.getCurrentTransitionState();
    }
    
    @Override
    public void hideSearchView() {
        binding.searchView.hide();
    }
    
    @Override
    public void freeze(boolean b) {
        binding.blockingOverlay.setClickable(b); 
    }
    
    @Override
    public int getLayoutState() {
        return currentState;
    }

}