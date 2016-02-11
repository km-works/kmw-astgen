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

public class AbstractVoidVisitorGenerator extends VisitorGenerator {

  public AbstractVoidVisitorGenerator(ASTModel ast) {
    super(ast);
  }

  @Override
  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    if (options.usePLT) {
      return singleDependency(VisitorRunnable1Generator.class);
    } else {
      return singleDependency(VoidVisitorInterfaceGenerator.class);
    }
  }

  @Override
  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
  }

  @Override
  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
  }

  @Override
  protected void generateVisitor(NodeType root) {
    String visitorName = root.name() + "AbstractVisitor_void";
    TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

    // Class header
    writer.startLine("/** An abstract implementation of a visitor over " + root.name());
    writer.print(" that does not return a value.");
    writer.startLine(" ** This visitor implements the visitor interface with methods that each ");
    writer.startLine(" ** delegate to a case representing their superclass.  At the top of this");
    writer.startLine(" ** delegation tree is the method defaultCase(), which (unless overridden)");
    writer.startLine(" ** is a no-op.");
    writer.startLine(" **/");
    writer.startLine("@SuppressWarnings(\"unused\")");
    writer.startLine("public class " + visitorName);
    if (options.usePLT) {
      writer.print(" extends " + root.name() + "VisitorRunnable1");
    } else {
      writer.print(" implements " + root.name() + "Visitor_void");
    }
    writer.print(" {");
    writer.indent();

    outputDefaultCaseVoidMethod(writer, root);
    writer.println();

    // Write out forCASE methods
    writer.startLine("/* Methods to visit an item. */");
    for (NodeType t : ast.descendents(root)) {
      outputDelegatingForCase(t, writer, root, "void", "", "defaultCase");
    }

    writer.unindent();
    writer.startLine("}");
    writer.println();
    writer.close();
  }
  
}
