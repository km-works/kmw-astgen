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

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.astgen.Types.*;

public class EqualsGenerator extends CodeGenerator {

  public EqualsGenerator(ASTModel ast) {
    super(ast);
  }

  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    return IterUtil.empty();
  }

  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    if (ast.isTop(i)) {
      writer.startLine("public int generateHashCode();");
    }
  }

  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    if (ast.isTopClass(c)) {
      writer.startLine("private int _hashCode;");
      writer.startLine("private boolean _hasHashCode = false;");
      writer.println();
      writer.startLine("public final int hashCode() {");
      writer.indent();
      writer.startLine("if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }");
      writer.startLine("return _hashCode;");
      writer.unindent();
      writer.startLine("}");
      writer.println();
    }
    if (c.isAbstract()) {
      writer.startLine("public abstract int generateHashCode();");
    } else {
      Iterable<Field> allFields = c.allFields(ast);
      outputEquals(c, writer, allFields);
      writer.println();
      outputGenerateHashCode(c, writer, allFields);
      writer.println();
    }
  }

  protected void outputEquals(NodeClass c, TabPrintWriter writer, Iterable<Field> allfields) {
    writer.startLine("/**");
    writer.startLine(" * Implementation of equals that is based on the values of the fields of the");
    writer.startLine(" * object. Thus, two objects created with identical parameters will be equal.");
    writer.startLine(" */");

    writer.startLine("public boolean equals(java.lang.Object obj) {");
    writer.indent();
    writer.startLine("if (obj == null) return false;");
    writer.startLine("if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {");
    writer.indent();
    writer.startLine("return false;");
    writer.unindent();
    writer.startLine("}");
    writer.startLine("else {");
    writer.indent();
    writer.startLine(c.name() + " casted = (" + c.name() + ") obj;");

    for (Field field : allfields) {
      if (!field.ignoreForEquals()) {
        String thisName = "temp_" + field.name();
        String objName = "casted_" + field.name();
        writer.startLine(field.type().name() + " " + thisName + " = " + field.getGetterName() + "();");
        writer.startLine(field.type().name() + " " + objName + " = casted." + field.getGetterName() + "();");
        equalsForVal(writer, field.type(), thisName, objName, field.allowNull());
      }
    }

    writer.startLine("return true;");
    writer.unindent();
    writer.startLine("}");
    writer.unindent();
    writer.startLine("}");
  }

  protected void equalsForVal(final TabPrintWriter writer, TypeName type, final String name1,
          final String name2, final boolean checkForNull) {
    type.accept(new TypeNameVisitor<Void>() {

      public Void forTreeNode(ClassName t) {
        checkEquals(t);
        return null;
      }

      public Void forPrimitive(PrimitiveName t) {
        checkIdentical(t);
        return null;
      }

      public Void forString(ClassName t) {
        checkIdentical(t);
        return null;
      }

      public Void forPrimitiveArray(PrimitiveArrayName t) {
        writer.startLine("if (!java.util.Arrays.equals(" + name1 + ", " + name2 + ")) return false;");
        return null;
      }

      public Void forReferenceArray(ReferenceArrayName t) {
        writer.startLine("if (" + name1 + " != " + name2 + ") {");
        writer.indent();
        if (checkForNull) {
          writer.startLine("if (" + name1 + " == null || " + name2 + " == null) return false;");
        }
        writer.startLine("if (" + name1 + ".length != " + name2 + ".length) return false;");
        String elt1 = "elt_" + name1;
        String elt2 = "elt_" + name2;
        String eltT = t.elementType().name();
        String index = "index_" + name1;
        writer.startLine("for (int " + index + " = 0; " + index + " < " + name1 + ".length; "
                + index + "++) {");
        writer.indent();
        writer.startLine(eltT + " " + elt1 + " = " + name1 + "[" + index + "];");
        writer.startLine(eltT + " " + elt2 + " = " + name2 + "[" + index + "];");
        equalsForVal(writer, t.elementType(), elt1, elt2, true);
        writer.unindent();
        writer.startLine("}");
        return null;
      }

      public Void forSequenceClass(SequenceClassName t) {
        String c = t.className();
        if (c.equals("Iterable") || c.equals("java.lang.Iterable")
                || c.equals("SizedIterable") || c.equals("edu.rice.cs.plt.iter.SizedIterable")) {
          writer.startLine("if (" + name1 + " != " + name2 + ") {");
          writer.indent();
          if (checkForNull) {
            writer.startLine("if (" + name1 + " == null || " + name2 + " == null) return false;");
          }
          if (c.equals("SizedIterable") || c.equals("edu.rice.cs.plt.iter.SizedIterable")) {
            writer.startLine("if (" + name1 + ".size() != " + name2 + ".size()) return false;");
          }
          String elt1 = "elt_" + name1;
          String elt2 = "elt_" + name2;
          String eltT = t.elementType().name();
          String eltArg = t.typeArguments().get(0).name();
          String iter1 = "iter_" + name1;
          String iter2 = "iter_" + name2;
          writer.startLine("java.util.Iterator<" + eltArg + "> " + iter1 + " = " + name1 + ".iterator();");
          writer.startLine("java.util.Iterator<" + eltArg + "> " + iter2 + " = " + name2 + ".iterator();");
          writer.startLine("while (" + iter1 + ".hasNext() && " + iter2 + ".hasNext()) {");
          writer.indent();
          writer.startLine(eltT + " " + elt1 + " = " + iter1 + ".next();");
          writer.startLine(eltT + " " + elt2 + " = " + iter2 + ".next();");
          equalsForVal(writer, t.elementType(), elt1, elt2, true);
          writer.unindent();
          writer.startLine("}");
          writer.startLine("if (" + iter1 + ".hasNext() || " + iter2 + ".hasNext()) return false;");
          writer.unindent();
          writer.startLine("}");
        } else {
          checkEquals(t);
        }
        return null;
      }

      public Void forOptionClass(OptionClassName t) {
        checkEquals(t);
        return null;
      }

      public Void forTupleClass(TupleClassName t) {
        checkEquals(t);
        return null;
      }

      public Void forGeneralClass(ClassName t) {
        checkEquals(t);
        return null;
      }

      private void checkEquals(TypeName t) {
        writer.startLine("if (!(");
        // nulls, optimization
        writer.print(name1 + " == " + name2);
        // handle non-null references that are equal but not ==
        writer.print(" || ");
        if (checkForNull) {
          writer.print(name1 + " != null && " + name2 + "!= null && ");
        }
        writer.print(name1 + ".equals(" + name2 + ")");
        writer.print(")) return false;");
      }

      private void checkIdentical(TypeName t) {
        writer.startLine("if (!(" + name1 + " == " + name2 + ")) return false;");
      }
      
    });
  }

  protected void outputGenerateHashCode(NodeClass c, TabPrintWriter writer, Iterable<Field> allfields) {
    writer.println();
    writer.startLine("/**");
    writer.startLine(" * Implementation of hashCode that is consistent with equals.  The value of");
    writer.startLine(" * the hashCode is formed by XORing the hashcode of the class object with");
    writer.startLine(" * the hashcodes of all the fields of the object.");
    writer.startLine(" */");

    writer.startLine("public int generateHashCode() {");
    writer.indent();

    writer.startLine("int code = getClass().hashCode();");
    for (Field field : allfields) {
      if (!field.ignoreForEquals()) {
        writer.startLine(field.type().name() + " temp_" + field.name() + " = "
                + field.getGetterName() + "();");
        hashCodeForVal(writer, field.type(), "temp_" + field.name(), field.allowNull());
      }
    }
    writer.startLine("return code;");
    writer.unindent();
    writer.startLine("}");
  }

  protected void hashCodeForVal(final TabPrintWriter writer, TypeName type, final String name,
          final boolean checkForNull) {
    writer.startLine("code ^= ");
    type.accept(new TypeNameVisitor<Void>() {

      public Void forTreeNode(ClassName t) {
        callHashCode();
        return null;
      }

      public Void forPrimitive(PrimitiveName t) {
        String primT = t.name();
        // Use hashcode that the wrapper classes use
        if (primT.equals("float")) {
          writer.print("java.lang.Float.floatToIntBits(" + name + ");");
        } else if (primT.equals("double")) {
          String v = "java.lang.Double.doubleToLongBits(" + name + ")";
          writer.print("(int) (" + v + "^(" + v + ">>>32));");
        } else if (primT.equals("long")) {
          writer.print("(int) (" + name + "^(" + name + ">>>32));");
        } else if (primT.equals("boolean")) {
          // this is what Boolean does, I swear!
          writer.print(name + " ? 1231 : 1237;");
        } else {
          writer.print(name + ";");
        }
        return null;
      }

      public Void forString(ClassName t) {
        callHashCode();
        return null;
      }

      public Void forPrimitiveArray(PrimitiveArrayName t) {
        writer.print("java.util.Arrays.hashCode(" + name + ");");
        return null;
      }

      public Void forReferenceArray(ReferenceArrayName t) {
        iterateHashes(t);
        return null;
      }

      public Void forSequenceClass(SequenceClassName t) {
        String c = t.className();
        if (c.equals("Iterable") || c.equals("java.lang.Iterable")
                || c.equals("SizedIterable") || c.equals("edu.rice.cs.plt.iter.SizedIterable")) {
          iterateHashes(t);
        } else {
          callHashCode();
        }
        return null;
      }

      public Void forOptionClass(OptionClassName t) {
        callHashCode();
        return null;
      }

      public Void forTupleClass(TupleClassName t) {
        callHashCode();
        return null;
      }

      public Void forGeneralClass(ClassName t) {
        callHashCode();
        return null;
      }

      private void iterateHashes(SequenceName t) {
        if (checkForNull) {
          // don't need to do anything for null, since doing "code ^= 0" won't change the value
          writer.startLine("if (!(" + name + " == null)) {");
          writer.indent();
        }
        String index = "index_" + name;
        String eltName = "elt_" + name;
        writer.print(name + ".getClass().hashCode();");
        writer.startLine("int " + index + " = 0;");
        writer.startLine("for (" + t.elementType().name() + " " + eltName + " : " + t.iterable(name) + ") {");
        writer.indent();
        writer.startLine("code ^= " + index + "++;"); // preserve information about order
        hashCodeForVal(writer, t.elementType(), eltName, true);
        writer.unindent();
        writer.startLine("}");
        if (checkForNull) {
          writer.unindent();
          writer.startLine("}");
        }
      }

      private void callHashCode() {
        if (checkForNull) {
          writer.print("(" + name + " == null) ? 0 : ");
        }
        writer.print(name + ".hashCode();");
      }
      
    });
  }
  

  public void generateAdditionalCode() {
  }
  
}
