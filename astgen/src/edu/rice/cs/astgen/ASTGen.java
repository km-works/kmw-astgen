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

import java.io.*;
import java.util.*;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.text.TextUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.reflect.PathClassLoader;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.astgen.Types.*;

/**
 * Generate AST classes from a single source file.
 * TODO: Cache hashCode, use hashCode as approximation to equals, fix hashCode/equals in treatment of arrays
 *
 * @version $Id: ASTGen.java,v 1.24 2008/07/14 21:27:34 dlsmith Exp $
 */
public class ASTGen {
    private static final int NORMAL_CLASS_TYPE = 1;
    private static final int ABSTRACT_CLASS_TYPE = 2;
    private static final int INTERFACE_TYPE = 3;

    private final Options _options;
    private final ASTModel _ast;

    private StreamTokenizer _tokenizer = null;
    private boolean _advanced = false; // used to track nextToken() to prevent infinite loops
    private Stack<NodeType> _ancestorStack;
    private int _spacesOnThisLine = 0;
    
    private ClassLoader _customLoader;
    private Set<Class<? extends CodeGenerator>> _generators;
    private Set<Class<? extends Runnable>> _preprocessors;

    // -1 when at the top
    private int _prevIndent = -1;

    public static void main(String[] args) throws Throwable {
      File inputFile = new File(args[0]);
      ASTGen a = new ASTGen(new FileReader(inputFile),inputFile.getParentFile());
      a.generate();
    }

    public ASTGen(Reader reader, File basedir) {
        _options = new Options(basedir);
        _ast = new ASTModel(_options);
        _ancestorStack = new Stack<NodeType>();
        
        _customLoader = getClass().getClassLoader();
        
        _generators = new LinkedHashSet<Class<? extends CodeGenerator>>();
        _generators.add(VisitorInterfaceGenerator.class);
        _generators.add(VoidVisitorInterfaceGenerator.class);
        _generators.add(AbstractVisitorGenerator.class);
        _generators.add(AbstractVoidVisitorGenerator.class);
        _generators.add(DepthFirstVisitorGenerator.class);
        _generators.add(DepthFirstVoidVisitorGenerator.class);
        _generators.add(DeepCopyVisitorGenerator.class);	///+ <CR003/>
        _generators.add(UpdateVisitorGenerator.class);
        _generators.add(ToStringGenerator.class);
        _generators.add(EqualsGenerator.class);
        
        _preprocessors = new LinkedHashSet<Class<? extends Runnable>>();
        
        _tokenizer = new StreamTokenizer(new BufferedReader(reader));
        _tokenizer.slashStarComments(true);
        _tokenizer.slashSlashComments(true);
        _tokenizer.eolIsSignificant(true);

        // Don't let spaces get ignored!
        _tokenizer.ordinaryChar(' ');
        _tokenizer.ordinaryChar('<');
        _tokenizer.ordinaryChar('>');
        _tokenizer.ordinaryChar('/');
        
        // Turn off number parsing
        _tokenizer.ordinaryChar('-');
        _tokenizer.ordinaryChar('.');
        _tokenizer.ordinaryChars('0', '9');

        // Make []._* and digits chars that stay part of words!
        _tokenizer.wordChars('[', '[');
        _tokenizer.wordChars(']', ']');
        _tokenizer.wordChars('_', '_');
        _tokenizer.wordChars('*', '*'); // to support on-demand import statements
        _tokenizer.wordChars('.', '.');
        _tokenizer.wordChars('0', '9');
    }

    /** Read and generate the AST */
    public void generate() {
        _nextTokenIgnoreWhitespace();   // Fetch the first token, to start things off.
        _setupOptions();
        _readAST();
        _preprocess();
        _output();
    }

  /* * * SOURCE FILE PARSING * * */
  
  /**
   * Read tokens until we reach "begin ast".
   * Precondition: nextToken has been called the first time.
   * Postcondition: Current token points to "begin ast;", if there is one.
   */
  private void _setupOptions() {
    _advanced = true;
    while ((_ttype() == StreamTokenizer.TT_WORD) && (!_sval().equals("begin")) && _advanced) {
      _advanced = false;
      _parsePackage();
      _parseVisitMethod();
      _parseVisitorMethodPrefix();
      _parseAutomaticRoots();
      _parseTabSize();
      _parseOutputDir();
      _parseImport();
      _parseAllowNulls();
      _parseAddGetterPrefixes();
      _parseCustomClassPath();
      _parseCustomGenerator();
      _parseCustomPreprocessor();
      _parseGenerateToString();
      _parseGenerateEquals();
      _parseGenerateAbstractVisitors();
      _parseGenerateRecursiveVisitors();
      _parseGenerateVoidVisitors();
      _parseGenerateSerializers();
      _parseGenerateEmptyConstructor();
      _parseUsePLT();
    }
  }
  
  /**
   * Checks that the current token is a TT_WORD with the
   * specified value.  If so, advances to the next non-whitespace token.
   */
  private boolean _checkKeyword(String value) {
    if (_ttype() == StreamTokenizer.TT_WORD && _sval().equals(value)) {
      _nextTokenIgnoreWhitespace();
      return true;
    }
    else {
      return false;
    }
  }
  
  /**
   * Checks that the current token is a TT_WORD with the
   * specified value.  If so, advances to the next non-whitespace token.
   */
  private boolean _checkCaseInsensitiveKeyword(String value) {
    if (_ttype() == StreamTokenizer.TT_WORD && _sval().equalsIgnoreCase(value)) {
      _nextTokenIgnoreWhitespace();
      return true;
    }
    else {
      return false;
    }
  }
  
  /**
   * Reads a String from the current token (assumed to be a TT_WORD)
   * and advances to the next non-whitespace token.  An error occurs
   * if an unexpected token is encountered.
   */
  private String _readString() {
    _assertTokenType(StreamTokenizer.TT_WORD);
    String result = _sval();
    _nextTokenIgnoreWhitespace();
    return result;
  }
  
  /**
   * Reads a boolean value from the current token (assumed to be a TT_WORD)
   * and advances to the next non-whitespace token.
   * An error occurs if an unexpected token is encountered.  Valid String
   * values are "yes", "true", "no", and "false".
   */
  private boolean _readBoolean() {
    _assertTokenType(StreamTokenizer.TT_WORD);
    String value = _sval().toLowerCase();
    if (value.equals("yes")||value.equals("true")) {
      _nextTokenIgnoreWhitespace();
      return true;
    }
    else if (value.equals("no")||value.equals("false")) {
      _nextTokenIgnoreWhitespace();
      return false;
    }
    else {
      _assert(false, "Parameter '" + value + "'was invalid. " +
              "Valid values are yes/true/no/false.");
      throw new RuntimeException(); // should never happen, since _assert() should fail
    }
  }
  
  /**
   * Reads an int value from the current token (assumed to be a TT_NUMBER) and advances
   * to the next non-whitespace token.
   */
  private int _readInt() {
    _assertTokenType(StreamTokenizer.TT_WORD);
    try {
      int result = Integer.parseInt(_sval());
      _nextTokenIgnoreWhitespace();
      return result;
    }
    catch (NumberFormatException e) {
      _assert(false, "Expected an integer; found " + _sval());
      throw new RuntimeException(); // should never happen, since _assert() should fail
    }
  }
  
  /**
   * Reads a semicolon (token with type ';') and
   * advances to the next non-whtespace token.
   * An error occurs if an unexpected token is encountered.
   */
  private void _readSemicolon() {
    _assertTokenType(';');
    _nextTokenIgnoreWhitespace();
  }

  private void _parsePackage() {
    if (_checkKeyword("package")) {
      _options.packageName = _readString();
      _readSemicolon();
    }
  }

  private void _parseImport() {
    if (_checkKeyword("import")) {
      _options.imports.add(_readString());
      _readSemicolon();
    }
  }

  private void _parseVisitMethod() {
    if (_checkCaseInsensitiveKeyword("visitmethod")) {
      _options.visitMethod = _readString();
      _readSemicolon();
    }
  }

  private void _parseOutputDir() {
    if (_checkCaseInsensitiveKeyword("outputdir")) {
      _options.outputDir = _readString();
      _readSemicolon();
    }
  }

  private void _parseVisitorMethodPrefix() {
    if (_checkCaseInsensitiveKeyword("visitormethodprefix")) {
      _options.visitorMethodPrefix = _readString();
      _readSemicolon();
    }
  }

  private void _parseAutomaticRoots() {
    if (_checkCaseInsensitiveKeyword("automaticroots")) {
      _options.automaticRoots = _readBoolean();
      _readSemicolon();
    }
  }

  private void _parseAllowNulls() {
    if (_checkCaseInsensitiveKeyword("allownulls")) {
      _options.allowNulls = _readBoolean();
      _readSemicolon();
    }
  }

  private void _parseTabSize() {
    if (_checkCaseInsensitiveKeyword("tabsize")) {
      _options.tabSize = _readInt();
      _readSemicolon();
    }
  }
  
  private void _parseAddGetterPrefixes() {
    if (_checkCaseInsensitiveKeyword("addgetterprefixes")) {
      _options.addGetterPrefixes = _readBoolean();
      _readSemicolon();
    }
  }
  
  private void _parseCustomClassPath() {
    if (_checkCaseInsensitiveKeyword("customclasspath")) {
      String path = _readFilePath();
      path = path.replace('/', File.separatorChar).replace(':', File.pathSeparatorChar);
      Iterable<File> files = IterUtil.map(IOUtil.parsePath(path), new Lambda<File, File>() {
        @Override
        public File value(File f) { return _options.fileRelativeToSource(f); }
      });
      _customLoader = new PathClassLoader(_customLoader, files);
      _readSemicolon();
    }
  }
  
  private void _parseCustomGenerator() {
    if (_checkCaseInsensitiveKeyword("customgenerator")) {
      String className = _readString();
      try {
        Class<?> gen = _customLoader.loadClass(className);
        _generators.add(gen.asSubclass(CodeGenerator.class));
      }
      catch (ClassNotFoundException e) {
        throw new ASTGenException("Can't load class " + className, e);
      }
      catch (ClassCastException e) {
        throw new ASTGenException("Custom generator is not a CodeGenerator: " + className, e);
      }
      _readSemicolon();
    }
  }
  
  private void _parseCustomPreprocessor() {
    if (_checkCaseInsensitiveKeyword("custompreprocessor")) {
      String className = _readString();
      try {
        Class<?> p = _customLoader.loadClass(className);
        _preprocessors.add(p.asSubclass(Runnable.class));
      }
      catch (ClassNotFoundException e) {
        throw new ASTGenException("Can't load class " + className, e);
      }
      catch (ClassCastException e) {
        throw new ASTGenException("Custom preprocessor is not a Runnable: " + className, e);
      }
      _readSemicolon();
    }
  }
  
  private void _parseGenerateToString() {
    if (_checkCaseInsensitiveKeyword("generatetostring")) {
      _options.generateToString = _readBoolean();
      if (_options.generateToString) { _generators.add(ToStringGenerator.class); }
      else { _generators.remove(ToStringGenerator.class); }
      _readSemicolon();
    }
  }
  
  private void _parseGenerateEquals() {
    if (_checkCaseInsensitiveKeyword("generateequals")) {
      _options.generateEquals = _readBoolean();
      if (_options.generateEquals) { _generators.add(EqualsGenerator.class); }
      else { _generators.remove(EqualsGenerator.class); }
      _readSemicolon();
    }
  }

  private void _parseGenerateAbstractVisitors() {
    if (_checkCaseInsensitiveKeyword("generateabstractvisitors")) {
      _options.generateAbstractVisitors = _readBoolean();
      if (_options.generateAbstractVisitors) {
        _generators.add(AbstractVisitorGenerator.class);
        if (_options.usePLT) { _generators.add(VisitorLambdaGenerator.class); }
        if (_options.generateVoidVisitors) {
          _generators.add(AbstractVoidVisitorGenerator.class);
          if (_options.usePLT) { _generators.add(VisitorRunnable1Generator.class); }
        }
      }
      else {
        _generators.remove(AbstractVisitorGenerator.class);
        _generators.remove(VisitorLambdaGenerator.class);
        _generators.remove(AbstractVoidVisitorGenerator.class);
        _generators.remove(VisitorRunnable1Generator.class);
      }
      _readSemicolon();
    }
  }
       
  private void _parseGenerateRecursiveVisitors() {
    if (_checkCaseInsensitiveKeyword("generaterecursivevisitors")) {
      _options.generateRecursiveVisitors = _readBoolean();
      if (_options.generateRecursiveVisitors) {
        _generators.add(DepthFirstVisitorGenerator.class);
        _generators.add(UpdateVisitorGenerator.class);
        if (_options.generateVoidVisitors) {
          _generators.add(DepthFirstVoidVisitorGenerator.class);
        }
      }
      else {
        _generators.remove(DepthFirstVisitorGenerator.class);
        _generators.remove(UpdateVisitorGenerator.class);
        _generators.remove(DepthFirstVoidVisitorGenerator.class);
      }
      _readSemicolon();
    }
  }
        
  private void _parseGenerateVoidVisitors() {
    if (_checkCaseInsensitiveKeyword("generatevoidvisitors")) {
      _options.generateVoidVisitors = _readBoolean();
      if (_options.generateVoidVisitors) {
        _generators.add(VoidVisitorInterfaceGenerator.class);
        if (_options.generateAbstractVisitors) {
          _generators.add(AbstractVoidVisitorGenerator.class);
          if (_options.usePLT) { _generators.add(VisitorRunnable1Generator.class); }
        }
        if (_options.generateRecursiveVisitors) {
          _generators.add(DepthFirstVoidVisitorGenerator.class);
        }
      }
      else {
        _generators.remove(VoidVisitorInterfaceGenerator.class);
        _generators.remove(AbstractVoidVisitorGenerator.class);
        _generators.remove(VisitorRunnable1Generator.class);
        _generators.remove(DepthFirstVoidVisitorGenerator.class);
      }
      _readSemicolon();
    }
  }        
  
  private void _parseGenerateSerializers() {
    if (_checkCaseInsensitiveKeyword("generateserializers")) {
      _options.generateSerializers = _readBoolean();
      if (_options.generateSerializers) { _generators.add(TextSerializationGenerator.class); }
      else { _generators.remove(TextSerializationGenerator.class); }
      _readSemicolon();
    }
  }
  
  private void _parseGenerateEmptyConstructor() {
    if (_checkCaseInsensitiveKeyword("generateemptyconstructor")) {
      _options.generateEmptyConstructor = _readBoolean();
      if (_options.generateEmptyConstructor) { _generators.add(EmptyConstructorGenerator.class); }
      else { _generators.remove(EmptyConstructorGenerator.class); }
      _readSemicolon();
    }
  }
  
  private void _parseUsePLT() {
    if (_checkCaseInsensitiveKeyword("useplt")) {
      _options.usePLT = _readBoolean();
      if (_options.usePLT) {
        if (_options.generateAbstractVisitors) {
          _generators.add(VisitorLambdaGenerator.class);
          if (_options.generateVoidVisitors) {
            _generators.add(VisitorRunnable1Generator.class);
          }
        }
      }
      else {
        _generators.remove(VisitorLambdaGenerator.class);
        _generators.remove(VisitorRunnable1Generator.class);
      }
      _readSemicolon();
    }
  }

  private void _readAST() {
    _assertTokenString("begin");
    _nextTokenIgnoreWhitespace();
    _assertTokenString("ast");
    _nextTokenIgnoreWhitespace();
    _readSemicolon();

    while (true) {
      if ((_ttype() == StreamTokenizer.TT_WORD) &&
          (_sval().equals("end")))
      {
        break;
      }
      else if (_ttype() == StreamTokenizer.TT_EOL) {
        _spacesOnThisLine = 0;
        _nextToken();
      }
      else if (_ttype() == ' ') {
        _spacesOnThisLine++;
        _nextToken();
      }
      else if (_ttype() == StreamTokenizer.TT_WORD) {
        _readASTLine();
      }
      else {
        throw new ASTGenException("Unexpected tokenizer state: lineno=" + _tokenizer.lineno() +
                                  " ttype=" + _ttype() + " as char=" + (char) _ttype());
      }
    }

    _assertEndSemicolon();
  }

  private void _assertEndSemicolon() {
    _assertTokenString("end");
    _nextTokenIgnoreWhitespace();
    _assertTokenType(';');
    _nextToken();
  }

  private void _readASTLine() {
    _assert(_spacesOnThisLine % _options.tabSize == 0,
            "Invalid number of preceding spaces on line. Found " +
            _spacesOnThisLine + " spaces, which is not an " +
            "even multiple of the tabsize, " +
            _options.tabSize);

    int curIndent = _spacesOnThisLine / _options.tabSize;
    _assert(curIndent <= _prevIndent + 1,
            "Line indent is more than one past previous line!");

    // If cur indent is less than prev indent, we need to get rid of
    // levels of stack that are no longer relevant
    int levelsToPop = _prevIndent - curIndent + 1;
    /**
    System.err.println("cur=" + curIndent + " prev=" + _prevIndent +
                       " spaces=" + _spacesOnThisLine +
                       " tabsize=" + _options.tabSize);

    System.err.print("\tPop " + levelsToPop + ":");
    **/
    for (int i = 0; i < levelsToPop; i++) {
      NodeType p = _ancestorStack.pop();
      // System.err.print(" ");
      // System.err.print(p.name());
    }

    // System.err.println(" size now=" + _ancestorStack.size());

    // Now the top of the stack is the parent of the next class/interface
    // Also set _prevIndent to the indent of this line, ready
    // for the next call to this function
    _prevIndent = curIndent;

    int type = NORMAL_CLASS_TYPE;
    boolean root = false;

    _assertTokenType(StreamTokenizer.TT_WORD);
    
    if (_sval().equals("root")) {
      root = true;
      _nextTokenIgnoreWhitespace();
      _assertTokenType(StreamTokenizer.TT_WORD);
    }
    
    // This token could be interface, abstract or the name of the class
    if (_sval().equals("interface")) {
      type = INTERFACE_TYPE;
      _nextTokenIgnoreWhitespace();
      _assertTokenType(StreamTokenizer.TT_WORD);
    }
    else if (_sval().equals("abstract")) {
      type = ABSTRACT_CLASS_TYPE;
      _nextTokenIgnoreWhitespace();
      _assertTokenType(StreamTokenizer.TT_WORD);
    }

    String name = _readString();

    _assertTokenType('(');
    _nextTokenIgnoreWhitespace();

    List<Field> fieldList = new LinkedList<Field>();
    // Now get all the fields
    while (_ttype() != ')') {
      _assertTokenType(StreamTokenizer.TT_WORD);

      boolean ignoreForEquals = false;
      if (_sval().equals("ignoreForEquals")) {
        ignoreForEquals = true;
        _nextTokenIgnoreWhitespace();
        _assertTokenType(StreamTokenizer.TT_WORD);
      }

      TypeName fieldType = _readType();
      _assertTokenType(StreamTokenizer.TT_WORD);
      String fieldName = _sval();
      _nextTokenIgnoreWhitespace();
      Option<String> defaultValue = Option.none();
      if (_ttype() == '=') {
        _nextTokenIgnoreWhitespace();
        defaultValue = Option.some(_readExpression());
      }
      fieldList.add(new Field(fieldType, fieldName, defaultValue, _options.allowNulls, ignoreForEquals, 
                              _options.addGetterPrefixes));

      if (_ttype() == ',') {
        _nextTokenIgnoreWhitespace();
      }
    }

    // Consume the close paren
    _nextTokenIgnoreWhitespace();
    
    TypeName superClass = null;
    List<TypeName> superInterfaces = new LinkedList<TypeName>();
    
    if (_ancestorStack.empty()) {
      root = root || _options.automaticRoots;
    }
    else {
      NodeType parent = _ancestorStack.peek();
      TypeName parentName = Types.parse(parent.name(), _ast);
      if (parent instanceof NodeClass) {
        superClass = parentName;
        _assert(type != INTERFACE_TYPE, "Interface cannot extend a class");
      }
      else { superInterfaces.add(parentName); }
    }

    if (type != INTERFACE_TYPE && _ttype() == StreamTokenizer.TT_WORD &&
        _sval().equals("extends")) {
        _nextTokenIgnoreWhitespace();
        _assertTokenType(StreamTokenizer.TT_WORD);
        _assert(superClass == null, "Class has more than one superclass");
        superClass = _readType();
    }
    
    if (superClass == null) { superClass = Types.parse("java.lang.Object", _ast); }
    
    // After paren we can either have implements or extends or semicolon
    if (_ttype() != ';') {
      _assertTokenType(StreamTokenizer.TT_WORD);
      if (type == INTERFACE_TYPE) {
        _assert(_sval().equals("extends"), "Expected extends or semicolon after parameter list");
      }
      else {
        _assert(_sval().equals("implements"), "Expected implements or semicolon after parameter list");
      }
      _nextTokenIgnoreWhitespace();

      while (_ttype() != ';') {
        _assertTokenType(StreamTokenizer.TT_WORD);
        superInterfaces.add(_readType());

        if (_ttype() == ',') {
          _nextTokenIgnoreWhitespace();
        }
      }
    }

    // Consume the semicolon. Don't ignore whitespace because readAST expects
    // to see EOLs to know when to reset the indent for each line.
    _assertTokenType(';');
    _nextToken();
    
    NodeType result;
    if (type == INTERFACE_TYPE) {
      result = new NodeInterface(name, fieldList, superInterfaces);
    }
    else {
      result = new NodeClass(name, type == ABSTRACT_CLASS_TYPE, fieldList, superClass, superInterfaces);
    }
    
    if (_ancestorStack.empty()) { _ast.addTopType(result, root); }
    else { _ast.addType(result, root, _ancestorStack.peek()); }
    _ancestorStack.push(result);
  }
  
  /**
   * Reads an arbitrary type expression, which is either a single word or a word followed by
   * a list of expressions enclosed in '<' and '>'.  Whitespace afterwards will be consumed.
   */
  private TypeName _readType() {
    StringBuilder result = new StringBuilder();
    result.append(_sval());
    _nextTokenIgnoreWhitespace();
    if (_ttype() == '<') {
      result.append('<');
      _nextToken();
      while (_ttype() != '>' && _ttype() != ')' && _ttype() != StreamTokenizer.TT_EOF) {
        if (_ttype() == ',') { result.append(", "); _nextTokenIgnoreWhitespace(); }
        else { result.append(_readExpression()); }
      }
      _assertTokenType('>');
      result.append('>');
      _nextTokenIgnoreWhitespace();
    }
    return Types.parse(result.toString(), _ast);
  }
  
  /**
   * Reads a file path, which is a sequence of words separated by '/' and ':'.
   * Whitespace afterwards will be consumed.
   */
  private String _readFilePath() {
    StringBuilder result = new StringBuilder();
    _assertTokenType(StreamTokenizer.TT_WORD);
    while (_ttype() == StreamTokenizer.TT_WORD || _ttype() == ':' || _ttype() == '/') {
      if (_ttype() == StreamTokenizer.TT_WORD) { result.append(_sval()); }
      else { result.append((char) _ttype()); }
      _nextToken();
    }
    if (_ttype() == ' ') { _nextTokenIgnoreWhitespace(); }
    return result.toString();
  }
  
  /**
   * Read an arbitrary expression, the end of which is delimited by one of '>', ')', or ','.
   * Afterwards, the current token will be that delimiter.
   */
  private String _readExpression() {
    StringBuilder result = new StringBuilder();
    int depth = 0;
    while (depth > 0 ||
           (_ttype() != '>' && _ttype() != ')' && _ttype() != ',' && _ttype() != StreamTokenizer.TT_EOF)) {
      switch (_ttype()) {
        case '<': depth++; result.append('<'); break;
        case '>': depth--; result.append('>'); break;
        case '(': depth++; result.append('('); break;
        case ')': depth--; result.append(')'); break;
        case '"': result.append("\"").append(TextUtil.javaEscape(_sval())).append("\""); break;
        case '\'': result.append("'").append(TextUtil.javaEscape(_sval())).append("'"); break;
        case StreamTokenizer.TT_WORD: result.append(_sval()); break;
        case StreamTokenizer.TT_NUMBER: throw new IllegalStateException("Parsed unexpected number");
        case StreamTokenizer.TT_EOL: result.append(' '); break;
        default: result.append((char) _ttype()); break;
      }
      _nextToken();
    }
    return result.toString();
  }


  /* * * CODE GENERATION * * */
  
  private void _preprocess() {
    for (Class<? extends Runnable> pClass : _preprocessors) {
      Runnable p = null;
      try { p = pClass.getConstructor(ASTModel.class).newInstance(_ast); }
      catch (Throwable e) {
        throw new ASTGenException("Unable to instantiate preprocessor " + pClass.getName(), e);
      }
      p.run();
    }
  }
  
  private void _output() {
    // Compute dependencies for and instantiate generators
    final Lambda<Class<? extends CodeGenerator>, CodeGenerator> genFactory = CodeGenerator.factory(_ast);
    Set<Class<? extends CodeGenerator>> genCs =
    	CollectUtil.graphClosure(_generators, new Lambda<Class<? extends CodeGenerator>, Iterable<Class<? extends CodeGenerator>>>() {

      @Override
      public Iterable<Class<? extends CodeGenerator>> value(Class<? extends CodeGenerator> c) {
        return genFactory.value(c).dependencies();
      }
    });
    Iterable<CodeGenerator> gens = IterUtil.mapSnapshot(genCs, genFactory);

    _options.makeOutputDir();
    for (NodeType t : _ast.types()) {
      t.output(_ast, gens);
    }
    for (CodeGenerator g : gens) {
      g.generateAdditionalCode();
    }
  }
  
  
  /* * * HELPER METHODS * * */

  private void _assert(boolean condition, String msg) {
    if (!condition) {
      throw new ASTGenException("Line " + _tokenizer.lineno() + ": " + msg);
    }
  }
  
  private int _nextToken() {
    _advanced = true;
    try {
      return _tokenizer.nextToken();
    }
    catch (IOException ioe) {
      throw new ASTGenException(ioe.toString());
    }
  }

  private int _nextTokenIgnoreWhitespace() {
    int ret;
    _advanced = true;
    do {
      ret = _nextToken();
    }
    while ((ret == StreamTokenizer.TT_EOL) ||
           (ret == ' '));

    return ret;
  }

  private int _ttype() { return _tokenizer.ttype; }
  private String _sval() { return _tokenizer.sval; }

  private void _assertTokenType(int type) {
    String expected = (type > 31 && type < 127) ? ("" + (char) type) : ("" + type);
    String actual = (type > 31 && type < 127) ? ("" + (char) _ttype()) : ("" + _ttype());
    _assert(_ttype() == type, "Expected token type " + expected + " but found type " + actual);
  }

  private void _assertTokenString(String value) {
    _assertTokenType(StreamTokenizer.TT_WORD);
    _assert(_sval().equals(value),
            "Expected token \"" + value + "\"" +
            " but found token \"" + _sval() + "\"");
  }

}
