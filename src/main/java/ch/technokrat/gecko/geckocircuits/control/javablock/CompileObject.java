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
//CHECKSTYLE:OFF // automatic import formatting introduces some checkstyle errors...
package ch.technokrat.gecko.geckocircuits.control.javablock;

import ch.technokrat.gecko.GeckoRuntimeException;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GetJarPath;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import javax.tools.*;
// CHECKSTYLE:ON

/**
 * CompileObject takes sourcecode and compiles it into a map, where the map gives a connection between the java file name and the
 * compiled JavaFileObject. CompileObject is similar to "immutable", everything is done in the constructor, output messages and
 * so on can therefore be final.
 *
 * @author andreas
 */
public final class CompileObject extends AbstractCompileObject {

    private final String _compilerMessage;
    private final String _className;
    private final String _sourceString;
    private final Map<String, RamJavaFileObject> _nameFileMap = new HashMap<String, RamJavaFileObject>();
    private final String _workingDirectory;
    private CompileStatus _compileStatus = CompileStatus.NOT_COMPILED;
    private final StringWriter _stringWriter = new StringWriter();
    private final PrintWriter _compilerWriter = new PrintWriter(_stringWriter, true);
    private final List<String> _compilerOptions;
    private static final Random _generator = new Random(System.currentTimeMillis());
    private final DiagnosticCollector<JavaFileObject> _diagnostics = new DiagnosticCollector<JavaFileObject>();
    private final String _classFileName;

    @Override
    public CompileStatus getCompileStatus() {
        return _compileStatus;
    }

    public CompileObject(final String sourceString, final String className, final List<GeckoFile> additionalSourceFiles) {
        super();
        _className = className;
        _classFileName = _className + ".java";
        _workingDirectory = findWorkingDirectory();

        _sourceString = sourceString;                                
        _compilerOptions = setCompilerOptions();

        try {
            final CompilationTask task = createCompilationTask(additionalSourceFiles);

            if (task.call()) {
                _compileStatus = CompileStatus.COMPILED_SUCCESSFULL;
                _compilerWriter.append("\n \tCOMPILATION FINISHED SUCESSFULLY!");


                for (Entry<String, RamJavaFileObject> entry : _nameFileMap.entrySet()) {
                    final String fileName = entry.getKey();
                    // I am using "startsWith" instead of equals, since local classes
                    // will be translated into classname$localclassname.
                    if (fileName.startsWith(_className)) {
                        _classMap.put(fileName, new CompiledClassContainer(entry.getValue().getByteArray(),
                                _sourceString));
                    }  
                    else {
                        for (GeckoFile externalSource : additionalSourceFiles) {
                            if (externalSource.getName().equals(fileName + ".java")) {
                                _classMap.put(fileName, new CompiledClassContainer(entry.getValue().getByteArray(),
                                        externalSource.getContentsString()));
                            }
                        }
                    }

                }



            } else {
                for (Diagnostic dm : _diagnostics.getDiagnostics()) {
                    _compilerWriter.println(dm);
                }
                _compileStatus = CompileStatus.COMPILE_ERROR;
                _compilerWriter.append("Compile status: ERROR");
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
        }

        _compilerWriter.close();
        _compilerMessage = _stringWriter.toString();

    }

    public static JavaFileObject generateJavaSourceCode(final String sourceText, final String className) {
        return new SimpleJavaFileObjectImpl(RamJavaFileObject.toURI(className), JavaFileObject.Kind.SOURCE, sourceText);
    }
    

    @Override
    public String getCompilerMessage() {
        return _compilerMessage;
    }

    @Override
    public String getClassName() {
        return _className;
    }

    @Override
    public String getSourceCode() {
        return _sourceString;
    }

    @Override
    void setErrorStatus() {
        _compileStatus = CompileStatus.COMPILE_ERROR;
    }

    private String findWorkingDirectory() {
        String returnValue;
        if (GlobalFilePathes.DATNAM == null || new File(GlobalFilePathes.DATNAM).getParent() != null) {
            returnValue = System.getProperty("user.dir");
        } else {
            returnValue = new File(GlobalFilePathes.DATNAM).getParent();
        }

        if (returnValue == null) {
            return "";
        }

        return returnValue;
    }

    private JavaCompiler findCompiler() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) { // this fixes the java 1.7 compilation problem
            try {
                try {
                    compiler = (JavaCompiler) Class.forName("com.sun.tools.javac.api.JavacTool").newInstance();
                } catch (InstantiationException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (compiler == null) {
                _compilerWriter.append("No Java compiler found. Aborting");
                JOptionPane.showMessageDialog(null, _compilerMessage, "Error!",
                        JOptionPane.ERROR_MESSAGE);
                throw new GeckoRuntimeException("Could not find compiler.");
            }
        }
        return compiler;
    }

    private List<String> setCompilerOptions() {
        final List<String> options = new ArrayList<String>();
     
        options.add("-classpath");

        String javaSeparator = ":";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            javaSeparator = ";";
        }

        final String directoryPath = System.getProperty("user.dir") + javaSeparator + _workingDirectory;
        options.add(directoryPath);

        final StringBuilder classPathString = new StringBuilder(System.getProperty("java.class.path"));

        final String jarFilePath = GetJarPath.getJarPath();
        if (jarFilePath != null) {
            final File directory = new File(jarFilePath + "/lib/");
            if (directory.isDirectory() && directory.isAbsolute()) {                
                final File[] children = directory.listFiles();
                for (File file : children) {
                    if (file.getName().endsWith(".jar")) {
                        classPathString.append(javaSeparator);
                        classPathString.append(file.getAbsolutePath());
                    }
                }
            }
        }

        //add classpath to running program to be able to access all gecko classes from the newly created class
        //System.out.println("classpath string: " + classPathString);
        options.addAll(Arrays.asList("-classpath", classPathString.toString()));
        
        return Collections.unmodifiableList(options);
    }

    public static String findUniqueClassName() {
        return "tmpJav" + Math.abs(_generator.nextInt());
    }

    private CompilationTask createCompilationTask(final List<GeckoFile> additionalSourceFiles) {

        final JavaCompiler compiler = findCompiler();
        final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(_diagnostics, null, null);
        final JavaFileManager jfm = new GeckoForwardingFileManager(standardFileManager, _nameFileMap);

        final List<JavaFileObject> filesToCompile = new ArrayList<JavaFileObject>();
        filesToCompile.add(generateJavaSourceCode(_sourceString, _classFileName));

        for (GeckoFile extraSourceFile : additionalSourceFiles) {
            final String fileContents = extraSourceFile.getContentsString();
            final String fileName = extraSourceFile.getName();
            filesToCompile.add(generateJavaSourceCode(fileContents, fileName));
        }

        return compiler.getTask(_compilerWriter, jfm, _diagnostics, _compilerOptions, null, filesToCompile);
    }

    private static class SimpleJavaFileObjectImpl extends SimpleJavaFileObject {

        private final String sourceText;

        public SimpleJavaFileObjectImpl(final URI uri, final Kind kind, final String sourceText) {
            super(uri, kind);
            this.sourceText = sourceText;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors)
                throws IOException {
            return sourceText;
        }
    }
}
