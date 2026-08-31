package reader.aigd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;

public class AddScriptActivity extends AppCompatActivity {

    // UI Components
    private TextInputEditText contentInput;
    private LinearLayout backButton;
    private LinearLayout clearButton;
    private LinearLayout undoButton;
    private LinearLayout pasteButton;
    private ScrollView scrollView;
    private ProgressBar loadingOverlay;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private TextInputLayout contentTextLayout;
    private FloatingActionButton fabSave;

    // Start.io Ad Elements
    private Banner startAppBanner;
    private StartAppAd startAppAd;

    // Data
    private DataManager dataManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isEditing = false;
    private long editingScriptId = -1;
    private String originalContent = "";
    private String originalTitle = "";
    private boolean isExiting = false;
    private boolean isSaving = false;
    private Typeface nolroFont;
    private String lastSavedContent = "";
    private boolean undoAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_script);

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
        checkEditingMode();
        initializeViews();
        loadStartIoBannerAd();
        setupTextWatchers();
        setupClickListeners();
        startEntranceAnimations();
        setupKeyboardFocus();
    }

    private void initializeStartIoAds() {
        StartAppSDK.init(this, "206823693", false);
        startAppAd = new StartAppAd(this);
    }

    private void loadStartIoBannerAd() {
        startAppBanner = findViewById(R.id.startAppBanner);
        if (startAppBanner != null) {
            startAppBanner.loadAd();
            startAppBanner.setVisibility(View.VISIBLE);
        }
    }

    private void checkEditingMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("script_id")) {
            isEditing = true;
            editingScriptId = intent.getLongExtra("script_id", -1);
            originalContent = intent.getStringExtra("script_content");
            originalTitle = intent.getStringExtra("script_title");
            if (originalTitle == null) originalTitle = "";
            if (originalContent == null) originalContent = "";
        }
    }

    private void initializeViews() {
        contentInput = findViewById(R.id.contentInput);
        backButton = findViewById(R.id.backButton);
        clearButton = findViewById(R.id.clearButton);
        undoButton = findViewById(R.id.undoButton);
        pasteButton = findViewById(R.id.pasteButton);
        scrollView = findViewById(R.id.scrollView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        contentTextLayout = findViewById(R.id.contentTextLayout);
        fabSave = findViewById(R.id.fabSave);

        if (isEditing) {
            if (contentInput != null) {
                contentInput.setText(originalContent);
                lastSavedContent = originalContent;
            }
            if (headerTitle != null && !originalTitle.isEmpty()) {
                headerTitle.setText(originalTitle);
            }
            if (headerSubtitle != null) {
                headerSubtitle.setText("Editing your script");
            }
        }

        if (undoButton != null) {
            undoButton.setVisibility(View.GONE);
        }
    }

    private void setupTextWatchers() {
        if (contentInput != null) {
            contentInput.addTextChangedListener(new TextWatcher() {
                private String previousText = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    previousText = s.toString();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();
                    updateLiveIndicators(text);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!s.toString().equals(previousText) && !previousText.isEmpty()) {
                        undoAvailable = true;
                        if (undoButton != null) {
                            undoButton.setVisibility(View.VISIBLE);
                            undoButton.setAlpha(0f);
                            undoButton.animate().alpha(1f).setDuration(300).start();
                        }
                    }
                }
            });
        }
    }

    private void updateLiveIndicators(String text) {
        int length = text.length();

        String generatedTitle = generateTitleFromContent(text);
        if (!isEditing && headerTitle != null && !headerTitle.getText().toString().equals(generatedTitle)) {
            animateTitleChange(generatedTitle);
        }

        if (headerSubtitle != null) {
            String newSubtitle;
            if (length == 0) {
                newSubtitle = "Start writing your script...";
            } else if (length < 50) {
                newSubtitle = "You're on your way! Keep going";
            } else if (length < 150) {
                newSubtitle = "Amazing progress! This looks great";
            } else {
                newSubtitle = "Outstanding! Your script is coming together";
            }

            if (!headerSubtitle.getText().toString().equals(newSubtitle)) {
                animateSubtitleChange(newSubtitle);
            }
        }

        if (fabSave != null) {
            if (length > 0 && fabSave.getVisibility() != View.VISIBLE) {
                fabSave.setVisibility(View.VISIBLE);
                fabSave.setScaleX(0f);
                fabSave.setScaleY(0f);
                fabSave.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            } else if (length == 0 && fabSave.getVisibility() == View.VISIBLE) {
                fabSave.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(200)
                        .withEndAction(() -> fabSave.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    private void animateTitleChange(String newTitle) {
        headerTitle.animate()
                .alpha(0f)
                .translationX(-12f)
                .setDuration(150)
                .withEndAction(() -> {
                    headerTitle.setText(newTitle);
                    headerTitle.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.1f))
                            .start();
                })
                .start();
    }

    private void animateSubtitleChange(String newText) {
        headerSubtitle.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    headerSubtitle.setText(newText);
                    headerSubtitle.animate()
                            .alpha(1f)
                            .setDuration(180)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "New Script";
        }

        String[] lines = content.split("\n");
        String firstLine = lines[0].trim();

        if (firstLine.isEmpty() && lines.length > 1) {
            firstLine = lines[1].trim();
        }

        if (firstLine.length() > 35) {
            firstLine = firstLine.substring(0, 32) + "...";
        }

        return firstLine.isEmpty() ? "Untitled Script" : firstLine;
    }

    private void setupClickListeners() {
        if (fabSave != null) {
            fabSave.setOnClickListener(v -> {
                String content = contentInput != null ? contentInput.getText().toString().trim() : "";
                if (content.isEmpty()) {
                    showCustomToast("Please write something first");
                    return;
                }
                String title = generateTitleFromContent(content);
                animateButtonPress(v);
                performSave(title, content);
            });
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                animateButtonTouch(v);
                checkUnsavedChangesAndExit();
            });
        }

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                animateButtonTouch(v);
                if (contentInput != null && !contentInput.getText().toString().isEmpty()) {
                    contentInput.setText("");
                    showCustomToast("Content cleared");
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                }
            });
        }

        if (undoButton != null) {
            undoButton.setOnClickListener(v -> {
                animateButtonTouch(v);
                if (undoAvailable && contentInput != null) {
                    String current = contentInput.getText().toString();
                    if (!current.equals(lastSavedContent)) {
                        contentInput.setText(lastSavedContent);
                        undoAvailable = false;
                        undoButton.setVisibility(View.GONE);
                        showCustomToast("Undo complete");
                        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    }
                }
            });
        }

        if (pasteButton != null) {
            pasteButton.setOnClickListener(v -> {
                animateButtonTouch(v);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        String pastedText = item.getText().toString();
                        if (contentInput != null) {
                            int start = contentInput.getSelectionStart();
                            int end = contentInput.getSelectionEnd();
                            String currentText = contentInput.getText().toString();
                            String newText = currentText.substring(0, start) + pastedText + currentText.substring(end);
                            contentInput.setText(newText);
                            contentInput.setSelection(start + pastedText.length());
                            showCustomToast("Pasted from clipboard");
                            performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                        }
                    }
                } else {
                    showCustomToast("No text to paste");
                }
            });
        }
    }

    private void performHapticFeedback(int intensity) {
        View view = getCurrentFocus();
        if (view != null) {
            view.performHapticFeedback(intensity);
        } else if (contentInput != null) {
            contentInput.performHapticFeedback(intensity);
        } else if (fabSave != null) {
            fabSave.performHapticFeedback(intensity);
        }
    }

    private void performSave(String title, String content) {
        if (isEditing && editingScriptId != -1) {
            updateScript(editingScriptId, title, content);
        } else {
            saveScript(title, content);
        }
    }

    private void saveScript(String title, String content) {
        showLoading(true);

        handler.postDelayed(() -> {
            Script newScript = new Script(title, content);
            newScript.updateTimestamp();
            long id = dataManager.saveScript(newScript);

            handler.postDelayed(() -> {
                showLoading(false);
                if (id != -1) {
                    lastSavedContent = content;
                    showSuccessAnimationAndExit();
                } else {
                    showCustomToast("Failed to save script");
                    isSaving = false;
                }
            }, 600);
        }, 200);
    }

    private void updateScript(long scriptId, String title, String content) {
        showLoading(true);

        handler.postDelayed(() -> {
            Script script = dataManager.getScriptById(scriptId);
            if (script != null) {
                script.setTitle(title);
                script.setContent(content);
                script.updateTimestamp();
                boolean success = dataManager.updateScript(script);

                handler.postDelayed(() -> {
                    showLoading(false);
                    if (success) {
                        lastSavedContent = content;
                        showSuccessAnimationAndExit();
                    } else {
                        showCustomToast("Failed to update script");
                        isSaving = false;
                    }
                }, 600);
            } else {
                showLoading(false);
                showCustomToast("Script not found");
                isSaving = false;
            }
        }, 200);
    }

    private void showLoading(boolean show) {
        isSaving = show;

        if (loadingOverlay != null) {
            if (show) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingOverlay.setAlpha(0f);
                loadingOverlay.animate()
                        .alpha(1f)
                        .setDuration(250)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                loadingOverlay.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                        .start();
            }
        }

        if (contentInput != null) contentInput.setEnabled(!show);
        if (fabSave != null) fabSave.setEnabled(!show);
    }

    private void showSuccessAnimationAndExit() {
        if (fabSave != null) {
            fabSave.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> {
                        fabSave.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start();
                        showCustomToast(isEditing ? "Script updated!" : "Script saved!");
                        handler.postDelayed(this::animateExit, 300);
                    })
                    .start();
            fabSave.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        } else {
            animateExit();
        }
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

    private void showCustomDialog(String title, String message, String positiveText, String negativeText, Runnable onPositive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        TextView positiveButton = dialogView.findViewById(R.id.positiveButton);
        TextView negativeButton = dialogView.findViewById(R.id.negativeButton);

        if (dialogTitle != null) {
            dialogTitle.setText(title);
            dialogTitle.setTypeface(nolroFont);
        }
        if (dialogMessage != null) {
            dialogMessage.setText(message);
            dialogMessage.setTypeface(nolroFont);
        }
        if (positiveButton != null) {
            positiveButton.setText(positiveText);
            positiveButton.setTypeface(nolroFont);
        }
        if (negativeButton != null) {
            negativeButton.setText(negativeText);
            negativeButton.setTypeface(nolroFont);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        if (positiveButton != null) {
            positiveButton.setOnClickListener(v -> {
                if (onPositive != null) onPositive.run();
                dialog.dismiss();
            });
        }

        if (negativeButton != null) {
            negativeButton.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();

        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.92f);
        dialogView.setScaleY(0.92f);
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .start();
    }

    private void checkUnsavedChangesAndExit() {
        if (isExiting || isSaving) return;

        String content = contentInput != null ? contentInput.getText().toString().trim() : "";
        boolean hasContent = !content.isEmpty();

        if (isEditing) {
            if (hasContent && !content.equals(originalContent)) {
                showUnsavedDialog();
            } else {
                finishWithAnimation();
            }
        } else if (hasContent) {
            showUnsavedDialog();
        } else {
            finishWithAnimation();
        }
    }

    private void showUnsavedDialog() {
        showCustomDialog(
                "Discard changes?",
                "Your unsaved changes will be lost forever.",
                "Discard",
                "Keep Editing",
                this::finishWithAnimation
        );
    }

    private void animateButtonTouch(View view) {
        if (view != null) {
            view.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(80)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .setInterpolator(new OvershootInterpolator())
                            .start())
                    .start();
        }
    }

    private void animateButtonPress(View view) {
        if (view != null) {
            view.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .setInterpolator(new OvershootInterpolator())
                            .start())
                    .start();
        }
    }

    private void startEntranceAnimations() {
        if (headerTitle != null) {
            headerTitle.setAlpha(0f);
            headerTitle.setTranslationX(-15f);
            headerTitle.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(400)
                    .setStartDelay(120)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (headerSubtitle != null) {
            headerSubtitle.setAlpha(0f);
            headerSubtitle.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(220)
                    .start();
        }

        if (scrollView != null) {
            scrollView.setAlpha(0f);
            scrollView.setTranslationY(30f);
            scrollView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(450)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (fabSave != null) {
            fabSave.setVisibility(View.GONE);
        }
    }

    private void setupKeyboardFocus() {
        if (contentInput != null) {
            handler.postDelayed(() -> {
                if (!isFinishing() && contentInput.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(contentInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }, 550);
        }
    }

    private void animateExit() {
        if (isExiting) return;
        isExiting = true;

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            rootView.animate()
                    .alpha(0f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        finish();
                        overridePendingTransition(0, android.R.anim.fade_out);
                    })
                    .start();
        } else {
            finishWithAnimation();
        }
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        if (!isExiting) {
            checkUnsavedChangesAndExit();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        super.onDestroy();
    }
}