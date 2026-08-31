package reader.aigd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.widget.NestedScrollView;

import java.util.List;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "reader_overlay_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREFS_OVERLAY = "overlay_settings";
    private final Handler handler = new Handler();
    // Window Manager Components
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams layoutParams;
    // XML UI Components
    private TextView scriptTextView;
    private TextView scriptTitleText;
    private View anchorLine;
    private View statusDot;
    private LinearLayout playButton;
    private LinearLayout closeButton;
    private LinearLayout dragHandle;
    private LinearLayout resizeHandle;
    private NestedScrollView overlayScrollView;
    private ImageView playIcon;
    private View topBar;
    // Speed Controller Views
    private LinearLayout speedMinus;
    private LinearLayout speedPlus;
    private SeekBar speedSeekBar;
    private TextView speedValueText;
    // Data Management
    private DataManager dataManager;
    private String scriptContent = "";
    private long currentScriptId = -1;
    private String scriptTitle = "Reader";
    // Teleprompter Scrolling States
    private boolean isPlaying = false;
    private ValueAnimator scrollAnimator;
    private int currentScrollY = 0;
    private int maxScrollY = 0;
    // Configurable Settings
    private int currentFontSize = 18;
    private float currentScrollSpeed = 0.10f;  // ✅ ULTRA SLOW default
    private int currentLineSpacing = 8;
    private SharedPreferences settingsPrefs;
    // Boundary Trackers
    private int screenWidth;
    private int screenHeight;
    private boolean isDragging = false;
    private boolean isResizing = false;

    // Status Dot Animation
    private ValueAnimator pulseAnimator;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        dataManager = DataManager.getInstance(this);
        settingsPrefs = getSharedPreferences(PREFS_OVERLAY, MODE_PRIVATE);
        loadSettings();
        loadLatestScript();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getScreenSize();

        createOverlayView();
        startStatusDotPulse();
    }

    private void getScreenSize() {
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.view.WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
                android.graphics.Rect bounds = windowMetrics.getBounds();
                screenWidth = bounds.width();
                screenHeight = bounds.height();
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                screenWidth = displayMetrics.widthPixels;
                screenHeight = displayMetrics.heightPixels;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reader Floating",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Floating teleprompter overlay for recording videos");
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent closeIntent = new Intent(this, OverlayService.class);
        closeIntent.setAction("CLOSE_OVERLAY");
        PendingIntent closePendingIntent = PendingIntent.getService(
                this, 1, closeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reader Floating")
                .setContentText("Floating overlay active - Smooth scrolling ready")
                .setSmallIcon(R.drawable.ic_logo)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_close, "Close", closePendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startMyForeground() {
        Notification notification = getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void loadSettings() {
        currentFontSize = settingsPrefs.getInt("font_size", 18);
        currentScrollSpeed = settingsPrefs.getFloat("scroll_speed", 0.10f);
        currentLineSpacing = settingsPrefs.getInt("line_spacing", 8);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = settingsPrefs.edit();
        editor.putInt("font_size", currentFontSize);
        editor.putFloat("scroll_speed", currentScrollSpeed);
        editor.putInt("line_spacing", currentLineSpacing);
        editor.apply();
    }

    private void loadSettingsFromIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("font_size")) {
                currentFontSize = intent.getIntExtra("font_size", 18);
            }
            if (intent.hasExtra("scroll_speed")) {
                currentScrollSpeed = intent.getFloatExtra("scroll_speed", 0.10f);
            }
            if (intent.hasExtra("line_spacing")) {
                currentLineSpacing = intent.getIntExtra("line_spacing", 8);
            }
            if (intent.hasExtra("script_title")) {
                scriptTitle = intent.getStringExtra("script_title");
            }

            saveSettings();
            applyTextStyling();
            updateSpeedUI();
            updateTitle();
        }
    }

    private void updateTitle() {
        if (scriptTitleText != null && scriptTitle != null) {
            scriptTitleText.setText(scriptTitle.toUpperCase());
        }
    }

    private void loadLatestScript() {
        List<Script> allScripts = dataManager.getAllScripts();

        if (allScripts != null && !allScripts.isEmpty()) {
            Script currentScript = allScripts.get(0);
            if (currentScript != null) {
                scriptContent = currentScript.getContent();
                currentScriptId = currentScript.getId();
                scriptTitle = currentScript.getTitle();
            }
        }

        if (scriptContent == null || scriptContent.isEmpty()) {
            scriptContent = "Welcome to Reader Floating Teleprompter!\n\n" +
                    "Tap Play to start scrolling!\n" +
                    "Drag the handle to move this window\n" +
                    "Resize from the bottom-right corner\n\n" +
                    "Adjust speed with the seek bar below";
        }
    }

    private void createOverlayView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_service, null);

        // Core Components Mapping
        scriptTextView = overlayView.findViewById(R.id.scriptTextView);
        anchorLine = overlayView.findViewById(R.id.anchorLine);
        statusDot = overlayView.findViewById(R.id.statusDot);
        playButton = overlayView.findViewById(R.id.playButton);
        closeButton = overlayView.findViewById(R.id.closeButton);
        dragHandle = overlayView.findViewById(R.id.dragHandle);
        resizeHandle = overlayView.findViewById(R.id.resizeHandle);
        overlayScrollView = overlayView.findViewById(R.id.overlayScrollView);
        playIcon = overlayView.findViewById(R.id.playIcon);
        topBar = overlayView.findViewById(R.id.topBar);

        // Speed Control Component Mapping
        speedMinus = overlayView.findViewById(R.id.speedMinus);
        speedPlus = overlayView.findViewById(R.id.speedPlus);
        speedSeekBar = overlayView.findViewById(R.id.speedSeekBar);
        speedValueText = overlayView.findViewById(R.id.speedValueText);

        scriptTextView.setText(scriptContent);
        updateTitle();

        applyTextStyling();
        updateSpeedUI();
        setupClickListeners();
        setupSpeedSeekBarListener();
        setupDragListener();
        setupResizeListener();

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams = new WindowManager.LayoutParams(
                dpToPx(380),
                dpToPx(500),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = (screenWidth - dpToPx(380)) / 2;
        layoutParams.y = 80;

        windowManager.addView(overlayView, layoutParams);

        // Entrance Dynamic Animation
        overlayView.setAlpha(0f);
        overlayView.setScaleX(0.85f);
        overlayView.setScaleY(0.85f);
        overlayView.setTranslationY(-50f);
        overlayView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void startStatusDotPulse() {
        if (statusDot == null) return;

        pulseAnimator = ValueAnimator.ofFloat(0.6f, 1f, 0.6f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            if (statusDot != null) {
                statusDot.setAlpha(alpha);
                statusDot.setScaleX(0.8f + (alpha * 0.2f));
                statusDot.setScaleY(0.8f + (alpha * 0.2f));
            }
        });
        pulseAnimator.start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void applyTextStyling() {
        if (scriptTextView != null) {
            scriptTextView.setTextSize(currentFontSize);
            scriptTextView.setLineSpacing(dpToPx(currentLineSpacing), 1.0f);
        }
    }

    private void updateSpeedUI() {
        if (speedSeekBar != null) {
            // ✅ Map speed range: 0.01 to 0.50 (ultra slow max)
            int progress = (int) (currentScrollSpeed * 100);
            if (progress < 1) progress = 1;
            if (progress > 50) progress = 50;
            speedSeekBar.setProgress(progress);
        }
        if (speedValueText != null) {
            speedValueText.setText(String.format("%.2fx", currentScrollSpeed));
        }
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> {
            animateButton(v);
            handler.postDelayed(this::stopSelf, 200);
        });

        playButton.setOnClickListener(v -> {
            animateButton(v);
            if (isPlaying) pauseScrolling();
            else startScrolling();
        });

        speedMinus.setOnClickListener(v -> {
            animateButton(v);
            // ✅ Ultra slow decrement: 0.005
            float newSpeed = currentScrollSpeed - 0.005f;
            if (newSpeed >= 0.005f) {
                adjustSpeedLive(newSpeed);
            } else {
                Toast.makeText(this, "Minimum speed reached", Toast.LENGTH_SHORT).show();
            }
        });

        speedPlus.setOnClickListener(v -> {
            animateButton(v);
            // ✅ Ultra slow increment: 0.005
            float newSpeed = currentScrollSpeed + 0.005f;
            if (newSpeed <= 0.50f) {
                adjustSpeedLive(newSpeed);
            } else {
                Toast.makeText(this, "Maximum speed reached", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpeedSeekBarListener() {
        if (speedSeekBar == null) return;

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // ✅ Map 1-50 to 0.005-0.50
                    float calculatedSpeed = progress / 100.0f;
                    if (calculatedSpeed < 0.005f) calculatedSpeed = 0.005f;
                    if (calculatedSpeed > 0.50f) calculatedSpeed = 0.50f;
                    currentScrollSpeed = Math.round(calculatedSpeed * 100) / 100.0f;

                    if (speedValueText != null) {
                        speedValueText.setText(String.format("%.2fx", currentScrollSpeed));
                    }

                    if (isPlaying && scrollAnimator != null && scrollAnimator.isRunning()) {
                        int currentPos = (int) scrollAnimator.getAnimatedValue();
                        scrollAnimator.cancel();
                        currentScrollY = currentPos;
                        resumeLiveScrolling();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });
    }

    private void adjustSpeedLive(float newSpeed) {
        currentScrollSpeed = Math.round(newSpeed * 100) / 100.0f;
        updateSpeedUI();
        saveSettings();

        if (isPlaying && scrollAnimator != null && scrollAnimator.isRunning()) {
            int currentPos = (int) scrollAnimator.getAnimatedValue();
            scrollAnimator.cancel();
            currentScrollY = currentPos;
            resumeLiveScrolling();
        }
    }

    private void animateButton(View view) {
        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(new OvershootInterpolator())
                        .start())
                .start();
    }

    // ==================== SMART DRAG CONTROLLER ====================
    private void setupDragListener() {
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getScreenSize();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = true;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);

                        layoutParams.x = initialX + deltaX;
                        layoutParams.y = initialY + deltaY;

                        // Boundary constraints
                        layoutParams.x = Math.max(0, Math.min(layoutParams.x, screenWidth - layoutParams.width));
                        layoutParams.y = Math.max(0, Math.min(layoutParams.y, screenHeight - layoutParams.height));

                        windowManager.updateViewLayout(overlayView, layoutParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });
    }

    // ==================== SMART RESIZE CONTROLLER ====================
    private void setupResizeListener() {
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialWidth;
            private int initialHeight;
            private float initialTouchX;
            private float initialTouchY;
            private boolean wasScrollingBeforeResize = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getScreenSize();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialWidth = layoutParams.width;
                        initialHeight = layoutParams.height;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isResizing = true;

                        if (isPlaying) {
                            wasScrollingBeforeResize = true;
                            pauseScrolling();
                        } else {
                            wasScrollingBeforeResize = false;
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);

                        int newWidth = initialWidth + deltaX;
                        int newHeight = initialHeight + deltaY;

                        int minW = dpToPx(280);
                        int minH = dpToPx(360);

                        layoutParams.width = Math.max(minW, Math.min(newWidth, screenWidth));
                        layoutParams.height = Math.max(minH, Math.min(newHeight, screenHeight));

                        windowManager.updateViewLayout(overlayView, layoutParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isResizing = false;
                        if (wasScrollingBeforeResize) {
                            handler.postDelayed(() -> startScrolling(), 300);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // ==================== ULTRA SLOW SCROLL ENGINE ====================
    private void startScrolling() {
        if (overlayScrollView == null) return;

        overlayScrollView.post(() -> {
            if (overlayScrollView.getChildAt(0) != null) {
                maxScrollY = overlayScrollView.getChildAt(0).getHeight() - overlayScrollView.getHeight();

                if (currentScrollY >= maxScrollY) {
                    currentScrollY = 0;
                    overlayScrollView.scrollTo(0, 0);
                }

                int scrollDistance = maxScrollY - currentScrollY;

                if (scrollDistance <= 0) {
                    resetScrolling();
                    return;
                }

                // ✅ ULTRA SLOW: divide by a MUCH smaller number
                // At speed 0.10, a 1000px scroll takes ~333 seconds (~5.5 minutes)
                // At speed 0.01, a 1000px scroll takes ~3333 seconds (~55 minutes)
                long duration = (long) (scrollDistance / (currentScrollSpeed * 0.003f));

                // Cap duration to prevent issues
                if (duration > 1800000) duration = 1800000; // 30 minutes max
                if (duration < 2000) duration = 2000; // Minimum 2 seconds

                isPlaying = true;

                if (playIcon != null) {
                    playIcon.setImageResource(R.drawable.ic_pause);
                }

                if (statusDot != null) {
                    statusDot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    getColor(android.R.color.holo_green_light)
                            )
                    );
                }

                startScrollAnimation(duration);
            }
        });
    }

    private void resumeLiveScrolling() {
        if (overlayScrollView == null || overlayScrollView.getChildAt(0) == null) return;

        maxScrollY = overlayScrollView.getChildAt(0).getHeight() - overlayScrollView.getHeight();

        if (currentScrollY >= maxScrollY) {
            currentScrollY = 0;
            overlayScrollView.scrollTo(0, 0);
        }

        int scrollDistance = maxScrollY - currentScrollY;

        if (scrollDistance <= 0) {
            isPlaying = false;
            if (playIcon != null) playIcon.setImageResource(R.drawable.ic_play);
            return;
        }

        // ✅ Same ultra slow calculation
        long duration = (long) (scrollDistance / (currentScrollSpeed * 0.003f));
        if (duration > 1800000) duration = 1800000;
        if (duration < 2000) duration = 2000;

        startScrollAnimation(duration);
    }

    private void startScrollAnimation(long duration) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        scrollAnimator = ValueAnimator.ofInt(currentScrollY, maxScrollY);
        scrollAnimator.setDuration(duration);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            if (overlayScrollView != null) {
                overlayScrollView.scrollTo(0, value);
            }
            currentScrollY = value;

            if (anchorLine != null && maxScrollY > 0) {
                float progress = (float) currentScrollY / maxScrollY;
                anchorLine.setAlpha(0.8f - (progress * 0.5f));
            }
        });
        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (currentScrollY >= maxScrollY - 5) {
                    isPlaying = false;
                    if (playIcon != null) {
                        playIcon.setImageResource(R.drawable.ic_play);
                    }
                    if (statusDot != null) {
                        statusDot.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        getColor(R.color.f)
                                )
                        );
                    }
                    if (anchorLine != null) {
                        anchorLine.animate()
                                .alpha(1f)
                                .scaleX(1.1f)
                                .setDuration(300)
                                .setInterpolator(new OvershootInterpolator())
                                .withEndAction(() -> anchorLine.animate()
                                        .alpha(0.6f)
                                        .scaleX(1f)
                                        .setDuration(300)
                                        .start())
                                .start();
                    }
                }
            }
        });
        scrollAnimator.start();
    }

    private void pauseScrolling() {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        isPlaying = false;
        if (playIcon != null) {
            playIcon.setImageResource(R.drawable.ic_play);
        }
        if (statusDot != null) {
            statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getColor(R.color.f)
                    )
            );
        }
        if (anchorLine != null) {
            anchorLine.animate()
                    .alpha(0.6f)
                    .setDuration(200)
                    .start();
        }
    }

    private void resetScrolling() {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        isPlaying = false;
        currentScrollY = 0;

        if (playIcon != null) {
            playIcon.setImageResource(R.drawable.ic_play);
        }
        if (overlayScrollView != null) {
            overlayScrollView.scrollTo(0, 0);
        }
        if (anchorLine != null) {
            anchorLine.setAlpha(0.8f);
        }
        if (statusDot != null) {
            statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getColor(R.color.f)
                    )
            );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMyForeground();

        if (intent != null) {
            if ("CLOSE_OVERLAY".equals(intent.getAction())) {
                stopSelf();
            } else {
                loadSettingsFromIntent(intent);

                if (intent.hasExtra("script_content")) {
                    scriptContent = intent.getStringExtra("script_content");
                    if (scriptContent == null || scriptContent.isEmpty()) {
                        scriptContent = "Welcome to Reader Floating Teleprompter!";
                    }
                    if (scriptTextView != null) {
                        scriptTextView.setText(scriptContent);
                        resetScrolling();
                        applyTextStyling();
                    }
                }

                if (intent.hasExtra("script_id")) {
                    currentScriptId = intent.getLongExtra("script_id", -1);
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);

        if (overlayView != null && windowManager != null) {
            overlayView.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        try {
                            windowManager.removeView(overlayView);
                            overlayView = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .start();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}