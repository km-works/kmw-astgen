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

///* <CR000/> Various stylistic and minor changes, e.g. StringBuffer -> StringBuilder, etc.
///+ <CR001/> Code for generation of field setter method added

package edu.rice.cs.astgen;

import edu.rice.cs.astgen.Types.KindTag;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.tuple.Option;

/**
 * Represents a field of an AST node.
 *
 * @version $Id: Field.java,v 1.9 2008/06/25 02:46:25 dlsmith Exp $
 */
public class Field {
  public final String _name;
  public final TypeName _type;
  public final Option<String> _defaultValue;
  public final boolean _allowNull;
  public final boolean _ignoreForEquals;
  public final boolean _addGetterPrefix;

  public Field(TypeName type, String name, Option<String> defaultValue, boolean allowNull,
          boolean ignoreForEquals, boolean addGetterPrefix) {
    _type = type;
    _name = name;
    _defaultValue = defaultValue;
    _allowNull = allowNull;
    _ignoreForEquals = ignoreForEquals;
    _addGetterPrefix = addGetterPrefix;
  }

  public String name() {
    return _name;
  }

  public TypeName type() {
    return _type;
  }

  public Option<String> defaultValue() {
    return _defaultValue;
  }

  public boolean allowNull() {
    return _allowNull;
  }

  public boolean ignoreForEquals() {
    return _ignoreForEquals;
  }

  public boolean addGetterPrefix() {
    return _addGetterPrefix;
  }

  public boolean matchesName(Field f) {
    return _name.equals(f._name);
  }

  public boolean matchesNameAndType(Field f) {
    return matchesName(f) && _type.name().equals(f._type.name());
  }

  public String getGetterName() {
    if (_addGetterPrefix) {
      StringBuilder buf = new StringBuilder();	///* <CR000/> StringBuilder instead of StringBufffer
      if (_type.name().equals("boolean")) {
        buf.append("is");
      } else {
        buf.append("get");
      }
      buf.append(CodeGenerator.upperCaseFirst(_name));
      return buf.toString();
    } else {
      return _name;
    }
  }

///+ <CR001>
  private String _getSetterName() {
    StringBuilder buf = new StringBuilder();
    buf.append("set").append(CodeGenerator.upperCaseFirst(_name));
    return buf.toString();
  }
///+ </CR001>

  private StringBuilder _getGetterSignature() {
    StringBuilder buf = new StringBuilder();		///* <CR000/> StringBuilder instead of StringBufffer
    buf.append("public ").append(_type.name()).append(" ").append(getGetterName()).append("()");
    return buf;
  }

///+ <CR001>
  private StringBuilder _getSetterSignature() {
    StringBuilder buf = new StringBuilder();
    buf.append("public void ").append(_getSetterName());
    buf.append("(").append(_type.name()).append(" ").append(_name).append(")");
    return buf;
  }
///+ </CR001>

  public String getGetterMethod(boolean makeFinal, boolean cast) {
    StringBuilder buf = _getGetterSignature();	///* <CR000/> StringBuilder instead of StringBufffer
    if (makeFinal) { buf.insert(0, "final "); }
    buf.append(" { return ");
    if (cast) {
      if (!_type.name().equals(_type.erasedName())) {
        buf.insert(0, "@SuppressWarnings(\"unchecked\") ");
      }
      buf.append("(").append(_type.name()).append(")");
      buf.append(" super.").append(getGetterName()).append("()");
    }
    else {
      buf.append("_").append(_name);
    }
    buf.append("; }");
    return buf.toString();
  }

///+ <CR001>
  public String getSetterMethod(boolean makeFinal) {
    StringBuilder buf = _getSetterSignature();
    if (makeFinal) {
      buf.insert(0, "final ");
    }
    buf.append(" { _").append(_name).append(" = ").append(_name).append("; }");
    return buf.toString();
  }
///+ </CR001>

  public String getGetterInterface() {
    StringBuilder buf = _getGetterSignature();	///* <CR000/> StringBuilder instead of StringBufffer
    buf.append(";");
    return buf.toString();
  }

  public String getFieldDefinition() {
    return "private " + _type.name() + " _" + _name + ";";  ///* <CR000/> "private final " useless -> removed
  }

  public String getFieldInitialization() {
    final String argName = getConstructorArgName();

    String start = "_" + _name + " = ";
    // auto intern strings!
    if (Types.kind(_type).equals(KindTag.STRING)) {
      if (_allowNull) {
        start += "(" + argName + " == null) ? null : " + argName + ".intern()";
      } else {
        start += argName + ".intern()";
      }
    } else {
      start += argName;
    }
    return start + ";";
  }

  public String getConstructorArgName() {
    return "in_" + _name;
  }

  // a hack used by NodeClass to generate the Constructor signature based on toString()
  @Override
  public String toString() {
    return _type.name() + " " + getConstructorArgName();
  }
}
