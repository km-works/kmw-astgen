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

public class ToStringGenerator extends CodeGenerator {

  public ToStringGenerator(ASTModel ast) {
    super(ast);
  }

  @Override
  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    return IterUtil.make(WalkMethodGenerator.class, TabPrintWriterGenerator.class);
  }

  @Override
  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    if (ast.isTop(i)) {
      writer.startLine("public void output(java.io.Writer writer);");
    }
  }

  @Override
  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    if (c.isAbstract()) {
      if (ast.isTopClass(c)) {
        writer.startLine("public abstract void output(java.io.Writer writer);");
      }
    } else {
      writer.startLine("/**");
      writer.startLine(" * Implementation of toString that uses");
      writer.startLine(" * {@link #output} to generate a nicely tabbed tree.");
      writer.startLine(" */");
      writer.startLine("public java.lang.String toString() {");
      writer.indent();
      writer.startLine("java.io.StringWriter w = new java.io.StringWriter();");
      writer.startLine("walk(new ToStringWalker(w, 2));");
      writer.startLine("return w.toString();");
      writer.unindent();
      writer.startLine("}");

      writer.println();
      writer.startLine("/**");
      writer.startLine(" * Prints this object out as a nicely tabbed tree.");
      writer.startLine(" */");
      writer.startLine("public void output(java.io.Writer writer) {");
      writer.indent();
      writer.startLine("walk(new ToStringWalker(writer, 2));");
      writer.unindent();
      writer.startLine("}");
      writer.println();
    }
  }

  @Override
  public void generateAdditionalCode() {
    TabPrintWriter writer = new TabPrintWriter(options.createFileInOutDir("ToStringWalker.java"), 2);
    options.outputPackageStatement(writer); // don't output import statements
    copyFromResource(writer, "ToStringWalker.java", "package");
    writer.close();
  }
}
