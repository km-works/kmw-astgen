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

public class ToStringWalker extends TreeWalker {

  private final TabPrintWriter _out;

  public ToStringWalker(java.io.Writer writer, int tabSize) {
    _out = new TabPrintWriter(writer, tabSize);
  }

  public boolean visitNode(java.lang.Object node, java.lang.String type, int fields) {
    _out.print(type);
    _out.print(':');
    _out.indent();
    return true;
  }

  public boolean visitNodeField(java.lang.String name, java.lang.Object value) {
    // Consider special case eliding single field
    _out.startLine(name);
    _out.print(" = ");
    return true;
  }

  public void endNode(java.lang.Object node, java.lang.String type, int fields) {
    _out.unindent();
  }

  public boolean visitIterated(java.lang.Object iterable) {
    _out.print("{");
    _out.indent();
    return true;
  }

  public boolean visitIteratedElement(int index, java.lang.Object element) {
    _out.startLine("* ");
    return true;
  }

  public void endIterated(java.lang.Object iterable, int size) {
    _out.unindent();
    if (size > 0) {
      _out.startLine("}");
    } else {
      _out.print("}");
    }
  }

  public boolean visitNonEmptyOption(java.lang.Object option) {
    _out.print('(');
    return true;
  }

  public void endNonEmptyOption(java.lang.Object option) {
    _out.print(')');
  }

  public void visitEmptyOption(java.lang.Object option) {
    _out.print("()");
  }

  public boolean visitTuple(java.lang.Object tuple, int arity) {
    _out.print(')');
    return true;
  }

  public boolean visitTupleElement(int index, java.lang.Object element) {
    if (index > 0) {
      _out.print(", ");
    }
    return true;
  }

  public void endTuple(java.lang.Object tuple, int arity) {
    _out.print(')');
  }

  public void visitString(java.lang.String s) {
    _out.print(s);
  }

  public void visitUnknownObject(java.lang.Object o) {
    _out.print(o);
  }

  public void visitNull() {
    _out.print("null");
  }

  public void visitBoolean(boolean b) {
    _out.print(b);
  }

  public void visitChar(char c) {
    _out.print(c);
  }

  public void visitByte(byte b) {
    _out.print(b);
  }

  public void visitShort(short s) {
    _out.print(s);
  }

  public void visitInt(int i) {
    _out.print(i);
  }

  public void visitLong(long l) {
    _out.print(l);
  }

  public void visitFloat(float f) {
    _out.print(f);
  }

  public void visitDouble(double d) {
    _out.print(d);
  }

  public void writeString(java.lang.String s) {
    _out.print(s);
  }

  public void writeUnknownObject(java.lang.Object o) {
    _out.print(o);
  }
}
