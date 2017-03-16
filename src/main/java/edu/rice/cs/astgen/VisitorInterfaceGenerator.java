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

public class VisitorInterfaceGenerator extends VisitorGenerator {

  public VisitorInterfaceGenerator(ASTModel ast) {
    super(ast);
  }

  protected String visitorName(NodeType root) {
    return root.name() + "Visitor";
  }

  @Override
  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    return IterUtil.empty();
  }

  @Override
  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    if (ast.isRoot(i)) {
      writer.startLine("public <RetType> RetType " + options.visitMethod);
      writer.print("(" + visitorName(i) + "<RetType> visitor);");
      writer.println();
    }
  }

  @Override
  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    if (c.isAbstract()) {
      if (ast.isTopClass(c)) {
        for (NodeType root : ast.ancestorRoots(c)) {
          writer.startLine("public abstract <RetType> RetType " + options.visitMethod);
          writer.print("(" + visitorName(root) + "<RetType> visitor);");
        }
        writer.println();
      } else if (ast.isRoot(c)) {
        writer.startLine("public abstract <RetType> RetType " + options.visitMethod);
        writer.print("(" + visitorName(c) + "<RetType> visitor);");
        writer.println();
      }
    } else {
      for (NodeType root : ast.ancestorRoots(c)) {
        writer.startLine("public <RetType> RetType " + options.visitMethod);
        writer.print("(" + visitorName(root) + "<RetType> visitor) {");
        writer.indent();
        writer.startLine("return visitor." + visitorMethodName(c) + "(this);");
        writer.unindent();
        writer.startLine("}");
        writer.println();
      }
    }
  }

  @Override
  protected void generateVisitor(NodeType root) {
    String visitorName = visitorName(root);
    TabPrintWriter writer = options.createJavaSourceInOutDir(visitorName);

    // Class header
    writer.startLine("/** A parametric interface for visitors over " + root.name());
    writer.print(" that return a value. */");
    writer.startLine("@SuppressWarnings(\"unused\")");
    writer.startLine("public interface " + visitorName + "<RetType>");
    boolean first = true;
    for (NodeType d : ast.descendents(root)) {
      if (!d.equals(root) && ast.isRoot(d)) {
        if (first) {
          writer.print(" extends ");
          first = false;
        } else {
          writer.print(", ");
        }
        writer.print(visitorName(d) + "<RetType>");
      }
    }
    writer.print(" {");
    writer.indent();

    outputVisitorInterfaceCases(root, writer, "RetType");

    writer.unindent();
    writer.startLine("}");
    writer.println();
    writer.close();
  }
  
}
