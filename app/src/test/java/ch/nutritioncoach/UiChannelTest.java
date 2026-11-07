package ch.nutritioncoach;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UiChannelTest {
    @Test
    public void verifiesPublishedBytes() {
        String digest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertTrue(UiChannel.matches("abc".getBytes(StandardCharsets.UTF_8), digest));
        assertFalse(UiChannel.matches("abd".getBytes(StandardCharsets.UTF_8), digest));
    }

    @Test
    public void readsDocumentAtLimit() throws IOException {
        byte[] bytes = {1, 2, 3};
        assertArrayEquals(bytes, UiChannel.readLimited(new ByteArrayInputStream(bytes), bytes.length));
    }

    @Test(expected = IOException.class)
    public void refusesOversizedDocument() throws IOException {
        UiChannel.readLimited(new ByteArrayInputStream(new byte[4]), 3);
    }
}
