package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NetzlisteCONTROL.IndexConnection inner class.
 * Tests the value object used to store element and block index connections.
 */
public class NetzlisteCONTROLTest {
    
    @Test
    public void testIndexConnectionExistence() {
        // Verify that IndexConnection class exists
        try {
            Class.forName("ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL$IndexConnection");
        } catch (ClassNotFoundException e) {
            fail("IndexConnection class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testIndexConnectionFieldNames() {
        // Verify that IndexConnection has the expected field names
        String[] fieldNames = {"_elementIndex", "_inBlockIndex_outputIndex"};
        
        try {
            Class<?> indexConnClass = Class.forName("ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL$IndexConnection");
            for (String fieldName : fieldNames) {
                assertNotNull("IndexConnection should have field: " + fieldName,
                             indexConnClass.getDeclaredField(fieldName));
            }
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            fail("IndexConnection class structure check failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testIndexConnectionIsPublic() {
        // Verify that IndexConnection is a public class
        try {
            Class<?> indexConnClass = Class.forName("ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL$IndexConnection");
            int modifiers = indexConnClass.getModifiers();
            assertTrue("IndexConnection should be public", java.lang.reflect.Modifier.isPublic(modifiers));
        } catch (ClassNotFoundException e) {
            fail("IndexConnection class not found: " + e.getMessage());
        }
    }
    
    @Test
    public void testIndexConnectionIsInnerClass() {
        // Verify that IndexConnection is an inner class of NetzlisteCONTROL
        try {
            Class<?> indexConnClass = Class.forName("ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL$IndexConnection");
            assertNotNull("IndexConnection should be a member of NetzlisteCONTROL",
                         indexConnClass.getEnclosingClass());
            assertEquals("IndexConnection should be inner class of NetzlisteCONTROL",
                        NetzlisteCONTROL.class, indexConnClass.getEnclosingClass());
        } catch (ClassNotFoundException e) {
            fail("IndexConnection class not found: " + e.getMessage());
        }
    }
}
