package reader.aigd.activity;

import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppSDK;

import reader.aigd.R;
import reader.aigd.model.DataManager;
import reader.aigd.model.Script;

public class AddScriptActivity extends AppCompatActivity {

    private TextInputEditText contentInput;
    private MaterialCardView backButton, clearButton, pasteButton, loadingOverlay;
    private FloatingActionButton fabSave;
    private Banner banner;

    private DataManager dataManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long editId = -1;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_script);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.b));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.b));
        StartAppSDK.init(this, "206823693", false);

        dataManager = DataManager.getInstance(this);
        checkEditMode();
        initViews();
        setup();
    }

    private void checkEditMode() {
        Intent i = getIntent();
        if (i != null && i.hasExtra("script_id")) {
            isEditing = true;
            editId = i.getLongExtra("script_id", -1);
        }
    }

    private void initViews() {
        contentInput = findViewById(R.id.contentInput);
        backButton = findViewById(R.id.backButton);
        clearButton = findViewById(R.id.clearButton);
        pasteButton = findViewById(R.id.pasteButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        fabSave = findViewById(R.id.fabSave);
        banner = findViewById(R.id.startAppBanner);
        if (banner != null) banner.loadAd();

        if (isEditing) {
            String content = getIntent().getStringExtra("script_content");
            if (content != null) contentInput.setText(content);
        }
    }

    private void setup() {
        backButton.setOnClickListener(v -> finish());

        clearButton.setOnClickListener(v -> {
            contentInput.setText("");
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
        });

        pasteButton.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                String paste = cm.getPrimaryClip().getItemAt(0).coerceToText(this).toString();
                int start = contentInput.getSelectionStart();
                if (contentInput.getText() != null) {
                    contentInput.getText().insert(start, paste);
                }
            }
        });

        fabSave.setOnClickListener(v -> save());

        contentInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fabSave.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void save() {
        String content = contentInput.getText() != null ? contentInput.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIX: Make final for lambda
        String tempTitle = content.split("\n")[0].trim();
        if (tempTitle.length() > 35) tempTitle = tempTitle.substring(0, 35) + "...";
        if (tempTitle.isEmpty()) tempTitle = "Untitled Script";

        final String finalTitle = tempTitle;
        final String finalContent = content;

        loadingOverlay.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            if (isEditing) {
                Script sc = dataManager.getScriptById(editId);
                if (sc != null) {
                    sc.setTitle(finalTitle);
                    sc.setContent(finalContent);
                    sc.updateTimestamp();
                    dataManager.updateScript(sc);
                }
            } else {
                Script newScript = new Script(finalTitle, finalContent);
                dataManager.saveScript(newScript);
            }

            loadingOverlay.setVisibility(View.GONE);
            setResult(RESULT_OK);
            finish();
        }, 400);
    }
}