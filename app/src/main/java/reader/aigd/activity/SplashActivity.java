package reader.aigd.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import reader.aigd.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 3000;
    private Handler handler;
    private AnimatorSet mainSet;

    private CardView logoCard;
    private ImageView logoImage;
    private TextView appName, tagline, footerText;
    private View decorativeLine, designCircle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        logoCard = findViewById(R.id.logoCard);
        logoImage = findViewById(R.id.logoImage);
        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);
        footerText = findViewById(R.id.footerText);
        decorativeLine = findViewById(R.id.decorativeLine);
        designCircle = findViewById(R.id.designCircle);
        progressBar = findViewById(R.id.progressBar);
        handler = new Handler(Looper.getMainLooper());

        setInitialStates();
        startPerfectAnimations();
        goToHome();
    }

    private void setInitialStates() {
        // Circle
        designCircle.setAlpha(0f);
        designCircle.setScaleX(0.7f);
        designCircle.setScaleY(0.7f);

        // Logo - NO WIND, only scale fade
        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.3f);
        logoCard.setScaleY(0.3f);
        logoCard.setTranslationY(60f);

        logoImage.setAlpha(0f);
        logoImage.setScaleX(0.5f);
        logoImage.setScaleY(0.5f);

        // Texts
        appName.setAlpha(0f);
        appName.setTranslationY(30f);

        decorativeLine.setAlpha(0f);
        decorativeLine.setScaleX(0f);

        tagline.setAlpha(0f);
        tagline.setTranslationY(20f);

        footerText.setAlpha(0f);
        progressBar.setAlpha(0f);
    }

    private void startPerfectAnimations() {
        // 1. Circle fade + scale
        ObjectAnimator circleAlpha = ObjectAnimator.ofFloat(designCircle, "alpha", 0f, 1f);
        ObjectAnimator circleScaleX = ObjectAnimator.ofFloat(designCircle, "scaleX", 0.7f, 1f);
        ObjectAnimator circleScaleY = ObjectAnimator.ofFloat(designCircle, "scaleY", 0.7f, 1f);

        AnimatorSet circleSet = new AnimatorSet();
        circleSet.playTogether(circleAlpha, circleScaleX, circleScaleY);
        circleSet.setDuration(900);
        circleSet.setInterpolator(new DecelerateInterpolator());

        // 2. Logo Card - smooth up + scale, NO ROTATION
        ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f);
        ObjectAnimator cardScaleX = ObjectAnimator.ofFloat(logoCard, "scaleX", 0.3f, 1f);
        ObjectAnimator cardScaleY = ObjectAnimator.ofFloat(logoCard, "scaleY", 0.3f, 1f);
        ObjectAnimator cardTransY = ObjectAnimator.ofFloat(logoCard, "translationY", 60f, 0f);

        AnimatorSet cardSet = new AnimatorSet();
        cardSet.playTogether(cardAlpha, cardScaleX, cardScaleY, cardTransY);
        cardSet.setDuration(1000);
        cardSet.setStartDelay(150);
        cardSet.setInterpolator(new OvershootInterpolator(1.6f));

        // 3. Logo Image inside - simple fade scale, NO WIND
        ObjectAnimator imgAlpha = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        ObjectAnimator imgScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f);
        ObjectAnimator imgScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f);

        AnimatorSet imgSet = new AnimatorSet();
        imgSet.playTogether(imgAlpha, imgScaleX, imgScaleY);
        imgSet.setDuration(600);
        imgSet.setStartDelay(500);
        imgSet.setInterpolator(new DecelerateInterpolator());

        // 4. App Name
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f);
        ObjectAnimator nameTrans = ObjectAnimator.ofFloat(appName, "translationY", 30f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTrans);
        nameSet.setDuration(600);
        nameSet.setStartDelay(650);
        nameSet.setInterpolator(new DecelerateInterpolator());

        // 5. Line
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(decorativeLine, "alpha", 0f, 0.5f);
        ObjectAnimator lineScale = ObjectAnimator.ofFloat(decorativeLine, "scaleX", 0f, 1f);
        AnimatorSet lineSet = new AnimatorSet();
        lineSet.playTogether(lineAlpha, lineScale);
        lineSet.setDuration(500);
        lineSet.setStartDelay(850);

        // 6. Tagline
        ObjectAnimator tagAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        ObjectAnimator tagTrans = ObjectAnimator.ofFloat(tagline, "translationY", 20f, 0f);
        AnimatorSet tagSet = new AnimatorSet();
        tagSet.playTogether(tagAlpha, tagTrans);
        tagSet.setDuration(600);
        tagSet.setStartDelay(950);
        tagSet.setInterpolator(new DecelerateInterpolator());

        // 7. Progress + Footer
        ObjectAnimator progAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progAlpha.setDuration(400);
        progAlpha.setStartDelay(1150);

        ObjectAnimator footAlpha = ObjectAnimator.ofFloat(footerText, "alpha", 0f, 1f);
        footAlpha.setDuration(500);
        footAlpha.setStartDelay(1200);

        mainSet = new AnimatorSet();
        mainSet.playTogether(circleSet, cardSet, imgSet, nameSet, lineSet, tagSet, progAlpha, footAlpha);
        mainSet.start();
    }

    private void goToHome() {
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        if (mainSet != null) mainSet.cancel();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}