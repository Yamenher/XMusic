package com.xapps.media.xmusic.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.google.android.material.transition.MaterialSharedAxis;

public class BaseFragment extends Fragment {

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

}
