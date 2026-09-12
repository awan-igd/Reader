package reader.aigd.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Locale;

import reader.aigd.R;

public class TTSActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int PERMISSION_CODE = 101;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private TextView scriptContent, audioTitle, pitchValue, speedValue, voiceName, fileLocation, autoDetectStatus;
    private SeekBar pitchSeek, speedSeek;
    private MaterialCardView playButton, backButton, saveButton, loadingOverlay, autoDetectCard;
    private LinearLayout voiceSelector;
    private ProgressBar audioProgress;
    private ImageView playIcon;
    private Banner banner;
    private String scriptText = "";
    private String scriptTitle = "";
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));
        StartAppSDK.init(this, "206823693", false);

        scriptText = getIntent().getStringExtra("script_content");
        scriptTitle = getIntent().getStringExtra("script_title");
        if (scriptText == null) scriptText = "";
        if (scriptTitle == null) scriptTitle = "My Audio";

        initViews();
        initTTS();
        setupClicks();
    }

    private void initViews() {
        scriptContent = findViewById(R.id.scriptContent);
        audioTitle = findViewById(R.id.audioTitle);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceName = findViewById(R.id.voiceName);
        fileLocation = findViewById(R.id.fileLocation);
        pitchSeek = findViewById(R.id.pitchSeekBar);
        speedSeek = findViewById(R.id.speedSeekBar);
        playButton = findViewById(R.id.playButton);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        audioProgress = findViewById(R.id.audioProgress);
        playIcon = findViewById(R.id.playIcon);
        banner = findViewById(R.id.startAppBanner);
        voiceSelector = findViewById(R.id.voiceSelector);
        autoDetectCard = findViewById(R.id.autoDetectCard);
        autoDetectStatus = findViewById(R.id.autoDetectStatus);
        if (banner != null) banner.loadAd();
        scriptContent.setText(scriptText.isEmpty() ? "No script" : scriptText);
        audioTitle.setText(scriptTitle);
    }

    private void initTTS() {
        loadingOverlay.setVisibility(View.VISIBLE);
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        handler.post(() -> {
            loadingOverlay.setVisibility(View.GONE);
            if (status == TextToSpeech.SUCCESS) {
                int res = tts.setLanguage(Locale.getDefault());
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                ttsReady = true;
                setupUtteranceListenerForPlay();
                Toast.makeText(this, "Voice Ready", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "TTS Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUtteranceListenerForPlay() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
                handler.post(() -> {
                    isPlaying = true;
                    playIcon.setImageResource(R.drawable.ic_pause);
                    audioProgress.setVisibility(View.VISIBLE);
                    audioProgress.setIndeterminate(false);
                });
            }

            @Override
            public void onDone(String id) {
                handler.post(() -> {
                    if (!id.equals("SAVE_FILE")) {
                        isPlaying = false;
                        playIcon.setImageResource(R.drawable.ic_play);
                        audioProgress.setProgress(0);
                    }
                });
            }

            @Override
            public void onError(String id) {
                handler.post(() -> Toast.makeText(TTSActivity.this, "Error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupClicks() {
        backButton.setOnClickListener(v -> finish());

        pitchSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean f) {
                float pitch = p / 100f;
                if (pitch < 0.1f) pitch = 0.1f;
                pitchValue.setText(String.format(Locale.US, "%.1f", pitch));
                if (ttsReady) tts.setPitch(pitch);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean f) {
                float speed = p / 100f;
                if (speed < 0.1f) speed = 0.1f;
                speedValue.setText(String.format(Locale.US, "%.1fx", speed));
                if (ttsReady) tts.setSpeechRate(speed);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        // ONE BUTTON TOGGLE PLAY/PAUSE
        playButton.setOnClickListener(v -> {
            if (!ttsReady) return;
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            if (isPlaying) {
                tts.stop();
                isPlaying = false;
                playIcon.setImageResource(R.drawable.ic_play);
            } else {
                speak();
            }
        });

        saveButton.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            checkPermissionAndSave();
        });
    }

    private void speak() {
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "PLAY");
        tts.speak(scriptText, TextToSpeech.QUEUE_FLUSH, params, "PLAY");
        simulateProgress();
    }

    private void simulateProgress() {
        int words = scriptText.split("\\s+").length;
        int duration = Math.max(5, words / 3);
        handler.post(new Runnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!isPlaying) return;
                elapsed++;
                int prog = (int) ((elapsed * 100f) / duration);
                if (prog > 100) prog = 100;
                audioProgress.setProgress(prog);
                if (elapsed < duration && isPlaying) handler.postDelayed(this, 1000);
            }
        });
    }

    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAudioFile();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
            } else saveAudioFile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveAudioFile();
        }
    }

    // REAL DOWNLOAD FIX
    private void saveAudioFile() {
        if (!ttsReady || scriptText.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawName = audioTitle.getText().toString().trim();
        if (rawName.isEmpty()) rawName = "My Audio";
        String cleanedName = rawName.replaceAll("[^a-zA-Z0-9-_ ]", "").trim() + ".wav";

        final String finalFileName = cleanedName;
        final File finalTempFile = new File(getCacheDir(), finalFileName);

        loadingOverlay.setVisibility(View.VISIBLE);

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SAVE_FILE");

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
            }

            @Override
            public void onError(String id) {
                handler.post(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(TTSActivity.this, "Save failed - TTS not support", Toast.LENGTH_SHORT).show();
                    setupUtteranceListenerForPlay();
                });
            }

            @Override
            public void onDone(String id) {
                if (!id.equals("SAVE_FILE")) return;
                handler.post(() -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Downloads.DISPLAY_NAME, finalFileName);
                            values.put(MediaStore.Downloads.MIME_TYPE, "audio/x-wav");
                            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ReaderTTS");
                            values.put(MediaStore.Downloads.IS_PENDING, 1);
                            android.net.Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                try (OutputStream out = getContentResolver().openOutputStream(uri);
                                     FileInputStream in = new FileInputStream(finalTempFile)) {
                                    byte[] buffer = new byte[8192];
                                    int len;
                                    while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                                }
                                values.clear();
                                values.put(MediaStore.Downloads.IS_PENDING, 0);
                                getContentResolver().update(uri, values, null, null);
                                fileLocation.setVisibility(View.VISIBLE);
                                fileLocation.setText("Downloaded to: Downloads/ReaderTTS/" + finalFileName);
                                findViewById(R.id.saveStatus).setVisibility(View.VISIBLE);
                                Toast.makeText(TTSActivity.this, "Downloaded!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ReaderTTS");
                            if (!dir.exists()) dir.mkdirs();
                            File dest = new File(dir, finalFileName);
                            try (FileInputStream in = new FileInputStream(finalTempFile);
                                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                            }
                            MediaScannerConnection.scanFile(TTSActivity.this, new String[]{dest.getAbsolutePath()}, null, null);
                            fileLocation.setVisibility(View.VISIBLE);
                            fileLocation.setText("Downloaded to: " + dest.getAbsolutePath());
                            findViewById(R.id.saveStatus).setVisibility(View.VISIBLE);
                            Toast.makeText(TTSActivity.this, "Downloaded to Downloads!", Toast.LENGTH_LONG).show();
                        }
                        finalTempFile.delete();
                    } catch (Exception e) {
                        Toast.makeText(TTSActivity.this, "Copy failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    loadingOverlay.setVisibility(View.GONE);
                    setupUtteranceListenerForPlay();
                });
            }
        });

        int result = tts.synthesizeToFile(scriptText, params, finalTempFile, "SAVE_FILE");
        if (result != TextToSpeech.SUCCESS) {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "This device doesn't support saving", Toast.LENGTH_SHORT).show();
            setupUtteranceListenerForPlay();
        }
    }
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}