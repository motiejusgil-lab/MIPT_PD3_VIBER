package lt.mipt.wordcounter.core;

import org.junit.Test;

import static org.junit.Assert.*;

public final class TextAnalyzerTest {
    private final TextAnalyzer analyzer = new TextAnalyzer();

    @Test public void rejectsNullEmptyAndUnicodeWhitespace() {
        for (String text : new String[]{null, "", " ", "\t\n\r", "\u00a0\u2003\u202f"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.analyze(text));
            assertEquals("Text must not be blank", error.getMessage());
        }
    }

    @Test public void blankDetectionAcceptsBothJavaWhitespaceAndSpaceCharacters() {
        assertTrue(TextAnalyzer.isBlank(null));
        assertTrue(TextAnalyzer.isBlank(""));
        assertTrue(TextAnalyzer.isBlank("\t\n\r \u00a0\u2003\u202f"));
        assertFalse(TextAnalyzer.isBlank(" x"));
        assertFalse(TextAnalyzer.isBlank("\ud83d\ude00"));
        // A zero-width space is neither Character.isWhitespace nor isSpaceChar.
        assertFalse(TextAnalyzer.isBlank("\u200b"));
    }

    @Test public void calculatesTheFirstDocumentedExample() {
        assertMetrics("Labas rytas", 1, 2, 11, 0);
    }

    @Test public void calculatesTheSecondDocumentedExample() {
        assertMetrics("Rugsėjo 1 prasidėjo karas. Liūdna", 2, 4, 33, 1);
    }

    @Test public void ignoresRepeatedPunctuationAndCountsUnterminatedFinalSentence() {
        assertMetrics("Labas!!! Kaip? Gerai… Pabaiga", 4, 4, 29, 0);
    }

    @Test public void decimalsAreNumbersAndDoNotSplitSentences() {
        assertMetrics("Kaina -3.50 ir +2,5. Likutis 0!", 2, 3, 31, 3);
    }

    @Test public void countsApostrophesHyphensAndCombiningMarksInsideWords() {
        String text = "can't l’homme well-being cafe\u0301";
        assertMetrics(text, 1, 4, 30, 0);
    }

    @Test public void supportsNonLatinLettersAndUnicodeDecimalDigits() {
        assertMetrics("你好 мир ١٢.", 1, 2, 10, 1);
    }

    @Test public void countsSupplementaryCharactersAsOneCodePoint() {
        assertMetrics("A\ud83d\ude00 B", 1, 2, 4, 0);
        // U+10400 is a supplementary Unicode letter, not two words or characters.
        assertMetrics("\ud801\udc00", 1, 1, 1, 0);
    }

    @Test public void digitsAloneAreNotWords() {
        assertMetrics("12 -7 +3,14 0.25", 1, 0, 16, 4);
    }

    @Test public void punctuationAndEmojiHaveCharactersButNoSentencesOrWords() {
        assertMetrics("...!?… \ud83d\ude00", 0, 0, 8, 0);
    }

    @Test public void separatorsBetweenSentencesMayContainOnlyWhitespace() {
        assertMetrics(". A. \n ! B?", 2, 2, 11, 0);
    }

    @Test public void repeatedCallsDoNotRetainPreviousMetrics() {
        assertMetrics("Vienas du 3.", 1, 2, 12, 1);
        assertMetrics("X", 1, 1, 1, 0);
    }

    @Test public void handlesTheMaximumAppInputLength() {
        String text = "a ".repeat(2500);
        assertMetrics(text, 1, 2500, 5000, 0);
    }

    private void assertMetrics(String text, int sentences, int words, int characters, int numbers) {
        TextMetrics result = analyzer.analyze(text);
        assertEquals("Sentences for: " + text, sentences, result.sentences);
        assertEquals("Words for: " + text, words, result.words);
        assertEquals("Characters for: " + text, characters, result.characters);
        assertEquals("Numbers for: " + text, numbers, result.numbers);
    }
}
