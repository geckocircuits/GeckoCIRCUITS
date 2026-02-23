package gecko.core.nativec;

import gecko.core.circuit.TokenMap;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CompiledClassContainerTest {

    @Test
    void testDefaultConstructor() {
        CompiledClassContainer container = new CompiledClassContainer();
        assertEquals("", container.getSourceString());
    }

    @Test
    void testConstructorWithBytesAndSource() {
        byte[] classBytes = new byte[]{1, 2, 3, 4, 5};
        String source = "public class Test {}";

        CompiledClassContainer container = new CompiledClassContainer(classBytes, source);

        assertNotNull(container.getClassBytes());
        assertEquals(5, container.getClassBytes().length);
        assertEquals(source, container.getSourceString());

        // Verify defensive copy - modifying original shouldn't affect container
        classBytes[0] = 99;
        assertEquals(1, container.getClassBytes()[0]);
    }

    @Test
    void testGetClassBytesCopy() {
        byte[] original = new byte[]{10, 20, 30};
        CompiledClassContainer container = new CompiledClassContainer(original, "test");

        byte[] retrieved1 = container.getClassBytes();
        byte[] retrieved2 = container.getClassBytes();

        // Should return different array instances (defensive copy)
        assertNotSame(retrieved1, retrieved2);
        assertArrayEquals(retrieved1, retrieved2);
    }

    @Test
    void testTokenMapConstructorWithEmptyTokenMap() {
        // TokenMap requires a String[] parameter, use empty array
        String[] emptyAscii = new String[0];
        TokenMap tokenMap = new TokenMap(emptyAscii);

        CompiledClassContainer container = new CompiledClassContainer(tokenMap);

        // Should create container with null bytes since token not present
        assertEquals("", container.getSourceString());
    }

    @Test
    void testTokenMapConstructorHandlesMissingToken() {
        // Create TokenMap without classBytesNew[] token
        String[] ascii = new String[]{"someOtherToken 123"};
        TokenMap tokenMap = new TokenMap(ascii);

        CompiledClassContainer container = new CompiledClassContainer(tokenMap);

        assertEquals("", container.getSourceString());
    }

    @Test
    void testSerializable() throws IOException {
        byte[] classBytes = new byte[]{5, 10, 15};
        String source = "test source";
        CompiledClassContainer container = new CompiledClassContainer(classBytes, source);

        // Test that it can be serialized
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(container);
        oos.close();

        assertTrue(baos.toByteArray().length > 0, "Should produce serialized data");
    }

    @Test
    void testSourceStringPreserved() {
        String longSource = "public class MyClass {\n    public void method() {\n        System.out.println(\"Hello\");\n    }\n}";
        CompiledClassContainer container = new CompiledClassContainer(new byte[0], longSource);

        assertEquals(longSource, container.getSourceString());
    }

    @Test
    void testEmptyClassBytes() {
        byte[] empty = new byte[0];
        CompiledClassContainer container = new CompiledClassContainer(empty, "empty");

        assertNotNull(container.getClassBytes());
        assertEquals(0, container.getClassBytes().length);
    }
}
