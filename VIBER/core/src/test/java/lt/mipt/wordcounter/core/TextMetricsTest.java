package lt.mipt.wordcounter.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;

import static org.junit.Assert.*;

public final class TextMetricsTest {
    @Test public void constructorKeepsEveryMetricInTheCorrectField() {
        TextMetrics result = new TextMetrics(2, 4, 31, 1);
        assertEquals(2, result.sentences);
        assertEquals(4, result.words);
        assertEquals(31, result.characters);
        assertEquals(1, result.numbers);
    }

    @Test public void resultTypeAndItsValuesAreImmutable() throws Exception {
        assertTrue(Modifier.isFinal(TextMetrics.class.getModifiers()));
        for (String name : new String[]{"sentences", "words", "characters", "numbers"}) {
            Field field = TextMetrics.class.getField(name);
            assertEquals(int.class, field.getType());
            assertTrue(Modifier.isFinal(field.getModifiers()));
        }
    }
}
