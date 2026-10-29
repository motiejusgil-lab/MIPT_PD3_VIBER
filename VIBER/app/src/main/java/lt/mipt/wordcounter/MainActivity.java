package lt.mipt.wordcounter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import lt.mipt.wordcounter.core.TextAnalyzer;
import lt.mipt.wordcounter.core.TextMetrics;
import lt.mipt.wordcounter.core.TextPreview;

public final class MainActivity extends Activity {
    private static final int MAX_INPUT = 5000;
    private static final int MAX_HISTORY = 30;
    private final TextAnalyzer analyzer = new TextAnalyzer();
    private final ArrayList<String> history = new ArrayList<>();
    private final Random random = new Random();
    private EditText input;
    private LinearLayout historyList;
    private View results;
    private String lastAnalyzed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 28) {
            findViewById(R.id.results_heading).setAccessibilityHeading(true);
            findViewById(R.id.history_heading).setAccessibilityHeading(true);
        }
        configureInsets();
        input = findViewById(R.id.input);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_INPUT)});
        results = findViewById(R.id.results);
        historyList = findViewById(R.id.history_list);
        if (state != null) {
            input.setText(state.getString("input", ""));
            ArrayList<String> saved = state.getStringArrayList("history");
            if (saved != null) history.addAll(saved);
            lastAnalyzed = state.getString("result");
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                input.setError(null);
                lastAnalyzed = null;
                results.setVisibility(View.GONE);
                updateInputCount();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.calculate).setOnClickListener(v -> calculate());
        findViewById(R.id.generate).setOnClickListener(v -> {
            String[] samples = getResources().getStringArray(R.array.sample_sentences);
            input.setText(samples[random.nextInt(samples.length)]);
            input.setSelection(input.length());
        });
        findViewById(R.id.clear_history).setOnClickListener(v -> {
            history.clear();
            renderHistory();
            Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
        });
        updateInputCount();
        renderHistory();
        if (lastAnalyzed != null) showMetrics(analyzer.analyze(lastAnalyzed));
    }

    private void calculate() {
        String text = input.getText().toString();
        if (TextAnalyzer.isBlank(text)) {
            input.setError(getString(R.string.empty_error));
            input.requestFocus();
            return;
        }
        TextMetrics metrics = analyzer.analyze(text);
        lastAnalyzed = text;
        showMetrics(metrics);
        history.add(0, text);
        if (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        renderHistory();
    }

    private void updateInputCount() {
        ((TextView) findViewById(R.id.input_count)).setText(getString(R.string.input_count, input.length(), MAX_INPUT));
    }

    private void showMetrics(TextMetrics metrics) {
        setValue(R.id.sentences, metrics.sentences, R.string.sentences);
        setValue(R.id.words, metrics.words, R.string.words);
        setValue(R.id.characters, metrics.characters, R.string.characters);
        setValue(R.id.numbers, metrics.numbers, R.string.numbers);
        results.setVisibility(View.VISIBLE);
    }

    private void setValue(int id, int value, int label) {
        TextView view = findViewById(id);
        String formatted = String.format(Locale.getDefault(), "%d", value);
        view.setText(formatted);
        view.setContentDescription(getString(R.string.metric_accessibility, getString(label), formatted));
    }

    private void renderHistory() {
        historyList.removeAllViews();
        findViewById(R.id.history_empty).setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.clear_history).setEnabled(!history.isEmpty());
        for (String text : history) {
            TextMetrics metrics = analyzer.analyze(text);
            TextView row = new TextView(this);
            String preview = TextPreview.shorten(text, 140);
            String metricText = getString(R.string.history_metrics,
                    metrics.sentences, metrics.words, metrics.characters, metrics.numbers);
            row.setText(getString(R.string.history_row, preview, metricText));
            row.setTextColor(getColor(R.color.ink));
            row.setTextSize(15);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setBackgroundResource(R.drawable.bg_card);
            row.setMinHeight(dp(48));
            row.setFocusable(true);
            row.setContentDescription(getString(R.string.history_accessibility,
                    getString(R.string.history_load, preview), metricText));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(12);
            row.setLayoutParams(params);
            row.setOnClickListener(v -> {
                input.setText(text);
                input.setSelection(input.length());
                lastAnalyzed = text;
                showMetrics(metrics);
                ((ScrollView) findViewById(R.id.page)).smoothScrollTo(0, 0);
            });
            historyList.addView(row);
        }
    }

    private void configureInsets() {
        View page = findViewById(R.id.page);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        page.requestApplyInsets();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("input", input.getText().toString());
        state.putStringArrayList("history", new ArrayList<>(history));
        state.putString("result", lastAnalyzed);
        super.onSaveInstanceState(state);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
