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

import java.util.Set;
import java.util.LinkedHashSet;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.astgen.Types.*;

/** Shared code for visitor code generators that produce code for recursion. */
public abstract class RecursiveVisitorGenerator extends VisitorGenerator {

  private Set<TypeName> _helpers; // types that need helper methods

  public RecursiveVisitorGenerator(ASTModel ast) {
    super(ast);
    // preserve textual order of helper types
    _helpers = new LinkedHashSet<TypeName>();
  }

  /** Get the list of helpers that need to be generated. */
  protected Iterable<TypeName> helpers() {
    return _helpers;
  }

  /** Clear the helper list.  Should be called after helpers have been generated. */
  protected void clearHelpers() {
    _helpers.clear();
  }

  /**
   * Tests whether the given type can be recurred on.  Note that if a recursion expression is
   * needed, {@link #recurExpression} should be called directly without making this test first.
   */
  protected boolean canRecurOn(TypeName t, NodeType root) {
    return recurExpression(t, "dummyExpr", root, false).isSome();
  }

  /** Outputs a method for recurring on types that can be handled directly by this visitor. */
  protected void outputRecurMethod(TabPrintWriter writer, NodeType root, String retType) {
    writer.startLine("public " + retType + " recur(" + root.name() + " that) {");
    writer.indent();
    String prefix = retType.equals("void") ? "" : "return ";
    writer.startLine(prefix + "that." + options.visitMethod + "(this);");
    writer.unindent();
    writer.startLine("}");
  }

  /**
   * Get an expression that will recur on an object of the given type (called from the visitor class).
   * @param t  Type to recur on.
   * @param valExpr  Expression with type {@code t} producing a value to be recurred on.
   * @param addHelpers  Whether the type and its dependencies should be added to the set of helpers 
   *                    to be generated.
   */
  protected Option<String> recurExpression(TypeName t, String valExpr, NodeType root, boolean addHelpers) {
    return t.accept(new RecurExpressionVisitor(valExpr, root, addHelpers));
  }

  protected class RecurExpressionVisitor implements TypeNameVisitor<Option<String>> {

    protected String _valExpr;
    protected NodeType _root;
    protected boolean _addHelpers;

    public RecurExpressionVisitor(String valExpr, NodeType root, boolean addHelpers) {
      _valExpr = valExpr;
      _root = root;
      _addHelpers = addHelpers;
    }

    public Option<String> forPrimitive(PrimitiveName t) {
      return Option.none();
    }

    public Option<String> forString(ClassName t) {
      return Option.none();
    }

    public Option<String> forPrimitiveArray(PrimitiveArrayName t) {
      return Option.none();
    }

    public Option<String> forGeneralClass(ClassName t) {
      return Option.none();
    }

    public Option<String> forTreeNode(ClassName t) {
      NodeType nodeType = ast.typeForName(t).unwrap();
      if (ast.isDescendent(_root, nodeType)) {
        return Option.some("recur(" + _valExpr + ")");
      } else {
        return handleType(t, true);
      }
    }

    public Option<String> forReferenceArray(ReferenceArrayName t) {
      return handleType(t, t.elementType().accept(this).isSome());
    }

    public Option<String> forSequenceClass(SequenceClassName t) {
      return handleType(t, t.elementType().accept(this).isSome());
    }

    public Option<String> forOptionClass(OptionClassName t) {
      return handleType(t, t.elementType().accept(this).isSome());
    }

    public Option<String> forTupleClass(TupleClassName t) {
      // take a snapshot to force recursion (and related effects) on all elements
      Iterable<Option<String>> elementStrings = IterUtil.snapshot(Types.mapVisitor(t.elementTypes(), this));
      boolean recur = false;
      for (Option<String> s : elementStrings) {
        recur |= s.isSome();
        if (recur) {
          break;
        }
      }
      return handleType(t, recur);
    }

    protected Option<String> handleType(TypeName t, boolean recur) {
      if (recur) {
        if (_addHelpers) {
          _helpers.add(t);
        }
        return Option.some("recurOn" + t.identifierName() + "(" + _valExpr + ")");
      } else {
        return Option.none();
      }
    }
  }

  /**
   * Skeleton of a visitor for generating a helper method for a type. Throws an exception
   * for types that should not appear in the helpers set.  Subclasses only need to handle
   * tree nodes (of types that can't be directly handled by this visitor), sequences,
   * options, and tuples.
   */
  protected static abstract class HelperGeneratingVisitor implements TypeNameVisitor<Void> {

    public Void forPrimitive(PrimitiveName t) {
      throw error(t);
    }

    public Void forString(ClassName t) {
      throw error(t);
    }

    public Void forPrimitiveArray(PrimitiveArrayName t) {
      throw error(t);
    }

    public Void forGeneralClass(ClassName t) {
      throw error(t);
    }

    protected RuntimeException error(TypeName t) {
      return new IllegalArgumentException("Unexpected type for helper method: " + t.name());
    }

    public Void forReferenceArray(ReferenceArrayName t) {
      handleSequence(t);
      return null;
    }

    public Void forSequenceClass(SequenceClassName t) {
      handleSequence(t);
      return null;
    }

    public abstract Void forTreeNode(ClassName t);

    protected abstract void handleSequence(SequenceName t);

    public abstract Void forOptionClass(OptionClassName t);

    public abstract Void forTupleClass(TupleClassName t);
  }
}
