package com.xapps.media.xmusic.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.xapps.media.xmusic.databinding.ActivityCrashReportBinding;
import java.io.File;
import java.io.FileOutputStream;

public class CrashReportActivity extends AppCompatActivity {
    
    private ActivityCrashReportBinding binding;
    private String error;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        error = getIntent().getStringExtra("error");
        binding.reportButton.setOnClickListener(v -> {
            try {
                File file = new File(getCacheDir(), "crash_report.txt");
                FileOutputStream fos = new FileOutputStream(file);
                String report = error == null? "Unknown error" : error;
                fos.write(report.getBytes());
                fos.close();

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(intent, "Share crash report"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
