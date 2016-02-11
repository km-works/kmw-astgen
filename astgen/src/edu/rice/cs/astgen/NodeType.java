/*BEGIN_COPYRIGHT_BLOCK*
 * 
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
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.astgen.Types.*;

/**
 * Represents a class or interface to be output.
 *
 * @version $Id: NodeType.java,v 1.1 2008/06/25 02:46:25 dlsmith Exp $
 */
public abstract class NodeType {

  protected final String _name;
  protected final List<Field> _fields;
  protected final List<TypeName> _interfaces;

  public NodeType(String name, List<Field> fields, List<TypeName> interfaces) {
    _name = name;
    _fields = fields;
    _interfaces = interfaces;
  }

  public abstract void output(ASTModel ast, Iterable<CodeGenerator> gens);

  public String name() {
    return _name;
  }

  public List<TypeName> interfaces() {
    return _interfaces;
  }

  public List<Field> fields() {
    return _fields;
  }

  /** True for interfaces and abstract classes. */
  public abstract boolean isAbstract();

  public Option<Field> fieldForName(String name, ASTModel ast) {
    for (Field f : _fields) {
      if (f.name().equals(name)) {
        return Option.some(f);
      }
    }
    // if no matches:
    Option<NodeType> parent = ast.parent(this);
    if (parent.isSome()) {
      return parent.unwrap().fieldForName(name, ast);
    } else {
      return Option.none();
    }
  }

  /** Get all fields in ancestors of this node and in this node.  Eliminates shadowing duplicates. */
  public Iterable<Field> allFields(ASTModel ast) {
    Option<NodeType> parent = ast.parent(this);
    if (parent.isSome()) {
      List<Field> result = CollectUtil.makeLinkedList(parent.unwrap().allFields(ast));
      for (Field f : _fields) {
        Iterator<Field> supers = result.iterator();
        while (supers.hasNext()) {
          Field superF = supers.next();
          if (superF.matchesName(f)) {
            supers.remove();
            break;
          }
        }
      }
      result.addAll(_fields);
      return result;
    } else {
      return _fields;
    }
  }
  
}
