package lt.mipt.wordcounter.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stateless, reusable Unicode text analysis. */
public final class TextAnalyzer {
    private static final Pattern WORD = Pattern.compile(
            "\\p{L}[\\p{L}\\p{M}]*+(?:['’\\-][\\p{L}\\p{M}]++)*+");
    private static final Pattern NUMBER = Pattern.compile("[+-]?\\p{Nd}+(?:[.,]\\p{Nd}+)?");
    private static final Pattern CONTENT = Pattern.compile("[\\p{L}\\p{N}]");

    public TextMetrics analyze(String text) {
        if (text == null || isBlank(text)) {
            throw new IllegalArgumentException("Text must not be blank");
        }
        // Decimal separators belong to numbers, not sentence boundaries.
        String sentenceText = NUMBER.matcher(text).replaceAll("0");
        int sentences = 0;
        for (String part : sentenceText.split("[.!?…]+", -1)) {
            if (CONTENT.matcher(part).find()) sentences++;
        }
        return new TextMetrics(sentences, count(WORD, text),
                text.codePointCount(0, text.length()), count(NUMBER, text));
    }

    public static boolean isBlank(String text) {
        if (text == null) return true;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    private static int count(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) count++;
        return count;
    }
}
