package gecko.core.datacontainer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScopeWrapperIndices}.
 *
 * Key behavior: After construction, _globalIndices is EMPTY.
 * You must call reset() to populate _globalIndices from _originalGlobalIndices.
 */
class ScopeWrapperIndicesTest {

    private DataContainerGlobal globalContainer;
    private ScopeWrapperIndices indices;

    @BeforeEach
    void setUp() {
        globalContainer = new DataContainerGlobal();
        globalContainer.init(3, new String[]{"sig0", "sig1", "sig2"}, "time");
        indices = new ScopeWrapperIndices(Arrays.asList(0, 1, 2), globalContainer);
    }

    // ---------------------------------------------------------------
    // Constructor tests
    // ---------------------------------------------------------------

    @Test
    void testConstructorLeavesGlobalIndicesEmpty() {
        // After construction, _globalIndices is empty, so getTotalSignalNumber() returns 0
        assertEquals(0, indices.getTotalSignalNumber(), "_globalIndices should be empty after construction");
    }

    @Test
    void testConstructorPopulatesIndexedDataContainers() {
        // Constructor populates _indexedDataContainers with globalDataContainer entries
        // for each index in the passed-in globalIndices list (size 3)
        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(globalContainer, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
    }

    @Test
    void testConstructorWithEmptyIndices() {
        ScopeWrapperIndices empty = new ScopeWrapperIndices(Collections.emptyList(), globalContainer);
        assertEquals(0, empty.getTotalSignalNumber());
    }

    @Test
    void testConstructorWithSingleIndex() {
        ScopeWrapperIndices single = new ScopeWrapperIndices(Arrays.asList(5), globalContainer);
        assertEquals(0, single.getTotalSignalNumber());
        // After reset, it should have one signal
        single.reset();
        assertEquals(1, single.getTotalSignalNumber());
        assertEquals(5, single.getContainerRowIndex(0));
    }

    // ---------------------------------------------------------------
    // reset() tests
    // ---------------------------------------------------------------

    @Test
    void testResetPopulatesGlobalIndices() {
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());
    }

    @Test
    void testResetCopiesOriginalIndices() {
        indices.reset();
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    @Test
    void testResetRebuildsIndexedDataContainers() {
        indices.reset();
        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(globalContainer, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
    }

    @Test
    void testResetClearsModifications() {
        // First reset to populate
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());

        // Add an extra signal
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 1, 99);
        assertEquals(4, indices.getTotalSignalNumber());
        assertEquals(99, indices.getContainerRowIndex(1));

        // Reset should restore to original state
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    @Test
    void testDoubleResetIsIdempotent() {
        indices.reset();
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    // ---------------------------------------------------------------
    // getContainerRowIndex() tests
    // ---------------------------------------------------------------

    @Test
    void testGetContainerRowIndexBeforeResetReturnsZero() {
        // _globalIndices is empty before reset(), so any row >= size returns 0
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(0, indices.getContainerRowIndex(5));
    }

    @Test
    void testGetContainerRowIndexAfterReset() {
        indices.reset();
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    @Test
    void testGetContainerRowIndexOutOfBoundsReturnsZero() {
        indices.reset();
        // row >= size should return 0
        assertEquals(0, indices.getContainerRowIndex(3));
        assertEquals(0, indices.getContainerRowIndex(100));
    }

    @Test
    void testGetContainerRowIndexAtExactBoundary() {
        indices.reset();
        // row exactly at size should return 0
        assertEquals(0, indices.getContainerRowIndex(3));
    }

    // ---------------------------------------------------------------
    // getTotalSignalNumber() tests
    // ---------------------------------------------------------------

    @Test
    void testGetTotalSignalNumberInitiallyZero() {
        assertEquals(0, indices.getTotalSignalNumber());
    }

    @Test
    void testGetTotalSignalNumberAfterReset() {
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());
    }

    @Test
    void testGetTotalSignalNumberAfterAddingSignal() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 0, 10);
        assertEquals(4, indices.getTotalSignalNumber());
    }

    @Test
    void testGetTotalSignalNumberAfterDeletingSignal() {
        indices.reset();
        indices.deleteSignal(0);
        assertEquals(2, indices.getTotalSignalNumber());
    }

    // ---------------------------------------------------------------
    // defineAdditionalSignal() tests
    // ---------------------------------------------------------------

    @Test
    void testDefineAdditionalSignalAtBeginning() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 0, 42);

        assertEquals(4, indices.getTotalSignalNumber());
        assertEquals(42, indices.getContainerRowIndex(0));
        assertEquals(0, indices.getContainerRowIndex(1));
        assertSame(extra, indices.getDataContainer(0));
        assertSame(globalContainer, indices.getDataContainer(1));
    }

    @Test
    void testDefineAdditionalSignalAtEnd() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 3, 77);

        assertEquals(4, indices.getTotalSignalNumber());
        assertEquals(77, indices.getContainerRowIndex(3));
        assertSame(extra, indices.getDataContainer(3));
    }

    @Test
    void testDefineAdditionalSignalInMiddle() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 1, 55);

        assertEquals(4, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(55, indices.getContainerRowIndex(1));
        assertEquals(1, indices.getContainerRowIndex(2));
        assertEquals(2, indices.getContainerRowIndex(3));
        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(extra, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
    }

    @Test
    void testDefineMultipleAdditionalSignals() {
        indices.reset();
        DataContainerGlobal extra1 = new DataContainerGlobal();
        DataContainerGlobal extra2 = new DataContainerGlobal();

        indices.defineAdditionalSignal(extra1, 1, 10);
        indices.defineAdditionalSignal(extra2, 3, 20);

        assertEquals(5, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(10, indices.getContainerRowIndex(1));
        assertEquals(1, indices.getContainerRowIndex(2));
        assertEquals(20, indices.getContainerRowIndex(3));
        assertEquals(2, indices.getContainerRowIndex(4));
    }

    // ---------------------------------------------------------------
    // deleteSignal() tests
    // ---------------------------------------------------------------

    @Test
    void testDeleteSignalFromBeginning() {
        indices.reset();
        indices.deleteSignal(0);

        assertEquals(2, indices.getTotalSignalNumber());
        assertEquals(1, indices.getContainerRowIndex(0));
        assertEquals(2, indices.getContainerRowIndex(1));
    }

    @Test
    void testDeleteSignalFromEnd() {
        indices.reset();
        indices.deleteSignal(2);

        assertEquals(2, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
    }

    @Test
    void testDeleteSignalFromMiddle() {
        indices.reset();
        indices.deleteSignal(1);

        assertEquals(2, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(2, indices.getContainerRowIndex(1));
    }

    @Test
    void testDeleteAllSignals() {
        indices.reset();
        indices.deleteSignal(0);
        indices.deleteSignal(0);
        indices.deleteSignal(0);

        assertEquals(0, indices.getTotalSignalNumber());
    }

    @Test
    void testDeleteThenReset() {
        indices.reset();
        indices.deleteSignal(0);
        assertEquals(2, indices.getTotalSignalNumber());

        // Reset restores original state
        indices.reset();
        assertEquals(3, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    // ---------------------------------------------------------------
    // getDataContainer() tests
    // ---------------------------------------------------------------

    @Test
    void testGetDataContainerReturnsGlobalContainerAfterConstruction() {
        // Constructor populates _indexedDataContainers for all indices
        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(globalContainer, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
    }

    @Test
    void testGetDataContainerAfterReset() {
        indices.reset();
        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(globalContainer, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
    }

    @Test
    void testGetDataContainerAfterDefineAdditional() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 1, 0);

        assertSame(globalContainer, indices.getDataContainer(0));
        assertSame(extra, indices.getDataContainer(1));
        assertSame(globalContainer, indices.getDataContainer(2));
        assertSame(globalContainer, indices.getDataContainer(3));
    }

    @Test
    void testGetDataContainerOutOfBoundsAfterReset() {
        indices.reset();
        assertThrows(IndexOutOfBoundsException.class, () -> indices.getDataContainer(3));
    }

    // ---------------------------------------------------------------
    // Combined operation tests
    // ---------------------------------------------------------------

    @Test
    void testAddThenDeleteRestoresState() {
        indices.reset();
        DataContainerGlobal extra = new DataContainerGlobal();
        indices.defineAdditionalSignal(extra, 1, 50);
        assertEquals(4, indices.getTotalSignalNumber());

        indices.deleteSignal(1);
        assertEquals(3, indices.getTotalSignalNumber());
        assertEquals(0, indices.getContainerRowIndex(0));
        assertEquals(1, indices.getContainerRowIndex(1));
        assertEquals(2, indices.getContainerRowIndex(2));
    }

    @Test
    void testNonSequentialOriginalIndices() {
        ScopeWrapperIndices nonSeq = new ScopeWrapperIndices(Arrays.asList(5, 10, 15), globalContainer);
        nonSeq.reset();

        assertEquals(3, nonSeq.getTotalSignalNumber());
        assertEquals(5, nonSeq.getContainerRowIndex(0));
        assertEquals(10, nonSeq.getContainerRowIndex(1));
        assertEquals(15, nonSeq.getContainerRowIndex(2));
    }
}
