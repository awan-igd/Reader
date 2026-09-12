package reader.aigd;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;

import reader.aigd.activity.HomeActivity;
import reader.aigd.model.DataManager;
import reader.aigd.model.Script;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "reader_overlay_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREFS_OVERLAY = "overlay_settings";

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams layoutParams;

    private TextView scriptTextView;
    private View anchorLine;
    private View statusDot;
    private MaterialCardView playButton;
    private MaterialCardView closeButton;
    private MaterialCardView dragHandle;
    private MaterialCardView resizeHandle;
    private NestedScrollView overlayScrollView;
    private ImageView playIcon;
    private View topBar;
    private MaterialCardView speedMinus;
    private MaterialCardView speedPlus;
    private SeekBar speedSeekBar;
    private TextView speedValueText;

    private DataManager dataManager;
    private String scriptContent = "";
    private long currentScriptId = -1;

    private boolean isPlaying = false;
    private ValueAnimator scrollAnimator;
    private int currentScrollY = 0;
    private int maxScrollY = 0;

    private int currentFontSize = 20;
    private float currentScrollSpeed = 0.10f;
    private int currentLineSpacing = 12;

    private SharedPreferences settingsPrefs;
    private final Handler handler = new Handler();
    private int screenWidth;
    private int screenHeight;
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
        if (windowManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
            screenWidth = bounds.width();
            screenHeight = bounds.height();
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Reader Floating", NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null, null);
            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) m.createNotificationChannel(ch);
        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent closeIntent = new Intent(this, OverlayService.class);
        closeIntent.setAction("CLOSE_OVERLAY");
        PendingIntent cpi = PendingIntent.getService(this, 1, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reader Floating").setContentText("Overlay active")
                .setSmallIcon(R.drawable.ic_reader).setContentIntent(pi).addAction(R.drawable.ic_close, "Close", cpi).setOngoing(true).build();
    }

    private void startMyForeground() {
        Notification n = getNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else startForeground(NOTIFICATION_ID, n);
    }

    private void loadSettings() {
        currentFontSize = settingsPrefs.getInt("font_size", 20);
        currentScrollSpeed = settingsPrefs.getFloat("scroll_speed", 0.10f);
        currentLineSpacing = settingsPrefs.getInt("line_spacing", 12);
    }

    private void saveSettings() {
        settingsPrefs.edit().putInt("font_size", currentFontSize).putFloat("scroll_speed", currentScrollSpeed).putInt("line_spacing", currentLineSpacing).apply();
    }

    private void loadLatestScript() {
        try {
            var list = dataManager.getAllScripts();
            if (list != null && !list.isEmpty()) {
                Script s = list.get(0);
                scriptContent = s.getContent();
                currentScriptId = s.getId();
            }
        } catch (Exception ignored) {
        }
        if (scriptContent == null || scriptContent.isEmpty())
            scriptContent = "Welcome to Reader Floating!\n\nTap Play to start scrolling.\nThis is test script line 1.\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7\nLine 8\nLine 9\nLine 10\nKeep reading...";
    }

    private void createOverlayView() {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_Reader);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.overlay_service, null);

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
        speedMinus = overlayView.findViewById(R.id.speedMinus);
        speedPlus = overlayView.findViewById(R.id.speedPlus);
        speedSeekBar = overlayView.findViewById(R.id.speedSeekBar);
        speedValueText = overlayView.findViewById(R.id.speedValueText);

        if (scriptTextView != null) scriptTextView.setText(scriptContent);
        applyTextStyling();
        updateSpeedUI();
        setupClickListeners();
        setupSpeedSeekBar();
        setupDrag();
        setupResize();

        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams = new WindowManager.LayoutParams(dp(360), dp(520), flag, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = (screenWidth - dp(360)) / 2;
        layoutParams.y = 80;

        windowManager.addView(overlayView, layoutParams);
        overlayView.setAlpha(0f);
        overlayView.setScaleX(0.9f);
        overlayView.setScaleY(0.9f);
        overlayView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    private void startStatusDotPulse() {
        if (statusDot == null) return;
        pulseAnimator = ValueAnimator.ofFloat(0.4f, 1f, 0.4f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(a -> {
            if (statusDot != null) statusDot.setAlpha((float) a.getAnimatedValue());
        });
        pulseAnimator.start();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void applyTextStyling() {
        if (scriptTextView != null) {
            scriptTextView.setTextSize(currentFontSize);
            scriptTextView.setLineSpacing(dp(currentLineSpacing), 1f);
        }
    }

    private void updateSpeedUI() {
        if (speedSeekBar != null) {
            int prog = Math.round(currentScrollSpeed * 100f);
            prog = Math.max(1, Math.min(prog, 50));
            speedSeekBar.setProgress(prog);
        }
        if (speedValueText != null)
            speedValueText.setText(String.format("%.2fx", currentScrollSpeed));
    }

    private void setupClickListeners() {
        if (closeButton != null) closeButton.setOnClickListener(v -> {
            anim(v);
            handler.postDelayed(this::stopSelf, 120);
        });
        if (playButton != null) playButton.setOnClickListener(v -> {
            anim(v);
            if (isPlaying) pauseScrolling();
            else startScrolling();
        });
        if (speedMinus != null) speedMinus.setOnClickListener(v -> {
            anim(v);
            float ns = currentScrollSpeed - 0.01f;
            if (ns < 0.01f) ns = 0.01f;
            adjustSpeed(ns);
        });
        if (speedPlus != null) speedPlus.setOnClickListener(v -> {
            anim(v);
            float ns = currentScrollSpeed + 0.01f;
            if (ns > 0.50f) ns = 0.50f;
            adjustSpeed(ns);
        });
    }

    private void setupSpeedSeekBar() {
        if (speedSeekBar == null) return;
        speedSeekBar.setMax(50);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                if (p < 1) p = 1;
                currentScrollSpeed = Math.round((p / 100f) * 100f) / 100f;
                if (speedValueText != null)
                    speedValueText.setText(String.format("%.2fx", currentScrollSpeed));
                if (isPlaying) {
                    int pos = currentScrollY;
                    if (scrollAnimator != null && scrollAnimator.isRunning())
                        pos = (int) scrollAnimator.getAnimatedValue();
                    if (scrollAnimator != null) scrollAnimator.cancel();
                    currentScrollY = pos;
                    resumeScrolling();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                saveSettings();
            }
        });
    }

    private void adjustSpeed(float ns) {
        currentScrollSpeed = Math.round(ns * 100f) / 100f;
        updateSpeedUI();
        saveSettings();
        if (isPlaying) {
            int pos = currentScrollY;
            if (scrollAnimator != null && scrollAnimator.isRunning())
                pos = (int) scrollAnimator.getAnimatedValue();
            if (scrollAnimator != null) scrollAnimator.cancel();
            currentScrollY = pos;
            resumeScrolling();
        }
    }

    private void anim(View v) {
        if (v == null) return;
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new OvershootInterpolator()).start()).start();
    }

    private void setupDrag() {
        View dv = dragHandle != null ? dragHandle : topBar;
        if (dv == null) return;
        dv.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy;
            float itx, ity;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                getScreenSize();
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ix = layoutParams.x;
                        iy = layoutParams.y;
                        itx = e.getRawX();
                        ity = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        layoutParams.x = Math.max(0, Math.min(ix + (int) (e.getRawX() - itx), screenWidth - layoutParams.width));
                        layoutParams.y = Math.max(0, Math.min(iy + (int) (e.getRawY() - ity), screenHeight - layoutParams.height));
                        windowManager.updateViewLayout(overlayView, layoutParams);
                        return true;
                }
                return false;
            }
        });
    }

    private void setupResize() {
        if (resizeHandle == null) return;
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            int iw, ih;
            float ix, iy;
            boolean wp;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        iw = layoutParams.width;
                        ih = layoutParams.height;
                        ix = e.getRawX();
                        iy = e.getRawY();
                        wp = isPlaying;
                        if (wp) pauseScrolling();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        layoutParams.width = Math.max(dp(300), Math.min(iw + (int) (e.getRawX() - ix), screenWidth));
                        layoutParams.height = Math.max(dp(380), Math.min(ih + (int) (e.getRawY() - iy), screenHeight));
                        windowManager.updateViewLayout(overlayView, layoutParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (wp) handler.postDelayed(() -> startScrolling(), 150);
                        return true;
                }
                return false;
            }
        });
    }

    // ===== WORKING SCROLL ENGINE =====
    private void startScrolling() {
        if (overlayScrollView == null || scriptTextView == null) return;
        overlayScrollView.post(() -> {
            int contentH = scriptTextView.getHeight();
            int scrollH = overlayScrollView.getHeight();
            if (contentH == 0) {
                handler.postDelayed(this::startScrolling, 150);
                return;
            }
            maxScrollY = contentH - scrollH + dp(100);
            if (maxScrollY <= 0) maxScrollY = contentH;

            if (currentScrollY >= maxScrollY - 20) {
                currentScrollY = 0;
                overlayScrollView.scrollTo(0, 0);
            }

            int dist = maxScrollY - currentScrollY;
            if (dist < 30) {
                reset();
                return;
            }

            // 0.10x = slow, 0.50x = 5x fast
            long duration = (long) (dist * 20f / currentScrollSpeed);
            duration = Math.max(4000, Math.min(duration, 600000));

            isPlaying = true;
            if (playIcon != null) playIcon.setImageResource(R.drawable.ic_pause);
            startAnim(duration);
        });
    }

    private void resumeScrolling() {
        if (overlayScrollView == null) return;
        int dist = maxScrollY - currentScrollY;
        if (dist < 30) {
            reset();
            return;
        }
        long duration = (long) (dist * 20f / currentScrollSpeed);
        duration = Math.max(4000, Math.min(duration, 600000));
        startAnim(duration);
    }

    private void startAnim(long dur) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) scrollAnimator.cancel();
        scrollAnimator = ValueAnimator.ofInt(currentScrollY, maxScrollY);
        scrollAnimator.setDuration(dur);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            int val = (int) a.getAnimatedValue();
            if (overlayScrollView != null) overlayScrollView.scrollTo(0, val);
            currentScrollY = val;
        });
        scrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (currentScrollY >= maxScrollY - 15) {
                    isPlaying = false;
                    if (playIcon != null) playIcon.setImageResource(R.drawable.ic_play);
                }
            }
        });
        scrollAnimator.start();
    }

    private void pauseScrolling() {
        if (scrollAnimator != null) scrollAnimator.cancel();
        isPlaying = false;
        if (playIcon != null) playIcon.setImageResource(R.drawable.ic_play);
    }

    private void reset() {
        if (scrollAnimator != null) scrollAnimator.cancel();
        isPlaying = false;
        currentScrollY = 0;
        if (playIcon != null) playIcon.setImageResource(R.drawable.ic_play);
        if (overlayScrollView != null) overlayScrollView.scrollTo(0, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMyForeground();
        if (intent != null) {
            if ("CLOSE_OVERLAY".equals(intent.getAction())) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (intent.hasExtra("font_size")) currentFontSize = intent.getIntExtra("font_size", 20);
            if (intent.hasExtra("scroll_speed"))
                currentScrollSpeed = intent.getFloatExtra("scroll_speed", 0.10f);
            if (intent.hasExtra("line_spacing"))
                currentLineSpacing = intent.getIntExtra("line_spacing", 12);
            if (intent.hasExtra("script_content")) {
                scriptContent = intent.getStringExtra("script_content");
                if (scriptTextView != null) {
                    scriptTextView.setText(scriptContent);
                    reset();
                    applyTextStyling();
                }
            }
            saveSettings();
            applyTextStyling();
            updateSpeedUI();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scrollAnimator != null) scrollAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
        handler.removeCallbacksAndMessages(null);
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}