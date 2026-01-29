package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.google.android.material.transition.MaterialSharedAxis;

public class BaseFragment extends Fragment {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(null);
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }
}
