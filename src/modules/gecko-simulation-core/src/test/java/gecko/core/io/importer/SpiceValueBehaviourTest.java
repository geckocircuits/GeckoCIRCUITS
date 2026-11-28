package gecko.core.io.importer;

import gecko.core.ac.AcComponent;
import gecko.core.io.AcMetadataCodec;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Behavioural expressions of the LTspice archive: probes, comparisons and exact gradients (ADR-0033). */
class SpiceValueBehaviourTest {

    @Test
    void readsProbesAsRawNamesInFirstUseOrder() {
        var law = SpiceValue.behaviour("V(1,2)*{ratio} + I(Vrac) - v(N001)/v(1, 2)", Map.of("RATIO", "2"));
        assertEquals(List.of(new SpiceValue.Probe(false, "1", "2"), new SpiceValue.Probe(true, "vrac", null),
                new SpiceValue.Probe(false, "n001", null)), law.probes());
        var point = law.at(new double[]{4, 3, 8});
        assertEquals(4 * 2 + 3 - 8 / 4.0, point.value(), 1e-15);
        assertArrayEquals(new double[]{2 + 8 / 16.0, 1, -1 / 4.0}, point.gradient(), 1e-15);
    }

    @Test
    void gradientsMatchCentralDifferencesOnTheArchiveFunctions() {
        String[] expressions = {
            "sqrt(V(alpha)**2+V(beta)**2)", "(atan2(V(beta),V(alpha)))*180/pi", "abs(v(a))*sin(v(b))+cos(v(a))",
            "(1-V(alpha))*V(beta)/V(alpha)", "V(alpha)*V(alpha)/(V(alpha)*V(alpha)+2*I(Vd)/V(beta))",
            "exp(v(a)/2)+ln(v(b))+log10(v(b))+atan(v(a))+tan(v(b))", "v(b)**v(a)"};
        for (String text : expressions) {
            var law = SpiceValue.behaviour(text, Map.of());
            double[] at = new double[law.probes().size()];
            for (int k = 0; k < at.length; k++) at[k] = 0.7 + 0.3 * k;
            var point = law.at(at);
            for (int k = 0; k < at.length; k++) {
                double h = 1e-6;
                double[] up = at.clone(), down = at.clone();
                up[k] += h;
                down[k] -= h;
                double expected = (law.at(up).value() - law.at(down).value()) / (2 * h);
                assertEquals(expected, point.gradient()[k], 1e-6 * Math.max(1, Math.abs(expected)), text + " probe " + k);
            }
        }
    }

    @Test
    void ifSelectsOneBranchAndRecordsTheComparisonOutcome() {
        var law = SpiceValue.behaviour("IF(I(V5)>28u,28u,IF(I(V5)<-28u,-28u,I(V5)))", Map.of());
        var inside = law.at(new double[]{1e-6});
        assertEquals(1e-6, inside.value());
        assertArrayEquals(new double[]{1}, inside.gradient());
        var above = law.at(new double[]{1e-3});
        assertEquals(28e-6, above.value(), 1e-20);
        assertArrayEquals(new double[]{0}, above.gradient());
        assertNotEquals(inside.branches(), above.branches());
        assertEquals(inside.branches(), law.at(new double[]{2e-6}).branches());
        // The unselected branch is not evaluated, so a guarded division stays finite.
        assertEquals(0, SpiceValue.behaviour("if(v(a)>0, 1/v(a), 0)", Map.of()).at(new double[]{0}).value());
    }

    @Test
    void booleanOperatorsBindBelowComparisonsAndTreatAboveHalfAsTrue() {
        var law = SpiceValue.behaviour("IF( V(UVLO,HB)>2.5 & V(GU)>2.5, V(Vdd,HB), 100m)", Map.of());
        assertEquals(12, law.at(new double[]{3, 3, 12}).value());
        assertEquals(0.1, law.at(new double[]{3, 2, 12}).value());
        var or = SpiceValue.behaviour("(v(a)>=1) | (v(b)<=0)", Map.of());
        assertEquals(1, or.at(new double[]{1, 5}).value());
        assertEquals(1, or.at(new double[]{0, 0}).value());
        assertEquals(0, or.at(new double[]{0, 5}).value());
        assertEquals(1, SpiceValue.evaluate("if(0.6, 1, 2)", Map.of()));
        assertEquals(2, SpiceValue.evaluate("if(0.5, 1, 2)", Map.of()));
    }

    @Test
    void scalarValuesKeepTheirGrammarAndRejectProbes() {
        assertEquals(1, SpiceValue.evaluate("2 > 1", Map.of()));
        assertEquals(Math.atan2(1, -1), SpiceValue.evaluate("atan2(1,-1)", Map.of()), 1e-15);
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("v(a)", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("max(1,2)", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("if(1,2)", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.behaviour("v(a,b,c)", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.behaviour("i(v1,v2)", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.behaviour("1/v(a)", Map.of()).at(new double[]{0}));
        var gain = SpiceValue.transfer("1/(1+s)", Map.of());
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.transfer("s > 1", Map.of())
                .evaluate(new gecko.core.ac.Complex(0, 1)));
        assertNotNull(gain);
    }

    @Test
    void savedLawKeepsItsKindAndExpression() {
        for (boolean current : new boolean[]{false, true}) {
            var component = new AcComponent("B1", List.of("hys", "ninv"), false,
                    new AcComponent.Behavioural("IF(V(out) > ({VHIGH}+{VLOW})/2,{VHYS},0)", current));
            assertEquals(component, AcMetadataCodec.decodeComponent(AcMetadataCodec.encode(component)));
        }
        assertThrows(IllegalArgumentException.class, () -> new AcComponent.Behavioural(" ", false));
    }
}
