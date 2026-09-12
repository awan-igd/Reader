package reader.aigd.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import reader.aigd.R;
import reader.aigd.model.DataManager;
import reader.aigd.model.Script;

public class ShareActivity extends AppCompatActivity {

    private static final int WATERMARK_ALPHA = 38;
    private static final int WATERMARK_SPACING = 90;
    private TextView scriptTitlePreview, scriptContentPreview, scriptWordCount, headerTitle, headerSubtitle;
    private FrameLayout loadingOverlay;

    private DataManager dataManager;
    private long scriptId = -1;
    private String scriptContent = "";
    private String scriptTitle = "";
    private MaterialCardView backButton, shareButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String WATERMARK_TEXT = "Reader by Awan IGD";
    private static final float WATERMARK_SIZE = 14f;
    private MaterialCardView shareCopy, shareSave, sharePrint;
    private Typeface aigdFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        aigdFont = ResourcesCompat.getFont(this, R.font.aigd);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.setAppearanceLightStatusBars(false);
            c.setAppearanceLightNavigationBars(false);
        }

        dataManager = DataManager.getInstance(this);
        initViews();
        getIntentData();
        setupUI();
        setupClicks();
        applyFonts();
    }

    private void initViews() {
        scriptTitlePreview = findViewById(R.id.scriptTitlePreview);
        scriptContentPreview = findViewById(R.id.scriptContentPreview);
        scriptWordCount = findViewById(R.id.scriptWordCount);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
        shareCopy = findViewById(R.id.shareCopy);
        shareSave = findViewById(R.id.shareSave);
        sharePrint = findViewById(R.id.sharePrint);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void applyFonts() {
        if (scriptTitlePreview != null) scriptTitlePreview.setTypeface(aigdFont);
        if (scriptContentPreview != null) scriptContentPreview.setTypeface(aigdFont);
        if (scriptWordCount != null) scriptWordCount.setTypeface(aigdFont);
        if (headerTitle != null) headerTitle.setTypeface(aigdFont);
        if (headerSubtitle != null) headerSubtitle.setTypeface(aigdFont);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            scriptId = intent.getLongExtra("script_id", -1);
            scriptContent = intent.getStringExtra("script_content");
            scriptTitle = intent.getStringExtra("script_title");
            if (scriptId != -1) {
                Script s = dataManager.getScriptById(scriptId);
                if (s != null) {
                    scriptContent = s.getContent();
                    scriptTitle = s.getTitle();
                }
            }
        }
        if (scriptContent == null || scriptContent.isEmpty())
            scriptContent = "No script content available.";
        if (scriptTitle == null || scriptTitle.isEmpty()) scriptTitle = "Untitled Script";
    }

    private void setupUI() {
        if (scriptTitlePreview != null) scriptTitlePreview.setText(scriptTitle);
        if (scriptContentPreview != null) {
            String preview = scriptContent.length() > 400 ? scriptContent.substring(0, 400) + "..." : scriptContent;
            scriptContentPreview.setText(preview);
        }
        if (scriptWordCount != null) {
            int wc = scriptContent.trim().isEmpty() ? 0 : scriptContent.trim().split("\\s+").length;
            scriptWordCount.setText(wc + " words • " + scriptContent.length() + " chars • 9/11/2026");
        }
        if (headerTitle != null) headerTitle.setText(scriptTitle);
        if (headerSubtitle != null) headerSubtitle.setText("Tap share icon to share via any app");
    }

    private void setupClicks() {
        if (backButton != null) backButton.setOnClickListener(v -> {
            haptic(v);
            anim(v);
            finish();
        });
        if (shareButton != null) shareButton.setOnClickListener(v -> {
            haptic(v);
            anim(v);
            shareViaSystem();
        });
        if (shareCopy != null) shareCopy.setOnClickListener(v -> {
            haptic(v);
            anim(v);
            copyToClipboard();
        });
        if (shareSave != null) shareSave.setOnClickListener(v -> {
            haptic(v);
            anim(v);
            saveToFile();
        });
        if (sharePrint != null) sharePrint.setOnClickListener(v -> {
            haptic(v);
            anim(v);
            printWithWatermark();
        });
    }

    private void shareViaSystem() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, scriptTitle);
        i.putExtra(Intent.EXTRA_TEXT, getShareText());
        startActivity(Intent.createChooser(i, "Share Script"));
    }

    private void copyToClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(scriptTitle, getShareText()));
        toast("Copied to clipboard!");
    }

    private void saveToFile() {
        showLoading(true);
        handler.postDelayed(() -> {
            String safeTitle = scriptTitle.replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = safeTitle + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Reader");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(getShareText().getBytes());
                showLoading(false);
                toast("Saved to Documents/Reader/" + fileName);
            } catch (IOException e) {
                showLoading(false);
                toast("Failed to save");
            }
        }, 400);
    }

    private void printWithWatermark() {
        PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (pm == null) {
            toast("Print not available");
            return;
        }

        PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
            PrintedPdfDocument pdfDoc;

            @Override
            public void onLayout(PrintAttributes oldA, PrintAttributes newA, CancellationSignal cs, LayoutResultCallback cb, Bundle extras) {
                if (cs.isCanceled()) {
                    cb.onLayoutCancelled();
                    return;
                }
                PrintDocumentInfo info = new PrintDocumentInfo.Builder("script.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build();
                cb.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor dest, CancellationSignal cs, WriteResultCallback cb) {
                try {
                    PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
                    pdfDoc = new PrintedPdfDocument(ShareActivity.this, attrs);
                    android.graphics.pdf.PdfDocument.Page page = pdfDoc.startPage(1);
                    Canvas canvas = page.getCanvas();
                    int pw = page.getInfo().getPageWidth();
                    int ph = page.getInfo().getPageHeight();

                    Paint tp = new Paint();
                    tp.setColor(Color.BLACK);
                    tp.setTextSize(12f);
                    tp.setAntiAlias(true);
                    float margin = 40f, y = margin, lh = 18f;
                    for (String line : getShareText().split("\n")) {
                        if (y > ph - margin) break;
                        canvas.drawText(line, margin, y, tp);
                        y += lh;
                    }

                    Paint wp = new Paint();
                    wp.setColor(Color.GRAY);
                    wp.setAlpha(WATERMARK_ALPHA);
                    wp.setTextSize(WATERMARK_SIZE);
                    wp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    wp.setAntiAlias(true);
                    canvas.save();
                    canvas.rotate(-25f, pw / 2f, ph / 2f);
                    float sx = -pw / 2f, sy = -ph / 2f;
                    int cols = pw / WATERMARK_SPACING + 4, rows = ph / WATERMARK_SPACING + 4;
                    for (int r = 0; r < rows; r++)
                        for (int c = 0; c < cols; c++) {
                            canvas.drawText(WATERMARK_TEXT, sx + c * WATERMARK_SPACING * 4.2f, sy + r * WATERMARK_SPACING * 2.2f, wp);
                        }
                    canvas.restore();

                    pdfDoc.finishPage(page);
                    pdfDoc.writeTo(new FileOutputStream(dest.getFileDescriptor()));
                    pdfDoc.close();
                    cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (Exception e) {
                    cb.onWriteFailed(e.getMessage());
                }
            }

            @Override
            public void onFinish() {
                if (pdfDoc != null) pdfDoc.close();
            }
        };
        pm.print("Reader Script", adapter, null);
    }

    private String getShareText() {
        return scriptTitle + "\n\n" + scriptContent + "\n\n— Shared from Reader App by Awan IGD\n9/11/2026";
    }

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;
        if (show) {
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.setAlpha(0f);
            loadingOverlay.animate().alpha(1f).setDuration(180).start();
        } else {
            loadingOverlay.animate().alpha(0f).setDuration(180).withEndAction(() -> loadingOverlay.setVisibility(View.GONE)).start();
        }
    }

    private void anim(View v) {
        if (v == null) return;
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
    }

    private void haptic(View v) {
        if (v != null) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}