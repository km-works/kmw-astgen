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

/**
 * Supports simplified reflective access by producing an empty constructor that instantiates 
 * each field to null, leaving clients to manually instantiate each field correctly.
 */
public class EmptyConstructorGenerator extends CodeGenerator {

  public EmptyConstructorGenerator(ASTModel ast) {
    super(ast);
  }

  @Override
  public Iterable<Class<? extends CodeGenerator>> dependencies() {
    return IterUtil.empty();
  }

  @Override
  public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
  }

  @Override
  public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    boolean hasEmptyConstructor = true;
    boolean allDefaults = true;
    for (Field f : c.allFields(ast)) {
      hasEmptyConstructor = false;
      allDefaults &= f.defaultValue().isSome();
    }
    hasEmptyConstructor |= allDefaults;

    if (!hasEmptyConstructor) {
      writer.startLine("/**");
      writer.startLine(" * Empty constructor, for reflective access.  Clients are ");
      writer.startLine(" * responsible for manually instantiating each field.");
      writer.startLine(" */");
      writer.startLine("protected " + c.name() + "() {");
      writer.indent();
      for (Field f : c.declaredFields(ast)) {
        String init;
        if (f.type() instanceof PrimitiveName) {
          if (f.type().name().equals("boolean")) {
            init = "false";
          } else {
            init = "0";
          }
        } else {
          init = "null";
        }
        writer.startLine("_" + f.name() + " = " + init + ";");
      }
      writer.unindent();
      writer.startLine("}");
      writer.println();
    }
  }

  @Override
  public void generateAdditionalCode() {
  }
}
