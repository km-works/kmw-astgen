/*BEGIN_COPYRIGHT_BLOCK*

ASTGen BSD License

Copyright (c) 2007 JavaPLT group at Rice University
All rights reserved.

Developed by:   Java Programming Languages Team
Rice University
http://www.cs.rice.edu/~javaplt/

Redistribution and use in source and binary forms, with or without modification, are permitted 
provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice, this list of conditions 
and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright notice, this list of 
conditions and the following disclaimer in the documentation and/or other materials provided 
with the distribution.
- Neither the name of the JavaPLT group, Rice University, nor the names of the tool's 
contributors may be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS AND 
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 *END_COPYRIGHT_BLOCK*/

package edu.rice.cs.astgen;

import java.io.*;
import java.util.*;

/**
 * AST generation options.
 *
 * @version $Id: Options.java,v 1.11 2008/07/14 21:27:34 dlsmith Exp $
 */
public class Options {

  private File _basedir;
  /** Name of the directory in which to place the generated source */
  public String outputDir = ".";
  /** Package to which all generated classes belong */
  public String packageName = "";
  /** Import statements to add to every generated class */
  public LinkedList<String> imports = new LinkedList<String>();
  /** Name of the generated visitor method in AST classes */
  public String visitMethod = "visit";
  /** Prefix of generated vistor classes' methods (e.g., forMyAstWidget) */
  public String visitorMethodPrefix = "for";
  /** Whether top nodes in the hierarchy are automatically roots for visiting. */
  public boolean automaticRoots = true;
  /** Should the AST classes' field accessors have a "get" prefix? (e.g., getSomeField()) */
  public boolean addGetterPrefixes = true;
  /** Allow the generated code to depend on the PLT Utilities library? */
  public boolean usePLT = false;
  /** Should generated tests to insure that AST classes' field values are never null be omitted? */
  public boolean allowNulls = false;
  public boolean generateToString = true;
  public boolean generateEquals = true;
  public boolean generateAbstractVisitors = true;
  public boolean generateRecursiveVisitors = true;
  public boolean generateVoidVisitors = true;
  public boolean generateSerializers = false;
  public boolean generateEmptyConstructor = false;
  /** Number of additional indentation spaces used to indicate a lower level in the hierarchy */
  public int tabSize = 2;

  public Options(File basedir) {
    _basedir = basedir;
  }

  private File _outputDirFile() {
    return fileRelativeToSource(new File(outputDir));
  }

  /** If the given file is a relative path, make it relative to the AST source file. */
  public File fileRelativeToSource(File f) {
    if (f.isAbsolute()) {
      return f;
    } else {
      return new File(_basedir, f.getPath());
    }
  }

  public FileWriter createFileInOutDir(String fname) {
    try {
      return new FileWriter(new File(_outputDirFile(), fname));
    } catch (IOException ioe) {
      throw new ASTGenException(ioe.toString());
    }
  }

  /**
   * Create a Java source file with name {@code fname.java} in the output directory.
   * Use the ASTGen file's tab size; output a package/import header.
   */
  public TabPrintWriter createJavaSourceInOutDir(String fname) {
    FileWriter f = createFileInOutDir(fname + ".java");
    TabPrintWriter result = new TabPrintWriter(f, tabSize);
    outputPackageStatement(result);
    outputImportStatements(result);
    return result;
  }

  public void makeOutputDir() {
    boolean ret = _outputDirFile().mkdir();
    // ignore return value. should mean dir already exists.
  }

  public void outputPackageStatement(TabPrintWriter writer) {
    if (!packageName.equals("")) {
      writer.println("package " + packageName + ";");
    }
  }

  public void outputImportStatements(TabPrintWriter writer) {
    ListIterator itor = imports.listIterator();
    while (itor.hasNext()) {
      String imp = (String) itor.next();
      writer.startLine("import " + imp + ";");
    }

    if (imports.size() > 0) {
      writer.println();
    }
  }
}
