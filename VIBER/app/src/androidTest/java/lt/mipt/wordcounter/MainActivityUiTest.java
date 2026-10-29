package lt.mipt.wordcounter;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real-device UI tests. Every expected text metric below is an independent literal. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class MainActivityUiTest {
    @Rule public final ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test public void initialScreenHasNoResultsOrHistory() {
        onView(withId(R.id.input)).check(matches(withText("")));
        onView(withId(R.id.input_count)).check(matches(withText(inputCount(0))));
        assertResultsHidden();
        assertHistorySize(0);
        onView(withId(R.id.history_empty)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.clear_history)).check(matches(not(isEnabled())));
    }

    @Test public void calculateShowsKnownLithuanianMetricsAndAddsHistory() {
        enterText("Rugsėjo 1 prasidėjo karas. Liūdna");
        calculate();
        assertMetrics(2, 4, 33, 1);
        assertHistorySize(1);
        onView(historyRowStartingWith("Rugsėjo 1 prasidėjo karas. Liūdna"))
                .perform(scrollTo()).check(matches(isDisplayed()))
                .check(matches(withText("Rugsėjo 1 prasidėjo karas. Liūdna\n\nSentences: "
                        + number(2) + " · Words: " + number(4)
                        + "\nCharacters: " + number(33) + " · Numbers: " + number(1))));
        onView(withId(R.id.history_empty)).check(matches(withEffectiveVisibility(GONE)));
    }

    @Test public void decimalAndEmojiUseTwoSentencesAndUnicodeCharacterCount() {
        enterText("Kava 3.50 €. Ačiū 😀!");
        calculate();
        assertMetrics(2, 2, 20, 1);
        // The input limit uses UTF-16 units; the result uses Unicode code points.
        onView(withId(R.id.input_count)).check(matches(withText(inputCount(21))));
    }

    @Test public void emptyAndUnicodeWhitespaceInputShowValidationWithoutHistory() {
        calculate();
        assertValidationError();
        enterText(" \n\t\u00a0\u2003");
        calculate();
        assertValidationError();
        assertResultsHidden();
        assertHistorySize(0);
        onView(withId(R.id.clear_history)).check(matches(not(isEnabled())));
    }

    @Test public void editingInvalidatesResultsAndClearsPreviousValidationError() {
        enterText("Labas rytas");
        calculate();
        assertMetrics(1, 2, 11, 0);
        enterText("Naujas tekstas");
        assertResultsHidden();
        onView(withId(R.id.input_count)).check(matches(withText(inputCount(14))));
        assertHistorySize(1);
        enterText("");
        calculate();
        assertValidationError();
        enterText("Dabar");
        activityRule.getScenario().onActivity(activity ->
                assertNull(((EditText) activity.findViewById(R.id.input)).getError()));
        assertResultsHidden();
    }

    @Test public void generatedExampleBelongsToKnownSamplesAndCalculatesCorrectly() {
        // A random choice is allowed, but each possible outcome has a fixed oracle.
        Map<String, int[]> examples = new HashMap<>();
        examples.put("Good morning", new int[]{1, 2, 12, 0});
        examples.put("The journey started on September 1. We arrived safely.",
                new int[]{2, 8, 54, 1});
        examples.put("I read 12 pages today. Tomorrow I will read 20!",
                new int[]{2, 8, 47, 2});
        examples.put("Coffee costs 3.50 euros. Would you like 2 cups?",
                new int[]{2, 7, 47, 2});
        examples.put("Are you ready? Yes! Let us start assignment 2.",
                new int[]{3, 8, 46, 1});
        examples.put("An oak grows by the river. There are 5 friends here.",
                new int[]{2, 10, 52, 1});
        clickButton(R.id.generate);
        AtomicReference<String> generated = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> generated.set(
                ((EditText) activity.findViewById(R.id.input)).getText().toString()));
        int[] expected = examples.get(generated.get());
        assertNotNull("Generate example must choose one of the six documented samples", expected);
        onView(withId(R.id.input_count))
                .check(matches(withText(inputCount(generated.get().length()))));
        assertResultsHidden();
        assertHistorySize(0);
        calculate();
        assertMetrics(expected[0], expected[1], expected[2], expected[3]);
        assertHistorySize(1);
    }

    @Test public void selectingOlderHistoryRestoresItsTextAndMetricsWithoutAddingEntry() {
        enterText("Labas rytas");
        calculate();
        enterText("Hello 2!");
        calculate();
        assertMetrics(1, 1, 8, 1);
        onView(historyRowStartingWith("Labas rytas")).perform(scrollTo(), click());
        onView(withId(R.id.input)).check(matches(withText("Labas rytas")));
        assertMetrics(1, 2, 11, 0);
        assertHistorySize(2);
    }

    @Test public void clearRemovesHistoryAndPreservesCurrentCalculation() {
        enterText("Labas rytas");
        calculate();
        enterText("Hello 2!");
        calculate();
        clickButton(R.id.clear_history);
        assertHistorySize(0);
        onView(withId(R.id.history_empty)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.clear_history)).check(matches(not(isEnabled())));
        onView(withId(R.id.input)).check(matches(withText("Hello 2!")));
        assertMetrics(1, 1, 8, 1);
    }

    @Test public void historyKeepsOnlyThirtyNewestCalculations() {
        for (int index = 1; index <= 31; index++) {
            enterText("Entry " + index + ".");
            calculate();
        }
        assertHistorySize(30);
        onView(withId(R.id.history_list)).check((view, error) -> {
            if (error != null) throw error;
            ViewGroup history = (ViewGroup) view;
            assertTrue(((TextView) history.getChildAt(0)).getText().toString()
                    .startsWith("Entry 31."));
            assertTrue(((TextView) history.getChildAt(29)).getText().toString()
                    .startsWith("Entry 2."));
            for (int index = 0; index < history.getChildCount(); index++) {
                assertFalse(((TextView) history.getChildAt(index)).getText().toString()
                        .startsWith("Entry 1."));
            }
        });
    }

    @Test public void inputLimitRejectsExtraCharactersAndDoesNotSplitEmoji() {
        String almostFull = repeated('a', 4999);
        enterText(almostFull + "😀");
        onView(withId(R.id.input)).check(matches(withText(almostFull)));
        onView(withId(R.id.input_count)).check(matches(withText(inputCount(4999))));
        String maximum = repeated('a', 5000);
        // A multiline EditText may now be taller than the viewport. It is already
        // visible, so avoid scrollTo(), which requires 90% of its height on screen.
        onView(withId(R.id.input)).perform(replaceText(maximum + "extra"), closeSoftKeyboard());
        onView(withId(R.id.input)).check(matches(withText(maximum)));
        onView(withId(R.id.input_count)).check(matches(withText(inputCount(5000))));
        calculate();
        assertMetrics(1, 1, 5000, 0);
    }

    @Test public void historyPreviewPreservesEmojiAtItsCodePointBoundary() {
        String prefix = repeated('a', 139) + "😀";
        String completeText = prefix + "tail";
        enterText(completeText);
        calculate();
        assertMetrics(1, 2, 144, 0);
        onView(historyRowStartingWith(prefix + "…"))
                .perform(scrollTo()).check(matches(isDisplayed()));
        onView(historyRowStartingWith(prefix + "…")).perform(click());
        onView(withId(R.id.input)).check(matches(withText(completeText)));
        assertMetrics(1, 2, 144, 0);
    }

    @Test public void recreationRestoresInputResultsAndHistory() {
        enterText("Labas rytas");
        calculate();
        enterText("Hello 2!");
        calculate();
        activityRule.getScenario().recreate();
        onView(withId(R.id.input)).check(matches(withText("Hello 2!")));
        assertMetrics(1, 1, 8, 1);
        assertHistorySize(2);
        onView(historyRowStartingWith("Labas rytas")).perform(scrollTo(), click());
        onView(withId(R.id.input)).check(matches(withText("Labas rytas")));
        assertMetrics(1, 2, 11, 0);
    }

    @Test public void screenRotationRestoresCalculationAndHistoryOnTheDevice() {
        enterText("Labas rytas");
        calculate();
        requestOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                Configuration.ORIENTATION_LANDSCAPE);
        onView(withId(R.id.input)).check(matches(withText("Labas rytas")));
        assertMetrics(1, 2, 11, 0);
        assertHistorySize(1);
        requestOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                Configuration.ORIENTATION_PORTRAIT);
        onView(withId(R.id.input)).check(matches(withText("Labas rytas")));
        assertMetrics(1, 2, 11, 0);
        assertHistorySize(1);
    }

    private void enterText(String text) {
        onView(isRoot()).perform(closeSoftKeyboard());
        onView(withId(R.id.input)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }

    private void calculate() {
        clickButton(R.id.calculate);
    }

    private void clickButton(int id) {
        onView(isRoot()).perform(closeSoftKeyboard());
        onView(withId(id)).perform(scrollTo(), click());
    }

    private void assertMetrics(int sentences, int words, int characters, int numbers) {
        onView(withId(R.id.results_heading)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.results)).check(matches(withEffectiveVisibility(VISIBLE)));
        onView(withId(R.id.sentences)).check(matches(withText(number(sentences))));
        onView(withId(R.id.words)).check(matches(withText(number(words))));
        onView(withId(R.id.characters)).check(matches(withText(number(characters))));
        onView(withId(R.id.numbers)).check(matches(withText(number(numbers))));
    }

    private void assertResultsHidden() {
        onView(withId(R.id.results)).check(matches(withEffectiveVisibility(GONE)));
    }

    private void assertValidationError() {
        onView(withId(R.id.input)).check(matches(
                hasErrorText("Enter some text. Spaces alone are not enough.")));
    }

    private void assertHistorySize(int expected) {
        onView(withId(R.id.history_list)).check((view, error) -> {
            if (error != null) throw error;
            assertEquals(expected, ((ViewGroup) view).getChildCount());
        });
    }

    private void requestOrientation(int requested, int expectedConfiguration) {
        activityRule.getScenario().onActivity(activity -> activity.setRequestedOrientation(requested));
        long deadline = SystemClock.uptimeMillis() + 15000;
        AtomicInteger actual = new AtomicInteger(Configuration.ORIENTATION_UNDEFINED);
        while (SystemClock.uptimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            actual.set(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    .getResources().getConfiguration().orientation);
            if (actual.get() == expectedConfiguration) return;
            SystemClock.sleep(50);
        }
        assertEquals("Device must apply the requested screen orientation",
                expectedConfiguration, actual.get());
    }

    private static Matcher<View> historyRowStartingWith(String prefix) {
        return allOf(isDescendantOfA(withId(R.id.history_list)), withText(startsWith(prefix)));
    }

    private static String number(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    private static String inputCount(int value) {
        return String.format(Locale.getDefault(), "%d / %d", value, 5000);
    }

    private static String repeated(char value, int count) {
        char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
