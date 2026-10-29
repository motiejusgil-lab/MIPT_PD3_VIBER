package lt.mipt.wordcounter.core;

/** Creates a bounded display preview without splitting UTF-16 surrogate pairs. */
public final class TextPreview {
    private TextPreview() {}
    public static String shorten(String text, int maxCodePoints) {
        if (maxCodePoints < 0) throw new IllegalArgumentException("Negative preview length");
        if (text.codePointCount(0, text.length()) <= maxCodePoints) return text;
        return text.substring(0, text.offsetByCodePoints(0, maxCodePoints)) + "…";
    }
}
