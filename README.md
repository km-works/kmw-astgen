# kmw-astgen

This is a slightly modified and enhanced version of ASTGen - a Java tool for generating heterogeneous AST classes from a
simple textual definition.

The original public version dated 2005-05-20 can be found [here](http://sourceforge.net/projects/astgen/).
Our code, however, is based on a later version dated 2009-05-15, which we have recovered from a source jar distributed
with [Project Fortress](http://java.net/projects/projectfortress/sources/sources/show) (BSD License).

## Change Log

vs. contributed code base: astgen_2009-05-15

CR000 Various stylistic and minor changes, e.g. StringBuffer -> StringBuilder, etc.

CR001 Code for generation of field setter method added

CR002 Change runtime dependency from edu.rice.cs.plt.lambda to kmworks.utils.generics

CR003 Added new DeepCopyVisitorGenerator

CR004 Remove generated-at date from header comment of generated classes because it forces a 
useless new svn version on every generation run absent substantial changes.
