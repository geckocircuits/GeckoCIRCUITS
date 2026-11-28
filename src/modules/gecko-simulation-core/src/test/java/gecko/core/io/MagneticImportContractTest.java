package gecko.core.io;

import gecko.core.circuit.netlist.*;
import gecko.core.simulation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class MagneticImportContractTest {
    static String element(int type,String name,String x,String y,String params,long id) {
        return """
            e
            <ElementLK>
            typ %d
            idStringDialog %s
            uniqueObjectIdentifier %d
            parameter[] %s
            labelAnfangsKnoten[] /%s
            labelEndKnoten[] /%s
            orientierung 503
            parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
            <\\ElementLK>
            """.formatted(type,name,id,params,x,y);
    }
    static String transformer(double k,int[] order,boolean ids) {
        String coupling=element(9,"K1","","",Double.toString(k),30)
            .replace("parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0","parameterString[] /Lp/Ls/0");
        if(ids) coupling=coupling.replace("<\\ElementLK>","coupledReferenceID[] 9007199254740993 9007199254740995\n<\\ElementLK>");
        String[] components={element(4,"V1","p","0","401 10",1),
            element(12,"Lp","p","0","4",9007199254740993L),
            element(12,"Ls","s","0","1",9007199254740995L),
            element(1,"load","s","0","100",4),coupling};
        StringBuilder text=new StringBuilder("tDURATION 0.005\ndt 0.001\nsolverType 0\n");
        for(int index:order) text.append(components[index]);
        return text.toString();
    }
    static CircuitModel parse(String text) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)),"transformer.ipes");
    }
    static Stream<Arguments> variants() {
        int[][] orders={{0,1,2,3,4},{4,3,2,1,0},{2,0,4,1,3}};
        return Stream.of(orders).flatMap(order->Stream.of(-1.0,-0.5,0.0,0.5,1.0)
            .flatMap(k->Stream.of(0,1,2).flatMap(method->Stream.of(false,true)
                .map(ids->Arguments.of(order,k,method,ids)))));
    }

    @ParameterizedTest @MethodSource("variants")
    void importedTransformerSolvesPhysicalVoltageCurrentAndEnergyContracts(int[] order,double k,int method,boolean ids) throws Exception {
        String text=transformer(k,order,ids).replace("solverType 0","solverType "+method);
        Path file=Files.createTempFile("magnetic-transformer-",".ipes");
        Files.writeString(file,text);
        HeadlessSimulationEngine engine=new HeadlessSimulationEngine();
        List<Double> outputs=new ArrayList<>();
        engine.setStepTraceListener((time,p,currents,netlist)->{
            assertEquals(4,netlist.getElementCount(),"K1 is metadata, not a two-terminal component");
            assertEquals(3,netlist.getVoltageSourceMax());
            int primary=netlist.getElementIndexByName("Lp");
            int secondary=netlist.getElementIndexByName("Ls");
            int source=netlist.getElementIndexByName("V1");
            int load=netlist.getElementIndexByName("load");
            double v=p[netlist.getNodeX(load)];outputs.add(v);
            if(outputs.size()==1) {
                double alpha=method==0?1:method==1?2:1.5;
                double mutual=2*k;
                double expected=100*mutual*10/(alpha/0.001*(4-mutual*mutual)+400);
                assertEquals(expected,v,1e-9);
            }
            if(Math.abs(k)==1) assertEquals(5*k,v,1e-8);
            assertEquals(-currents[primary],currents[source],1e-9,"Primary KCL");
            assertEquals(-currents[secondary],currents[load],1e-9,"Secondary KCL");
            double ip=currents[primary],is=currents[secondary];
            double energy=0.5*4*ip*ip+0.5*is*is+2*k*ip*is;
            assertTrue(energy>=-1e-10,"Passive magnetic energy");
            assertEquals(2*k,netlist.getMutualInductance(primary,secondary),1e-12);
        });
        var result=engine.runSimulation(SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(result.isSuccess(),result.getErrorMessage());assertTrue(outputs.size()>=4);
    }

    @Test void uniqueReferenceIdentifiersAreNotRoundedToDoublePrecision() throws Exception {
        var model=parse(transformer(0.5,new int[]{0,1,2,3,4},true));
        assertEquals(9007199254740993L,model.getCircuitComponents().get(1).getParameters().get("uniqueObjectIdentifier"));
        assertEquals(9007199254740995L,model.getCircuitComponents().get(2).getParameters().get("uniqueObjectIdentifier"));
        var netlist=NetlistBuilder.buildFromCircuitModel(model);
        assertEquals(1,netlist.getAllCouplings().size());
    }

    @ParameterizedTest
    @ValueSource(strings={"missing","same","resistor","duplicate","coefficient","inductance","stale-id","duplicate-id","missing-value"})
    void invalidSavedCouplingFailsWithComponentContext(String failure) throws Exception {
        String text=transformer(0.5,new int[]{0,1,2,3,4},false);
        text=switch(failure) {
            case "missing"->text.replace("/Lp/Ls/0","/Lp/absent/0");
            case "same"->text.replace("/Lp/Ls/0","/Lp/Lp/0");
            case "resistor"->text.replace("/Lp/Ls/0","/Lp/load/0");
            case "duplicate"->text+element(9,"K2","","","0.5",31).replace("/NIX_NIX_NIX/NIX_NIX_NIX/0","/Ls/Lp/0");
            case "coefficient"->text.replace("parameter[] 0.5","parameter[] 1.1");
            case "inductance"->text.replace("parameter[] 4\n","parameter[] -4\n");
            case "stale-id"->text.replace("idStringDialog K1","coupledReferenceID[] 700 800\nidStringDialog K1");
            case "duplicate-id"->text.replace("uniqueObjectIdentifier 9007199254740995","uniqueObjectIdentifier 9007199254740993");
            default->text.replace("parameter[] 0.5","parameter[] ");
        };
        String saved=text;
        var error=assertThrows(IllegalArgumentException.class,()->NetlistBuilder.buildFromCircuitModel(parse(saved)));
        assertTrue(error.getMessage().contains("K1")||error.getMessage().contains("K2")
            ||error.getMessage().contains("Lp")||error.getMessage().contains("identifier"),error.getMessage());
    }

    @ParameterizedTest @ValueSource(strings={"1","1 bad","1 2 3"})
    void malformedSavedReferenceArrayFailsDuringParsing(String ids) {
        String text=transformer(0.5,new int[]{0,1,2,3,4},false)
            .replace("idStringDialog K1","coupledReferenceID[] "+ids+"\nidStringDialog K1");
        assertThrows(CircuitFileParser.CircuitParseException.class,()->parse(text));
    }

    @ParameterizedTest @ValueSource(ints={0,1,2})
    void reversingASecondaryWindingReversesItsOutput(int method) throws Exception {
        String text=transformer(1,new int[]{0,1,2,3,4},false)
            .replace("solverType 0","solverType "+method)
            .replace(element(12,"Ls","s","0","1",9007199254740995L),
                element(12,"Ls","0","s","1",9007199254740995L));
        assertOutputRatios(text,-5,Double.NaN);
    }

    @ParameterizedTest @ValueSource(ints={0,1,2})
    void threeIdealWindingsProduceBothTurnsRatiosAndAddTheirLoads(int method) throws Exception {
        String text=transformer(-1,new int[]{0,1,2,3,4},false).replace("solverType 0","solverType "+method)
            +element(12,"Lt","t","0","9",50)+element(1,"tertiary-load","t","0","50",51)
            +element(9,"K2","","","1",52).replace("/NIX_NIX_NIX/NIX_NIX_NIX/0","/Lp/Lt/0")
            +element(9,"K3","","","-1",53).replace("/NIX_NIX_NIX/NIX_NIX_NIX/0","/Ls/Lt/0");
        assertOutputRatios(text,-5,15);
    }

    private static void assertOutputRatios(String text,double secondary,double tertiary) throws Exception {
        Path file=Files.createTempFile("magnetic-multiwinding-",".ipes");Files.writeString(file,text);
        List<Double> results=new ArrayList<>();var engine=new HeadlessSimulationEngine();
        engine.setStepTraceListener((time,p,currents,netlist)->{
            assertEquals(secondary,p[netlist.getNodeX(netlist.getElementIndexByName("load"))],1e-8);
            if(Double.isFinite(tertiary)) {
                assertEquals(tertiary,p[netlist.getNodeX(netlist.getElementIndexByName("tertiary-load"))],1e-8);
                assertEquals(3,netlist.getAllCouplings().size());
                double supplied=-10*currents[netlist.getElementIndexByName("V1")];
                assertTrue(supplied>=secondary*secondary/100+tertiary*tertiary/50-1e-8);
            }
            results.add(time);
        });
        var result=engine.runSimulation(SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(result.isSuccess(),result.getErrorMessage());assertTrue(results.size()>=4);
    }

    @Test void realCoupledRectifierPreservesItsWindingsAndMutualValue() throws Exception {
        Path file=Path.of(java.util.Objects.requireNonNull(MagneticImportContractTest.class
            .getResource("/ipes/coupled_rectifier.ipes")).toURI());
        CircuitModel model=new CircuitFileParser().parse(file.toString());
        var netlist=NetlistBuilder.buildFromCircuitModel(model);
        assertEquals(12,model.getCircuitComponents().size());
        assertEquals(11,netlist.getElementCount());
        assertEquals(3,netlist.getVoltageSourceMax());
        int first=netlist.getElementIndexByName("Lc.1"),second=netlist.getElementIndexByName("Lc.2");
        assertEquals(0.989*Math.sqrt(7.5e-4)*Math.sqrt(3.63e-4),netlist.getMutualInductance(first,second),1e-15);
        assertEquals(0,netlist.getNodeY(second));
        assertEquals(1,netlist.getAllCouplings().size());
    }

    @Test void couplingWithoutAnyPhysicalWindingIsRejected() throws Exception {
        var model=parse(element(9,"orphan","","","0.5",3));
        var error=assertThrows(IllegalArgumentException.class,()->NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("orphan"));
    }

    @Test void missingProgrammaticCoefficientCannotSilentlyDisableCoupling() throws Exception {
        var model=parse(transformer(0.5,new int[]{0,1,2,3,4},false));
        model.getCircuitComponents().get(4).getParameters().remove("param0");
        var error=assertThrows(IllegalArgumentException.class,()->NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("K1"));
    }

    @ParameterizedTest @ValueSource(strings={"","/Lp"})
    void incompleteNameReferencesAreRejected(String references) throws Exception {
        String text=transformer(0.5,new int[]{0,1,2,3,4},false)
            .replace("parameterString[] /Lp/Ls/0","parameterString[] "+references);
        var error=assertThrows(IllegalArgumentException.class,()->NetlistBuilder.buildFromCircuitModel(parse(text)));
        assertTrue(error.getMessage().contains("K1"));
    }

    @Test void olderFilesWithZeroIdentifiersStillResolveUniqueWindingNames() throws Exception {
        var model=parse(transformer(0.5,new int[]{0,1,2,3,4},false)
            .replace("uniqueObjectIdentifier 9007199254740993","uniqueObjectIdentifier 0"));
        assertEquals(1,NetlistBuilder.buildFromCircuitModel(model).getAllCouplings().size());
    }

    @ParameterizedTest @ValueSource(strings={"0","NaN","Infinity"})
    void invalidSelfInductanceCannotEnterTheMatrix(String value) throws Exception {
        var model=parse(transformer(0.5,new int[]{0,1,2,3,4},false).replace("parameter[] 4\n","parameter[] "+value+"\n"));
        var error=assertThrows(IllegalArgumentException.class,()->NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("Lp"));
    }
}
