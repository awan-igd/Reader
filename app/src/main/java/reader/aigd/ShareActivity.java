package reader.aigd;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShareActivity extends AppCompatActivity {

    // UI Components
    private TextView scriptTitlePreview;
    private TextView scriptContentPreview;
    private TextView scriptWordCount;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private LinearLayout backButton;
    private LinearLayout shareButton;
    private LinearLayout shareWhatsApp;
    private LinearLayout shareTelegram;
    private LinearLayout shareEmail;
    private LinearLayout shareSMS;
    private LinearLayout shareCopy;
    private LinearLayout shareSave;
    private LinearLayout sharePrint;
    private LinearLayout shareMore;
    private FrameLayout loadingOverlay;
    private ProgressBar loadingSpinner;

    // Data
    private DataManager dataManager;
    private long scriptId = -1;
    private String scriptContent = "";
    private String scriptTitle = "";
    private Typeface nolroFont;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Watermark settings
    private static final String WATERMARK_TEXT = "Reader by Awan IGD";
    private static final float WATERMARK_SIZE = 14f;
    private static final int WATERMARK_ALPHA = 40;
    private static final int WATERMARK_SPACING = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Load font
        nolroFont = ResourcesCompat.getFont(this, R.font.aigd);

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

        dataManager = DataManager.getInstance(this);

        initializeViews();
        getIntentData();
        setupUI();
        setupClickListeners();
        applyFonts();
        startEntranceAnimations();
    }

    private void initializeViews() {
        scriptTitlePreview = findViewById(R.id.scriptTitlePreview);
        scriptContentPreview = findViewById(R.id.scriptContentPreview);
        scriptWordCount = findViewById(R.id.scriptWordCount);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
        shareWhatsApp = findViewById(R.id.shareWhatsApp);
        shareTelegram = findViewById(R.id.shareTelegram);
        shareEmail = findViewById(R.id.shareEmail);
        shareSMS = findViewById(R.id.shareSMS);
        shareCopy = findViewById(R.id.shareCopy);
        shareSave = findViewById(R.id.shareSave);
        sharePrint = findViewById(R.id.sharePrint);
        shareMore = findViewById(R.id.shareMore);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingSpinner = findViewById(R.id.loadingSpinner);
    }

    private void applyFonts() {
        if (scriptTitlePreview != null) scriptTitlePreview.setTypeface(nolroFont);
        if (scriptContentPreview != null) scriptContentPreview.setTypeface(nolroFont);
        if (scriptWordCount != null) scriptWordCount.setTypeface(nolroFont);
        if (headerTitle != null) headerTitle.setTypeface(nolroFont);
        if (headerSubtitle != null) headerSubtitle.setTypeface(nolroFont);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            scriptId = intent.getLongExtra("script_id", -1);
            scriptContent = intent.getStringExtra("script_content");
            scriptTitle = intent.getStringExtra("script_title");

            if (scriptId != -1) {
                Script currentScript = dataManager.getScriptById(scriptId);
                if (currentScript != null) {
                    scriptContent = currentScript.getContent();
                    scriptTitle = currentScript.getTitle();
                }
            }
        }

        if (scriptContent == null || scriptContent.isEmpty()) {
            scriptContent = "No script content available to share.";
        }

        if (scriptTitle == null || scriptTitle.isEmpty()) {
            scriptTitle = "Untitled Script";
        }
    }

    private void setupUI() {
        if (scriptTitlePreview != null) {
            scriptTitlePreview.setText(scriptTitle);
        }

        if (scriptContentPreview != null) {
            String preview = scriptContent.length() > 300
                    ? scriptContent.substring(0, 300) + "..."
                    : scriptContent;
            scriptContentPreview.setText(preview);
        }

        if (scriptWordCount != null) {
            int wordCount = scriptContent.split("\\s+").length;
            scriptWordCount.setText(wordCount + " words");
        }

        if (headerTitle != null) {
            headerTitle.setText(scriptTitle);
        }

        if (headerSubtitle != null) {
            headerSubtitle.setText("Share this script with others");
        }
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                finishWithAnimation();
            });
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaSystem();
            });
        }

        if (shareWhatsApp != null) {
            shareWhatsApp.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaWhatsApp();
            });
        }

        if (shareTelegram != null) {
            shareTelegram.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaTelegram();
            });
        }

        if (shareEmail != null) {
            shareEmail.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaEmail();
            });
        }

        if (shareSMS != null) {
            shareSMS.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaSMS();
            });
        }

        if (shareCopy != null) {
            shareCopy.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                copyToClipboard();
            });
        }

        if (shareSave != null) {
            shareSave.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                saveToFile();
            });
        }

        // ✅ FIXED: Print with Watermark using proper PrintDocumentAdapter
        if (sharePrint != null) {
            sharePrint.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                printScriptWithWatermark();
            });
        }

        if (shareMore != null) {
            shareMore.setOnClickListener(v -> {
                performHapticFeedback();
                animateButton(v);
                shareViaSystem();
            });
        }
    }

    // ==============================================
    // SHARE METHODS
    // ==============================================

    private void shareViaSystem() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, scriptTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
        startActivity(Intent.createChooser(shareIntent, "Share Script"));
    }

    private void shareViaWhatsApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
        shareIntent.setPackage("com.whatsapp");
        try {
            startActivity(shareIntent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            shareViaSystem();
        }
    }

    private void shareViaTelegram() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
        shareIntent.setPackage("org.telegram.messenger");
        try {
            startActivity(shareIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Telegram not installed", Toast.LENGTH_SHORT).show();
            shareViaSystem();
        }
    }

    private void shareViaEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, scriptTitle);
        emailIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
        try {
            startActivity(emailIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            shareViaSystem();
        }
    }

    private void shareViaSMS() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.putExtra("sms_body", getShareText());
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No SMS app found", Toast.LENGTH_SHORT).show();
            shareViaSystem();
        }
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(scriptTitle, getShareText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void saveToFile() {
        showLoading(true);

        handler.postDelayed(() -> {
            String fileName = scriptTitle + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";

            File directory;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Reader");
            } else {
                directory = new File(Environment.getExternalStorageDirectory(), "Documents/Reader");
            }

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File textFile = new File(directory, fileName);

            try {
                FileOutputStream fos = new FileOutputStream(textFile);
                fos.write(getShareText().getBytes());
                fos.close();

                showLoading(false);
                Toast.makeText(this, "Saved: " + fileName, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                showLoading(false);
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }, 300);
    }

    // ==============================================
    // PRINT WITH WATERMARK - ✅ FIXED
    // ==============================================

    private void printScriptWithWatermark() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

            if (printManager == null) {
                Toast.makeText(this, "Print service not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a custom PrintDocumentAdapter
            PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
                private PrintedPdfDocument pdfDocument;
                private int pageHeight;
                private int pageWidth;

                @Override
                public void onLayout(PrintAttributes oldAttributes,
                                     PrintAttributes newAttributes,
                                     CancellationSignal cancellationSignal,
                                     LayoutResultCallback callback,
                                     Bundle extras) {

                    // Set up page dimensions
                    pageWidth = newAttributes.getMediaSize().getWidthMils() * 72 / 1000;
                    pageHeight = newAttributes.getMediaSize().getHeightMils() * 72 / 1000;

                    if (cancellationSignal.isCanceled()) {
                        callback.onLayoutCancelled();
                        return;
                    }

                    PrintDocumentInfo info = new PrintDocumentInfo.Builder("script.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build();

                    callback.onLayoutFinished(info, true);
                }

                @Override
                public void onWrite(PageRange[] pages,
                                    ParcelFileDescriptor destination,
                                    CancellationSignal cancellationSignal,
                                    WriteResultCallback callback) {

                    try {
                        // Create PDF document with proper print attributes
                        PrintAttributes printAttributes = new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build();

                        pdfDocument = new PrintedPdfDocument(ShareActivity.this, printAttributes);
                        PdfDocument.Page page = pdfDocument.startPage(1);

                        // Get canvas
                        Canvas canvas = page.getCanvas();
                        int pageWidth = page.getInfo().getPageWidth();
                        int pageHeight = page.getInfo().getPageHeight();

                        // === 1. Draw the content ===
                        Paint textPaint = new Paint();
                        textPaint.setColor(Color.BLACK);
                        textPaint.setTextSize(12f);
                        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                        textPaint.setAntiAlias(true);

                        float margin = 40f;
                        float x = margin;
                        float y = margin;
                        float lineHeight = 16f;

                        String fullText = getShareText();
                        String[] lines = fullText.split("\n");

                        // Draw content with word wrap
                        for (String line : lines) {
                            if (y + lineHeight > pageHeight - margin) {
                                break; // Single page only
                            }
                            canvas.drawText(line, x, y, textPaint);
                            y += lineHeight;
                        }

                        // === 2. Draw WATERMARK ===
                        Paint watermarkPaint = new Paint();
                        watermarkPaint.setColor(Color.GRAY);
                        watermarkPaint.setAlpha(WATERMARK_ALPHA);
                        watermarkPaint.setTextSize(WATERMARK_SIZE);
                        watermarkPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                        watermarkPaint.setAntiAlias(true);

                        // Draw diagonal watermark pattern
                        float angle = -25f;
                        canvas.save();
                        canvas.rotate(angle, pageWidth / 2f, pageHeight / 2f);

                        int cols = (int) (pageWidth / WATERMARK_SPACING) + 2;
                        int rows = (int) (pageHeight / WATERMARK_SPACING) + 2;
                        float startX = -pageWidth / 2f;
                        float startY = -pageHeight / 2f;

                        for (int row = 0; row < rows; row++) {
                            for (int col = 0; col < cols; col++) {
                                float wx = startX + col * WATERMARK_SPACING;
                                float wy = startY + row * WATERMARK_SPACING;
                                canvas.drawText(WATERMARK_TEXT, wx, wy, watermarkPaint);
                            }
                        }
                        canvas.restore();

                        // === 3. Finish page ===
                        pdfDocument.finishPage(page);

                        // Write to output
                        pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                        pdfDocument.close();

                        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onWriteFailed(e.getMessage());
                    }
                }

                @Override
                public void onFinish() {
                    if (pdfDocument != null) {
                        pdfDocument.close();
                    }
                }
            };

            try {
                printManager.print("Script with Watermark", adapter, null);
                Toast.makeText(this, "Printing with watermark...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Print failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Print not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    // ==============================================
    // HELPER METHODS
    // ==============================================

    private String getShareText() {
        return scriptTitle + "\n\n" + scriptContent + "\n\n" +
                "Shared from Reader App\n" +
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    }

    // ==============================================
    // UI HELPERS
    // ==============================================

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;

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

    private void performHapticFeedback() {
        View view = getCurrentFocus();
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void startEntranceAnimations() {
        View headerContainer = findViewById(R.id.headerContainer);
        if (headerContainer != null) {
            headerContainer.setAlpha(0f);
            headerContainer.setTranslationY(-60f);
            headerContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        View scriptCard = findViewById(R.id.scriptCard);
        if (scriptCard != null) {
            scriptCard.setAlpha(0f);
            scriptCard.setTranslationY(40f);
            scriptCard.setScaleX(0.95f);
            scriptCard.setScaleY(0.95f);
            scriptCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void finishWithAnimation() {
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

    @Override
    public void onBackPressed() {
        finishWithAnimation();
    }
}