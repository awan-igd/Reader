package reader.aigd;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

public class AboutActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    // ==============================================
    // UI COMPONENTS
    // ==============================================
    private LinearLayout backButton;
    private LinearLayout websiteButton;
    private LinearLayout footerWebsiteButton;
    private LinearLayout emailButton;
    private LinearLayout phoneButton;
    private TextView appNameText;
    private TextView companyNameText;
    // ==============================================
    // START.IO AD ELEMENTS
    // ==============================================
    private Banner startAppBanner;
    private StartAppAd startAppAd;
    private boolean isExiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // ==============================================
        // INITIALIZE START.IO SDK
        // ==============================================
        initializeStartIoAds();

        // Set status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.f));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.f));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        loadStartIoBannerAd();
        setupClickListeners();
        startEntranceAnimations();
    }

    // ==============================================
    // START.IO INITIALIZATION
    // ==============================================
    private void initializeStartIoAds() {
        StartAppSDK.init(this, "206823693", false);
        startAppAd = new StartAppAd(this);
    }

    private void loadStartIoBannerAd() {
        startAppBanner = findViewById(R.id.startAppBanner);
        if (startAppBanner != null) {
            startAppBanner.loadAd();
            handler.postDelayed(() -> {
                if (startAppBanner != null) {
                    startAppBanner.setVisibility(View.VISIBLE);
                    startAppBanner.setAlpha(0f);
                    startAppBanner.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }, 400);
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        websiteButton = findViewById(R.id.websiteButton);
        footerWebsiteButton = findViewById(R.id.footerWebsiteButton);
        emailButton = findViewById(R.id.emailButton);
        phoneButton = findViewById(R.id.phoneButton);
        appNameText = findViewById(R.id.appNameText);
        companyNameText = findViewById(R.id.companyNameText);
    }

    // ==============================================
    // CLICK LISTENERS
    // ==============================================
    private void setupClickListeners() {
        // Back button with premium animation
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                animateBackButton(v);
                handler.postDelayed(this::finishWithAnimation, 200);
            });
        }

        // Website button (both top and footer)
        View.OnClickListener websiteClickListener = v -> {
            animateButton(v);
            performHapticFeedback(v);
            openUrl("https://awan-igd.web.app/");
        };

        if (websiteButton != null) {
            websiteButton.setOnClickListener(websiteClickListener);
        }

        if (footerWebsiteButton != null) {
            footerWebsiteButton.setOnClickListener(websiteClickListener);
        }

        // Email button
        if (emailButton != null) {
            emailButton.setOnClickListener(v -> {
                animateButton(v);
                performHapticFeedback(v);
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:awanigd@gmail.com"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    showToast("No email app available");
                }
            });
        }

        // Phone button
        if (phoneButton != null) {
            phoneButton.setOnClickListener(v -> {
                animateButton(v);
                performHapticFeedback(v);
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:+923499689400"));
                startActivity(intent);
            });
        }
    }

    // ==============================================
    // HELPER METHODS
    // ==============================================
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            showToast("No browser available");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performHapticFeedback(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
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

    private void animateBackButton(View view) {
        if (view == null) return;

        // Premium back animation with rotation
        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(view, "rotation", 0f, -15f, 15f, 0f);
        rotateAnim.setDuration(400);
        rotateAnim.setInterpolator(new OvershootInterpolator(1.5f));
        rotateAnim.start();

        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(150)
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
        // Header slide down with spring
        View headerContainer = findViewById(R.id.headerContainer);
        if (headerContainer != null) {
            headerContainer.setAlpha(0f);
            headerContainer.setTranslationY(-80f);
            headerContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        // App name fade in
        if (appNameText != null) {
            appNameText.setAlpha(0f);
            appNameText.setScaleX(0.8f);
            appNameText.setScaleY(0.8f);
            appNameText.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setStartDelay(200)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }

        // Company name fade in
        if (companyNameText != null) {
            companyNameText.setAlpha(0f);
            companyNameText.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(350)
                    .start();
        }

        // ScrollView content fade in
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setAlpha(0f);
            scrollView.setTranslationY(30f);
            scrollView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Back button bounce
        if (backButton != null) {
            backButton.setScaleX(0f);
            backButton.setScaleY(0f);
            backButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setStartDelay(150)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        // Animate feature items with staggered delay
        animateFeatureItems();
    }

    private void animateFeatureItems() {
        // Find all feature item containers and animate them
        LinearLayout featureContainer = findViewById(R.id.featureContainer);
        if (featureContainer != null) {
            int childCount = featureContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = featureContainer.getChildAt(i);
                if (child != null) {
                    child.setAlpha(0f);
                    child.setTranslationX(-20f);
                    final int position = i;
                    child.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(300)
                            .setStartDelay(400 + (position * 80L))
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        }
    }

    // ==============================================
    // FINISH & EXIT
    // ==============================================
    private void finishWithAnimation() {
        if (isExiting) return;
        isExiting = true;

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
    public void onBackPressed() {
        if (!isExiting) {
            finishWithAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}