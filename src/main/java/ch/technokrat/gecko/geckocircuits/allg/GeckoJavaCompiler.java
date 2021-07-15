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
package ch.technokrat.gecko.geckocircuits.allg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException; 
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import javax.tools.JavaCompiler.CompilationTask;

public class GeckoJavaCompiler {

    // fields contain source code, that is also saved in the JAVA-object .ipes stuff
    private String _javaSourceCode = "";
    private String _javaImportCode = "";
    private String _javaStaticCode = "";
    private String _javaStaticVariables = "";
    private String compilerMessage = "";
    // directory where class file will be located.
    private String _workingDirectory;
    private Method _run_script;
    private Method _setGecko;
    //----------------------
    private int lineCounter = 0;
    private String _sourceString = new String();
    // needed for counting the java block objects;
    private static int _objectCounter = 0;
    private String _className;
    //

    public static enum COMPILESTATUS {

        NOT_COMPILED, COMPILED_SUCCESSFULL, COMPILE_ERROR
    };
    private COMPILESTATUS _compileStatus = COMPILESTATUS.NOT_COMPILED;
    //-------------------------------------------------------------------
    private Fenster gecko;

    public GeckoJavaCompiler() {
    }

    public void setGecko(Fenster gecko) {
        this.gecko = gecko;
    }

    public boolean startCalculation() {
        try {
            //------------
            if (_compileStatus == COMPILESTATUS.NOT_COMPILED) {
                doCompilation();
            } else if (_compileStatus == COMPILESTATUS.COMPILE_ERROR) {
                return false;
            }
//            ausgangssignal = (double[]) _externYOUT.invoke(null, new Object[]{xIN, t});
            _setGecko.invoke(null, new Object[]{gecko});
            _run_script.invoke(null);
            //------------
        } catch (InvocationTargetException ex) {
            //showMsg("Exception in main: " + ex.getTargetException());
            ex.getTargetException().printStackTrace();  // Exception in the main method that we just tried to run
            return false;
        } catch (NullPointerException npe) {
            System.out.println("Error: Could not invoke external Java method!");
            _compileStatus = COMPILESTATUS.COMPILE_ERROR;
            return false;
        } catch (Exception ex) {
            System.err.println(ex.toString());
            return false;
        }
        return true;
    }

    public COMPILESTATUS getCompileStatus() {
        return _compileStatus;
    }

    public String getSourceCode() {
        return _javaSourceCode;
    }

    public String getImportCode() {
        return _javaImportCode;
    }

    public String getStaticInitCode() {
        return _javaStaticCode;
    }

    public String getCompilerMessage() {
        return compilerMessage;
    }

    public String getStaticVariables() {
        return _javaStaticVariables;
    }

    public void setStaticVariables(String staticVarCode) {
        _javaStaticVariables = staticVarCode;
    }

    public void setSourceCode(String javaSourceCode) {
        _javaSourceCode = javaSourceCode;
        _compileStatus = COMPILESTATUS.NOT_COMPILED;
    }

    public void setImportCode(final String importCode) {
        _javaImportCode = importCode;
        _compileStatus = COMPILESTATUS.NOT_COMPILED;
    }

    public void setStaticInitCode(final String staticCode) {
        _javaStaticCode = staticCode;
        _compileStatus = COMPILESTATUS.NOT_COMPILED;
    }

    private void createSourceCode(final String className) {
        try {
            lineCounter = 0;
            _sourceString = "";
            //-------------
            String strLine;
            BufferedReader reader = new BufferedReader(new StringReader(_javaImportCode));
            while ((strLine = reader.readLine()) != null) {
                appendSourcLine("\t\t" + strLine);
            }
            //-------------
            appendSourcLine("/**");
            appendSourcLine(" * Source created on " + new Date());
            appendSourcLine(" */");
            appendSourcLine("public class " + className + " { ");
            appendSourcLine("\nprivate static Fenster GECKO;\n");
            appendSourcLine("// static variables: ");
            //-------------
            reader = new BufferedReader(new StringReader(_javaStaticVariables));
            while ((strLine = reader.readLine()) != null) {
                appendSourcLine("\t\t" + strLine);
            }
            //appendSourcLine("private static double[] yOUT = new double[" + "tnY" + "];"); 
            appendSourcLine("static {");
            reader = new BufferedReader(new StringReader(_javaStaticCode));
            while ((strLine = reader.readLine()) != null) {
                appendSourcLine("\t\t" + strLine);
            }
            appendSourcLine("}");
            appendSourcLine("    public static void _setGecko (Fenster gecko) throws Exception { GECKO=gecko; }");
            appendSourcLine("    public static void run_script () throws Exception {");
            appendSourcLine("// Your code here:");
            appendSourcLine("// ****************** your code segment **********************");
            reader = new BufferedReader(new StringReader(_javaSourceCode));
            while ((strLine = reader.readLine()) != null) {
                appendSourcLine("\t\t" + strLine);
            }
            //-------------
            appendSourcLine("// ****************** end of code segment **********************");
            appendSourcLine("    }");
            appendSourcLine("}");
        } catch (IOException ex) {
            Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void appendSourcLine(String newLine) {
        //_sourceString += newLine;
        lineCounter++;
        compilerMessage += lineCounter + " " + newLine + "\n";
        _sourceString += newLine + "\n";
    }

    /**
     * Compiles and runs the short code segment.
     *
     * @throws IOException if there was an error creating the source file.
     */
    public void doCompilation() throws IOException {
        try {
            compilerMessage = "starting compilation....\n";
            compilerMessage += "";

            _className = "tmpJav" + _objectCounter;
            _objectCounter++;
            String classFileName = _className + ".java";

            createSourceCode(_className);

            compilerMessage += "------------------------------- Compiler output ------------------------------------\n";

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            final Map<String, JavaFileObject> output = new HashMap<String, JavaFileObject>();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

            JavaFileManager jfm =
                    new ForwardingJavaFileManager<StandardJavaFileManager>(
                    compiler.getStandardFileManager(diagnostics, null, null)) {

                        @Override
                        public JavaFileObject getJavaFileForOutput(Location location, String name, Kind kind, FileObject sibling)
                                throws IOException {
                            JavaFileObject jfo = new RAMJavaFileObject(name, kind);
                            output.put(name, jfo);
                            return jfo;
                        }
                    };

            OutputStream outStream = new OutputStream() {

                @Override
                public void write(byte[] b) {
                }

                public void write(byte[] b, int off, int len) {
                    compilerMessage += new String(b).substring(off, len);
                    
                }

                public void write(int b) {
                }
            };
            PrintWriter compilerWriter = new PrintWriter(outStream, true);

            // Compile --> 


            _workingDirectory = (new File(GlobalFilePathes.PFAD_JAR_HOME)).getParent();
            if (_workingDirectory == null) {
                _workingDirectory = System.getProperty("user.dir");
            }

            Vector<String> opt = new Vector<String>();

            opt.add("-classpath");
            String cp0 = System.getProperty("user.dir");
            String cp1 = _workingDirectory;
            String cp2 = cp0 + "/build/classes";
            String cp3 = GlobalFilePathes.PFAD_JAR_HOME + "GeckoCIRCUITS.jar";
            String cp4 = "GeckoCIRCUITS.jar";
            opt.add(cp0 + ";" + cp1 + ";" + cp2 + ";" + cp3 + ";");
            //System.out.println("options --> \n"+cp0+"\n"+cp1+"\n"+cp2+"\n"+cp3+"\n----");

            CompilationTask task = compiler.getTask(
                    compilerWriter, jfm, diagnostics, opt, null,
                    Arrays.asList(generateJavaSourceCode(_sourceString, classFileName)));

            if (!task.call()) {
                //------------------
                for (Diagnostic dm : diagnostics.getDiagnostics()) {
                    compilerWriter.println(dm);
                }
                _compileStatus = COMPILESTATUS.COMPILE_ERROR;
                compilerMessage += "Compile status: ERROR";
                //------------------
            } else {
                //------------------
                _compileStatus = COMPILESTATUS.COMPILED_SUCCESSFULL;
                compilerMessage += "\n \tCOMPILATION FINISHED SUCESSFULLY!";
                // Try to access the class and run its main method                

                ClassLoader cl = new ClassLoader() {

                    @Override
                    protected Class<?> findClass(String name) throws ClassNotFoundException {
                        JavaFileObject jfo = output.get(name);
                        if (jfo != null) {
                            byte[] bytes = ((RAMJavaFileObject) jfo).baos.toByteArray();
                            return defineClass(name, bytes, 0, bytes.length);
                        }
                        return super.findClass(name);
                    }
                };

                Class<?> c = Class.forName(_className, false, cl);
                Class clazz = c;// Class.forName(_className, true, urlCl);                

                Class[] partypes = new Class[0];
                try {
                    //---------
                    _run_script = clazz.getMethod("run_script", partypes);
                    if (_run_script == null) {
                        System.err.println("could not set extern Java code method (_run_script)!");
                        _compileStatus = COMPILESTATUS.COMPILE_ERROR;
                    }
                    //---------
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
                Class[] partypes2 = new Class[1];
                partypes2[0] = gecko.getClass();
                try {
                    //---------
                    _setGecko = clazz.getMethod("_setGecko", partypes2);
                    if (_setGecko == null) {
                        System.err.println("could not set extern Java code method (_setGecko)!");
                        _compileStatus = COMPILESTATUS.COMPILE_ERROR;
                    }
                    //---------
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
                //------------------
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GeckoJavaCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static JavaFileObject generateJavaSourceCode(final String sourceText, final String className) {

        return new SimpleJavaFileObject(toURI(className), JavaFileObject.Kind.SOURCE) {

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
                    throws IOException, IllegalStateException,
                    UnsupportedOperationException {
                return sourceText;
            }
        };
    }

    //==============================================================
    class RAMJavaFileObject extends SimpleJavaFileObject {
        // 

        RAMJavaFileObject(String name, Kind kind) {
            super(toURI(name), kind);
        }
        ByteArrayOutputStream baos;

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException, IllegalStateException,
                UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream openInputStream() throws IOException,
                IllegalStateException, UnsupportedOperationException {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        @Override
        public OutputStream openOutputStream() throws IOException,
                IllegalStateException, UnsupportedOperationException {
            return baos = new ByteArrayOutputStream();
        }
    }
    //==============================================================

    private static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void test() {
        this.setSourceCode("double cc=5;\nSystem.out.println(\"javaSourceCode --> \"+cc+\"   xx=\"+GeckoSim.xx);\n");
        this.setImportCode("import ch.technokrat.gecko.GeckoSim;\nimport ch.technokrat.gecko.geckocircuits.allg.Fenster;\n");
        this.setStaticInitCode("//staticCode");
        //-------
        try {
            this.doCompilation();
        } catch (Exception e) {
            System.out.println("Error in GeckoJavaCompiler.test(): " + e);
        }
        System.out.println("_compilerMessage= \n" + compilerMessage + "\n\n===========\n");
        //System.out.println("_sourceString= \n"+_sourceString+"\n\n===========\n"); 
        boolean calcOK = this.startCalculation();
        System.out.println("Fertig. calcOK=" + calcOK);
    }
}
