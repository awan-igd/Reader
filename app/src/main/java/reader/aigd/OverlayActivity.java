package reader.aigd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import java.util.Locale;

public class OverlayActivity extends AppCompatActivity {

    private static final String PREFS_OVERLAY = "overlay_settings";
    private static final String KEY_FONT_SIZE = "overlay_font_size";
    private static final String KEY_SCROLL_SPEED = "overlay_scroll_speed";
    private static final String KEY_LINE_SPACING = "overlay_line_spacing";
    private static final String KEY_OPACITY = "overlay_opacity";
    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1002;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // UI Components
    private TextView scriptContent;
    private NestedScrollView scrollView;
    private LinearLayout playButton;
    private LinearLayout playOverlayButton;
    private LinearLayout settingsButton;
    private LinearLayout settingsPanel;
    private View settingsScrim;
    private TextView fontSizeValue;
    private TextView fontSizeMinus;
    private TextView fontSizePlus;
    private TextView speedValue;
    private TextView speedMinus;
    private TextView speedPlus;
    private TextView spacingValue;
    private TextView spacingMinus;
    private TextView spacingPlus;
    private ImageView playIcon;
    private FrameLayout loadingOverlay;
    private View anchorLine;
    private LinearLayout closeButton;
    private LinearLayout closeSettingsButton;
    private LinearLayout resetSettingsButton;
    private View progressLine;

    // Data Parameters
    private DataManager dataManager;
    private long scriptId = -1;
    private String originalContent = "";
    private String scriptTitle = "";

    // Teleprompter Configuration State
    private boolean isPlaying = false;
    private ValueAnimator scrollAnimator;

    // Settings Defaults
    private int currentFontSize = 18;
    private float currentScrollSpeed = 1.0f;
    private int currentLineSpacing = 8;
    private int currentOpacity = 100;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean settingsPanelVisible = false;

    // SharedPreferences Storage
    private SharedPreferences settingsPrefs;

    // Smooth Scrolling Coordinates
    private int currentScrollY = 0;
    private int maxScrollY = 0;
    private boolean isScrolling = false;
    private float progressPercent = 0f;

    // Permission flags
    private boolean hasOverlayPermission = false;
    private boolean hasNotificationPermission = false;

    // Font
    private Typeface nolroFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay);

        // Load font
        nolroFont = ResourcesCompat.getFont(this, R.font.aigd);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        dataManager = DataManager.getInstance(this);
        settingsPrefs = getSharedPreferences(PREFS_OVERLAY, MODE_PRIVATE);

        loadSavedSettings();
        initializeViews();
        getScriptData();
        setupUI();
        setupClickListeners();
        setupScrollListener();
        startEntranceAnimations();
        updateProgressLine();

        checkPermissions();
    }

    private void loadSavedSettings() {
        currentFontSize = settingsPrefs.getInt(KEY_FONT_SIZE, 18);
        currentScrollSpeed = settingsPrefs.getFloat(KEY_SCROLL_SPEED, 1.0f);
        currentLineSpacing = settingsPrefs.getInt(KEY_LINE_SPACING, 8);
        currentOpacity = settingsPrefs.getInt(KEY_OPACITY, 100);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = settingsPrefs.edit();
        editor.putInt(KEY_FONT_SIZE, currentFontSize);
        editor.putFloat(KEY_SCROLL_SPEED, currentScrollSpeed);
        editor.putInt(KEY_LINE_SPACING, currentLineSpacing);
        editor.putInt(KEY_OPACITY, currentOpacity);
        editor.apply();
    }

    private void initializeViews() {
        scriptContent = findViewById(R.id.scriptContent);
        scrollView = findViewById(R.id.scrollView);
        playButton = findViewById(R.id.playButton);
        playOverlayButton = findViewById(R.id.playOverlayButton);
        settingsButton = findViewById(R.id.settingsButton);
        settingsPanel = findViewById(R.id.settingsPanel);
        settingsScrim = findViewById(R.id.settingsScrim);
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
        loadingOverlay = findViewById(R.id.loadingOverlay);
        anchorLine = findViewById(R.id.anchorLine);
        closeButton = findViewById(R.id.closeButton);
        closeSettingsButton = findViewById(R.id.closeSettingsButton);
        resetSettingsButton = findViewById(R.id.resetSettingsButton);
        progressLine = findViewById(R.id.progressLine);

        applyFontToViews();
    }

    private void applyFontToViews() {
        if (scriptContent != null) scriptContent.setTypeface(nolroFont);
        if (fontSizeValue != null) fontSizeValue.setTypeface(nolroFont);
        if (speedValue != null) speedValue.setTypeface(nolroFont);
        if (spacingValue != null) spacingValue.setTypeface(nolroFont);
    }

    private void getScriptData() {
        Intent intent = getIntent();
        if (intent != null) {
            scriptId = intent.getLongExtra("script_id", -1);
            originalContent = intent.getStringExtra("script_content");
            scriptTitle = intent.getStringExtra("script_title");

            if (scriptId != -1) {
                Script currentScript = dataManager.getScriptById(scriptId);
                if (currentScript != null) {
                    originalContent = currentScript.getContent();
                    scriptTitle = currentScript.getTitle();
                }
            }
        }

        if (originalContent == null || originalContent.isEmpty()) {
            originalContent = "Welcome to Floating Overlay. Your script will appear here.";
        }
    }

    private void setupUI() {
        if (originalContent != null && !originalContent.isEmpty()) {
            scriptContent.setText(originalContent);
            applyTextStyling();
        }

        updateUIComponents();
        applyOpacity();
    }

    private void applyOpacity() {
        float alpha = currentOpacity / 100f;
        if (scriptContent != null) {
            scriptContent.setAlpha(alpha);
        }
        if (anchorLine != null) {
            anchorLine.setAlpha(alpha * 0.8f);
        }
    }

    private void updateUIComponents() {
        if (speedValue != null) {
            speedValue.setText(String.format(Locale.US, "%.1fx", currentScrollSpeed));
        }
        if (fontSizeValue != null) {
            fontSizeValue.setText(String.valueOf(currentFontSize));
        }
        if (spacingValue != null) {
            spacingValue.setText(String.valueOf(currentLineSpacing));
        }
    }

    private void applyTextStyling() {
        if (scriptContent != null) {
            scriptContent.setTextSize(currentFontSize);
            scriptContent.setLineSpacing(currentLineSpacing, 1.0f);
        }
    }

    private void updateProgressLine() {
        if (progressLine == null || scrollView == null) return;

        updateMaxScroll();
        if (maxScrollY > 0) {
            int scrollY = scrollView.getScrollY();
            progressPercent = Math.min((float) scrollY / maxScrollY, 1f);

            ValueAnimator widthAnim = ValueAnimator.ofFloat(0f, progressPercent);
            widthAnim.setDuration(300);
            widthAnim.setInterpolator(new DecelerateInterpolator());
            widthAnim.addUpdateListener(anim -> {
                float progress = (float) anim.getAnimatedValue();
                if (progressLine != null) {
                    progressLine.setScaleX(progress);
                }
            });
            widthAnim.start();
        }
    }

    // ==============================================
    // PERMISSION MANAGEMENT
    // ==============================================
    private void checkPermissions() {
        checkOverlayPermission();
        checkNotificationPermission();
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(this);
            if (!hasOverlayPermission) {
                requestOverlayPermission();
            }
        } else {
            hasOverlayPermission = true;
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasNotificationPermission = true;
        }
    }

    private void requestOverlayPermission() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permission_request, null);

        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        TextView messageText = dialogView.findViewById(R.id.dialogMessage);
        TextView permissionName = dialogView.findViewById(R.id.permissionName);
        TextView permissionDescription = dialogView.findViewById(R.id.permissionDescription);
        LinearLayout allowButton = dialogView.findViewById(R.id.allowButton);
        LinearLayout cancelButton = dialogView.findViewById(R.id.cancelButton);
        ImageView iconImage = dialogView.findViewById(R.id.iconImage);
        View iconContainer = dialogView.findViewById(R.id.iconContainer);

        if (titleText != null) {
            titleText.setText("Overlay Permission");
            titleText.setTypeface(nolroFont);
        }
        if (messageText != null) {
            messageText.setText("Reader needs this permission to show the floating teleprompter on top of other apps.");
            messageText.setTypeface(nolroFont);
        }
        if (permissionName != null) {
            permissionName.setText("Display over other apps");
            permissionName.setTypeface(nolroFont);
        }
        if (permissionDescription != null) {
            permissionDescription.setText("Allows you to read scripts while using other apps like TikTok, Camera, and YouTube.");
            permissionDescription.setTypeface(nolroFont);
        }
        if (iconImage != null) {
            iconImage.setImageResource(R.drawable.ic_logo);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (iconContainer != null) {
            iconContainer.setScaleX(0f);
            iconContainer.setScaleY(0f);
            iconContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }

        if (allowButton != null) {
            allowButton.setOnClickListener(v -> {
                animateButtonClick(v);
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            });
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                animateButtonClick(v);
                dialog.dismiss();
                Toast.makeText(OverlayActivity.this,
                        "Overlay permission required for floating teleprompter", Toast.LENGTH_LONG).show();
                finish();
            });
        }

        dialog.show();

        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.9f);
        dialogView.setScaleY(0.9f);
        dialogView.setTranslationY(40f);
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();
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

    // ==============================================
    // CLICK LISTENERS
    // ==============================================
    private void setupClickListeners() {
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                closeOverlay();
            });
        }

        if (closeSettingsButton != null) {
            closeSettingsButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                toggleSettingsPanel();
            });
        }

        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animatePlayButton(v);
                if (isPlaying) pauseScrolling();
                else startScrolling();
            });
        }

        if (playOverlayButton != null) {
            playOverlayButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                startOverlayService();
            });
        }

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                toggleSettingsPanel();
            });
        }

        if (settingsScrim != null) {
            settingsScrim.setOnClickListener(v -> toggleSettingsPanel());
        }

        if (fontSizeMinus != null) {
            fontSizeMinus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustFontSize(-2);
            });
        }

        if (fontSizePlus != null) {
            fontSizePlus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustFontSize(2);
            });
        }

        if (speedMinus != null) {
            speedMinus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustScrollSpeed(-0.1f);
            });
        }

        if (speedPlus != null) {
            speedPlus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustScrollSpeed(0.1f);
            });
        }

        if (spacingMinus != null) {
            spacingMinus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustLineSpacing(-2);
            });
        }

        if (spacingPlus != null) {
            spacingPlus.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                adjustLineSpacing(2);
            });
        }

        if (resetSettingsButton != null) {
            resetSettingsButton.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                resetSettings();
            });
        }
    }

    // ==============================================
    // START OVERLAY SERVICE
    // ==============================================
    private void startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
                return;
            }
        }

        String content = scriptContent != null ? scriptContent.getText().toString() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, "No script content to display", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, OverlayService.class);
        intent.putExtra("script_id", scriptId);
        intent.putExtra("script_content", content);
        intent.putExtra("script_title", scriptTitle);
        intent.putExtra("font_size", currentFontSize);
        intent.putExtra("scroll_speed", currentScrollSpeed);
        intent.putExtra("line_spacing", currentLineSpacing);
        intent.putExtra("opacity", currentOpacity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "Floating overlay started!", Toast.LENGTH_SHORT).show();
        finishWithAnimation();
    }

    private void closeOverlay() {
        finishWithAnimation();
    }

    // ==============================================
    // SCROLLING FUNCTIONALITY
    // ==============================================
    private void setupScrollListener() {
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                if (!isScrolling) {
                    currentScrollY = scrollView.getScrollY();
                    updateMaxScroll();
                    updateProgressLine();
                }
            });
        }
    }

    private void startScrolling() {
        if (scrollView == null) return;

        updateMaxScroll();
        int scrollDistance = maxScrollY - currentScrollY;
        if (scrollDistance <= 0) {
            scrollView.smoothScrollTo(0, 0);
            currentScrollY = 0;
            mainHandler.postDelayed(this::updateProgressLine, 500);
            return;
        }

        long duration = (long) (scrollDistance / (currentScrollSpeed * 0.05f));
        duration = Math.max(duration, 1000);

        isPlaying = true;
        updatePlayButton(true);

        if (anchorLine != null) {
            anchorLine.animate()
                    .alpha(0.8f)
                    .setDuration(200)
                    .start();
        }

        startScrollAnimation(duration);
    }

    private void startScrollAnimation(long duration) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        int startY = currentScrollY;
        int endY = maxScrollY;

        // Prevent division by zero
        if (endY - startY <= 0) {
            updatePlayButton(false);
            return;
        }

        scrollAnimator = ValueAnimator.ofInt(startY, endY);
        scrollAnimator.setDuration(duration);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            isScrolling = true;
            if (scrollView != null) {
                scrollView.scrollTo(0, value);
            }
            currentScrollY = value;

            // Safe progress calculation
            float progress = 0f;
            if (endY - startY > 0) {
                progress = (float) (value - startY) / (endY - startY);
                progress = Math.min(Math.max(progress, 0f), 1f);
            }
            if (progressLine != null) {
                progressLine.setScaleX(progress);
            }
        });
        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isScrolling = false;
                if (currentScrollY >= maxScrollY - 15) {
                    isPlaying = false;
                    updatePlayButton(false);
                    animateCompletion();
                }
            }
        });

        if (scrollView != null) {
            scrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        scrollAnimator.start();
    }

    private void pauseScrolling() {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        isScrolling = false;
        isPlaying = false;
        updatePlayButton(false);
        if (scrollView != null) {
            scrollView.setLayerType(View.LAYER_TYPE_NONE, null);
        }

        if (anchorLine != null) {
            anchorLine.animate()
                    .alpha(0.3f)
                    .setDuration(200)
                    .start();
        }
    }

    private void animateCompletion() {
        if (anchorLine != null) {
            ObjectAnimator glowAnim = ObjectAnimator.ofFloat(anchorLine, "alpha", 0.8f, 1f, 0.8f);
            glowAnim.setDuration(600);
            glowAnim.setRepeatCount(2);
            glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            glowAnim.start();

            anchorLine.animate()
                    .scaleX(1.05f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> anchorLine.animate()
                            .scaleX(1f)
                            .setDuration(300)
                            .start())
                    .start();
        }

        Toast.makeText(this, "End of script!", Toast.LENGTH_SHORT).show();
    }

    private void updatePlayButton(boolean playing) {
        if (playIcon != null) {
            playIcon.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
            playIcon.animate()
                    .rotationBy(playing ? 90f : -90f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    // ==============================================
    // SETTINGS PANEL - BOTTOM SHEET
    // ==============================================
    private void toggleSettingsPanel() {
        if (settingsPanel == null) return;

        if (settingsPanelVisible) {
            // Close panel
            if (settingsScrim != null) {
                settingsScrim.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> settingsScrim.setVisibility(View.GONE))
                        .start();
            }
            settingsPanel.animate()
                    .translationY(settingsPanel.getHeight())
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        settingsPanel.setVisibility(View.GONE);
                        settingsPanelVisible = false;
                    }).start();
        } else {
            // Open panel
            if (settingsScrim != null) {
                settingsScrim.setVisibility(View.VISIBLE);
                settingsScrim.setAlpha(0f);
                settingsScrim.animate()
                        .alpha(0.4f)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
            settingsPanel.setVisibility(View.VISIBLE);
            settingsPanel.setTranslationY(settingsPanel.getHeight());
            settingsPanel.animate()
                    .translationY(0f)
                    .setDuration(350)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
            settingsPanelVisible = true;
        }
    }

    // ==============================================
    // SETTINGS ADJUSTMENTS
    // ==============================================
    private void adjustFontSize(int delta) {
        int newSize = currentFontSize + delta;
        if (newSize >= 10 && newSize <= 36) {
            currentFontSize = newSize;
            if (fontSizeValue != null) {
                fontSizeValue.setText(String.valueOf(currentFontSize));
                animateSettingValue(fontSizeValue);
            }
            applyTextStyling();
            saveSettings();
            updateMaxScroll();
            updateProgressLine();
        }
    }

    private void adjustScrollSpeed(float delta) {
        float newSpeed = currentScrollSpeed + delta;
        if (newSpeed >= 0.1f && newSpeed <= 3.0f) {
            currentScrollSpeed = Math.round(newSpeed * 10.0f) / 10.0f;
            updateUIComponents();
            saveSettings();
            if (isPlaying) {
                pauseScrolling();
                startScrolling();
            }
        }
    }

    private void adjustLineSpacing(int delta) {
        int newSpacing = currentLineSpacing + delta;
        if (newSpacing >= 2 && newSpacing <= 24) {
            currentLineSpacing = newSpacing;
            if (spacingValue != null) {
                spacingValue.setText(String.valueOf(currentLineSpacing));
                animateSettingValue(spacingValue);
            }
            applyTextStyling();
            saveSettings();
            updateMaxScroll();
            updateProgressLine();
        }
    }

    private void resetSettings() {
        currentFontSize = 18;
        currentScrollSpeed = 1.0f;
        currentLineSpacing = 8;
        currentOpacity = 100;
        updateUIComponents();
        applyTextStyling();
        applyOpacity();
        saveSettings();
        updateMaxScroll();
        updateProgressLine();

        Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show();
    }

    private void animateSettingValue(TextView view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 0.9f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(400);
        set.setInterpolator(new OvershootInterpolator(1.5f));
        set.start();
    }

    private void updateMaxScroll() {
        if (scrollView != null && scrollView.getChildAt(0) != null) {
            maxScrollY = scrollView.getChildAt(0).getHeight() - scrollView.getHeight();
            if (maxScrollY < 0) maxScrollY = 0;
        }
    }

    // ==============================================
    // ANIMATIONS
    // ==============================================
    private void animateButton(View view) {
        if (view == null) return;
        view.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
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

    private void animatePlayButton(View view) {
        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(view, "rotation", 0f, 15f, -15f, 0f);
        rotateAnim.setDuration(400);
        rotateAnim.setInterpolator(new OvershootInterpolator(1.5f));
        rotateAnim.start();

        view.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(120)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator())
                        .start())
                .start();
    }

    private void performHapticFeedback(int intensity) {
        View view = getCurrentFocus();
        if (view == null) view = playButton;
        if (view == null) view = findViewById(android.R.id.content);
        if (view != null) {
            view.performHapticFeedback(intensity);
        }
    }

    private void startEntranceAnimations() {
        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            topBar.setAlpha(0f);
            topBar.setTranslationY(-80f);
            topBar.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        if (scriptContent != null) {
            scriptContent.setAlpha(0f);
            scriptContent.setTranslationY(30f);
            scriptContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (anchorLine != null) {
            anchorLine.setAlpha(0f);
            anchorLine.animate()
                    .alpha(0.8f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (playButton != null) {
            playButton.setScaleX(0f);
            playButton.setScaleY(0f);
            playButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(450)
                    .setStartDelay(150)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    private void finishWithAnimation() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(280)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        finish();
                        overridePendingTransition(0, android.R.anim.fade_out);
                    })
                    .start();
        } else {
            finish();
        }
    }

    // ==============================================
    // OVERRIDE METHODS
    // ==============================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasOverlayPermission = Settings.canDrawOverlays(this);
                if (hasOverlayPermission) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            hasNotificationPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (hasNotificationPermission) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (settingsPanelVisible) {
            toggleSettingsPanel();
        } else if (isPlaying) {
            pauseScrolling();
        } else {
            finishWithAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}