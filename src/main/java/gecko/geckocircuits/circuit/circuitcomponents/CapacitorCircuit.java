package gecko.geckocircuits.circuit.circuitcomponents;

import gecko.geckocircuits.circuit.AbstractTypeInfo;
import gecko.geckocircuits.circuit.CircuitTypeInfo;
import gecko.i18n.resources.I18nKeys;

public class CapacitorCircuit extends AbstractCapacitor {
    static final AbstractTypeInfo TYPE_INFO =
            new CircuitTypeInfo(CapacitorCircuit.class, "C", I18nKeys.CAPACITOR_C_F);

}
