package lt.mipt.wordcounter.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.Test;

import static org.junit.Assert.*;

public final class TextPreviewTest {
    @Test public void shorterTextIsReturnedUnchanged() {
        String text = new String("Labas");
        assertSame(text, TextPreview.shorten(text, 10));
        assertSame(text, TextPreview.shorten(text, 5));
    }

    @Test public void longTextReceivesAnEllipsisAfterTheRequestedCodePoints() {
        assertEquals("Lab…", TextPreview.shorten("Labas", 3));
    }

    @Test public void supplementaryCharacterIsNeverSplitIntoSurrogateHalves() {
        assertEquals("A\ud83d\ude00…", TextPreview.shorten("A\ud83d\ude00B", 2));
        assertEquals("\ud83d\ude00…", TextPreview.shorten("\ud83d\ude00B", 1));
        assertEquals("\ud83d\ude00B", TextPreview.shorten("\ud83d\ude00B", 2));
    }

    @Test public void zeroLimitAndEmptyTextHaveDefinedResults() {
        assertEquals("", TextPreview.shorten("", 0));
        assertEquals("…", TextPreview.shorten("abc", 0));
    }

    @Test public void negativeLimitIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TextPreview.shorten("abc", -1));
        assertEquals("Negative preview length", error.getMessage());
    }

    @Test public void nullInputIsRejectedRatherThanConvertedToText() {
        assertThrows(NullPointerException.class, () -> TextPreview.shorten(null, 1));
    }

    @Test public void utilityClassHasOnePrivateParameterlessConstructor() throws Exception {
        Constructor<?>[] constructors = TextPreview.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        Constructor<?> constructor = constructors[0];
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertEquals(0, constructor.getParameterCount());
        assertThrows(IllegalAccessException.class, constructor::newInstance);
        // Verify that bypassing access does not expose instance state or fail at construction.
        constructor.setAccessible(true);
        assertEquals(TextPreview.class, constructor.newInstance().getClass());
        for (java.lang.reflect.Field field : TextPreview.class.getDeclaredFields()) {
            assertTrue(Modifier.isStatic(field.getModifiers()));
        }
    }
}
