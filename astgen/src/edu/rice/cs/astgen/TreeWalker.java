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

/**
 * <p>A lightweight visitor for traversing (in depth-first order) arbitrary ASTGen-produced
 * trees.  Unlike the Visitor classes generated for a specific AST, implementations of
 * TreeWalker are defined for all ASTs.  They are more limited, having far fewer hooks
 * available for manipulating the traversal process and handling different types of nodes.
 * This minimal interface is enough, however, for simple visitors which aren't concerned
 * with the semantic content of a tree and simply need to traverse its structure.</p>
 * 
 * <p>When passed to a node's {@code walk()} method, a TreeWalker can expect to receive
 * a specific sequence of method calls: {@link #visitNode}, then {@link #visitField}
 * and a recursive invocation for each of the node's fields.  "Recursive invocations"
 * include additional {@code visitNode()} calls and invocations of the the other "visit"
 * methods for handling Strings, lists, primitives, and objects of unrecognized type.
 * (Note that the method to call is determined statically based on the field's declared
 * type, not at runtime.)  Clients can prevent recursion into a non-atomic element's
 * children by returning {@code false} in the appropriate visit method.</p>
 */
public abstract class TreeWalker {

  /** Visit an AST node.  Return {@code true} to recur on each of the node's fields. */
  public boolean visitNode(java.lang.Object node, java.lang.String type, int fields) {
    return true;
  }

  /** Visit an AST node's field.  Return {@code true} to recur on the field's value. */
  public boolean visitNodeField(java.lang.String name, java.lang.Object value) {
    return true;
  }

  /** Signal the end of recursion on an AST node's field. */
  public void endNodeField(java.lang.String name, java.lang.Object value) {
  }

  /** Signal the end of a sequence of node fields. */
  public void endNode(java.lang.Object node, java.lang.String type, int fields) {
  }

  /** Visit an Iterable or array.  Return {@code true} to recur on each element. */
  public boolean visitIterated(java.lang.Object iterable) {
    return true;
  }

  /** Visit an iterated element.  Return {@code true} to recur on the element value. */
  public boolean visitIteratedElement(int index, java.lang.Object element) {
    return true;
  }

  /** Signal the end of recursion on an iterated element. */
  public void endIteratedElement(int index, java.lang.Object element) {
  }

  /** Signal the end of a sequence of iterated elements. */
  public void endIterated(java.lang.Object iterable, int size) {
  }

  /** Visit a non-empty option-typed value.  Return {@code true} to recur on the nested value. */
  public boolean visitNonEmptyOption(java.lang.Object option) {
    return true;
  }

  /** Signal the end of recursion on a non-empty option. */
  public void endNonEmptyOption(java.lang.Object option) {
  }

  /** Visit an empty option-typed value. */
  public void visitEmptyOption(java.lang.Object option) {
  }

  /** Visit a tuple-typed value.  Return {@code true} to recur on the nested elements. */
  public boolean visitTuple(java.lang.Object tuple, int arity) {
    return true;
  }

  /** Visit a tuple element.  Return {@code true} to recur on the element value. */
  public boolean visitTupleElement(int index, java.lang.Object element) {
    return true;
  }

  /** Signal the end of a tuple element. */
  public void endTupleElement(int index, java.lang.Object element) {
  }

  /** Signal the end of a sequence of tuple elements. */
  public void endTuple(java.lang.Object tuple, int arity) {
  }

  /** Visit a string value. */
  public void visitString(java.lang.String s) {
  }

  /**
   * Visit an object that is not known statically to have an AST Node or
   * other supported type.
   */
  public void visitUnknownObject(java.lang.Object o) {
  }

  /**
   * Visit a null reference.  This is called rather than {@code visitNode}, {@code visitList},
   * {@code visitUnknownObject}, etc., where a field or element's value is {@code null}.
   */
  public void visitNull() {
  }

  /** Visit a boolean primitive. */
  public void visitBoolean(boolean b) {
  }

  /** Visit a char primitive. */
  public void visitChar(char c) {
  }

  /** Visit a byte primitive. */
  public void visitByte(byte b) {
  }

  /** Visit a short primitive. */
  public void visitShort(short s) {
  }

  /** Visit a int primitive. */
  public void visitInt(int i) {
  }

  /** Visit a long primitive. */
  public void visitLong(long l) {
  }

  /** Visit a float primitive. */
  public void visitFloat(float f) {
  }

  /** Visit a double primitive. */
  public void visitDouble(double d) {
  }
}
