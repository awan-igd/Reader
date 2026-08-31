package reader.aigd;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private final List<Script> scriptList = new ArrayList<>();
    private final List<Script> filteredList = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final String currentSort = "newest";
    // UI Components
    private RecyclerView scriptsRecyclerView;
    private FloatingActionButton fabAddScript;
    private TextView scriptCount;
    private TextInputEditText searchInput;
    private TextInputLayout searchTextLayout;
    private View emptyState;
    private View headerContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateSubtitle;
    private TextView greetingText;
    private LinearLayout aboutButton;
    private ProgressBar searchProgress;
    private TextView searchIndicator;
    private View accentLine;
    private com.google.android.material.card.MaterialCardView loadingProgress;
    // Start.io Ad Elements
    private Banner startAppBanner;
    private StartAppAd startAppAd;
    // Data
    private ScriptAdapter scriptAdapter;
    private Runnable searchRunnable;
    private boolean isSearching = false;
    private boolean isLoading = false;
    private Typeface nolroFont;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeStartIoAds();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        nolroFont = ResourcesCompat.getFont(this, R.font.aigd);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        dataManager = DataManager.getInstance(this);

        initializeViews();
        loadStartIoBannerAd();
        loadScripts();
        setupRecyclerView();
        setupSearchListener();
        setupClickListeners();
        updateGreeting();
        startPremiumEntranceAnimations();
    }

    private void initializeStartIoAds() {
        StartAppSDK.init(this, "206823693", false);
        startAppAd = new StartAppAd(this);
    }

    private void loadStartIoBannerAd() {
        startAppBanner = findViewById(R.id.startAppBanner);
        if (startAppBanner != null) {
            startAppBanner.loadAd();
        }
    }

    private void initializeViews() {
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

        applyFontToViews();
    }

    private void applyFontToViews() {
        if (scriptCount != null) scriptCount.setTypeface(nolroFont);
        if (emptyStateTitle != null) emptyStateTitle.setTypeface(nolroFont);
        if (emptyStateSubtitle != null) emptyStateSubtitle.setTypeface(nolroFont);
        if (greetingText != null) greetingText.setTypeface(nolroFont);
        if (searchIndicator != null) searchIndicator.setTypeface(nolroFont);
    }

    private void showCustomToast(String message) {
        Toast toast = new Toast(this);
        View toastView = LayoutInflater.from(this).inflate(R.layout.custom_toast, null);
        TextView toastText = toastView.findViewById(R.id.toastText);
        if (toastText != null) {
            toastText.setText(message);
            toastText.setTypeface(nolroFont);
        }
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    private void performHapticFeedback(int intensity) {
        View view = getCurrentFocus();
        if (view != null) {
            view.performHapticFeedback(intensity);
        } else {
            fabAddScript.performHapticFeedback(intensity);
        }
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator())
                        .start())
                .start();
    }

    // ==============================================
    // PREMIUM DELETE DIALOG
    // ==============================================
    private void showPremiumDeleteDialog(Script script) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_premium_delete, null);

        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        TextView messageText = dialogView.findViewById(R.id.dialogMessage);
        TextView scriptNameText = dialogView.findViewById(R.id.scriptName);
        LinearLayout deleteButton = dialogView.findViewById(R.id.deleteButton);
        LinearLayout cancelButton = dialogView.findViewById(R.id.cancelButton);
        View iconContainer = dialogView.findViewById(R.id.iconContainer);

        if (titleText != null) {
            titleText.setText("Delete Script");
            titleText.setTypeface(nolroFont);
        }
        if (messageText != null) {
            messageText.setText("This action cannot be undone. The script will be permanently removed.");
            messageText.setTypeface(nolroFont);
        }
        if (scriptNameText != null) {
            scriptNameText.setText("\"" + script.getTitle() + "\"");
            scriptNameText.setTypeface(nolroFont);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (iconContainer != null) {
            iconContainer.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> iconContainer.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start())
                    .start();
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                animateButtonClick(v);
                dialog.dismiss();
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                dataManager.deleteScript(script.getId());
                refreshData();
                showCustomToast("Script deleted permanently");
            });
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                animateButtonClick(v);
                dialog.dismiss();
            });
        }

        dialog.show();

        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.85f);
        dialogView.setScaleY(0.85f);
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

    // ==============================================
    // PREMIUM SCRIPT ACTIONS DIALOG
    // ==============================================
    private void showScriptActionsDialog(Script script) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_premium_actions, null);

        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        TextView scriptNameText = dialogView.findViewById(R.id.scriptName);
        TextView scriptPreviewText = dialogView.findViewById(R.id.scriptPreview);

        LinearLayout actionTTS = dialogView.findViewById(R.id.actionTTS);
        LinearLayout actionOverlay = dialogView.findViewById(R.id.actionOverlay);
        LinearLayout actionShare = dialogView.findViewById(R.id.actionShare);
        LinearLayout actionDelete = dialogView.findViewById(R.id.actionDelete);
        LinearLayout actionCancel = dialogView.findViewById(R.id.actionCancel);

        if (titleText != null) {
            titleText.setText("Script Actions");
            titleText.setTypeface(nolroFont);
        }
        if (scriptNameText != null) {
            scriptNameText.setText(script.getTitle());
            scriptNameText.setTypeface(nolroFont);
        }
        if (scriptPreviewText != null) {
            scriptPreviewText.setText(script.getPreview(60));
            scriptPreviewText.setTypeface(nolroFont);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // TTS
        if (actionTTS != null) {
            actionTTS.setOnClickListener(v -> {
                animateActionItem(v);
                dialog.dismiss();
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                Intent intent = new Intent(HomeActivity.this, TTSActivity.class);
                intent.putExtra("script_id", script.getId());
                intent.putExtra("script_content", script.getContent());
                intent.putExtra("script_title", script.getTitle());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Floating Overlay
        if (actionOverlay != null) {
            actionOverlay.setOnClickListener(v -> {
                animateActionItem(v);
                dialog.dismiss();
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                Intent intent = new Intent(HomeActivity.this, OverlayActivity.class);
                intent.putExtra("script_id", script.getId());
                intent.putExtra("script_content", script.getContent());
                intent.putExtra("script_title", script.getTitle());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Share
        if (actionShare != null) {
            actionShare.setOnClickListener(v -> {
                animateActionItem(v);
                dialog.dismiss();
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                Intent intent = new Intent(HomeActivity.this, ShareActivity.class);
                intent.putExtra("script_id", script.getId());
                intent.putExtra("script_content", script.getContent());
                intent.putExtra("script_title", script.getTitle());
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Print

        // Delete
        if (actionDelete != null) {
            actionDelete.setOnClickListener(v -> {
                animateActionItem(v);
                dialog.dismiss();
                showPremiumDeleteDialog(script);
            });
        }

        // Cancel
        if (actionCancel != null) {
            actionCancel.setOnClickListener(v -> {
                animateActionItem(v);
                dialog.dismiss();
            });
        }

        dialog.show();

        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.9f);
        dialogView.setScaleY(0.9f);
        dialogView.setRotationY(15f);
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotationY(0f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void animateActionItem(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
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
    // LOAD SCRIPTS
    // ==============================================
    private void loadScripts() {
        if (isLoading) return;
        isLoading = true;

        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
            loadingProgress.setAlpha(0f);
            loadingProgress.animate().alpha(1f).setDuration(200).start();
        }

        searchHandler.postDelayed(() -> {
            scriptList.clear();
            filteredList.clear();

            List<Script> allScripts = dataManager.getAllScripts(currentSort);
            scriptList.addAll(allScripts);
            applyFilterAndSearch();
            isLoading = false;

            if (loadingProgress != null) {
                loadingProgress.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> loadingProgress.setVisibility(View.GONE))
                        .start();
            }
        }, 300);
    }

    private void refreshData() {
        scriptList.clear();
        List<Script> allScripts = dataManager.getAllScripts(currentSort);
        scriptList.addAll(allScripts);
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
        filterScripts(query, true);
    }

    private void filterScripts(String query, boolean silent) {
        if (isSearching) return;
        isSearching = true;

        if (!silent && searchProgress != null) {
            searchProgress.setVisibility(View.VISIBLE);
            searchProgress.setAlpha(0f);
            searchProgress.animate().alpha(1f).setDuration(150).start();
        }

        searchHandler.postDelayed(() -> {
            filteredList.clear();

            if (query == null || query.isEmpty()) {
                filteredList.addAll(scriptList);
                if (!silent) searchIndicator.setText(filteredList.size() + " scripts");
            } else {
                String lowerQuery = query.toLowerCase().trim();
                for (Script script : scriptList) {
                    String content = script.getContent();
                    String title = script.getTitle();
                    if ((content != null && content.toLowerCase().contains(lowerQuery)) ||
                            (title != null && title.toLowerCase().contains(lowerQuery))) {
                        filteredList.add(script);
                    }
                }
                if (!silent) searchIndicator.setText("Found " + filteredList.size() + " results");
            }

            scriptAdapter.updateData(filteredList);
            updateScriptCount();
            checkEmptyState();

            if (!silent && searchProgress != null) {
                searchProgress.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction(() -> searchProgress.setVisibility(View.GONE))
                        .start();
            }

            isSearching = false;
        }, silent ? 0 : 150);
    }

    private void updateScriptCount() {
        int count = filteredList.size();
        scriptCount.setText(String.valueOf(count));

        ValueAnimator bounceAnim = ValueAnimator.ofFloat(1f, 1.4f, 0.9f, 1f);
        bounceAnim.setDuration(600);
        bounceAnim.setInterpolator(new OvershootInterpolator());
        bounceAnim.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (scriptCount != null) {
                scriptCount.setScaleX(value);
                scriptCount.setScaleY(value);
            }
        });
        bounceAnim.start();
    }

    private void checkEmptyState() {
        if (filteredList.isEmpty()) {
            if (emptyState.getVisibility() != View.VISIBLE) {
                emptyState.setVisibility(View.VISIBLE);
                scriptsRecyclerView.setVisibility(View.GONE);
                animateEmptyState();
            }
        } else {
            if (emptyState.getVisibility() == View.VISIBLE) {
                emptyState.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            emptyState.setVisibility(View.GONE);
                            scriptsRecyclerView.setVisibility(View.VISIBLE);
                            scriptsRecyclerView.setAlpha(0f);
                            scriptsRecyclerView.animate()
                                    .alpha(1f)
                                    .setDuration(500)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
                        })
                        .start();
            } else {
                scriptsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void animateEmptyState() {
        emptyState.setAlpha(0f);
        emptyState.setScaleX(0.8f);
        emptyState.setScaleY(0.8f);
        emptyState.setRotationY(-15f);

        emptyState.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotationY(0f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }

    // ==============================================
    // RECYCLER VIEW SETUP
    // ==============================================
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        scriptsRecyclerView.setLayoutManager(layoutManager);
        if (scriptsRecyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) scriptsRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        scriptAdapter = new ScriptAdapter(filteredList);
        scriptsRecyclerView.setAdapter(scriptAdapter);

        scriptsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isFabVisible = true;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && isFabVisible) {
                    fabAddScript.animate()
                            .scaleX(0f)
                            .scaleY(0f)
                            .alpha(0f)
                            .setDuration(250)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    isFabVisible = false;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE && !isFabVisible) {
                    fabAddScript.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(350)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                    isFabVisible = true;
                }
            }
        });
    }

    // ==============================================
    // SEARCH SETUP
    // ==============================================
    private void setupSearchListener() {
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            private String previousText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String currentText = s.toString();

                if (currentText.length() > 0 && previousText.isEmpty()) {
                    searchIndicator.setVisibility(View.VISIBLE);
                    searchIndicator.setAlpha(0f);
                    searchIndicator.animate().alpha(1f).setDuration(200).start();
                } else if (currentText.isEmpty() && previousText.length() > 0) {
                    searchIndicator.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction(() -> searchIndicator.setVisibility(View.GONE))
                            .start();
                }

                previousText = currentText;

                searchRunnable = () -> {
                    String query = currentText;
                    filterScripts(query, false);
                };
                searchHandler.postDelayed(searchRunnable, 350);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    // ==============================================
    // CLICK LISTENERS
    // ==============================================
    private void setupClickListeners() {
        fabAddScript.setOnClickListener(v -> {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animateButtonClick(v);
            searchHandler.postDelayed(() -> {
                Intent intent = new Intent(HomeActivity.this, AddScriptActivity.class);
                startActivityForResult(intent, 100);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 150);
        });

        aboutButton.setOnClickListener(v -> {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            animateButtonClick(v);
            Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // ==============================================
    // GREETING
    // ==============================================
    private void updateGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int day = calendar.get(java.util.Calendar.DAY_OF_WEEK);

        String greeting;
        if (hour < 12) greeting = "Good morning";
        else if (hour < 17) greeting = "Good afternoon";
        else greeting = "Good evening";

        if (day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY) {
            greeting += " Weekend!";
        }

        greetingText.setText(greeting);

        greetingText.setAlpha(0f);
        greetingText.setTranslationY(20f);
        greetingText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(400)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    // ==============================================
    // ENTRANCE ANIMATIONS
    // ==============================================
    private void startPremiumEntranceAnimations() {
        headerContainer.setAlpha(0f);
        headerContainer.setTranslationY(-80f);
        headerContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        searchTextLayout.setAlpha(0f);
        searchTextLayout.setTranslationY(-40f);
        searchTextLayout.setScaleX(0.9f);
        searchTextLayout.setScaleY(0.9f);
        searchTextLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(250)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        fabAddScript.setScaleX(0f);
        fabAddScript.setScaleY(0f);
        fabAddScript.setRotation(-180f);
        fabAddScript.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(600)
                .setStartDelay(500)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        scriptsRecyclerView.setAlpha(0f);
        scriptsRecyclerView.setTranslationY(30f);
        scriptsRecyclerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(650)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        accentLine.setScaleX(0f);
        accentLine.animate()
                .scaleX(1f)
                .setDuration(600)
                .setStartDelay(300)
                .setInterpolator(new OvershootInterpolator())
                .start();

        View badgeContainer = findViewById(R.id.badgeContainer);
        if (badgeContainer != null) {
            badgeContainer.setScaleX(0f);
            badgeContainer.setScaleY(0f);
            badgeContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            refreshData();
            if (scriptsRecyclerView != null) {
                scriptsRecyclerView.smoothScrollToPosition(0);
            }
            showCustomToast("Script saved successfully!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onBackPressed() {
        if (searchInput.getText() != null && !searchInput.getText().toString().isEmpty()) {
            searchInput.setText("");
            showCustomToast("Search cleared");
        } else {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    // ==============================================
    // SCRIPT ADAPTER
    // ==============================================
    private class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.ViewHolder> {

        private List<Script> scripts;
        private int lastAnimatedPosition = -1;

        ScriptAdapter(List<Script> scripts) {
            this.scripts = scripts;
        }

        void updateData(List<Script> newScripts) {
            this.scripts = newScripts;
            lastAnimatedPosition = -1;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_script, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Script script = scripts.get(position);

            String preview = script.getPreview(120);
            holder.previewText.setText(preview);
            holder.previewText.setTypeface(nolroFont);

            String metadata = script.getLastEdited();
            holder.metadataText.setText(metadata);
            holder.metadataText.setTypeface(nolroFont);

            // Click on card - opens actions dialog
            holder.cardView.setOnClickListener(v -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                showScriptActionsDialog(script);
            });

            // Swipe to show dialog
            holder.cardView.setOnTouchListener(new View.OnTouchListener() {
                private float startX;
                private boolean isSwiping = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            isSwiping = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getX() - startX;
                            if (Math.abs(deltaX) > 50) {
                                isSwiping = true;
                            }
                            if (deltaX < -80) {
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                showScriptActionsDialog(script);
                                return true;
                            }
                            return false;
                        case MotionEvent.ACTION_UP:
                            if (!isSwiping) {
                                holder.cardView.performClick();
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });

            // Item animation
            if (position > lastAnimatedPosition) {
                lastAnimatedPosition = position;
                holder.itemView.setAlpha(0f);
                holder.itemView.setTranslationY(60f);
                holder.itemView.setScaleX(0.95f);
                holder.itemView.setScaleY(0.95f);
                holder.itemView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .setStartDelay(Math.min(position * 50, 800))
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
            }
        }

        @Override
        public int getItemCount() {
            return scripts.size();
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.itemView.clearAnimation();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            androidx.cardview.widget.CardView cardView;
            TextView metadataText;
            TextView previewText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.scriptCard);
                metadataText = itemView.findViewById(R.id.scriptMetadata);
                previewText = itemView.findViewById(R.id.scriptPreview);
            }
        }
    }
}