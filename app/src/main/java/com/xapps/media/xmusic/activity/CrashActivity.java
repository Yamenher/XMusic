package com.xapps.media.xmusic.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.xapps.media.xmusic.databinding.ActivityCrashBinding;

public class CrashActivity extends AppCompatActivity {
    private ActivityCrashBinding binding;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String error = getIntent().getStringExtra("error");
        binding.errorText.setText(error);
    }
}
