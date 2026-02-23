package gecko.core.datacontainer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataIndexItemTest {

    @Test
    void testConstructorAndGetIndex() {
        DataIndexItem item = new DataIndexItem(5, "voltage");
        assertEquals(5, item.getIndex());
    }

    @Test
    void testGetIndexZero() {
        DataIndexItem item = new DataIndexItem(0, "time");
        assertEquals(0, item.getIndex());
    }

    @Test
    void testGetIndexNegative() {
        DataIndexItem item = new DataIndexItem(-1, "invalid");
        assertEquals(-1, item.getIndex());
    }

    @Test
    void testGetIndexLargeValue() {
        DataIndexItem item = new DataIndexItem(Integer.MAX_VALUE, "max");
        assertEquals(Integer.MAX_VALUE, item.getIndex());
    }

    @Test
    void testToStringReturnsName() {
        DataIndexItem item = new DataIndexItem(0, "current_A");
        assertEquals("current_A", item.toString());
    }

    @Test
    void testToStringEmptyName() {
        DataIndexItem item = new DataIndexItem(1, "");
        assertEquals("", item.toString());
    }

    @Test
    void testToStringNullName() {
        DataIndexItem item = new DataIndexItem(2, null);
        assertNull(item.toString());
    }

    @Test
    void testToStringWithSpecialCharacters() {
        String name = "V_out [mV] (node #3)";
        DataIndexItem item = new DataIndexItem(3, name);
        assertEquals(name, item.toString());
    }

    @Test
    void testMultipleInstancesAreIndependent() {
        DataIndexItem item1 = new DataIndexItem(0, "alpha");
        DataIndexItem item2 = new DataIndexItem(1, "beta");
        assertEquals(0, item1.getIndex());
        assertEquals("alpha", item1.toString());
        assertEquals(1, item2.getIndex());
        assertEquals("beta", item2.toString());
    }

    @Test
    void testSameIndexDifferentNames() {
        DataIndexItem a = new DataIndexItem(7, "first");
        DataIndexItem b = new DataIndexItem(7, "second");
        assertEquals(a.getIndex(), b.getIndex());
        assertNotEquals(a.toString(), b.toString());
    }
}
