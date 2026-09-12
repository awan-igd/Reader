package reader.aigd.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.util.ArrayList;
import java.util.List;

import reader.aigd.R;
import reader.aigd.adapter.ScriptAdapter;
import reader.aigd.model.DataManager;
import reader.aigd.model.Script;

public class HomeActivity extends AppCompatActivity implements ScriptAdapter.OnScriptActionListener {

    private final List<Script> scriptList = new ArrayList<>();
    private final List<Script> filteredList = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private RecyclerView scriptsRecyclerView;
    private FloatingActionButton fabAddScript;
    private TextView scriptCount, emptyStateTitle, emptyStateSubtitle, greetingText, searchIndicator;
    private TextInputEditText searchInput;
    private TextInputLayout searchTextLayout;
    private View emptyState, headerContainer, accentLine;
    private MaterialCardView loadingProgress, aboutButton;
    private ProgressBar searchProgress;
    private Banner startAppBanner;

    private ScriptAdapter adapter;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        StartAppSDK.init(this, "206823693", false);
        dataManager = DataManager.getInstance(this);

        initViews();
        loadBanner();
        setupRecycler();
        setupSearch();
        setupClicks();
        loadScripts();
        animateEntrance();
    }

    private void initViews() {
        scriptsRecyclerView = findViewById(R.id.scriptsRecyclerView);
        fabAddScript = findViewById(R.id.fabAddScript);
        scriptCount = findViewById(R.id.scriptCount);
        searchInput = findViewById(R.id.searchInput);
        searchTextLayout = findViewById(R.id.searchTextLayout);
        emptyState = findViewById(R.id.emptyState);
        headerContainer = findViewById(R.id.headerContainer);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateSubtitle = findViewById(R.id.emptyStateSubtitle);
        greetingText = findViewById(R.id.greetingText);
        aboutButton = findViewById(R.id.aboutButton);
        searchProgress = findViewById(R.id.searchProgress);
        searchIndicator = findViewById(R.id.searchIndicator);
        accentLine = findViewById(R.id.accentLine);
        loadingProgress = findViewById(R.id.loadingProgress);
        startAppBanner = findViewById(R.id.startAppBanner);
    }

    private void loadBanner() {
        if (startAppBanner != null) startAppBanner.loadAd();
    }

    private void loadScripts() {
        loadingProgress.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            scriptList.clear();
            scriptList.addAll(dataManager.getAllScripts("newest"));
            filter("");
            loadingProgress.setVisibility(View.GONE);
        }, 300);
    }

    private void setupRecycler() {
        scriptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScriptAdapter(filteredList, this);
        scriptsRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                String q = s.toString();
                searchRunnable = () -> filter(q);
                handler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(scriptList);
            searchIndicator.setVisibility(View.GONE);
        } else {
            String low = query.toLowerCase();
            for (Script sc : scriptList) {
                if (sc.getTitle().toLowerCase().contains(low) || sc.getContent().toLowerCase().contains(low)) {
                    filteredList.add(sc);
                }
            }
            searchIndicator.setVisibility(View.VISIBLE);
            searchIndicator.setText("Found " + filteredList.size() + " results");
        }
        adapter.update(filteredList);
        scriptCount.setText(String.valueOf(filteredList.size()));
        emptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        scriptsRecyclerView.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupClicks() {
        fabAddScript.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(new OvershootInterpolator()).start();
                startActivityForResult(new Intent(this, AddScriptActivity.class), 100);
            }).start();
        });
        aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
    }

    private void animateEntrance() {
        headerContainer.setAlpha(0f);
        headerContainer.setTranslationY(-40f);
        headerContainer.animate().alpha(1f).translationY(0f).setDuration(600).start();
        searchTextLayout.setAlpha(0f);
        searchTextLayout.setTranslationY(20f);
        searchTextLayout.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(150).start();
        accentLine.setScaleX(0f);
        accentLine.animate().scaleX(1f).setDuration(500).setStartDelay(300).start();
        fabAddScript.setScaleX(0f);
        fabAddScript.setScaleY(0f);
        fabAddScript.animate().scaleX(1f).scaleY(1f).setDuration(500).setStartDelay(400).setInterpolator(new OvershootInterpolator()).start();
    }

    // ==================== ULTRA ACTION DIALOG WITH ANIMATIONS ====================
    @Override
    public void onScriptClick(Script script) {
        View view = getLayoutInflater().inflate(R.layout.dialog_premium_actions, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Views
        View tts = view.findViewById(R.id.actionTTS);
        View overlay = view.findViewById(R.id.actionOverlay);
        View share = view.findViewById(R.id.actionShare);
        View delete = view.findViewById(R.id.actionDelete);
        View cancel = view.findViewById(R.id.actionCancel);
        TextView name = view.findViewById(R.id.scriptName);
        if (name != null) name.setText(script.getTitle());

        // ---- ENTRANCE ANIMATION LIKE SCRIPT CARD ----
        tts.setAlpha(0f);
        tts.setTranslationY(30f);
        overlay.setAlpha(0f);
        overlay.setTranslationY(30f);
        share.setAlpha(0f);
        share.setTranslationY(30f);
        delete.setAlpha(0f);
        delete.setTranslationY(30f);
        cancel.setAlpha(0f);
        cancel.setScaleX(0.8f);
        cancel.setScaleY(0.8f);

        view.setAlpha(0f);
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);

        dialog.show();

        // Animate main card
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new OvershootInterpolator(1.2f)).start();

        // Staggered items animation
        tts.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(100).setInterpolator(new DecelerateInterpolator()).start();
        overlay.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(150).setInterpolator(new DecelerateInterpolator()).start();
        share.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
        delete.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(250).setInterpolator(new DecelerateInterpolator()).start();
        cancel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(300).setInterpolator(new OvershootInterpolator()).start();

        // ---- SELECTED ACTION ANIMATION - PERFECT ----
        tts.setOnClickListener(v -> animateSelectedAction(v, () -> {
            dialog.dismiss();
            openScript(TTSActivity.class, script);
        }));
        overlay.setOnClickListener(v -> animateSelectedAction(v, () -> {
            dialog.dismiss();
            openScript(OverlayActivity.class, script);
        }));
        share.setOnClickListener(v -> animateSelectedAction(v, () -> {
            dialog.dismiss();
            openScript(ShareActivity.class, script);
        }));
        delete.setOnClickListener(v -> animateSelectedAction(v, () -> {
            dialog.dismiss();
            showDeleteDialog(script);
        }));
        cancel.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                dialog.dismiss();
            }).start();
        });
    }

    // PERFECT SELECTED ACTION ANIMATION
    private void animateSelectedAction(View view, Runnable after) {
        // Scale down + glow effect
        view.animate()
                .scaleX(0.92f).scaleY(0.92f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.05f).scaleY(1.05f)
                            .setDuration(100)
                            .setInterpolator(new OvershootInterpolator())
                            .withEndAction(() -> {
                                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                                handler.postDelayed(after, 120);
                            }).start();
                }).start();

        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void openScript(Class<?> cls, Script script) {
        Intent i = new Intent(this, cls);
        i.putExtra("script_id", script.getId());
        i.putExtra("script_content", script.getContent());
        i.putExtra("script_title", script.getTitle());
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showDeleteDialog(Script script) {
        View v = getLayoutInflater().inflate(R.layout.dialog_premium_delete, null);
        AlertDialog d = new AlertDialog.Builder(this).setView(v).create();
        if (d.getWindow() != null)
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView name = v.findViewById(R.id.scriptName);
        if (name != null) name.setText(script.getTitle());

        View icon = v.findViewById(R.id.iconContainer);
        View deleteBtn = v.findViewById(R.id.deleteButton);
        View cancelBtn = v.findViewById(R.id.cancelButton);

        v.setAlpha(0f);
        v.setScaleX(0.8f);
        v.setScaleY(0.8f);
        d.show();
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new OvershootInterpolator(1.2f)).start();

        if (icon != null) {
            icon.setScaleX(0f);
            icon.setScaleY(0f);
            icon.animate().scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(150).setInterpolator(new OvershootInterpolator(2f)).start();
        }

        deleteBtn.setOnClickListener(x -> animateSelectedAction(x, () -> {
            d.dismiss();
            dataManager.deleteScript(script.getId());
            loadScripts();
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
        }));

        cancelBtn.setOnClickListener(x -> {
            x.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction(() -> d.dismiss()).start();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) loadScripts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScripts();
    }
}