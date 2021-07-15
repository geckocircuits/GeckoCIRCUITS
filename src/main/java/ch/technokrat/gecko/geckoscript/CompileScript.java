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

package ch.technokrat.gecko.geckoscript;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoRuntimeException;
import ch.technokrat.gecko.geckocircuits.control.javablock.AbstractCompileObject;
import ch.technokrat.gecko.geckocircuits.control.javablock.CodeWindow;
import ch.technokrat.gecko.geckocircuits.control.javablock.CompileObject;
import ch.technokrat.gecko.geckocircuits.control.javablock.CompileObjectNull;
import ch.technokrat.gecko.geckocircuits.control.javablock.CompileStatus;
import ch.technokrat.gecko.geckocircuits.control.javablock.CompiledClassContainer;
import ch.technokrat.gecko.geckocircuits.control.javablock.JavaBlockClassLoader;
import ch.technokrat.gecko.geckocircuits.control.javablock.ReglerJavaFunction;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/**
 *
 * @author andy
 */
public class CompileScript {

    static protected AbstractCompileObject _compileObject = new CompileObjectNull();
    protected static Map<String, CompiledClassContainer> _classNameFileMap;
    private static AbstractGeckoCustom _compiledInstance;
    
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
        if(Fenster.IS_APPLET) return;
        
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
            sw.addSourceLine("import ch.technokrat.gecko.geckoscript.AbstractGeckoCustom;");
            sw.addSourceLine("import ch.technokrat.gecko.geckoscript.SimulationAccess;");
            sw.addSourceLine("import javax.swing.JTextArea;");
            BufferedReader reader = new BufferedReader(new StringReader(sw._importsTextArea.getText()));
            while ((strLine = reader.readLine()) != null) {
                sw.addSourceLine(strLine);
            }
            sw.addSourceLine("");
            sw.addSourceLine("/**");
            sw.addSourceLine(" * Source created on " + new Date());
            sw.addSourceLine(" */");
            sw.addSourceLine("");
            sw.addSourceLine("public class " + sw._className + " extends AbstractGeckoCustom { ");
            sw.addSourceLine("");
            reader = new BufferedReader(new StringReader(sw._declarations));
            while ((strLine = reader.readLine()) != null) {
                sw.addSourceLine("\t\t" + strLine);
            }
            if (sw._advancedOption) {
                reader = new BufferedReader(new StringReader(sw._advancedVariables));
                while ((strLine = reader.readLine()) != null) {
                    sw.addSourceLine("\t\t" + strLine);
                }
            }
            sw.addSourceLine("");
            if (sw._advancedOption) {
                sw.addSourceLine("    public " + sw._className + "(SimulationAccess simaccess, JTextArea outputArea, HashMap element_map) {");
            } else {
                sw.addSourceLine("    public " + sw._className + "(SimulationAccess simaccess, JTextArea outputArea) {");
            }
            sw.addSourceLine("\t\t     super(simaccess, outputArea);");
            if (sw._advancedOption) {
                reader = new BufferedReader(new StringReader(sw._advancedConstructor));
                while ((strLine = reader.readLine()) != null) {
                    sw.addSourceLine("\t\t     " + strLine);
                }
            }
            sw.addSourceLine("    }");
            sw.addSourceLine("");
            sw.addSourceLine("    public void runScript() {");
            sw.addSourceLine("    try {");
            sw.addSourceLine("// ****************** your code segment **********************");
            reader = new BufferedReader(new StringReader(sw._codeTextArea.getText()));
            while ((strLine = reader.readLine()) != null) {
                sw.addSourceLine("\t\t" + strLine);
            }
            sw.addSourceLine("// ****************** end of code segment **********************");
            sw.addSourceLine("    } catch(Throwable ex) { writeOutputLn(\"An error occured during script execution:\");");
            sw.addSourceLine("\t\tendScript();");
            sw.addSourceLine("\t\tthrow new RuntimeException(ex);");
            sw.addSourceLine("    }");
            sw.addSourceLine("  }");
            sw.addSourceLine("}");
            //
            //System.out.println("createSourceCode() --> \n\n_compilerMessage= \n"+_compilerMessage+"\n\n===========\n_sourceString= \n"+_sourceString+"\n\n===========\n");
        } catch (IOException ex) {
            Logger.getLogger(ScriptWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
       

        sw._compileSourceCode += sw._sourceCode + "\n";

        
        
        //compile the constructed source code into a new class
        try {                               
            _compileObject = new CompileObject(sw._compileSourceCode, sw._className, sw._circuit._additionalSourceFiles);                                
            sw.compilerMessages = _compileObject.getCompilerMessage();

            if (_compileObject.getCompileStatus() != CompileStatus.COMPILED_SUCCESSFULL) {                
                sw._compileStatus = CompileStatus.COMPILE_ERROR;                
                sw.compilerMessages = CodeWindow.checkForOldCompiler(sw.compilerMessages);
            } else {
                sw._compileStatus = CompileStatus.COMPILED_SUCCESSFULL;                                
            }

        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CompileScript.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(CompileScript.class.getName()).log(Level.SEVERE, null, ex);
        } 
          
        

        sw._compMessagesTextArea.setText(sw.compilerMessages);
        sw._sourceCodeCompilerTextArea.setText(sw._compileSourceCode);
        findAndLoadClass(sw);
    }
    
    
        
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
            } catch (InstantiationException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(CompileScript.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(CompileScript.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
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
