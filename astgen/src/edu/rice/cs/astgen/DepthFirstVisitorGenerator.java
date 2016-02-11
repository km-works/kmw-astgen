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

import java.util.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.astgen.Types.*;

public class DepthFirstVisitorGenerator extends RecursiveVisitorGenerator {

  private Set<TypeName> _arraySeeds; // classes needed for array seeds

  public DepthFirstVisitorGenerator(ASTModel ast) {
    super(ast);
    // sort seeds smallest to largest (necessary for dependencies between fields)
    _arraySeeds = new TreeSet<TypeName>(new Comparator<TypeName>() {

      @Override
      public int compare(TypeName t1, TypeName t2) {
        return t1.identifierName().compareTo(t2.identifierName());
      }
    });
  }

  protected Iterable<TypeName> arraySeeds() {
    return _arraySeeds;
  }

  /** Clear the arraySeeds list.  Should be called after arraySeeds have been generated. */
  protected void clearArraySeeds() {
    _arraySeeds.clear();
  }

  @Override
  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    if (options.usePLT) {
      return singleDependency(VisitorLambdaGenerator.class);
    } else {
      return singleDependency(VisitorInterfaceGenerator.class);
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
    String visitorName = root.name() + "DepthFirstVisitor";
    TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

    // Class header
    writer.startLine("/** A parametric abstract implementation of a visitor over " + root.name());
    writer.print(" that returns a value.");
    writer.startLine(" ** This visitor implements the visitor interface with methods that ");
    writer.startLine(" ** first visit children, and then call forCASEOnly(), passing in ");
    writer.startLine(" ** the values of the visits of the children. (CASE is replaced by the case name.)");
    writer.startLine(" ** By default, each of forCASEOnly delegates to a more general case; at the");
    writer.startLine(" ** top of this delegation tree is defaultCase(), which (unless overridden)");
    writer.startLine(" ** throws an exception.");
    writer.startLine(" **/");
    writer.startLine("@SuppressWarnings(\"unused\")");
    writer.startLine("public abstract class " + visitorName + "<RetType>");
    if (options.usePLT) {
      writer.print(" extends " + root.name() + "VisitorLambda<RetType>");
    } else {
      writer.print(" implements " + root.name() + "Visitor<RetType>");
    }
    writer.print(" {");
    writer.indent();

    outputDefaultCaseMethod(writer, root);
    writer.println();

    // Write out forCASEOnly methods
    writer.startLine("/* Methods to handle a node after recursion. */");
    for (NodeType t : ast.descendents(root)) {
      outputForCaseOnly(t, writer, root);
    }
    writer.println();


    // Write implementation of visit methods
    writer.startLine("/** Methods to recur on each child. */");
    for (NodeType t : ast.descendents(root)) {
      if (!t.isAbstract()) {
        outputVisitMethod(t, writer, root);
      }
    }

    writer.println();
    outputRecurMethod(writer, root, "RetType");

    // Output helpers, if necessary
    for (TypeName t : helpers()) {
      writer.println();
      generateHelper(t, writer, root);
    }
    clearHelpers();

    // output array seeds, if necessary
    for (TypeName t : _arraySeeds) {
      writer.println();
      generateArraySeed(t, writer);
    }
    clearArraySeeds();

    writer.unindent();
    writer.startLine("}");
    writer.println();
    writer.close();
  }

  protected void outputVisitMethod(NodeType t, TabPrintWriter writer, NodeType root) {
    outputForCaseHeader(t, writer, "RetType", "");
    writer.indent();
    List<String> recurVals = new LinkedList<String>();
    for (Field f : t.allFields(ast)) {
      Option<String> recur = recurExpression(f.type(), "that." + f.getGetterName() + "()", root, true);
      if (recur.isSome()) {
        TypeName recurT = resultType(f.type());
        String recurName = f.name() + "_result";
        writer.startLine(recurT.name() + " " + recurName + " = " + recur.unwrap() + ";");
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
    List<String> recurDecls = new LinkedList<String>();
    for (Field f : t.allFields(ast)) {
      if (canRecurOn(f.type(), root)) {
        recurDecls.add(resultType(f.type()).name() + " " + f.name() + "_result");
      }
    }
    outputForCaseHeader(t, writer, "RetType", "Only", recurDecls);
    writer.indent();
    writer.startLine("return ");
    Option<NodeType> sup = ast.parent(t);
    if (sup.isSome() && !t.equals(root)) {
      writer.print(visitorMethodName(sup.unwrap()) + "Only(that");
      for (Field f : sup.unwrap().allFields(ast)) {
        if (canRecurOn(f.type(), root)) {
          writer.print(", " + f.name() + "_result");
        }
      }
      writer.print(");");
    } else {
      writer.print("defaultCase(that);");
    }
    writer.unindent();
    writer.startLine("}");
    writer.println();
  }

  protected void generateHelper(TypeName t, final TabPrintWriter writer, final NodeType root) {
    final TypeName resultT = resultType(t);
    writer.startLine("public " + resultT.name() + " recurOn" + t.identifierName()
            + "(" + t.name() + " that) {");
    writer.indent();

    t.accept(new HelperGeneratingVisitor() {

      @Override
      public Void forTreeNode(ClassName t) {
        throw error(t);
      }

      @Override
      protected void handleSequence(SequenceName t) {
        SequenceName resultSeqT = (SequenceName) resultT;
        writer.startLine(resultSeqT.accumulator("accum"));
        writer.startLine("for (" + t.elementType().name() + " elt : " + t.iterable("that") + ") {");
        writer.indent();
        // we assume t has an element type that can be recurred on
        String recur = recurExpression(t.elementType(), "elt", root, false).unwrap();
        writer.startLine(resultSeqT.addToAccumulator("accum", recur));
        writer.unindent();
        writer.startLine("}");
        writer.startLine("return " + resultSeqT.constructor("accum") + ";");
      }

      @Override
      public Void forOptionClass(OptionClassName t) {
        OptionClassName resultOptT = (OptionClassName) resultT;
        writer.startLine("if (" + t.emptyTester("that") + ") return " + resultOptT.emptyConstructor() + ";");
        // we assume t has an element type that can be recurred on
        String recur = recurExpression(t.elementType(), t.nonEmptyGetter("that"), root, false).unwrap();
        writer.startLine("else return " + resultOptT.nonEmptyConstructor(recur) + ";");
        return null;
      }

      @Override
      public Void forTupleClass(TupleClassName t) {
        TupleClassName resultTupT = (TupleClassName) resultT;
        List<TypeName> eltTs = t.elementTypes();
        List<String> resultElts = new ArrayList<String>(eltTs.size());
        for (int i = 0; i < eltTs.size(); i++) {
          String getter = t.getter("that", i);
          Option<String> recur = recurExpression(eltTs.get(i), getter, root, false);
          if (recur.isSome()) {
            String var = "result_" + i;
            writer.startLine(var + " = " + recur.unwrap() + ";");
            resultElts.add(var);
          } else {
            resultElts.add(getter);
          }
        }
        writer.startLine("return " + resultTupT.constructor(resultElts) + ";");
        return null;
      }
      
    });

    writer.unindent();
    writer.startLine("}");
  }

  protected void generateArraySeed(TypeName t, TabPrintWriter writer) {
    if (t instanceof ReferenceArrayName) {
      TypeName eltT = ((ReferenceArrayName) t).elementType();
      writer.startLine("private final java.lang.Class<?> " + arraySeedExpression(t, false) + " = "
              + "java.lang.reflect.Array.newInstance(" + arraySeedExpression(eltT, false)
              + ", 0).getClass();");
    } else {
      writer.startLine("protected abstract java.lang.Class<" + t.name() + "> "
              + arraySeedExpression(t, false) + ";");
    }
  }

  /**
   * Overridden to skip tree nodes that are not subtypes of root (there's no good default to
   * return, and we don't want to force users to provide a number of defaults for types on which
   * they don't want to recur).
   */
  @Override
  protected Option<String> recurExpression(TypeName t, String valExpr, NodeType root,
          boolean addHelpers) {
    return t.accept(new RecurExpressionVisitor(valExpr, root, addHelpers) {

      @Override
      public Option<String> forTreeNode(ClassName t) {
        NodeType nodeType = ast.typeForName(t).unwrap();
        if (ast.isDescendent(_root, nodeType)) {
          return Option.some("recur(" + _valExpr + ")");
        } else {
          return Option.none();
        }
      }
    });
  }

  /** Determine the type of the result of recurring on the given type. */
  protected TypeName resultType(TypeName t) {
    if (t instanceof SequenceName) {
      t = ((SequenceName) t).withMappedOriginal("that");
    }
    final TypeName retT = Types.parse("RetType", ast);
    // boolean indicates whether "RetType" is exposed -- "RetType", "RetType[]", "RetType[][]", etc.
    return t.accept(new TypeNameVisitor<Pair<TypeName, Boolean>>() {

      @Override
      public Pair<TypeName, Boolean> forTreeNode(ClassName t) {
        return new Pair<TypeName, Boolean>(retT, true);
      }

      @Override
      public Pair<TypeName, Boolean> forPrimitive(PrimitiveName t) {
        return new Pair<TypeName, Boolean>(t, false);
      }

      @Override
      public Pair<TypeName, Boolean> forString(ClassName t) {
        return new Pair<TypeName, Boolean>(t, false);
      }

      @Override
      public Pair<TypeName, Boolean> forPrimitiveArray(PrimitiveArrayName t) {
        return new Pair<TypeName, Boolean>(t, false);
      }

      @Override
      public Pair<TypeName, Boolean> forGeneralClass(ClassName t) {
        return new Pair<TypeName, Boolean>(t, false);
      }

      @Override
      public Pair<TypeName, Boolean> forReferenceArray(ReferenceArrayName t) {
        Pair<TypeName, Boolean> eltTData = t.elementType().accept(this);
        TypeName eltT = eltTData.first();
        ReferenceArrayName resultT = t.withElementType(eltT);
        if (eltTData.second()) {
          // RetType is exposed -- we need a seed to be able to safely create instances
          return new Pair<TypeName, Boolean>(resultT.withSeed(arraySeedExpression(eltT, true)), true);
        } else {
          return new Pair<TypeName, Boolean>(resultT, false);
        }
      }

      @Override
      public Pair<TypeName, Boolean> forSequenceClass(SequenceClassName t) {
        TypeName eltT = t.elementType().accept(this).first();
        return new Pair<TypeName, Boolean>(t.withElementType(eltT), false);
      }

      @Override
      public Pair<TypeName, Boolean> forOptionClass(OptionClassName t) {
        return new Pair<TypeName, Boolean>(t.withElementType(t.elementType().accept(this).first()), false);
      }

      @Override
      public Pair<TypeName, Boolean> forTupleClass(TupleClassName t) {
        Iterable<TypeName> eltTs = IterUtil.pairFirsts(Types.mapVisitor(t.elementTypes(), this));
        return new Pair<TypeName, Boolean>(t.withElementTypes(eltTs), false);
      }
    }).first();
  }

  /** Get an expression that will produce a class of the given type. */
  protected String arraySeedExpression(TypeName t, boolean addToSet) {
    if (addToSet) {
      _arraySeeds.add(t);
    }
    if (t instanceof ReferenceArrayName) {
      return "classFor" + t.identifierName();
    } else {
      return "get" + t.identifierName() + "()";
    }
  }
  
}
