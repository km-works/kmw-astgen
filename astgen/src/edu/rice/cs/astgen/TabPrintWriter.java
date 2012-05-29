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
 * An extension of PrintWriter to support indenting levels.
 */
public class TabPrintWriter extends java.io.PrintWriter {

  private final int _tabSize;
  private int _numSpaces;

  public TabPrintWriter(java.io.Writer writer, int tabSize) {
    super(writer);
    _tabSize = tabSize;
    _numSpaces = 0;
  }

  /** ups indent for any future new lines. */
  public void indent() {
    _numSpaces += _tabSize;
  }

  public void unindent() {
    _numSpaces -= _tabSize;
  }

  public void startLine(java.lang.Object s) {
    startLine();
    print(s);
  }

  public void startLine() {
    println();
    for (int i = 0; i < _numSpaces; i++) {
      print(' ');
    }
  }

  public void printEscaped(java.lang.Object o) {
    printEscaped(o.toString());
  }

  /** Print a string in Java source-compatible escaped form.  All control characters
   * (including line breaks) and quoting punctuation are escaped with a backslash.
   */
  public void printEscaped(java.lang.String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\b':
          print("\\b");
          break;
        case '\t':
          print("\\t");
          break;
        case '\n':
          print("\\n");
          break;
        case '\f':
          print("\\f");
          break;
        case '\r':
          print("\\r");
          break;
        case '\"':
          print("\\\"");
          break;
        case '\'':
          print("\\\'");
          break;
        case '\\':
          print("\\\\");
          break;
        default:
          if (c < ' ' || c == '\u007f') {
            print('\\');
            // must use 3 digits so that unescaping doesn't consume too many chars ("\12" vs. "\0012")
            java.lang.String num = java.lang.Integer.toOctalString(c);
            while (num.length() < 3) {
              num = "0" + num;
            }
            print(num);
          } else {
            print(c);
          }
          break;
      }
    }
  }

  /** Conditionally print the serialzed form of the given object. */
  public void printPossiblyEscaped(java.lang.String s, boolean lossless) {
    if (lossless) {
      print("\"");
      printEscaped(s);
      print("\"");
    } else {
      print(s);
    }
  }

  /** Print the serialized form of the given object as a hexadecimal number.
   * @throws RuntimeException  If the object is not serializable.
   */
  public void printSerialized(java.lang.Object o) {
    java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
    try {
      java.io.ObjectOutputStream objOut = new java.io.ObjectOutputStream(bs);
      try {
        objOut.writeObject(o);
      } finally {
        objOut.close();
      }
    } catch (java.io.IOException e) {
      throw new java.lang.RuntimeException(e);
    }
    printBytes(bs.toByteArray());
  }

  /** Conditionally print the serialzed form of the given object. */
  public void printPossiblySerialized(java.lang.Object o, boolean lossless) {
    if (lossless) {
      printSerialized(o);
      print(" ");
      printEscaped(o);
    } else {
      print(o);
    }
  }

  private void printBytes(byte[] bs) {
    for (byte b : bs) {
      int unsigned = ((int) b) & 0xff;
      java.lang.String num = java.lang.Integer.toHexString(unsigned);
      if (num.length() == 1) {
        print("0");
      }
      print(num);
    }
  }

  public void startObject(String name) {
    print(name);
    indent();
  }
}
