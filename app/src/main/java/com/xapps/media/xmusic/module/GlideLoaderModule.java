package com.xapps.media.xmusic.module;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.annotation.GlideModule;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy;




@GlideModule
public final class GlideLoaderModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        int cores = Runtime.getRuntime().availableProcessors();
        builder.setSourceExecutor(GlideExecutor.newSourceExecutor(cores * 4, "LoadingThread",UncaughtThrowableStrategy.DEFAULT)); // Or whatever multiplier your weak CPU can handle
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
