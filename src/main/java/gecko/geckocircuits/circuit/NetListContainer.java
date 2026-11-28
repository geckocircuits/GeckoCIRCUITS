/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.geckocircuits.circuit;

import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitSource;
import gecko.geckocircuits.control.NetzlisteCONTROL;

public final class NetListContainer {

    public final NetzlisteCONTROL _nlControl;
    public final NetListLK _nlLK;
    public final NetListLK _nlTH;

    public static void requireLegacySourceSupport(final SchematicEditor2 editor) {
        requireLegacySourceSupport(editor.getElementLK());
    }

    static void requireLegacySourceSupport(final Iterable<? extends AbstractCircuitBlockInterface> elements) {
        for (var element : elements) {
            if (element.isCircuitEnabled() == Enabled.ENABLED && element.getAcComponent() != null
                    && element.getAcComponent().law() instanceof gecko.core.ac.AcComponent.Mosfet) {
                throw new IllegalArgumentException("Use the Core backend for Level 1 MOSFET " + element.getStringID());
            }
            if (element.isCircuitEnabled() == Enabled.ENABLED && element.getAcComponent() != null
                    && element.getAcComponent().law() instanceof gecko.core.ac.AcComponent.ExponentialDiode diode
                    && diode.transientLaw()) {
                throw new IllegalArgumentException("Use the Core backend for exponential diode " + element.getStringID());
            }
            if (element instanceof AbstractCircuitSource source
                    && source.getSimulationDomain() == ConnectorType.LK && source.isPwlSource()
                    && source.isCircuitEnabled() == Enabled.ENABLED) {
                throw new IllegalArgumentException("Use the Core backend for PWL source " + source.getStringID());
            }
        }
    }

    public static NetListContainer fabricStartSimulation(final SchematicEditor2 schematicEntry, SimulationsKern simKern) {
        if (simKern != null) requireLegacySourceSupport(schematicEntry);
        schematicEntry.checkNameOptParameters();
        NetzlisteAllg nlC1 = NetzlisteAllg.fabricNetzlistDisabledParentSubsRemoved(schematicEntry.getConnection(ConnectorType.CONTROL), schematicEntry.getElementCONTROL());
        NetListLK nlL = schematicEntry.getNetzliste(ConnectorType.LK_AND_RELUCTANCE);

        NetListLK nlT = schematicEntry.getNetzliste(ConnectorType.THERMAL);
        NetzlisteCONTROL nlC = NetzlisteCONTROL.fabricRunSimulation(nlC1);

        for(AbstractCircuitSheetComponent elem : schematicEntry._circuitSheet.getAllElements()) {
            if(elem instanceof AbstractCircuitBlockInterface) {
                AbstractCircuitBlockInterface comp = (AbstractCircuitBlockInterface) elem;
                comp._currentInAmps = 0;
                comp._voltage = 0;
            }
            if(elem instanceof AbstractCircuitBlockInterface) {
                ((AbstractCircuitBlockInterface) elem).setzeParameterZustandswerteAufNULL();
            }

        }

        return new NetListContainer(nlC, nlL, nlT);
    }

    public static NetListContainer fabricContinueSimulation(final SchematicEditor2 schematicEntry, SimulationsKern simKern,
            NetListContainer oldNetlist) {
        if (simKern != null) requireLegacySourceSupport(schematicEntry);
        schematicEntry.checkNameOptParameters();
        return new NetListContainer(NetzlisteCONTROL.fabricContinueSimulation(oldNetlist._nlControl), oldNetlist._nlLK, oldNetlist._nlTH);
    }



    public static NetListContainer fabricGuiUpdate(NetListLK circuitNL, NetListLK thermNL, NetzlisteCONTROL nlCONTROL) {
        return new NetListContainer(nlCONTROL, circuitNL, thermNL);
    }

    private NetListContainer(NetzlisteCONTROL nlc, NetListLK nlk, NetListLK nlTH) {
        _nlControl = nlc;
        _nlLK = nlk;
        _nlTH = nlTH;
        // use the fabric methods!
    }

}
