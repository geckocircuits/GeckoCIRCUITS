/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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

package gecko.geckoscript;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.GeckoSim;
import gecko.geckocircuits.control.javablock.AbstractCompileObject;
import gecko.geckocircuits.control.javablock.CodeWindowModern;
import gecko.geckocircuits.control.javablock.CompileObject;
import gecko.geckocircuits.control.javablock.CompileObjectNull;
import gecko.geckocircuits.control.javablock.CompileStatus;
import gecko.geckocircuits.control.javablock.CompiledClassContainer;
import gecko.geckocircuits.control.javablock.JavaBlockClassLoader;
import gecko.geckocircuits.control.javablock.ControlJavaFunction;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.tools.SimpleJavaFileObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author andy
 */
public class CompileScript {
    private static final Logger LOGGER = LogManager.getLogger(CompileScript.class);


    static AbstractCompileObject _compileObject = new CompileObjectNull();
    static Map<String, CompiledClassContainer> _classNameFileMap;

    static class scriptRAMJavaFileObject extends SimpleJavaFileObject {

        scriptRAMJavaFileObject(String name, Kind kind) {
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

    static void compile(final ScriptWindow sw) {
        if(GeckoSim.compiler_toolsjar_missing) {
              JOptionPane.showMessageDialog(null, "No tools.jar library found!", "Error", JOptionPane.ERROR_MESSAGE);
            sw._compMessagesTextArea.setText("Compilar library tools.jar is missing in the ./lib directory!");
            return;
        }

        sw._compileStatus = CompileStatus.NOT_COMPILED;
        sw._declarations = sw._declarationsTextArea.getText();
        sw._className = "GeckoCustom" + sw._nameGenerator.nextInt(100000) + "";

        sw._sourceCode = "";
        sw._compileSourceCode = "";


        //create source code from user input
        try {
            String strLine;
            sw.addSourceLine("import gecko.geckoscript.AbstractGeckoCustom;");
            sw.addSourceLine("import gecko.geckoscript.SimulationAccess;");
            sw.addSourceLine("import javax.swing.JTextArea;");
            try (BufferedReader reader = new BufferedReader(new StringReader(sw._importsTextArea.getText()))) {
                while ((strLine = reader.readLine()) != null) {
                    sw.addSourceLine(strLine);
                }
            }
            sw.addSourceLine("");
            sw.addSourceLine("/**");
            sw.addSourceLine(" * Source created on " + new Date());
            sw.addSourceLine(" */");
            sw.addSourceLine("");
            sw.addSourceLine("public class " + sw._className + " extends AbstractGeckoCustom { ");
            sw.addSourceLine("");
            try (BufferedReader reader = new BufferedReader(new StringReader(sw._declarations))) {
                while ((strLine = reader.readLine()) != null) {
                    sw.addSourceLine("\t\t" + strLine);
                }
            }
            if (sw._advancedOption) {
                try (BufferedReader reader = new BufferedReader(new StringReader(sw._advancedVariables))) {
                    while ((strLine = reader.readLine()) != null) {
                        sw.addSourceLine("\t\t" + strLine);
                    }
                }
            }
            sw.addSourceLine("");
            if (sw._advancedOption) {
                sw.addSourceLine("    public " + sw._className + "(SimulationAccess simaccess, JTextArea outputArea, HashMap<String, Object> element_map) {");
            } else {
                sw.addSourceLine("    public " + sw._className + "(SimulationAccess simaccess, JTextArea outputArea) {");
            }
            sw.addSourceLine("\t\t     super(simaccess, outputArea);");
            if (sw._advancedOption) {
                try (BufferedReader reader = new BufferedReader(new StringReader(sw._advancedConstructor))) {
                    while ((strLine = reader.readLine()) != null) {
                        sw.addSourceLine("\t\t     " + strLine);
                    }
                }
            }
            sw.addSourceLine("    }");
            sw.addSourceLine("");
            sw.addSourceLine("    public void runScript() {");
            sw.addSourceLine("    try {");
            sw.addSourceLine("// ****************** your code segment **********************");
            try (BufferedReader reader = new BufferedReader(new StringReader(sw._codeTextArea.getText()))) {
                while ((strLine = reader.readLine()) != null) {
                    sw.addSourceLine("\t\t" + strLine);
                }
            }
            sw.addSourceLine("// ****************** end of code segment **********************");
            sw.addSourceLine("    } catch(Throwable ex) { writeOutputLn(\"An error occured during script execution:\");");
            sw.addSourceLine("\t\tendScript();");
            sw.addSourceLine("\t\tthrow new RuntimeException(ex);");
            sw.addSourceLine("    }");
            sw.addSourceLine("  }");
            sw.addSourceLine("}");
            //System.out.println("createSourceCode() --> \n\n_compilerMessage= \n"+_compilerMessage+"\n\n===========\n_sourceString= \n"+_sourceString+"\n\n===========\n");
        } catch (IOException ex) {LogManager.getLogger(ScriptWindow.class).error("Exception occurred", ex);
        }


        sw._compileSourceCode += sw._sourceCode + "\n";



        //compile the constructed source code into a new class
        try {
            _compileObject = new CompileObject(sw._compileSourceCode, sw._className, sw._circuit._additionalSourceFiles);
            sw.compilerMessages = _compileObject.getCompilerMessage();

            if (_compileObject.getCompileStatus() != CompileStatus.COMPILED_SUCCESSFULL) {
                sw._compileStatus = CompileStatus.COMPILE_ERROR;
                sw.compilerMessages = CodeWindowModern.checkForOldCompiler(sw.compilerMessages);
            } else {
                sw._compileStatus = CompileStatus.COMPILED_SUCCESSFULL;
            }

        } catch (IllegalArgumentException | SecurityException ex) {LogManager.getLogger(CompileScript.class).error("Exception occurred", ex);
        }



        sw._compMessagesTextArea.setText(sw.compilerMessages);
        sw._sourceCodeCompilerTextArea.setText(sw._compileSourceCode);
        findAndLoadClass(sw);
    }



    @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED",
            justification = "ClassLoader creation is intentional for dynamic class loading in scripting code")
    @SuppressWarnings("PMD.CloseResource") // ClassLoader must persist for dynamically loaded class lifecycle
    public static void findAndLoadClass(ScriptWindow sw) {
        try {
            _classNameFileMap = _compileObject.getClassNameFileMap();

            final ClassLoader classLoader = new JavaBlockClassLoader(_classNameFileMap);
            final Class clazz = Class.forName(_compileObject.getClassName(), false, classLoader);

            try {

                Constructor[] constructorlist = clazz.getConstructors();
                Constructor constructor = constructorlist[0];

                if (sw._advancedOption) {
                        sw._scriptObject = (AbstractGeckoCustom) constructor.newInstance(new Object[]{sw._circuit,
                            sw.jTextAreaOutput, sw._advancedObjects});
                    } else {
                        sw._scriptObject = (AbstractGeckoCustom) constructor.newInstance(new Object[]{sw._circuit,
                            sw.jTextAreaOutput});
                    }
            } catch (NoClassDefFoundError err) {
                err.printStackTrace();
            } catch (InstantiationException | IllegalAccessException | SecurityException ex) {LogManager.getLogger(ControlJavaFunction.class).error("Exception occurred", ex);
            } catch (IllegalArgumentException | InvocationTargetException ex) {LogManager.getLogger(CompileScript.class).error("Exception occurred", ex);
            }
        } catch (ClassNotFoundException ex) {LogManager.getLogger(ControlJavaFunction.class).error("Exception occurred", ex);
        }
    }


    private static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
