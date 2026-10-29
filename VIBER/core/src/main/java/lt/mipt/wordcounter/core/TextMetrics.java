package lt.mipt.wordcounter.core;

/** Immutable calculation result; deliberately independent of Android. */
public final class TextMetrics {
    public final int sentences;
    public final int words;
    public final int characters;
    public final int numbers;

    public TextMetrics(int sentences, int words, int characters, int numbers) {
        this.sentences = sentences;
        this.words = words;
        this.characters = characters;
        this.numbers = numbers;
    }
}
