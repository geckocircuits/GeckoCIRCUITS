package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitTypeInfo;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.List;

public class CapacitorCircuit extends AbstractCapacitor {
    static final AbstractTypeInfo TYPE_INFO = 
            new CircuitTypeInfo(CapacitorCircuit.class, "C", I18nKeys.CAPACITOR_C_F);       
    
}
