package reader.aigd;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 3200;
    private Handler handler;
    private AnimatorSet mainAnimatorSet;
    private AnimatorSet pulseAnimatorSet;

    private CardView logoCard;
    private ImageView logoImage;
    private TextView appName;
    private TextView tagline;
    private TextView footerText;
    private View decorativeLine;
    private View designCircle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        hideActionBar();
        setupStatusBar();
        initializeViews();
        setInitialStates();
        startEntranceAnimations();
        scheduleNavigation();
    }

    private void hideActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void setupStatusBar() {
        int color = ContextCompat.getColor(this, R.color.b);
        getWindow().setStatusBarColor(color);
        getWindow().setNavigationBarColor(color);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        );

        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }
    }

    private void initializeViews() {
        logoCard = findViewById(R.id.logoCard);
        logoImage = findViewById(R.id.logoImage);
        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);
        footerText = findViewById(R.id.footerText);
        decorativeLine = findViewById(R.id.decorativeLine);
        designCircle = findViewById(R.id.designCircle);
        progressBar = findViewById(R.id.progressBar);
        handler = new Handler(Looper.getMainLooper());
    }

    private void setInitialStates() {
        if (designCircle != null) {
            designCircle.setAlpha(0f);
            designCircle.setScaleX(0.5f);
            designCircle.setScaleY(0.5f);
            designCircle.setTranslationX(-300f);
        }

        if (logoCard != null) {
            logoCard.setAlpha(0f);
            logoCard.setScaleX(0.2f);
            logoCard.setScaleY(0.2f);
            logoCard.setRotation(-270f);
            logoCard.setTranslationY(100f);
        }

        if (logoImage != null) {
            logoImage.setScaleX(0f);
            logoImage.setScaleY(0f);
            logoImage.setRotation(180f);
        }

        if (appName != null) {
            appName.setAlpha(0f);
            appName.setTranslationX(-120f);
            appName.setScaleX(0.8f);
            appName.setScaleY(0.8f);
        }

        if (tagline != null) {
            tagline.setAlpha(0f);
            tagline.setTranslationX(-80f);
        }

        if (decorativeLine != null) {
            decorativeLine.setScaleX(0f);
            decorativeLine.setAlpha(0f);
        }

        if (footerText != null) {
            footerText.setAlpha(0f);
            footerText.setTranslationY(40f);
        }

        if (progressBar != null) {
            progressBar.setAlpha(0f);
            progressBar.setScaleX(0.3f);
            progressBar.setScaleY(0.3f);
        }
    }

    private void startEntranceAnimations() {
        mainAnimatorSet = new AnimatorSet();

        AnimatorSet circleAnimations = createCircleAnimation();
        AnimatorSet cardAnimations = createCardAnimation();
        AnimatorSet imageAnimations = createImageAnimation();
        AnimatorSet nameAnimations = createNameAnimation();
        AnimatorSet lineAnimations = createLineAnimation();
        AnimatorSet taglineAnimations = createTaglineAnimation();
        AnimatorSet footerAnimations = createFooterAnimation();
        AnimatorSet progressAnimations = createProgressAnimation();

        mainAnimatorSet.playTogether(
                circleAnimations,
                cardAnimations,
                imageAnimations,
                nameAnimations,
                lineAnimations,
                taglineAnimations,
                footerAnimations,
                progressAnimations
        );

        mainAnimatorSet.start();
        startPulseAnimations();
        startProgressIndeterminateAnimation();
    }

    private AnimatorSet createCircleAnimation() {
        ObjectAnimator circleAlpha = ObjectAnimator.ofFloat(designCircle, "alpha", 0f, 1f);
        ObjectAnimator circleScaleX = ObjectAnimator.ofFloat(designCircle, "scaleX", 0.5f, 1f);
        ObjectAnimator circleScaleY = ObjectAnimator.ofFloat(designCircle, "scaleY", 0.5f, 1f);
        ObjectAnimator circleTranslation = ObjectAnimator.ofFloat(designCircle, "translationX", -300f, 0f);

        AnimatorSet circleSet = new AnimatorSet();
        circleSet.playTogether(circleAlpha, circleScaleX, circleScaleY, circleTranslation);
        circleSet.setDuration(1000);
        circleSet.setInterpolator(new DecelerateInterpolator(1.5f));
        return circleSet;
    }

    private AnimatorSet createCardAnimation() {
        ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f);
        ObjectAnimator cardScaleX = ObjectAnimator.ofFloat(logoCard, "scaleX", 0.2f, 1f);
        ObjectAnimator cardScaleY = ObjectAnimator.ofFloat(logoCard, "scaleY", 0.2f, 1f);
        ObjectAnimator cardRotation = ObjectAnimator.ofFloat(logoCard, "rotation", -270f, 0f);
        ObjectAnimator cardTranslation = ObjectAnimator.ofFloat(logoCard, "translationY", 100f, 0f);

        AnimatorSet cardSet = new AnimatorSet();
        cardSet.playTogether(cardAlpha, cardScaleX, cardScaleY, cardRotation, cardTranslation);
        cardSet.setDuration(1100);
        cardSet.setInterpolator(new OvershootInterpolator(1.8f));
        cardSet.setStartDelay(150);
        return cardSet;
    }

    private AnimatorSet createImageAnimation() {
        ObjectAnimator imageScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0f, 1f);
        ObjectAnimator imageScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0f, 1f);
        ObjectAnimator imageRotation = ObjectAnimator.ofFloat(logoImage, "rotation", 180f, 0f);

        AnimatorSet imageSet = new AnimatorSet();
        imageSet.playTogether(imageScaleX, imageScaleY, imageRotation);
        imageSet.setDuration(500);
        imageSet.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
        imageSet.setStartDelay(400);
        return imageSet;
    }

    private AnimatorSet createNameAnimation() {
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        ObjectAnimator nameTranslation = ObjectAnimator.ofFloat(appName, "translationX", -120f, 0f);
        ObjectAnimator nameScaleX = ObjectAnimator.ofFloat(appName, "scaleX", 0.8f, 1f);
        ObjectAnimator nameScaleY = ObjectAnimator.ofFloat(appName, "scaleY", 0.8f, 1f);

        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTranslation, nameScaleX, nameScaleY);
        nameSet.setDuration(700);
        nameSet.setInterpolator(new DecelerateInterpolator(1.2f));
        nameSet.setStartDelay(350);
        return nameSet;
    }

    private AnimatorSet createLineAnimation() {
        ObjectAnimator lineScale = ObjectAnimator.ofFloat(decorativeLine, "scaleX", 0f, 1f);
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(decorativeLine, "alpha", 0f, 1f);

        AnimatorSet lineSet = new AnimatorSet();
        lineSet.playTogether(lineScale, lineAlpha);
        lineSet.setDuration(600);
        lineSet.setInterpolator(new AccelerateDecelerateInterpolator());
        lineSet.setStartDelay(550);
        return lineSet;
    }

    private AnimatorSet createTaglineAnimation() {
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        ObjectAnimator taglineTranslation = ObjectAnimator.ofFloat(tagline, "translationX", -80f, 0f);

        AnimatorSet taglineSet = new AnimatorSet();
        taglineSet.playTogether(taglineAlpha, taglineTranslation);
        taglineSet.setDuration(600);
        taglineSet.setInterpolator(new DecelerateInterpolator(1.2f));
        taglineSet.setStartDelay(650);
        return taglineSet;
    }

    private AnimatorSet createFooterAnimation() {
        ObjectAnimator footerAlpha = ObjectAnimator.ofFloat(footerText, "alpha", 0f, 1f);
        ObjectAnimator footerTranslation = ObjectAnimator.ofFloat(footerText, "translationY", 40f, 0f);

        AnimatorSet footerSet = new AnimatorSet();
        footerSet.playTogether(footerAlpha, footerTranslation);
        footerSet.setDuration(600);
        footerSet.setInterpolator(new DecelerateInterpolator(1.2f));
        footerSet.setStartDelay(850);
        return footerSet;
    }

    private AnimatorSet createProgressAnimation() {
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        ObjectAnimator progressScaleX = ObjectAnimator.ofFloat(progressBar, "scaleX", 0.3f, 1f);
        ObjectAnimator progressScaleY = ObjectAnimator.ofFloat(progressBar, "scaleY", 0.3f, 1f);

        AnimatorSet progressSet = new AnimatorSet();
        progressSet.playTogether(progressAlpha, progressScaleX, progressScaleY);
        progressSet.setDuration(500);
        progressSet.setInterpolator(new OvershootInterpolator(1.5f));
        progressSet.setStartDelay(700);
        return progressSet;
    }

    private void startPulseAnimations() {
        handler.postDelayed(() -> {
            if (progressBar != null && !isFinishing() && !isDestroyed()) {
                pulseAnimatorSet = new AnimatorSet();

                ObjectAnimator pulseX = ObjectAnimator.ofFloat(progressBar, "scaleX", 1f, 1.3f, 1f);
                ObjectAnimator pulseY = ObjectAnimator.ofFloat(progressBar, "scaleY", 1f, 1.3f, 1f);
                ObjectAnimator pulseAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 1f, 0.7f, 1f);

                pulseX.setDuration(1000);
                pulseY.setDuration(1000);
                pulseAlpha.setDuration(1000);

                pulseAnimatorSet.playTogether(pulseX, pulseY, pulseAlpha);
                pulseAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                pulseAnimatorSet.start();
            }
        }, 900);
    }

    private void startProgressIndeterminateAnimation() {
        if (progressBar != null) {
            progressBar.setIndeterminate(true);
        }
    }

    private void scheduleNavigation() {
        handler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                navigateToHome();
            }
        }, SPLASH_DURATION);
    }

    private void navigateToHome() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(400)
                    .setInterpolator(new AccelerateInterpolator(1.5f))
                    .withEndAction(this::launchHomeActivity)
                    .start();
        } else {
            launchHomeActivity();
        }
    }

    private void launchHomeActivity() {
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(0, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mainAnimatorSet != null && mainAnimatorSet.isRunning()) {
            mainAnimatorSet.cancel();
        }
        if (pulseAnimatorSet != null && pulseAnimatorSet.isRunning()) {
            pulseAnimatorSet.cancel();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}