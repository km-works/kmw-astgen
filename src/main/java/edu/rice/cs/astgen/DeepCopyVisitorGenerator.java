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

///+ <CR003>

package edu.rice.cs.astgen;

import java.util.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.astgen.Types.*;

public class DeepCopyVisitorGenerator extends RecursiveVisitorGenerator {

  public DeepCopyVisitorGenerator(ASTModel ast) {
    super(ast);
  }

  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    if (options.usePLT) {
      return singleDependency(VisitorLambdaGenerator.class);
    } else {
      return singleDependency(VisitorInterfaceGenerator.class);
    }
  }

  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
  }

  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
  }

  protected void generateVisitor(NodeType root) {
    String visitorName = root.name() + "DeepCopyVisitor";
    TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

    // Class header
    writer.startLine("/** ");
    writer.startLine(" * A depth-first visitor that makes a deep copy as it visits (by default).");
    writer.startLine(" * The type of the result is generally the same as that of the argument; where");
    writer.startLine(" * automatic recursion on a field of type T occurs, this must be true for T.");
    writer.startLine(" * This visitor implements the visitor interface with methods that ");
    writer.startLine(" * first update the children, and then call forCASEOnly(), passing in ");
    writer.startLine(" * the values of the updated children. (CASE is replaced by the case name.)");
    writer.startLine(" * Override forCASE or forCASEOnly if you want to transform an AST subtree.");
    writer.startLine(" * There is no automatic delegation to more general cases, because each concrete");
    writer.startLine(" * case has a default implementation.");
    writer.startLine(" */");
    writer.startLine("@SuppressWarnings(\"unused\")");
    writer.startLine("public abstract class " + visitorName);
    if (options.usePLT) {
      writer.print(" extends " + root.name() + "VisitorLambda<" + root.name() + ">");
    } else {
      writer.print(" implements " + root.name() + "Visitor<" + root.name() + ">");
    }
    writer.print(" {");
    writer.indent();
    writer.println();

    writer.startLine("/* Methods to handle a node after recursion. */");
    for (NodeType t : ast.descendents(root)) {
      if (!t.isAbstract()) {
        writer.println();
        outputForCaseOnly(t, writer, root);
      }
    }
    writer.println();

    writer.startLine("/** Methods to recur on each child. */");
    for (NodeType t : ast.descendents(root)) {
      if (!t.isAbstract()) {
        writer.println();
        outputVisitMethod(t, writer, root);
      }
    }

    writer.println();
    outputRecurMethod(writer, root, root.name());

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
    outputForCaseHeader(t, writer, root.name(), "");
    writer.indent();
    List<String> recurVals = new LinkedList<String>();
    for (Field f : t.allFields(ast)) {
      Option<String> recur = recurExpression(f.type(), "that." + f.getGetterName() + "()", root, true);
      if (recur.isSome()) {
        String recurName = f.name() + "_result";
        writer.startLine(f.type().name() + " " + recurName + " = " + recur.unwrap() + ";");
        recurVals.add(recurName);
      }
    }
    writer.startLine("return " + visitorMethodName(t) + "Only(that");
    for (String recurVal : recurVals) {
      writer.print(", " + recurVal);
    }
    writer.print(");");
    writer.unindent();
    writer.startLine("}");
    writer.println();
  }

  protected void outputForCaseOnly(NodeType t, TabPrintWriter writer, NodeType root) {
    // only called for concrete cases; must not delegate
    List<String> params = new LinkedList<String>(); // "type name" strings
    List<String> getters = new LinkedList<String>(); // expressions
    List<String> paramRefs = new LinkedList<String>(); // variable names or null
    for (Field f : t.allFields(ast)) {
      getters.add("that." + f.getGetterName() + "()");
      if (canRecurOn(f.type(), root)) {
        String paramName = f.name() + "_result";
        params.add(f.type().name() + " " + paramName);
        paramRefs.add(paramName);
      } else {
        paramRefs.add(null);
      }
    }
    outputForCaseHeader(t, writer, root.name(), "Only", params);
    writer.indent();
    /*!*/
    boolean first = true;
    writer.startLine("return new " + t.name() + "(");
    first = true;
    for (Pair<String, String> getterAndRef : IterUtil.zip(getters, paramRefs)) {
      if (first) {
        first = false;
      } else {
        writer.print(", ");
      }
      if (getterAndRef.second() == null) {
        writer.print(getterAndRef.first());
      } else {
        writer.print(getterAndRef.second());
      }
    }
    writer.print(");");
    /*!*/
    writer.unindent();
    writer.startLine("}");
  }

  protected void generateHelper(TypeName t, final TabPrintWriter writer, final NodeType root) {
    writer.startLine("public " + t.name() + " recurOn" + t.identifierName() + "(" + t.name() + " that) {");
    writer.indent();

    t.accept(new HelperGeneratingVisitor() {

      public Void forTreeNode(ClassName t) {
        writer.startLine("return that;");
        return null;
      }

      protected void handleSequence(SequenceName t) {
        writer.startLine(t.accumulator("accum"));
        writer.startLine("boolean unchanged = true;");
        writer.startLine("for (" + t.elementType().name() + " elt : " + t.iterable("that") + ") {");
        writer.indent();
        // we assume t has an element type that can be recurred on
        String recur = recurExpression(t.elementType(), "elt", root, false).unwrap();
        writer.startLine(t.elementType().name() + " update_elt = " + recur + ";");
        writer.startLine("unchanged &= (elt == update_elt);");
        writer.startLine(t.addToAccumulator("accum", "update_elt"));
        writer.unindent();
        writer.startLine("}");
        writer.startLine("return unchanged ? that : " + t.constructor("accum") + ";");
      }

      public Void forOptionClass(OptionClassName t) {
        TypeName eltT = t.elementType();
        writer.startLine("if (" + t.emptyTester("that") + ") { return that; }");
        writer.startLine("else {");
        writer.indent();
        writer.startLine(eltT.name() + " original = " + t.nonEmptyGetter("that") + ";");
        // we assume t has an element type that can be recurred on
        String recur = recurExpression(eltT, "original", root, false).unwrap();
        writer.startLine(eltT.name() + " updated = " + recur + ";");
        writer.startLine("return (original == updated) ? that : " + t.nonEmptyConstructor("updated") + ";");
        writer.unindent();
        writer.startLine("}");
        return null;
      }

      public Void forTupleClass(TupleClassName t) {
        List<TypeName> eltTs = t.elementTypes();
        List<String> resultElts = new ArrayList<String>(eltTs.size());
        // expressions that must all be true for the tuple to be unchanged
        List<String> unchangedTests = new ArrayList<String>();
        for (int i = 0; i < eltTs.size(); i++) {
          TypeName eltT = eltTs.get(i);
          String getter = t.getter("that", i);
          String origVar = "original_" + i;
          String updateVar = "updated_" + i;
          Option<String> recur = recurExpression(eltT, origVar, root, false);
          if (recur.isSome()) {
            writer.startLine(eltT.name() + " " + origVar + " = " + getter + ";");
            writer.startLine(eltT.name() + " " + updateVar + " = " + recur.unwrap() + ";");
            resultElts.add(updateVar);
            unchangedTests.add(origVar + " == " + updateVar);
          } else {
            resultElts.add(getter);
          }
        }
        // we assume at least one element can be recurred on
        writer.startLine("if " + IterUtil.toString(unchangedTests, "(", " && ", ")") + " {");
        writer.indent();
        writer.startLine("return that;");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("else {");
        writer.indent();
        writer.startLine("return " + t.constructor(resultElts) + ";");
        writer.unindent();
        writer.startLine("}");
        return null;
      }
    });
    writer.unindent();
    writer.startLine("}");
  }

  @Override
  protected Option<String> recurExpression(TypeName t, String valExpr, NodeType root,
          boolean addHelpers) {
    // overridden to downcast tree nodes when necessary
    Option<String> result = super.recurExpression(t, valExpr, root, addHelpers);
    if (result.isSome()) {
      Option<NodeType> treeNode = ast.typeForName(t);
      if (treeNode.isSome() && ast.isDescendent(root, treeNode.unwrap())
              && !treeNode.unwrap().equals(root)) {
        return Option.some("(" + t.name() + ") " + result.unwrap());
      }
    }
    return result;
  }
}

///+ </CR003>

