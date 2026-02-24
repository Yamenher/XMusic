package com.xapps.media.xmusic.viewmodel;

import android.util.Log;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import com.xapps.media.xmusic.R;

public class MainActivityViewModel extends ViewModel {

    private static final String KEY_FAB_MARGIN = "key_fab_margin";
    private static final String KEY_IS_BNV_HIDDEN = "key_is_bnv_hidden";
    private static final String KEY_DATA_SAVED = "key_data_saved";
    private static final String KEY_LAST_POSITION = "key_last_position";
    private static final String KEY_BNV_POSITION = "key_bnv_position";

    private final SavedStateHandle mSavedStateHandle;

    public MainActivityViewModel(SavedStateHandle savedStateHandle) {
        this.mSavedStateHandle = savedStateHandle;
    }
    
    public void markDataAsSaved(boolean b) {
        mSavedStateHandle.set(KEY_DATA_SAVED, b);
    }
    
    public void saveFabMargin(float f) {
        mSavedStateHandle.set(KEY_FAB_MARGIN, f);
    }
    
    public void setBNVAsHidden(boolean b) {
        mSavedStateHandle.set(KEY_IS_BNV_HIDDEN, b);
    }
    
    public void setLastPosition(int i) {
        mSavedStateHandle.set(KEY_LAST_POSITION, i);
    }
    
    public float loadFabMargin() {
        Float margin = mSavedStateHandle.get(KEY_FAB_MARGIN);
        return margin != null ? margin : 0f;
    }
    
    public int loadLastPosition() {
        Integer position = mSavedStateHandle.get(KEY_LAST_POSITION);
        return position != null ? position : 0;
    }

    public boolean isDataSaved() {
        Boolean dataSaved = mSavedStateHandle.get(KEY_DATA_SAVED);
        return dataSaved != null ? dataSaved : false;
    }

    public boolean wasBNVHidden() {
        Boolean isHidden = mSavedStateHandle.get(KEY_IS_BNV_HIDDEN);
        return isHidden != null ? isHidden : false;
    }

    public void saveBNVPosition(int i) {
        mSavedStateHandle.set(KEY_BNV_POSITION, i);
    }

    public int loadBNVPosition() {
        try {
            int bnvpos = mSavedStateHandle.get(KEY_BNV_POSITION);
            return bnvpos; 
        } catch (NullPointerException e) {
            return R.id.menuHomeFragment;
        }
    }
}
