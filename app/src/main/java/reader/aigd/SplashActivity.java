package reader.aigd;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 3200;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // UI Elements
    private ImageView appIcon;
    private TextView appName;
    private TextView tagline;
    private TextView footerText;
    private View decorativeLine;
    private ProgressBar loadingProgress;

    private ValueAnimator progressAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set status bar colors
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        // Status bar icons - dark for white background
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        initializeViews();
        startPremiumAnimations();
        startProgressAnimation();

        // ✅ Directly navigate to Home after splash
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToHome();
            }
        }, SPLASH_DURATION);
    }

    private void initializeViews() {
        appIcon = findViewById(R.id.appIcon);
        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);
        footerText = findViewById(R.id.footerText);
        decorativeLine = findViewById(R.id.decorativeLine);
        loadingProgress = findViewById(R.id.circularProgress);

        // Set initial states
        if (appIcon != null) {
            appIcon.setAlpha(0f);
            appIcon.setScaleX(0.3f);
            appIcon.setScaleY(0.3f);
        }
        if (appName != null) {
            appName.setAlpha(0f);
            appName.setTranslationY(40f);
        }
        if (tagline != null) {
            tagline.setAlpha(0f);
            tagline.setTranslationY(20f);
        }
        if (decorativeLine != null) {
            decorativeLine.setScaleX(0f);
        }
        if (footerText != null) {
            footerText.setAlpha(0f);
        }
        if (loadingProgress != null) {
            loadingProgress.setProgress(0);
        }
    }

    private void startPremiumAnimations() {
        // ==============================================
        // 1. APP ICON ENTRY ANIMATION
        // ==============================================
        if (appIcon != null) {
            appIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();
        }

        // ==============================================
        // 2. APP NAME ENTRY
        // ==============================================
        if (appName != null) {
            appName.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1f))
                    .setStartDelay(300)
                    .start();
        }

        // ==============================================
        // 3. DECORATIVE LINE REVEAL
        // ==============================================
        if (decorativeLine != null) {
            decorativeLine.animate()
                    .scaleX(1f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .setStartDelay(600)
                    .start();
        }

        // ==============================================
        // 4. TAGLINE ENTRY
        // ==============================================
        if (tagline != null) {
            tagline.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .setStartDelay(750)
                    .start();
        }

        // ==============================================
        // 5. FOOTER FADE-IN
        // ==============================================
        if (footerText != null) {
            footerText.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .setStartDelay(1100)
                    .start();
        }
    }

    private void startProgressAnimation() {
        // Progress animation
        progressAnim = ValueAnimator.ofInt(0, 100);
        progressAnim.setDuration(SPLASH_DURATION - 500);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int progress = (int) animation.getAnimatedValue();
                if (loadingProgress != null) {
                    loadingProgress.setProgress(progress);
                }
            }
        });
        progressAnim.setStartDelay(500);
        progressAnim.start();
    }

    // ==============================================
    // NAVIGATE TO HOME
    // ==============================================
    private void navigateToHome() {
        // Cancel progress animation
        if (progressAnim != null && progressAnim.isRunning()) {
            progressAnim.cancel();
        }

        // Exit animation
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.animate()
                    .alpha(0f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            // ✅ Directly go to Home - NO ADS!
                            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                            startActivity(intent);
                            overridePendingTransition(0, android.R.anim.fade_out);
                            finish();
                        }
                    })
                    .start();
        } else {
            // ✅ Directly go to Home - NO ADS!
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (progressAnim != null && progressAnim.isRunning()) {
            progressAnim.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}