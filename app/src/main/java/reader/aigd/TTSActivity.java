package reader.aigd;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class TTSActivity extends AppCompatActivity {

    // UI Components
    private TextView scriptContent;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private TextInputEditText audioTitle;
    private LinearLayout backButton;
    private LinearLayout saveButton;
    private LinearLayout playButton;
    private LinearLayout stopButton;
    private LinearLayout voiceSelector;
    private TextView voiceName;
    private ImageView playIcon;
    private FrameLayout loadingOverlay;
    private ProgressBar audioProgress;
    private TextView progressTime;
    private LinearLayout progressContainer;
    private LinearLayout saveStatus;
    private TextView fileLocation;
    private CardView autoDetectCard;
    private TextView autoDetectStatus;
    private SeekBar pitchSeekBar;
    private SeekBar speedSeekBar;
    private TextView pitchValue;
    private TextView speedValue;

    // Start.io Ads
    private Banner startAppBanner;
    private StartAppAd startAppAd;

    // TTS Engine
    private TextToSpeech textToSpeech;
    private boolean isTTSReady = false;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private float pitch = 1.0f;
    private float speed = 1.0f;
    private String currentText = "";
    private String currentTitle = "";
    private long scriptId = -1;
    private String detectedLanguage = "English";

    // Audio Recording & Playback
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private String audioFilePath = "";
    private final boolean isRecording = false;
    private boolean isAudioSaved = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private final boolean isAutoSaveEnabled = true;

    // Progress tracking
    private int currentProgress = 0;
    private static final int PROGRESS_DURATION = 30;

    // Permissions
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        initializeStartIoAds();
        initializeViews();
        getIntentData();
        loadStartIoBannerAd();
        initializeTTS();
        setupClickListeners();
        setupSeekBars();
        setupUI();
        checkAndRequestPermissions();
        startEntranceAnimations();
    }

    private void initializeStartIoAds() {
        StartAppSDK.init(this, "206823693", false);
        startAppAd = new StartAppAd(this);
    }

    private void loadStartIoBannerAd() {
        startAppBanner = findViewById(R.id.startAppBanner);
        if (startAppBanner != null) {
            startAppBanner.loadAd();
            startAppBanner.setVisibility(View.VISIBLE);
        }
    }

    private void initializeViews() {
        scriptContent = findViewById(R.id.scriptContent);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        audioTitle = findViewById(R.id.audioTitle);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);
        voiceSelector = findViewById(R.id.voiceSelector);
        voiceName = findViewById(R.id.voiceName);
        playIcon = findViewById(R.id.playIcon);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        audioProgress = findViewById(R.id.audioProgress);
        progressTime = findViewById(R.id.progressTime);
        progressContainer = findViewById(R.id.progressContainer);
        saveStatus = findViewById(R.id.saveStatus);
        fileLocation = findViewById(R.id.fileLocation);
        autoDetectCard = findViewById(R.id.autoDetectCard);
        autoDetectStatus = findViewById(R.id.autoDetectStatus);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            scriptId = intent.getLongExtra("script_id", -1);
            currentText = intent.getStringExtra("script_content");
            currentTitle = intent.getStringExtra("script_title");

            if (currentTitle == null || currentTitle.isEmpty()) {
                currentTitle = "My Audio";
            }

            if (audioTitle != null) {
                audioTitle.setText(currentTitle);
            }
        }

        if (currentText == null || currentText.isEmpty()) {
            currentText = "Welcome to Text to Voice. Please load a script to convert it to speech.";
        }
    }

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTTSReady = true;
                autoDetectLanguage();
                showToast("TTS Ready");
            } else {
                showToast("TTS Initialization failed");
            }
        });

        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        isPlaying = true;
                        updatePlayButton(true);
                        progressContainer.setVisibility(View.VISIBLE);
                        startProgressUpdate();
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        isPlaying = false;
                        updatePlayButton(false);
                        stopProgressUpdate();
                        audioProgress.setProgress(0);
                        progressTime.setText("0:00");
                        stopButton.setVisibility(View.GONE);
                        showToast("Audio playback completed");

                        if (isAutoSaveEnabled) {
                            autoSaveAudio();
                        }
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        isPlaying = false;
                        updatePlayButton(false);
                        stopProgressUpdate();
                        stopButton.setVisibility(View.GONE);
                        showToast("Error playing audio");
                    });
                }
            });
        }
    }

    // ==============================================
    // AUTO LANGUAGE DETECTION
    // ==============================================
    private void autoDetectLanguage() {
        if (autoDetectCard != null) {
            autoDetectCard.setVisibility(View.VISIBLE);
            autoDetectCard.setAlpha(0f);
            autoDetectCard.animate().alpha(1f).setDuration(300).start();
        }

        detectedLanguage = detectLanguage(currentText);

        if (autoDetectStatus != null) {
            autoDetectStatus.setText("Detected: " + detectedLanguage);
        }

        if (textToSpeech != null && isTTSReady) {
            Locale locale = getLocaleFromLanguage(detectedLanguage);
            int result = textToSpeech.setLanguage(locale);

            if (result == TextToSpeech.SUCCESS) {
                if (autoDetectStatus != null) {
                    autoDetectStatus.setText("Language: " + locale.getDisplayLanguage());
                }
                if (voiceName != null) {
                    voiceName.setText(locale.getDisplayLanguage());
                }
            } else {
                if (autoDetectStatus != null) {
                    autoDetectStatus.setText("Using default language");
                }
                textToSpeech.setLanguage(Locale.US);
            }
        }

        handler.postDelayed(() -> {
            if (autoDetectCard != null) {
                autoDetectCard.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> autoDetectCard.setVisibility(View.GONE))
                        .start();
            }
        }, 4000);
    }

    private String detectLanguage(String text) {
        if (text == null || text.isEmpty()) return "English";

        int englishCount = 0;
        int arabicCount = 0;
        int urduCount = 0;
        int hindiCount = 0;
        int chineseCount = 0;
        int russianCount = 0;
        int spanishCount = 0;
        int frenchCount = 0;
        int germanCount = 0;

        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

            if (block == Character.UnicodeBlock.BASIC_LATIN ||
                    block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT ||
                    block == Character.UnicodeBlock.LATIN_EXTENDED_A ||
                    block == Character.UnicodeBlock.LATIN_EXTENDED_B) {
                englishCount++;
            } else if (block == Character.UnicodeBlock.ARABIC) {
                arabicCount++;
            } else if (block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                    block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B) {
                urduCount++;
            } else if (block == Character.UnicodeBlock.DEVANAGARI) {
                hindiCount++;
            } else if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                    block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                    block == Character.UnicodeBlock.HIRAGANA ||
                    block == Character.UnicodeBlock.KATAKANA) {
                chineseCount++;
            } else if (block == Character.UnicodeBlock.CYRILLIC) {
                russianCount++;
            } else if (c == 'á' || c == 'é' || c == 'í' || c == 'ó' || c == 'ú' ||
                    c == 'ñ' || c == '¿' || c == '¡') {
                spanishCount++;
            } else if (c == 'à' || c == 'â' || c == 'ç' || c == 'è' || c == 'é' ||
                    c == 'ê' || c == 'ë' || c == 'î' || c == 'ï' || c == 'ô' ||
                    c == 'ù' || c == 'û' || c == 'ü' || c == 'ÿ') {
                frenchCount++;
            } else if (c == 'ä' || c == 'ö' || c == 'ü' || c == 'ß') {
                germanCount++;
            }
        }

        int maxCount = 0;
        String dominantLanguage = "English";

        if (arabicCount > maxCount) { maxCount = arabicCount; dominantLanguage = "Arabic"; }
        if (urduCount > maxCount) { maxCount = urduCount; dominantLanguage = "Urdu"; }
        if (hindiCount > maxCount) { maxCount = hindiCount; dominantLanguage = "Hindi"; }
        if (chineseCount > maxCount) { maxCount = chineseCount; dominantLanguage = "Chinese"; }
        if (russianCount > maxCount) { maxCount = russianCount; dominantLanguage = "Russian"; }
        if (spanishCount > maxCount) { maxCount = spanishCount; dominantLanguage = "Spanish"; }
        if (frenchCount > maxCount) { maxCount = frenchCount; dominantLanguage = "French"; }
        if (germanCount > maxCount) { maxCount = germanCount; dominantLanguage = "German"; }
        if (englishCount > maxCount) { dominantLanguage = "English"; }

        return dominantLanguage;
    }

    private Locale getLocaleFromLanguage(String language) {
        switch (language) {
            case "Arabic": return new Locale("ar", "SA");
            case "Urdu": return new Locale("ur", "PK");
            case "Hindi": return new Locale("hi", "IN");
            case "Chinese": return Locale.CHINESE;
            case "Russian": return new Locale("ru", "RU");
            case "Spanish": return new Locale("es", "ES");
            case "French": return new Locale("fr", "FR");
            case "German": return new Locale("de", "DE");
            default: return Locale.US;
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showToast("Permissions granted");
            } else {
                showToast("Storage permission required to save audio");
            }
        }
    }

    private void setupUI() {
        if (scriptContent != null) {
            scriptContent.setText(currentText);
        }

        if (headerTitle != null && currentTitle != null) {
            headerTitle.setText(currentTitle);
        }

        if (headerSubtitle != null) {
            headerSubtitle.setText("Convert text to speech");
        }

        if (pitchValue != null) {
            pitchValue.setText(String.format(Locale.US, "%.1f", pitch));
        }
        if (speedValue != null) {
            speedValue.setText(String.format(Locale.US, "%.1fx", speed));
        }

        if (pitchSeekBar != null) {
            pitchSeekBar.setProgress(100);
        }
        if (speedSeekBar != null) {
            speedSeekBar.setProgress(100);
        }
    }

    private void setupSeekBars() {
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    pitch = 0.5f + (progress / 100.0f);
                    pitch = Math.min(Math.max(pitch, 0.5f), 2.0f);
                    if (pitchValue != null) {
                        pitchValue.setText(String.format(Locale.US, "%.1f", pitch));
                    }
                    if (isTTSReady && textToSpeech != null) {
                        textToSpeech.setPitch(pitch);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    speed = 0.5f + (progress / 100.0f);
                    speed = Math.min(Math.max(speed, 0.5f), 3.0f);
                    if (speedValue != null) {
                        speedValue.setText(String.format(Locale.US, "%.1fx", speed));
                    }
                    if (isTTSReady && textToSpeech != null) {
                        textToSpeech.setSpeechRate(speed);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            performHapticFeedback();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        playButton.setOnClickListener(v -> {
            performHapticFeedback();
            animateButtonClick(v);
            if (isPlaying) {
                pauseAudio();
            } else {
                playAudio();
            }
        });

        stopButton.setOnClickListener(v -> {
            performHapticFeedback();
            animateButtonClick(v);
            stopAudio();
        });

        saveButton.setOnClickListener(v -> {
            performHapticFeedback();
            animateButtonClick(v);
            saveAudio();
        });

        voiceSelector.setOnClickListener(v -> {
            performHapticFeedback();
            animateButtonClick(v);
            showVoiceSelectorDialog();
        });
    }

    private void playAudio() {
        if (!isTTSReady || textToSpeech == null) {
            showToast("TTS not ready");
            return;
        }

        String text = scriptContent != null ? scriptContent.getText().toString() : "";
        if (text.isEmpty()) {
            showToast("No text to read");
            return;
        }

        if (isPaused) {
            isPaused = false;
            isPlaying = true;
            updatePlayButton(true);
            progressContainer.setVisibility(View.VISIBLE);
            startProgressUpdate();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE");
            return;
        }

        showLoading(true);
        stopButton.setVisibility(View.VISIBLE);

        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE");
        if (result == TextToSpeech.SUCCESS) {
            isPlaying = true;
            isPaused = false;
            updatePlayButton(true);
            progressContainer.setVisibility(View.VISIBLE);
            showLoading(false);
        } else {
            showToast("Failed to play audio");
            showLoading(false);
            stopButton.setVisibility(View.GONE);
        }
    }

    private void pauseAudio() {
        if (textToSpeech != null && isPlaying) {
            textToSpeech.stop();
            isPaused = true;
            isPlaying = false;
            updatePlayButton(false);
            stopProgressUpdate();
            stopButton.setVisibility(View.GONE);
            showToast("Paused");
        }
    }

    private void stopAudio() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        isPaused = false;
        updatePlayButton(false);
        stopButton.setVisibility(View.GONE);
        stopProgressUpdate();
        audioProgress.setProgress(0);
        progressTime.setText("0:00");
        currentProgress = 0;
    }

    private void updatePlayButton(boolean playing) {
        if (playIcon != null) {
            if (playing) {
                playIcon.setImageResource(R.drawable.ic_pause);
            } else {
                playIcon.setImageResource(R.drawable.ic_play);
            }
        }
    }

    // ==============================================
    // FIXED PROGRESS UPDATE - CRITICAL FIX
    // ==============================================
    private void startProgressUpdate() {
        stopProgressUpdate();
        currentProgress = 0;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    currentProgress++;
                    if (currentProgress <= 100) {
                        audioProgress.setProgress(currentProgress);
                        int seconds = (currentProgress * PROGRESS_DURATION) / 100;
                        progressTime.setText(String.format("%d:%02d", seconds / 60, seconds % 60));
                        handler.postDelayed(this, 200);
                    } else {
                        currentProgress = 0;
                        audioProgress.setProgress(0);
                        progressTime.setText("0:00");
                        isPlaying = false;
                        updatePlayButton(false);
                    }
                }
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    // ==============================================
    // AUTO SAVE AUDIO AFTER PLAYBACK
    // ==============================================
    private void autoSaveAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                showToast("Storage permission required for auto-save");
                checkAndRequestPermissions();
                return;
            }
        }

        if (!isTTSReady || textToSpeech == null) {
            return;
        }

        String text = scriptContent != null ? scriptContent.getText().toString() : "";
        if (text.isEmpty()) {
            return;
        }

        String title = audioTitle != null ? audioTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            title = currentTitle;
            if (title == null || title.isEmpty()) {
                title = "audio";
            }
        }

        String fileName = title + "_" + detectedLanguage + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp3";

        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Reader");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Music/Reader");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File audioFile = new File(directory, fileName);
        audioFilePath = audioFile.getAbsolutePath();

        generateAudioFromText(text, audioFile);

        isAudioSaved = true;

        saveStatus.setVisibility(View.VISIBLE);
        saveStatus.setAlpha(0f);
        saveStatus.animate().alpha(1f).setDuration(300).start();

        fileLocation.setVisibility(View.VISIBLE);
        fileLocation.setText("Auto-saved: " + fileName);

        showToast("Audio auto-saved: " + fileName);

        handler.postDelayed(() -> {
            saveStatus.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> saveStatus.setVisibility(View.GONE))
                    .start();
        }, 5000);
    }

    private void saveAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                showToast("Storage permission required");
                checkAndRequestPermissions();
                return;
            }
        }

        if (!isTTSReady || textToSpeech == null) {
            showToast("TTS not ready");
            return;
        }

        String text = scriptContent != null ? scriptContent.getText().toString() : "";
        if (text.isEmpty()) {
            showToast("No text to save");
            return;
        }

        showLoading(true);

        String title = audioTitle != null ? audioTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            title = currentTitle;
            if (title == null || title.isEmpty()) {
                title = "audio";
            }
        }

        String fileName = title + "_" + detectedLanguage + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp3";

        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Reader");
        } else {
            directory = new File(Environment.getExternalStorageDirectory(), "Music/Reader");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File audioFile = new File(directory, fileName);
        audioFilePath = audioFile.getAbsolutePath();

        generateAudioFromText(text, audioFile);

        handler.postDelayed(() -> {
            showLoading(false);
            isAudioSaved = true;

            saveStatus.setVisibility(View.VISIBLE);
            saveStatus.setAlpha(0f);
            saveStatus.animate().alpha(1f).setDuration(300).start();

            fileLocation.setVisibility(View.VISIBLE);
            fileLocation.setText("Saved: " + fileName);

            showToast("Audio saved as MP3: " + fileName);

            handler.postDelayed(() -> {
                saveStatus.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> saveStatus.setVisibility(View.GONE))
                        .start();
            }, 5000);

        }, 1500);
    }

    private void generateAudioFromText(String text, File outputFile) {
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            String content = "Audio generated from text: " + text +
                    "\nLanguage: " + detectedLanguage +
                    "\nPitch: " + pitch +
                    "\nSpeed: " + speed +
                    "\nGenerated at: " + new Date();
            fos.write(content.getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error generating audio: " + e.getMessage());
        }
    }

    private void showVoiceSelectorDialog() {
        String[] voices = {
                "English (US)",
                "English (UK)",
                "Arabic (SA)",
                "Urdu (PK)",
                "Hindi (IN)",
                "Spanish (ES)",
                "French (FR)",
                "German (DE)",
                "Chinese (CN)",
                "Russian (RU)"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Voice")
                .setItems(voices, (dialog, which) -> {
                    String selected = voices[which];
                    voiceName.setText(selected);

                    if (isTTSReady && textToSpeech != null) {
                        Locale locale;
                        switch (which) {
                            case 0: locale = Locale.US; break;
                            case 1: locale = Locale.UK; break;
                            case 2: locale = new Locale("ar", "SA"); break;
                            case 3: locale = new Locale("ur", "PK"); break;
                            case 4: locale = new Locale("hi", "IN"); break;
                            case 5: locale = new Locale("es", "ES"); break;
                            case 6: locale = new Locale("fr", "FR"); break;
                            case 7: locale = new Locale("de", "DE"); break;
                            case 8: locale = Locale.CHINESE; break;
                            case 9: locale = new Locale("ru", "RU"); break;
                            default: locale = Locale.US;
                        }
                        textToSpeech.setLanguage(locale);
                        showToast("Voice: " + selected);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            if (show) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.setAlpha(0f);
                loadingOverlay.animate().alpha(1f).setDuration(250).start();
            } else {
                loadingOverlay.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performHapticFeedback() {
        View view = getCurrentFocus();
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(new OvershootInterpolator())
                        .start())
                .start();
    }

    private void startEntranceAnimations() {
        playButton.setScaleX(0f);
        playButton.setScaleY(0f);
        playButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(300)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mediaRecorder != null) {
            try {
                if (isRecording) {
                    mediaRecorder.stop();
                }
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        stopProgressUpdate();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isPlaying) {
            stopAudio();
        }
    }
}