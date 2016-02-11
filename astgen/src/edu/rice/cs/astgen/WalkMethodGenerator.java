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
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.astgen.Types.*;

/** Generates a walk() method, used for AST traversal via a TreeWalker. */
public class WalkMethodGenerator extends CodeGenerator {

  public WalkMethodGenerator(ASTModel ast) {
    super(ast);
  }

  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    return IterUtil.empty();
  }

  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    if (ast.isTop(i)) {
      writer.startLine("public void walk(TreeWalker w);");
    }
  }

  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    if (c.isAbstract()) {
      if (ast.isTopClass(c)) {
        writer.startLine("public abstract void walk(TreeWalker w);");
      }
    } else {
      Iterable<Field> allFields = c.allFields(ast);
      int fieldCount = IterUtil.sizeOf(allFields);
      String type = '"' + c.name() + '"';
      writer.startLine("public void walk(TreeWalker w) {");
      writer.indent();

      writer.startLine("if (w.visitNode(this, " + type + ", " + fieldCount + ")) {");
      writer.indent();
      for (Field field : allFields) {
        String fieldName = '"' + field.name() + '"';
        String localName = "temp_" + field.name();
        writer.startLine(field.type().name() + " " + localName + " = " + field.getGetterName() + "();");
        writer.startLine("if (w.visitNodeField(" + fieldName + ", " + localName + ")) {");
        writer.indent();
        walkForVal(writer, field.type(), localName, field.allowNull());
        writer.startLine("w.endNodeField(" + fieldName + ", " + localName + ");");
        writer.unindent();
        writer.startLine("}");
      }
      writer.startLine("w.endNode(this, " + type + ", " + fieldCount + ");");
      writer.unindent();
      writer.startLine("}");
      writer.unindent();
      writer.startLine("}");
      writer.println();
    }
  }

  /** Generate the walk method text for a single field or array element */
  protected void walkForVal(final TabPrintWriter writer, TypeName type,
          final String name, final boolean checkForNull) {
    type.accept(new TypeNameVisitor<Void>() {

      public Void forTreeNode(ClassName type) {
        if (checkForNull) {
          nullCheck();
        }
        writer.startLine(name + ".walk(w);");
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      public Void forPrimitive(PrimitiveName type) {
        String primT = type.name();
        String method = "visit" + primT.substring(0, 1).toUpperCase() + primT.substring(1);
        writer.startLine("w." + method + "(" + name + ");");
        return null;
      }

      public Void forString(ClassName type) {
        if (checkForNull) {
          nullCheck();
        }
        writer.startLine("w.visitString(" + name + ");");
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      public Void forPrimitiveArray(PrimitiveArrayName type) {
        if (checkForNull) {
          nullCheck();
        }
        walkIterated(name, type.elementType());
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      public Void forReferenceArray(ReferenceArrayName type) {
        return handleSequence(type);
      }

      public Void forSequenceClass(SequenceClassName type) {
        return handleSequence(type);
      }

      private Void handleSequence(SequenceName type) {
        if (checkForNull) {
          nullCheck();
        }
        walkIterated(type.iterable(name), type.elementType());
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      private void walkIterated(String iterableExpr, TypeName elementType) {
        writer.startLine("if (w.visitIterated(" + name + ")) {");
        writer.indent();
        String index = "i_" + name;
        String eltName = "elt_" + name;
        writer.startLine("int " + index + " = 0;");

        writer.startLine("for (" + elementType.name() + " " + eltName + " : " + iterableExpr + ") {");
        writer.indent();
        writer.startLine("if (w.visitIteratedElement(" + index + ", " + eltName + ")) {");
        writer.indent();
        walkForVal(writer, elementType, eltName, true);
        writer.unindent();
        writer.startLine("}");
        writer.startLine(index + "++;");
        writer.unindent();
        writer.startLine("}");

        writer.startLine("w.endIterated(" + name + ", " + index + ");");
        writer.unindent();
        writer.startLine("}");
      }

      public Void forOptionClass(OptionClassName type) {
        if (checkForNull) {
          nullCheck();
        }
        writer.startLine("if (" + type.emptyTester(name) + ") {");
        writer.indent();
        writer.startLine("w.visitEmptyOption(" + name + ");");
        writer.unindent();
        writer.startLine("}");
        writer.startLine("else if (w.visitNonEmptyOption(" + name + ")) {");
        writer.indent();
        TypeName eltT = type.elementType();
        String eltName = "elt_" + name;
        writer.startLine(eltT.name() + " " + eltName + " = " + type.nonEmptyGetter(name) + ";");
        walkForVal(writer, eltT, eltName, true);
        writer.startLine("w.endNonEmptyOption(" + name + ");");
        writer.unindent();
        writer.startLine("}");
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      public Void forTupleClass(TupleClassName type) {
        if (checkForNull) {
          nullCheck();
        }
        List<TypeName> eltTs = type.elementTypes();
        int arity = eltTs.size();
        writer.startLine("if (w.visitTuple(" + name + ", " + arity + ")) {");
        writer.indent();
        for (int i = 0; i < arity; i++) {
          String eltName = "elt_" + name + "_" + i;
          TypeName eltT = eltTs.get(i);
          writer.startLine(eltT.name() + " " + eltName + " = " + type.getter(name, i) + ";");
          writer.startLine("if (w.visitTupleElement(" + i + ", " + eltName + ")) {");
          writer.indent();
          walkForVal(writer, eltT, eltName, true);
          writer.unindent();
          writer.startLine("}");
          writer.startLine("w.endTupleElement(" + i + ", " + eltName + ");");
        }
        writer.startLine("w.endTuple(" + name + ", " + arity + ");");
        writer.unindent();
        writer.startLine("}");
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      public Void forGeneralClass(ClassName type) {
        if (checkForNull) {
          nullCheck();
        }
        writer.startLine("w.visitUnknownObject(" + name + ");");
        if (checkForNull) {
          endNullCheck();
        }
        return null;
      }

      private void nullCheck() {
        writer.startLine("if (" + name + " == null) w.visitNull();");
        writer.startLine("else {");
        writer.indent();
      }

      private void endNullCheck() {
        writer.unindent();
        writer.startLine("}");
      }
      
    });
  }

  public void generateAdditionalCode() {
    TabPrintWriter writer = new TabPrintWriter(options.createFileInOutDir("TreeWalker.java"), 2);
    options.outputPackageStatement(writer); // don't output import statements
    copyFromResource(writer, "TreeWalker.java", "package");
    writer.close();
  }
  
}
