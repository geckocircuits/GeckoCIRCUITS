package gecko.rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiKeyProperties.
 */
class ApiKeyPropertiesTest {

    private ApiKeyProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ApiKeyProperties();
    }

    // Test 1: isValidKey with valid key → true
    @Test
    void testIsValidKeyWithValidKey() {
        properties.setAuthEnabled(true);
        properties.setKeys(Set.of("valid-key-123", "another-key"));

        assertTrue(properties.isValidKey("valid-key-123"));
    }

    // Test 2: isValidKey with invalid key → false
    @Test
    void testIsValidKeyWithInvalidKey() {
        properties.setAuthEnabled(true);
        properties.setKeys(Set.of("valid-key-123", "another-key"));

        assertFalse(properties.isValidKey("wrong-key"));
    }

    // Test 3: isValidKey with null → false
    @Test
    void testIsValidKeyWithNull() {
        properties.setAuthEnabled(true);
        properties.setKeys(Set.of("valid-key-123"));

        assertFalse(properties.isValidKey(null));
    }

    // Test 4: authEnabled=false → isValidKey always true
    @Test
    void testIsValidKeyWhenAuthDisabled() {
        properties.setAuthEnabled(false);
        properties.setKeys(Set.of("valid-key-123"));

        assertTrue(properties.isValidKey("any-key"));
        assertTrue(properties.isValidKey("wrong-key"));
        assertTrue(properties.isValidKey(null));
    }

    // Test 5: Empty keys set → isValidKey always true (open mode)
    @Test
    void testIsValidKeyWithEmptyKeysSet() {
        properties.setAuthEnabled(true);
        properties.setKeys(Set.of());  // Empty set

        assertTrue(properties.isValidKey("any-key"));
        assertTrue(properties.isValidKey("wrong-key"));
        assertTrue(properties.isValidKey(null));
    }

    // Test 6: setKeys(String) parses comma-separated values
    @Test
    void testSetKeysStringParsesCsvValues() {
        properties.setKeys("key1,key2,key3");

        assertTrue(properties.getKeys().contains("key1"));
        assertTrue(properties.getKeys().contains("key2"));
        assertTrue(properties.getKeys().contains("key3"));
        assertEquals(3, properties.getKeys().size());
    }

    // Test 7: setKeys(String) trims whitespace
    @Test
    void testSetKeysStringTrimsWhitespace() {
        properties.setKeys("  key1  ,  key2  ,  key3  ");

        assertTrue(properties.getKeys().contains("key1"));
        assertTrue(properties.getKeys().contains("key2"));
        assertTrue(properties.getKeys().contains("key3"));
        assertEquals(3, properties.getKeys().size());
    }

    // Test 8: setKeys(null) → empty set (no NPE)
    @Test
    void testSetKeysNullDoesNotThrow() {
        properties.setKeys((String) null);

        assertTrue(properties.getKeys().isEmpty());
    }
}
