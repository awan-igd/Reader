package reader.aigd.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import reader.aigd.OverlayService;
import reader.aigd.R;
import reader.aigd.model.DataManager;
import reader.aigd.model.Script;

public class OverlayActivity extends AppCompatActivity {

    private static final String PREFS_OVERLAY = "overlay_settings";
    private static final String KEY_FONT_SIZE = "overlay_font_size";
    private static final String KEY_SCROLL_SPEED = "overlay_scroll_speed";
    private static final String KEY_LINE_SPACING = "overlay_line_spacing";
    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1002;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView scriptContent;
    private NestedScrollView scrollView;
    private MaterialCardView playButton;
    private MaterialCardView playOverlayButton;
    private MaterialCardView settingsButton;
    private MaterialCardView closeButton;
    private MaterialCardView settingsPanel;
    private View settingsScrim;
    private MaterialCardView closeSettingsButton;
    private MaterialCardView resetSettingsButton;
    private MaterialCardView fontSizeMinus;
    private MaterialCardView fontSizePlus;
    private MaterialCardView speedMinus;
    private MaterialCardView speedPlus;
    private MaterialCardView spacingMinus;
    private MaterialCardView spacingPlus;
    private TextView fontSizeValue;
    private TextView speedValue;
    private TextView spacingValue;
    private ImageView playIcon;
    private View progressLine;

    private DataManager dataManager;
    private long scriptId = -1;
    private String originalContent = "";
    private String scriptTitle = "";

    private boolean isPlaying = false;
    private ValueAnimator scrollAnimator;

    private int currentFontSize = 20;
    private float currentScrollSpeed = 1.0f;
    private int currentLineSpacing = 12;
    private boolean settingsPanelVisible = false;
    private SharedPreferences settingsPrefs;
    private int currentScrollY = 0;
    private int maxScrollY = 0;
    private boolean isScrolling = false;
    private Typeface aigdFont; // FIXED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);
        aigdFont = ResourcesCompat.getFont(this, R.font.aigd); // FIXED
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        dataManager = DataManager.getInstance(this);
        settingsPrefs = getSharedPreferences(PREFS_OVERLAY, MODE_PRIVATE);
        loadSavedSettings();
        initializeViews();
        getScriptData();
        setupUI();
        setupClickListeners();
        setupScrollListener();
        startEntranceAnimations();
        checkPermissions();
    }

    private void loadSavedSettings() {
        currentFontSize = settingsPrefs.getInt(KEY_FONT_SIZE, 20);
        currentScrollSpeed = settingsPrefs.getFloat(KEY_SCROLL_SPEED, 1.0f);
        currentLineSpacing = settingsPrefs.getInt(KEY_LINE_SPACING, 12);
    }

    private void saveSettings() {
        settingsPrefs.edit().putInt(KEY_FONT_SIZE, currentFontSize).putFloat(KEY_SCROLL_SPEED, currentScrollSpeed).putInt(KEY_LINE_SPACING, currentLineSpacing).apply();
    }

    private void initializeViews() {
        scriptContent = findViewById(R.id.scriptContent);
        scrollView = findViewById(R.id.scrollView);
        playButton = findViewById(R.id.playButton);
        playOverlayButton = findViewById(R.id.playOverlayButton);
        settingsButton = findViewById(R.id.settingsButton);
        closeButton = findViewById(R.id.closeButton);
        settingsPanel = findViewById(R.id.settingsPanel);
        settingsScrim = findViewById(R.id.settingsScrim);
        closeSettingsButton = findViewById(R.id.closeSettingsButton);
        resetSettingsButton = findViewById(R.id.resetSettingsButton);
        fontSizeValue = findViewById(R.id.fontSizeValue);
        fontSizeMinus = findViewById(R.id.fontSizeMinus);
        fontSizePlus = findViewById(R.id.fontSizePlus);
        speedValue = findViewById(R.id.speedValue);
        speedMinus = findViewById(R.id.speedMinus);
        speedPlus = findViewById(R.id.speedPlus);
        spacingValue = findViewById(R.id.spacingValue);
        spacingMinus = findViewById(R.id.spacingMinus);
        spacingPlus = findViewById(R.id.spacingPlus);
        playIcon = findViewById(R.id.playIcon);
        progressLine = findViewById(R.id.progressLine);
        if (scriptContent != null && aigdFont != null) scriptContent.setTypeface(aigdFont);
        if (fontSizeValue != null && aigdFont != null) fontSizeValue.setTypeface(aigdFont);
        if (speedValue != null && aigdFont != null) speedValue.setTypeface(aigdFont);
        if (spacingValue != null && aigdFont != null) spacingValue.setTypeface(aigdFont);
    }

    private void getScriptData() {
        Intent intent = getIntent();
        if (intent != null) {
            scriptId = intent.getLongExtra("script_id", -1);
            originalContent = intent.getStringExtra("script_content");
            scriptTitle = intent.getStringExtra("script_title");
            if (scriptId != -1) {
                Script s = dataManager.getScriptById(scriptId);
                if (s != null) {
                    originalContent = s.getContent();
                    scriptTitle = s.getTitle();
                }
            }
        }
        if (originalContent == null || originalContent.isEmpty()) {
            originalContent = "Welcome to Teleprompter.\n\nYour script will appear here. Press play to start scrolling.\n\nAdjust size, speed and spacing from settings.";
        }
    }

    private void setupUI() {
        scriptContent.setText(originalContent);
        applyTextStyling();
        updateUIComponents();
    }

    private void updateUIComponents() {
        if (speedValue != null)
            speedValue.setText(String.format(Locale.US, "%.1fx", currentScrollSpeed));
        if (fontSizeValue != null) fontSizeValue.setText(String.valueOf(currentFontSize));
        if (spacingValue != null) spacingValue.setText(String.valueOf(currentLineSpacing));
    }

    private void applyTextStyling() {
        if (scriptContent != null) {
            scriptContent.setTextSize(currentFontSize);
            scriptContent.setLineSpacing(currentLineSpacing, 1.0f);
        }
    }

    private void setupClickListeners() {
        if (closeButton != null) closeButton.setOnClickListener(v -> {
            haptic();
            anim(v);
            finishWithAnimation();
        });
        if (closeSettingsButton != null) closeSettingsButton.setOnClickListener(v -> {
            haptic();
            anim(v);
            toggleSettingsPanel();
        });
        if (playButton != null) playButton.setOnClickListener(v -> {
            haptic();
            animPlay(v);
            if (isPlaying) pauseScrolling();
            else startScrolling();
        });
        if (playOverlayButton != null) playOverlayButton.setOnClickListener(v -> {
            haptic();
            anim(v);
            startOverlayService();
        });
        if (settingsButton != null) settingsButton.setOnClickListener(v -> {
            haptic();
            anim(v);
            toggleSettingsPanel();
        });
        if (settingsScrim != null) settingsScrim.setOnClickListener(v -> toggleSettingsPanel());
        if (fontSizeMinus != null) fontSizeMinus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustFontSize(-2);
        });
        if (fontSizePlus != null) fontSizePlus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustFontSize(2);
        });
        if (speedMinus != null) speedMinus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustScrollSpeed(-0.2f);
        });
        if (speedPlus != null) speedPlus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustScrollSpeed(0.2f);
        });
        if (spacingMinus != null) spacingMinus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustLineSpacing(-2);
        });
        if (spacingPlus != null) spacingPlus.setOnClickListener(v -> {
            haptic();
            anim(v);
            adjustLineSpacing(2);
        });
        if (resetSettingsButton != null) resetSettingsButton.setOnClickListener(v -> {
            haptic();
            anim(v);
            resetSettings();
        });
    }

    private void startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        String content = scriptContent.getText().toString();
        Intent intent = new Intent(this, OverlayService.class);
        intent.putExtra("script_id", scriptId);
        intent.putExtra("script_content", content);
        intent.putExtra("script_title", scriptTitle);
        intent.putExtra("font_size", currentFontSize);
        intent.putExtra("scroll_speed", currentScrollSpeed);
        intent.putExtra("line_spacing", currentLineSpacing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        Toast.makeText(this, "Floating started!", Toast.LENGTH_SHORT).show();
        finishWithAnimation();
    }

    private void setupScrollListener() {
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                if (!isScrolling) {
                    currentScrollY = scrollView.getScrollY();
                    updateProgress();
                }
            });
        }
    }

    private void startScrolling() {
        updateMaxScroll();
        int dist = maxScrollY - currentScrollY;
        if (dist <= 0) {
            if (scrollView != null) scrollView.smoothScrollTo(0, 0);
            currentScrollY = 0;
            return;
        }
        long duration = (long) ((dist * 50L) / currentScrollSpeed);
        duration = Math.max(3000, duration);
        isPlaying = true;
        updatePlayButton(true);
        startScrollAnimation(duration);
    }

    private void startScrollAnimation(long duration) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) scrollAnimator.cancel();
        int startY = currentScrollY;
        int endY = maxScrollY;
        scrollAnimator = ValueAnimator.ofInt(startY, endY);
        scrollAnimator.setDuration(duration);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            int val = (int) a.getAnimatedValue();
            isScrolling = true;
            if (scrollView != null) scrollView.scrollTo(0, val);
            currentScrollY = val;
            float prog = (float) (val) / (float) Math.max(1, endY);
            if (progressLine != null) progressLine.setScaleX(prog);
        });
        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isScrolling = false;
                isPlaying = false;
                updatePlayButton(false);
                Toast.makeText(OverlayActivity.this, "Done!", Toast.LENGTH_SHORT).show();
            }
        });
        if (scrollView != null) scrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        scrollAnimator.start();
    }

    private void pauseScrolling() {
        if (scrollAnimator != null) scrollAnimator.cancel();
        isScrolling = false;
        isPlaying = false;
        updatePlayButton(false);
        if (scrollView != null) scrollView.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    private void updatePlayButton(boolean playing) {
        if (playIcon != null) {
            playIcon.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
            playIcon.animate().rotationBy(playing ? 180f : -180f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
        }
    }

    private void toggleSettingsPanel() {
        if (settingsPanel == null || settingsScrim == null) return;
        if (settingsPanelVisible) {
            settingsScrim.animate().alpha(0f).setDuration(200).withEndAction(() -> settingsScrim.setVisibility(View.GONE)).start();
            settingsPanel.animate().translationY(settingsPanel.getHeight() + 100).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(() -> {
                settingsPanel.setVisibility(View.GONE);
                settingsPanelVisible = false;
            }).start();
        } else {
            settingsScrim.setVisibility(View.VISIBLE);
            settingsScrim.setAlpha(0f);
            settingsScrim.animate().alpha(0.5f).setDuration(250).start();
            settingsPanel.setVisibility(View.VISIBLE);
            settingsPanel.setTranslationY(settingsPanel.getHeight() + 100);
            settingsPanel.animate().translationY(0f).setDuration(350).setInterpolator(new OvershootInterpolator(1.1f)).start();
            settingsPanelVisible = true;
        }
    }

    private void adjustFontSize(int d) {
        int n = currentFontSize + d;
        if (n >= 12 && n <= 40) {
            currentFontSize = n;
            if (fontSizeValue != null) fontSizeValue.setText(String.valueOf(n));
            scaleAnim(fontSizeValue);
            applyTextStyling();
            saveSettings();
            updateMaxScroll();
        }
    }

    private void adjustScrollSpeed(float d) {
        float n = Math.round((currentScrollSpeed + d) * 10f) / 10f;
        if (n >= 0.2f && n <= 5.0f) {
            currentScrollSpeed = n;
            updateUIComponents();
            scaleAnim(speedValue);
            saveSettings();
            if (isPlaying) {
                pauseScrolling();
                startScrolling();
            }
        }
    }

    private void adjustLineSpacing(int d) {
        int n = currentLineSpacing + d;
        if (n >= 4 && n <= 30) {
            currentLineSpacing = n;
            if (spacingValue != null) spacingValue.setText(String.valueOf(n));
            scaleAnim(spacingValue);
            applyTextStyling();
            saveSettings();
            updateMaxScroll();
        }
    }

    private void resetSettings() {
        currentFontSize = 20;
        currentScrollSpeed = 1.0f;
        currentLineSpacing = 12;
        updateUIComponents();
        applyTextStyling();
        saveSettings();
        updateMaxScroll();
        Toast.makeText(this, "Reset done", Toast.LENGTH_SHORT).show();
    }

    private void scaleAnim(TextView v) {
        if (v == null) return;
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(new OvershootInterpolator()).start()).start();
    }

    private void updateMaxScroll() {
        if (scrollView != null && scrollView.getChildAt(0) != null) {
            maxScrollY = scrollView.getChildAt(0).getHeight() - scrollView.getHeight();
            if (maxScrollY < 0) maxScrollY = 0;
        }
    }

    private void updateProgress() {
        updateMaxScroll();
        if (maxScrollY > 0 && progressLine != null) {
            float p = (float) (scrollView != null ? scrollView.getScrollY() : 0) / maxScrollY;
            progressLine.setScaleX(Math.min(p, 1f));
        }
    }

    private void anim(View v) {
        if (v == null) return;
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(new OvershootInterpolator()).start()).start();
    }

    private void animPlay(View v) {
        if (v == null) return;
        v.animate().rotationBy(15f).setDuration(200).withEndAction(() -> v.animate().rotationBy(-15f).setDuration(200).start()).start();
        anim(v);
    }

    private void haptic() {
        View root = findViewById(android.R.id.content);
        if (root != null) root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void startEntranceAnimations() {
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            topBar.setAlpha(0f);
            topBar.setTranslationY(-60f);
            topBar.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(new OvershootInterpolator(1.1f)).start();
        }
        if (scriptContent != null) {
            scriptContent.setAlpha(0f);
            scriptContent.setTranslationY(20f);
            scriptContent.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(150).start();
        }
    }

    private void finishWithAnimation() {
        View root = findViewById(android.R.id.content);
        if (root != null)
            root.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(250).withEndAction(() -> {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            }).start();
        else finish();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            requestOverlayPermission();
    }

    private void requestOverlayPermission() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permission_request, null);
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        if (title != null && aigdFont != null) title.setTypeface(aigdFont);
        TextView msg = dialogView.findViewById(R.id.dialogMessage);
        if (msg != null && aigdFont != null) msg.setTypeface(aigdFont);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme).setView(dialogView).create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        View allow = dialogView.findViewById(R.id.allowButton);
        if (allow != null) allow.setOnClickListener(v -> {
            anim(v);
            dialog.dismiss();
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), OVERLAY_PERMISSION_REQUEST);
        });
        View cancel = dialogView.findViewById(R.id.cancelButton);
        if (cancel != null) cancel.setOnClickListener(v -> {
            anim(v);
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            finish();
    }

    @Override
    public void onBackPressed() {
        if (settingsPanelVisible) toggleSettingsPanel();
        else if (isPlaying) pauseScrolling();
        else finishWithAnimation();
    }

    @Override
    protected void onDestroy() {
        if (scrollAnimator != null) scrollAnimator.cancel();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}