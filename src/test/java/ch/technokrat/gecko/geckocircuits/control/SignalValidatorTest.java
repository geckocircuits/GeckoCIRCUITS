package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for SignalValidator.
 */
public class SignalValidatorTest {
    
    private SignalValidator validator;
    private AbstractDataContainer mockData;
    
    @Before
    public void setUp() {
        validator = new SignalValidator();
        mockData = mock(AbstractDataContainer.class);
    }
    
    @Test
    public void testValidateSignals_allValid_success() {
        // Setup: 3 signals with correct indices
        when(mockData.getRowLength()).thenReturn(3);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("current");
        when(mockData.getSignalName(2)).thenReturn("power");
        
        List<String> names = Arrays.asList("voltage", "current", "power");
        List<Integer> indices = Arrays.asList(0, 1, 2);
        
        SignalValidator.ValidationResult result = 
            validator.validateSignals(names, indices, mockData);
        
        assertTrue("All signals should be valid", result.isValid());
        assertFalse("No corrections should be needed", result.hasCorrections());
    }
    
    @Test
    public void testValidateSignals_wrongIndex_corrects() {
        // Setup: signal "current" is at index 2, but we think it's at index 1
        when(mockData.getRowLength()).thenReturn(3);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("power");
        when(mockData.getSignalName(2)).thenReturn("current");
        
        List<String> names = Arrays.asList("voltage", "current");
        List<Integer> indices = Arrays.asList(0, 1); // Wrong: current is at 2, not 1
        
        SignalValidator.ValidationResult result = 
            validator.validateSignals(names, indices, mockData);
        
        assertTrue("Should be valid after correction", result.isValid());
        assertTrue("Should have corrections", result.hasCorrections());
        assertTrue("Should mention correction", 
                   result.getCorrectionsMessage().contains("current"));
    }
    
    @Test
    public void testValidateSignals_signalMissing_fails() {
        // Setup: only 2 signals available, but we want 3
        when(mockData.getRowLength()).thenReturn(2);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("current");
        
        List<String> names = Arrays.asList("voltage", "current", "missing");
        List<Integer> indices = Arrays.asList(0, 1, 2);
        
        SignalValidator.ValidationResult result = 
            validator.validateSignals(names, indices, mockData);
        
        assertFalse("Should be invalid", result.isValid());
        assertTrue("Error message should mention missing signal", 
                   result.getErrorMessage().contains("missing"));
    }
    
    @Test
    public void testValidateSignals_outOfBoundsIndex_handled() {
        // Setup: index is out of bounds, but signal name exists elsewhere
        when(mockData.getRowLength()).thenReturn(2);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("current");
        
        List<String> names = Arrays.asList("voltage", "missing_signal");
        List<Integer> indices = Arrays.asList(0, 10); // Index 10 is out of bounds and signal doesn't exist
        
        SignalValidator.ValidationResult result = 
            validator.validateSignals(names, indices, mockData);
        
        assertFalse("Should be invalid due to missing signal", result.isValid());
    }
    
    @Test
    public void testValidateSignals_multipleCorrections() {
        // Setup: multiple signals in wrong order
        when(mockData.getRowLength()).thenReturn(4);
        when(mockData.getSignalName(0)).thenReturn("signal_a");
        when(mockData.getSignalName(1)).thenReturn("signal_b");
        when(mockData.getSignalName(2)).thenReturn("signal_c");
        when(mockData.getSignalName(3)).thenReturn("signal_d");
        
        List<String> names = Arrays.asList("signal_c", "signal_a", "signal_d");
        List<Integer> indices = Arrays.asList(0, 1, 2); // All wrong
        
        SignalValidator.ValidationResult result = 
            validator.validateSignals(names, indices, mockData);
        
        assertTrue("Should be valid after corrections", result.isValid());
        assertTrue("Should have corrections", result.hasCorrections());
    }
    
    @Test
    public void testFindSignalIndexByName_found() {
        when(mockData.getRowLength()).thenReturn(3);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("current");
        when(mockData.getSignalName(2)).thenReturn("power");
        
        int index = validator.findSignalIndexByName("current", mockData);
        
        assertEquals("Should find correct index", 1, index);
    }
    
    @Test
    public void testFindSignalIndexByName_notFound_returnsNegative() {
        when(mockData.getRowLength()).thenReturn(2);
        when(mockData.getSignalName(0)).thenReturn("voltage");
        when(mockData.getSignalName(1)).thenReturn("current");
        
        int index = validator.findSignalIndexByName("nonexistent", mockData);
        
        assertEquals("Should return -1 when not found", -1, index);
    }
    
    @Test
    public void testValidationResult_multipleErrors() {
        SignalValidator.ValidationResult result = new SignalValidator.ValidationResult();
        
        result.addMissingSignal("signal1");
        result.addMissingSignal("signal2");
        
        assertFalse("Should be invalid", result.isValid());
        String errorMsg = result.getErrorMessage();
        assertTrue("Should mention both signals", 
                   errorMsg.contains("signal1") && errorMsg.contains("signal2"));
    }
}
