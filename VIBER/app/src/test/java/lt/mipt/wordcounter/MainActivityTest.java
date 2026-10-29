package lt.mipt.wordcounter;

import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.*;

/** Local JVM tests of the real Activity, using Android framework shadows. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private Locale previousLocale;

    @Before public void createActivity() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        start(null);
    }

    @After public void destroyActivity() {
        controller.pause().stop().destroy();
        Locale.setDefault(previousLocale);
    }

    @Test public void startsWithAnEmptyInputHiddenResultsAndEmptyHistory() {
        assertEquals("", input().getText().toString());
        assertEquals("0 / 5000", text(R.id.input_count));
        assertEquals(View.GONE, view(R.id.results).getVisibility());
        assertEquals(View.VISIBLE, view(R.id.history_empty).getVisibility());
        assertEquals(0, history().getChildCount());
        assertFalse(view(R.id.clear_history).isEnabled());
        assertTrue(view(R.id.results_heading).isAccessibilityHeading());
        assertTrue(view(R.id.history_heading).isAccessibilityHeading());
    }

    @Test public void calculatesKnownMetricsAndProvidesAccessibleLabels() {
        calculate("Labas rytas");
        assertMetrics("1", "2", "11", "0");
        assertEquals("Words: 2", view(R.id.words).getContentDescription().toString());
        assertEquals(1, history().getChildCount());
        assertEquals(View.GONE, view(R.id.history_empty).getVisibility());
        assertTrue(view(R.id.clear_history).isEnabled());
        assertTrue(((TextView) history().getChildAt(0)).getText().toString().startsWith("Labas rytas"));
    }

    @Test public void rejectsBothEmptyAndUnicodeWhitespaceWithoutAddingHistory() {
        view(R.id.calculate).performClick();
        assertEquals(activity.getString(R.string.empty_error), input().getError().toString());
        assertTrue(input().hasFocus());
        input().setText(" \t\u00a0\u2003");
        assertNull(input().getError());
        view(R.id.calculate).performClick();
        assertEquals(activity.getString(R.string.empty_error), input().getError().toString());
        assertEquals(0, history().getChildCount());
        assertEquals(View.GONE, view(R.id.results).getVisibility());
    }

    @Test public void editingInvalidatesTheVisibleAndSavedResult() {
        calculate("Labas rytas");
        input().setText("Kitas");
        assertEquals(View.GONE, view(R.id.results).getVisibility());
        assertEquals("5 / 5000", text(R.id.input_count));
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        assertEquals("Kitas", state.getString("input"));
        assertNull(state.getString("result"));
        assertEquals(Arrays.asList("Labas rytas"), state.getStringArrayList("history"));
    }

    @Test public void inputFilterEnforces5000Utf16Units() {
        input().setText("x".repeat(5001));
        assertEquals(5000, input().length());
        assertEquals("5000 / 5000", text(R.id.input_count));
        view(R.id.calculate).performClick();
        assertMetrics("1", "1", "5000", "0");
    }

    @Test public void generatingAnExampleChoosesAKnownSampleAndMovesCursorToEnd() {
        calculate("Old result");
        view(R.id.generate).performClick();
        assertTrue(Arrays.asList(activity.getResources().getStringArray(R.array.sample_sentences))
                .contains(input().getText().toString()));
        assertEquals(input().length(), input().getSelectionStart());
        assertEquals(input().length(), input().getSelectionEnd());
        assertEquals(View.GONE, view(R.id.results).getVisibility());
        assertEquals(1, history().getChildCount());
    }

    @Test public void historyKeepsTheLatest30EntriesInReverseOrder() {
        for (int index = 0; index <= 30; index++) calculate("Entry " + index);
        assertEquals(30, history().getChildCount());
        assertTrue(((TextView) history().getChildAt(0)).getText().toString().startsWith("Entry 30"));
        assertTrue(((TextView) history().getChildAt(29)).getText().toString().startsWith("Entry 1"));
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        ArrayList<String> saved = state.getStringArrayList("history");
        assertNotNull(saved);
        assertEquals(30, saved.size());
        assertEquals("Entry 30", saved.get(0));
        assertEquals("Entry 1", saved.get(29));
        assertFalse(saved.contains("Entry 0"));
    }

    @Test public void historyPreviewPreservesUnicodeAndClickRestoresTheWholeInput() {
        String original = "a".repeat(139) + "\ud83d\ude00" + "b";
        calculate(original);
        TextView row = (TextView) history().getChildAt(0);
        String preview = "a".repeat(139) + "\ud83d\ude00…";
        assertTrue(row.getText().toString().startsWith(preview + "\n\n"));
        assertTrue(row.getText().toString().contains("\nCharacters: 141 · Numbers: 0"));
        assertTrue(row.isFocusable());
        assertTrue(row.getContentDescription().toString().startsWith("Restore text: " + preview));
        assertEquals(Math.round(48 * activity.getResources().getDisplayMetrics().density),
                row.getMinHeight());
        input().setText("Changed");
        row.performClick();
        assertEquals(original, input().getText().toString());
        assertEquals(original.length(), input().getSelectionStart());
        assertMetrics("1", "2", "141", "0");
        assertEquals(1, history().getChildCount());
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        assertEquals(original, state.getString("result"));
    }

    @Test public void clearingHistoryDisablesClearAndShowsConfirmation() {
        calculate("Keep this input");
        view(R.id.clear_history).performClick();
        assertEquals(0, history().getChildCount());
        assertEquals(View.VISIBLE, view(R.id.history_empty).getVisibility());
        assertFalse(view(R.id.clear_history).isEnabled());
        assertEquals(activity.getString(R.string.history_cleared), ShadowToast.getTextOfLatestToast());
        assertEquals("Keep this input", input().getText().toString());
        assertEquals(View.VISIBLE, view(R.id.results).getVisibility());
    }

    @Test public void savedStateRestoresInputHistoryAndTheCalculatedResult() {
        calculate("First");
        calculate("Labas rytas");
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        assertEquals("Labas rytas", state.getString("result"));
        replaceActivity(state);
        assertEquals("Labas rytas", input().getText().toString());
        assertEquals("11 / 5000", text(R.id.input_count));
        assertMetrics("1", "2", "11", "0");
        assertEquals(2, history().getChildCount());
        assertTrue(((TextView) history().getChildAt(1)).getText().toString().startsWith("First"));
    }

    @Test public void savedUncalculatedInputDoesNotRestoreAStaleResult() {
        calculate("Old");
        input().setText("Draft");
        Bundle state = new Bundle();
        controller.saveInstanceState(state);
        replaceActivity(state);
        assertEquals("Draft", input().getText().toString());
        assertEquals(View.GONE, view(R.id.results).getVisibility());
        assertEquals(1, history().getChildCount());
    }

    @Test public void incompleteSavedStateFallsBackToEmptyInputAndHistory() {
        replaceActivity(new Bundle());
        assertEquals("", input().getText().toString());
        assertEquals(0, history().getChildCount());
        assertEquals(View.GONE, view(R.id.results).getVisibility());
    }

    @Test @Config(sdk = 23) public void legacyAndroidUsesSystemWindowInsets() throws ReflectiveOperationException {
        View page = view(R.id.page);
        // The API 23 constructor is absent from newer SDK compile-time stubs.
        java.lang.reflect.Constructor<WindowInsets> constructor =
                WindowInsets.class.getDeclaredConstructor(Rect.class);
        constructor.setAccessible(true);
        WindowInsets supplied = constructor.newInstance(new Rect(3, 24, 5, 48));
        page.dispatchApplyWindowInsets(supplied);
        assertEquals(3, page.getPaddingLeft());
        assertEquals(24, page.getPaddingTop());
        assertEquals(5, page.getPaddingRight());
        assertEquals(48, page.getPaddingBottom());
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertTrue((flags & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0);
        assertTrue((flags & View.SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0);
    }

    @Test @Config(sdk = 30) public void modernAndroidUsesTheLargerKeyboardBottomInset() {
        View page = view(R.id.page);
        WindowInsets supplied = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.systemBars(), Insets.of(3, 24, 5, 48))
                .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, 220))
                .build();
        page.dispatchApplyWindowInsets(supplied);
        assertEquals(3, page.getPaddingLeft());
        assertEquals(24, page.getPaddingTop());
        assertEquals(5, page.getPaddingRight());
        assertEquals(220, page.getPaddingBottom());
        WindowInsets keyboardHidden = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.systemBars(), Insets.of(3, 24, 5, 48))
                .setInsets(WindowInsets.Type.ime(), Insets.NONE)
                .build();
        page.dispatchApplyWindowInsets(keyboardHidden);
        assertEquals(48, page.getPaddingBottom());
    }

    private void start(Bundle state) {
        controller = Robolectric.buildActivity(MainActivity.class);
        controller.create(state).start().resume().visible();
        activity = controller.get();
    }

    private void replaceActivity(Bundle state) {
        controller.pause().stop().destroy();
        start(state);
    }

    private View view(int id) { return activity.findViewById(id); }
    private EditText input() { return activity.findViewById(R.id.input); }
    private LinearLayout history() { return activity.findViewById(R.id.history_list); }
    private String text(int id) { return ((TextView) view(id)).getText().toString(); }
    private void calculate(String text) {
        input().setText(text);
        view(R.id.calculate).performClick();
    }
    private void assertMetrics(String sentences, String words, String characters, String numbers) {
        assertEquals(View.VISIBLE, view(R.id.results).getVisibility());
        assertEquals(sentences, text(R.id.sentences));
        assertEquals(words, text(R.id.words));
        assertEquals(characters, text(R.id.characters));
        assertEquals(numbers, text(R.id.numbers));
    }
}
