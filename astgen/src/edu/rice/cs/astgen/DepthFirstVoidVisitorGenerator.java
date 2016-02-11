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

import java.util.List;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.astgen.Types.*;

public class DepthFirstVoidVisitorGenerator extends RecursiveVisitorGenerator {

  public DepthFirstVoidVisitorGenerator(ASTModel ast) {
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
    String visitorName = root.name() + "DepthFirstVisitor_void";
    TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

    // Class header
    writer.startLine("/** An abstract implementation of a visitor over " + root.name());
    writer.print(" that does not return a value.");
    writer.startLine(" ** This visitor implements the visitor interface with methods that ");
    writer.startLine(" ** first call forCASEDoFirst(), second visit the children, and finally ");
    writer.startLine(" ** call forCASEOnly().  (CASE is replaced by the case name.)");
    writer.startLine(" ** By default, each of forCASEDoFirst and forCASEOnly delegates");
    writer.startLine(" ** to a more general case.  At the top of this delegation tree are");
    writer.startLine(" ** defaultDoFirst() and defaultCase(), respectively, which (unless");
    writer.startLine(" ** overridden) are no-ops.");
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

    writer.startLine("/**");
    writer.startLine(" * This method is run for all DoFirst cases that are not handled elsewhere.");
    writer.startLine(" * By default, it is a no-op; subclasses may override this behavior.");
    writer.startLine("**/");
    writer.startLine("public void defaultDoFirst(" + root.name() + " that) {");
    writer.print("}");
    writer.println();

    writer.startLine("/* Methods to handle a node before recursion. */");
    for (NodeType t : ast.descendents(root)) {
      outputDelegatingForCase(t, writer, root, "void", "DoFirst", "defaultDoFirst");
    }

    writer.startLine("/* Methods to handle a node after recursion. */");
    for (NodeType t : ast.descendents(root)) {
      outputDelegatingForCase(t, writer, root, "void", "Only", "defaultCase");
    }

    writer.startLine("/* Methods to recur on each child. */");
    for (NodeType t : ast.descendents(root)) {
      if (!t.isAbstract()) {
        outputVisitMethod(t, writer, root);
      }
    }

    writer.println();
    outputRecurMethod(writer, root, "void");

    // Output helpers
    for (TypeName t : helpers()) {
      writer.println();
      generateHelper(t, writer, root);
    }
    clearHelpers();

    writer.unindent();
    writer.startLine("}");
    writer.println();
    writer.close();
  }

  protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
    outputForCaseHeader(t, writer, "void", "");
    writer.indent();
    writer.startLine(visitorMethodName(t) + "DoFirst(that);");
    for (Field f : t.allFields(ast)) {
      Option<String> recur = recurExpression(f.type(), "that." + f.getGetterName() + "()", root, true);
      if (recur.isSome()) {
        writer.startLine(recur.unwrap() + ";");
      }
    }
    writer.startLine(visitorMethodName(t) + "Only(that);");
    writer.unindent();
    writer.startLine("}");
    writer.println();
  }

  protected void generateHelper(TypeName t, final TabPrintWriter writer, final NodeType root) {
    writer.startLine("public void recurOn" + t.identifierName() + "(" + t.name() + " that) {");
    writer.indent();

    t.accept(new HelperGeneratingVisitor() {

      @Override
      public Void forTreeNode(ClassName t) {
        // empty method body
        return null;
      }

      @Override
      protected void handleSequence(SequenceName t) {
        writer.startLine("for (" + t.elementType().name() + " elt : " + t.iterable("that") + ") {");
        writer.indent();
        // we assume t has an element type that can be recurred on
        writer.startLine(recurExpression(t.elementType(), "elt", root, false).unwrap() + ";");
        writer.unindent();
        writer.startLine("}");
      }

      @Override
      public Void forOptionClass(OptionClassName t) {
        // we assume t has an element type that can be recurred on
        String recur = recurExpression(t.elementType(), t.nonEmptyGetter("that"), root, false).unwrap();
        writer.startLine("if (!" + t.emptyTester("that") + ") " + recur + ";");
        return null;
      }

      @Override
      public Void forTupleClass(TupleClassName t) {
        List<TypeName> eltTs = t.elementTypes();
        for (int i = 0; i < eltTs.size(); i++) {
          Option<String> recur = recurExpression(eltTs.get(i), t.getter("that", i), root, false);
          if (recur.isSome()) {
            writer.startLine(recur.unwrap() + ";");
          }
        }
        return null;
      }
      
    });

    writer.unindent();
    writer.startLine("}");
  }
  
}
