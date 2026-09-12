package reader.aigd.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import reader.aigd.R;

public class AboutActivity extends AppCompatActivity {

    private MaterialCardView backButton;
    private MaterialCardView websiteButton;
    private MaterialCardView ceoWebsiteButton;
    private MaterialCardView phoneButton;
    private MaterialCardView emailButton;

    private Banner startAppBanner;
    private StartAppAd startAppAd;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isExiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        initializeStartIoAds();

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.f));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initializeViews();
        loadBanner();
        setupClickListeners();
    }

    private void initializeStartIoAds() {
        StartAppSDK.init(this, "206823693", false);
        startAppAd = new StartAppAd(this);
    }

    private void loadBanner() {
        startAppBanner = findViewById(R.id.startAppBanner);
        if (startAppBanner != null) {
            startAppBanner.loadAd();
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        websiteButton = findViewById(R.id.websiteButton);
        ceoWebsiteButton = findViewById(R.id.ceoWebsiteButton);
        phoneButton = findViewById(R.id.phoneButton);
        emailButton = findViewById(R.id.emailButton);
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                anim(v);
                finishWithAnim();
            });
        }

        // Visit our Website -> awanigd.com
        if (websiteButton != null) {
            websiteButton.setOnClickListener(v -> {
                anim(v);
                haptic(v);
                openUrl("https://awanigd.com");
            });
        }

        // CEO Official Website -> bilal-rasheed-official.web.app
        if (ceoWebsiteButton != null) {
            ceoWebsiteButton.setOnClickListener(v -> {
                anim(v);
                haptic(v);
                openUrl("https://bilal-rasheed-official.web.app/");
            });
        }

        if (phoneButton != null) {
            phoneButton.setOnClickListener(v -> {
                anim(v);
                haptic(v);
                Intent i = new Intent(Intent.ACTION_DIAL);
                i.setData(Uri.parse("tel:+923494360484"));
                startActivity(i);
            });
        }

        if (emailButton != null) {
            emailButton.setOnClickListener(v -> {
                anim(v);
                haptic(v);
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:awanigd@gmail.com"));
                try {
                    startActivity(i);
                } catch (Exception e) {
                    toast("No email app");
                }
            });
        }
    }

    private void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            toast("No browser found");
        }
    }

    private void anim(View v) {
        if (v == null) return;
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        ).start();
    }

    private void haptic(View v) {
        if (v != null) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private void finishWithAnim() {
        if (isExiting) return;
        isExiting = true;
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        finishWithAnim();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}