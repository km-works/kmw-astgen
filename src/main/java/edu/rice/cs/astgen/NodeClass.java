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
conditions and the following disclaimer in the documentatsion and/or other materials provided 
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

import edu.rice.cs.astgen.Types.PrimitiveName;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import java.util.*;

/**
 * ClassBox for a normal class.
 *
 * @version $Id: NodeClass.java,v 1.2 2008/08/18 21:34:19 dlsmith Exp $
 */
public class NodeClass extends NodeType {

  private boolean _isAbstract;
  private TypeName _superClass;

  public NodeClass(String name, boolean isAbstract, List<Field> fields, TypeName superClass, List<TypeName> interfaces) {
    super(name, fields, interfaces);
    _isAbstract = isAbstract;
    _superClass = superClass;
  }

  public boolean isAbstract() { return _isAbstract; }
  public TypeName superClass() { return _superClass; }

  /**
   * The fields that are declared by this class (rather than being inherited).  These include
   * all fields that are not declared by a parent NodeClass, plus any fields inherited from an
   * interface parent.
   */
  public Iterable<Field> declaredFields(ASTModel ast) {
    Option<NodeType> parent = ast.parent(this);
    if (parent.isSome() && parent.unwrap() instanceof NodeClass) {
      NodeType parentType = parent.unwrap();
      List<Field> result = new LinkedList<Field>();
      for (Field f : _fields) {
        Option<Field> supF = parentType.fieldForName(f.name(), ast);
        if (supF.isNone()) { result.add(f); }
      }
      return result;
    }
    else {
      // may include fields in a parent interface
      return allFields(ast);
    }
  }

  public void output(ASTModel ast, Iterable<CodeGenerator> gens) {
    TabPrintWriter writer = ast.options().createJavaSourceInOutDir(_name);

    // Class header
    writer.startLine("/**");
    writer.startLine(" * Class " + _name + ", a component of the ");
    writer.print("ASTGen-generated composite hierarchy.");

    if (!ast.options().allowNulls) {
      writer.startLine(" * Note: null is not allowed as a value for any field.");
    }

    ///- <CR004/> writer.startLine(" * @version  Generated automatically by ASTGen at ");  
    ///- <CR004/> writer.print(new Date());
    writer.startLine(" */");

    writer.startLine("@SuppressWarnings(\"unused\")");
    writer.startLine("public ");
    if (_isAbstract) { writer.print("abstract "); }
    writer.print("class " + _name + " extends " + _superClass.name());
    if (_interfaces.size() > 0) {
      writer.print(" implements ");
      writer.print(IterUtil.toString(IterUtil.map(_interfaces, Types.GET_NAME), "", ", ", ""));
    }
    writer.print(" {");
    writer.indent();

    Option<NodeType> parent = ast.parent(this);
    Iterable<Field> superFields = IterUtil.empty();
    if (parent.isSome() && parent.unwrap() instanceof NodeClass) {
      superFields = parent.unwrap().allFields(ast);
    }
    Iterable<Field> allFields = allFields(ast);
    Iterable<Field> declaredFields = declaredFields(ast);

    // Fields for this class
    for (Field f : declaredFields(ast)) {
      writer.startLine(f.getFieldDefinition());
    }

    writer.println(); // skip line after fields

    // Constructors
    Set<List<String>> constructorTypes = new HashSet<List<String>>();
    List<String> mainConstructorType = new LinkedList<String>();
    for (Field f : allFields) { mainConstructorType.add(f.type().erasedName()); }
    constructorTypes.add(mainConstructorType);
    _outputMainConstructor(writer, allFields, superFields, declaredFields, ast.options().allowNulls);

    List<Iterable<Pair<Field, Boolean>>> constructorParams = new LinkedList<Iterable<Pair<Field, Boolean>>>();
    for (Field f : allFields) {
      if (f.defaultValue().isNone()) { constructorParams.add(IterUtil.singleton(Pair.make(f, true))); }
      else { constructorParams.add(IterUtil.make(Pair.make(f, true), Pair.make(f, false))); }
    }

    for (Iterable<Pair<Field, Boolean>> params : IterUtil.cross(constructorParams)) {
      List<String> ts = _erasedTypesOfFields(params);
      if (!constructorTypes.contains(ts)) {
        constructorTypes.add(ts);
        _outputDelegatingConstructor(writer, params);
      }
    }


    ///* Getters & Setters (only for fields defined in this class)
    for (Field f : declaredFields(ast)) {
      writer.startLine(f.getGetterMethod(!_isAbstract, false));
      writer.startLine(f.getSetterMethod(!_isAbstract));	///+ <CR001/>
    }

    if (parent.isSome()) {
      NodeType parentType = parent.unwrap();
      for (Field f : _fields) {
        Option<Field> supF = parentType.fieldForName(f.name(), ast);
        if (supF.isSome() && !f.matchesNameAndType(supF.unwrap())) {
          writer.startLine(f.getGetterMethod(!_isAbstract, true));
        }
      }
    }
    writer.println();

    for (CodeGenerator g : gens) { g.generateClassMembers(writer, this); }

    writer.unindent();
    writer.startLine("}");
    writer.println();
    writer.close();
  }

  private void _outputMainConstructor(TabPrintWriter writer, Iterable<Field> allFields, Iterable<Field> superFields,
          Iterable<Field> declaredFields, boolean allowNulls) {
    writer.startLine("/**");
    writer.startLine(" * Constructs a " + _name + ".");

    if (!allowNulls) {
      writer.startLine(" * @throws java.lang.IllegalArgumentException");
      writer.print("  If any parameter to the constructor is null.");
    }
    writer.startLine(" */");

    writer.startLine("public " + _name + IterUtil.toString(allFields, "(", ", ", ")") + " {");
    writer.indent();

    writer.startLine("super(");
    boolean first = true;
    for (Field f : superFields) {
      if (first) { first = false; }
      else { writer.print(", "); }
      writer.print(f.getConstructorArgName());
    }
    writer.print(");");

    for (Field curField : declaredFields) {
      // Each class is only responsible for checking its own fields (not the super fields) for null
      if (!curField.allowNull() && !(curField.type() instanceof PrimitiveName)) {
        writer.startLine("if (" + curField.getConstructorArgName() + " == null) {");
        writer.indent();
        writer.startLine("throw new java.lang.IllegalArgumentException(");
        writer.print("\"Parameter '" + curField.name());
        writer.print("' to the " + _name + " constructor was null\"");
        writer.print(");");
        writer.unindent();
        writer.startLine("}");
      }
      writer.startLine(curField.getFieldInitialization());
    }

    writer.unindent();
    writer.startLine("}");
    writer.println(); // skip line after constructor
  }

  private void _outputDelegatingConstructor(TabPrintWriter writer, Iterable<Pair<Field, Boolean>> fields) {
    writer.startLine("/**");
    writer.startLine(" * A constructor with some fields provided by default values.");
    writer.startLine(" */");
    writer.startLine("public " + _name + "(");
    boolean first = true;
    for (Pair<Field, Boolean> field : fields) {
      if (field.second()) {
        if (first) { first = false; }
        else { writer.print(", "); }
        writer.print(field.first());
      }
    }
    writer.print(") {");
    writer.indent();

    writer.startLine("this(");
    first = true;
    for (Pair<Field, Boolean> field : fields) {
      if (first) { first = false; }
      else { writer.print(", "); }
      if (field.second()) { writer.print(field.first().getConstructorArgName()); }
      else {
        // field.second() == false implies the default is defined
        writer.print(field.first().defaultValue().unwrap());
      }
    }
    writer.print(");");
    writer.unindent();
    writer.startLine("}");
    writer.println();
  }

  private List<String> _erasedTypesOfFields(Iterable<Pair<Field, Boolean>> fields) {
    List<String> result = new LinkedList<String>();
    for (Pair<Field, Boolean> f : fields) {
      if (f.second()) { result.add(f.first().type().erasedName()); }
    }
    return result;
  }
  
}
