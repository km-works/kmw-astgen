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
conditions and the following disclaimer in the documLientation and/or other materials provided 
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
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.tuple.Triple;
import edu.rice.cs.plt.text.TextUtil;

/**
 * Represents the type of a field.  TypeNames are created using {@link #parse}.
 * TypeNames are categorized as either "atomic" or "composite".  Composite types
 * are those that ASTGen knows how to decompose.  There are currently four
 * kinds of composites: Iterables, arrays, Options, and Tuples.
 */
public final class Types {
  // prevent construction

  private Types() {
  }

  public static interface TypeNameVisitor<T> {

    /** A type declared in the AST.  Has 0 type arguments. */
    public T forTreeNode(ClassName t);

    /** A primitive type. */
    public T forPrimitive(PrimitiveName t);

    /** A {@code java.lang.String}.  Has 0 type arguments. */
    public T forString(ClassName t);

    /** An array of primitives. */
    public T forPrimitiveArray(PrimitiveArrayName t);

    /** An array of reference types (non-primitives). */
    public T forReferenceArray(ReferenceArrayName t);

    /** A list, set, or other subtype of {@code java.lang.Iterable}. */
    public T forSequenceClass(SequenceClassName t);

    /** A {@code edu.rice.cs.plt.tuple.Option}.  Has 1 type argument. */
    public T forOptionClass(OptionClassName t);

    /** A tuple (see definition in {@link TupleName} documentation). */
    public T forTupleClass(TupleClassName t);

    /** A type for which none of the other cases apply. */
    public T forGeneralClass(ClassName t);
  }

  /**
   * Since visitor variants aren't distinguished by class, this offers a way
   * to represent which variant a certain type corresponds to.
   */
  public static enum KindTag {

    TREE_NODE, PRIMITIVE, STRING, PRIMITIVE_ARRAY, REFERENCE_ARRAY,
    SEQUENCE_CLASS, OPTION_CLASS, TUPLE_CLASS, GENERAL_CLASS;
  };

  /** Get the KindTag corresponding to the visit method called by the given TypeName. */
  public static KindTag kind(TypeName t) {
    return t.accept(new TypeNameVisitor<KindTag>() {

      public KindTag forTreeNode(ClassName t) {
        return KindTag.TREE_NODE;
      }

      public KindTag forPrimitive(PrimitiveName t) {
        return KindTag.PRIMITIVE;
      }

      public KindTag forString(ClassName t) {
        return KindTag.STRING;
      }

      public KindTag forPrimitiveArray(PrimitiveArrayName t) {
        return KindTag.PRIMITIVE_ARRAY;
      }

      public KindTag forReferenceArray(ReferenceArrayName t) {
        return KindTag.REFERENCE_ARRAY;
      }

      public KindTag forSequenceClass(SequenceClassName t) {
        return KindTag.SEQUENCE_CLASS;
      }

      public KindTag forOptionClass(OptionClassName t) {
        return KindTag.OPTION_CLASS;
      }

      public KindTag forTupleClass(TupleClassName t) {
        return KindTag.TUPLE_CLASS;
      }

      public KindTag forGeneralClass(ClassName t) {
        return KindTag.GENERAL_CLASS;
      }
    });
  }

  ;
  
  /**
   * {@code toString} implementation for TypeNames.  All TypeName implementations of {code toString()}
   * should delegate to this method.
   */
  public static String toString(TypeName t) {
    return kind(t) + ": " + t.name();
  }

  /**
   * {@code equals} implementation for TypeNames.  All TypeName implementations of {code equals()}
   * should delegate to this method.
   */
  public static boolean equals(TypeName t1, Object obj) {
    if (!(obj instanceof TypeName)) {
      return false;
    }
    final TypeName t2 = (TypeName) obj;
    if (!kind(t1).equals(kind(t2))) {
      return false;
    }
    return t1.accept(new TypeNameVisitor<Boolean>() {

      public Boolean forPrimitive(PrimitiveName t1) {
        return t1.name().equals(t2.name());
      }

      public Boolean forPrimitiveArray(PrimitiveArrayName t1) {
        return handleArrayName(t1);
      }

      public Boolean forReferenceArray(ReferenceArrayName t1) {
        return handleArrayName(t1);
      }

      public Boolean forTreeNode(ClassName t1) {
        return handleClassName(t1);
      }

      public Boolean forString(ClassName t1) {
        return handleClassName(t1);
      }

      public Boolean forSequenceClass(SequenceClassName t1) {
        return handleClassName(t1);
      }

      public Boolean forOptionClass(OptionClassName t1) {
        return handleClassName(t1);
      }

      public Boolean forTupleClass(TupleClassName t1) {
        return handleClassName(t1);
      }

      public Boolean forGeneralClass(ClassName t1) {
        return handleClassName(t1);
      }

      private Boolean handleArrayName(ArrayName t1) {
        return Types.equals(t1.elementType(), ((ArrayName) t2).elementType());
      }

      private Boolean handleClassName(ClassName t1) {
        ClassName t2Class = (ClassName) t2;
        if (t1.className().equals(t2Class.className())) {
          List<TypeArgumentName> t1Args = t1.typeArguments();
          List<TypeArgumentName> t2Args = t2Class.typeArguments();
          if (t1Args.size() == t2Args.size()) {
            for (Pair<TypeArgumentName, TypeArgumentName> p : IterUtil.zip(t1Args, t2Args)) {
              TypeArgumentName t1Arg = p.first();
              TypeArgumentName t2Arg = p.second();
              boolean equalArgs;
              if (t1Arg instanceof ExtendsWildcardName && t2Arg instanceof ExtendsWildcardName) {
                equalArgs = Types.equals(((ExtendsWildcardName) t1Arg).upperBound(),
                        ((ExtendsWildcardName) t2Arg).upperBound());
              } else if (t1Arg instanceof SuperWildcardName && t2Arg instanceof SuperWildcardName) {
                equalArgs = Types.equals(((SuperWildcardName) t1Arg).lowerBound(),
                        ((SuperWildcardName) t2Arg).lowerBound());
              } else if (t1Arg instanceof TypeName && t2Arg instanceof TypeName) {
                equalArgs = Types.equals((TypeName) t1Arg, t2Arg);
              } else {
                equalArgs = false;
              }
              if (!equalArgs) {
                return false;
              }
            }
            return true;
          }
        }
        return false;
      }
    });
  }

  /**
   * {@code hashCode} implementation for TypeNames.  All TypeName implementations of {code hashCode()}
   * should delegate to this method.
   */
  public static int hashCode(TypeName t) {
    return kind(t).hashCode() ^ t.accept(new TypeNameVisitor<Integer>() {

      public Integer forPrimitive(PrimitiveName t) {
        return t.name().hashCode();
      }

      public Integer forTreeNode(ClassName t) {
        return handleClassName(t);
      }

      public Integer forString(ClassName t) {
        return handleClassName(t);
      }

      public Integer forSequenceClass(SequenceClassName t) {
        return handleClassName(t);
      }

      public Integer forOptionClass(OptionClassName t) {
        return handleClassName(t);
      }

      public Integer forTupleClass(TupleClassName t) {
        return handleClassName(t);
      }

      public Integer forGeneralClass(ClassName t) {
        return handleClassName(t);
      }

      public Integer forPrimitiveArray(PrimitiveArrayName t) {
        return handleArrayName(t);
      }

      public Integer forReferenceArray(ReferenceArrayName t) {
        return handleArrayName(t);
      }

      public Integer handleClassName(ClassName t) {
        int result = t.className().hashCode();
        List<TypeArgumentName> targs = t.typeArguments();
        int i = 0;
        for (TypeArgumentName targ : targs) {
          i++;
          if (targ instanceof ExtendsWildcardName) {
            result ^= (ExtendsWildcardName.class.hashCode()
                    ^ Types.hashCode(((ExtendsWildcardName) targ).upperBound())) << i;
          } else if (targ instanceof SuperWildcardName) {
            result ^= (SuperWildcardName.class.hashCode()
                    ^ Types.hashCode(((SuperWildcardName) targ).lowerBound())) << i;
          } else { // TypeName
            result ^= Types.hashCode((TypeName) targ) << i;
          }
        }
        return result;
      }

      public Integer handleArrayName(ArrayName t) {
        return t.elementType().hashCode();
      }
    });
  }

  /** Lazily apply a TypeNameVisitor to a list. */
  public static <T> Iterable<T> mapVisitor(Iterable<? extends TypeName> ts,
          final TypeNameVisitor<? extends T> visitor) {
    return IterUtil.map(ts, new Lambda<TypeName, T>() {

      public T value(TypeName t) {
        return t.accept(visitor);
      }
    });
  }
  
  public static final Lambda<TypeArgumentName, String> GET_NAME = new Lambda<TypeArgumentName, String>() {

    public String value(TypeArgumentName t) {
      return t.name();
    }
  };
  public static final Lambda<TypeArgumentName, TypeName> TYPE_ARG_BOUND =
          new Lambda<TypeArgumentName, TypeName>() {

            public TypeName value(TypeArgumentName targ) {
              if (targ instanceof ExtendsWildcardName) {
                return ((ExtendsWildcardName) targ).upperBound();
              } else if (targ instanceof SuperWildcardName) {
                return ConcreteClassName.OBJECT;
              } else {
                return ((TypeName) targ);
              }
            }
          };

  /** A type argument: one of ExtendsWidcardName, SuperWildcardName, or TypeName. */
  public static interface TypeArgumentName {

    /** The representation of this type argument in Java syntax. */
    public String name();

    /**
     * An (ideally unique) identifier name describing this type argument.  By convention,
     * is capitalized and camel-case.
     */
    public String identifierName();
  }

  public static interface ExtendsWildcardName extends TypeArgumentName {

    public TypeName upperBound();
  }

  public static interface SuperWildcardName extends TypeArgumentName {

    public TypeName lowerBound();
  }

  public static interface TypeName extends TypeArgumentName {

    /**
     * The representation of the erasure of this type in Java syntax.  Important due to
     * the related overloading constraints.
     */
    public String erasedName();

    public <T> T accept(TypeNameVisitor<? extends T> visitor);
  }

  public static interface PrimitiveName extends TypeName {

    public TypeName boxed();
  }

  public static interface ArrayName extends TypeName {

    public TypeName elementType();
  }

  public static interface PrimitiveArrayName extends ArrayName {

    public PrimitiveName elementType();
  }

  public static interface ReferenceArrayName extends ArrayName, SequenceName {

    public ReferenceArrayName withElementType(TypeName eltType);

    public ReferenceArrayName withMappedOriginal(String origVar);

    /**
     * Produce an equivalent reference array that uses the given seed class expression to
     * create array instances with {@link java.lang.reflect.Array#newInstance(Class, int)}
     * rather than {@code new}.  This is necessary where the element type (or multi-
     * dimensional array base element type) is a type variable -- otherwise, the code
     * produced will not compile.
     * @param seed  An expression of type {@code Class<T>}, where {@code T} is represented
     *              by {@code elementType()}.
     */
    public ReferenceArrayName withSeed(String seed);
  }

  public static interface ClassName extends TypeName {

    public String className();

    public List<TypeArgumentName> typeArguments();
  }

  /** A class type representing a sequence. */
  public static interface SequenceClassName extends ClassName, SequenceName {

    public SequenceClassName withElementType(TypeName eltType);

    public SequenceClassName withMappedOriginal(String origVar);
  }

  /** A class type with an empty and non-empty variant. */
  public static interface OptionClassName extends ClassName {

    /** The type represented by the non-empty variant. */
    public TypeName elementType();

    /**
     * Produce a similar type name with a different element type.  The result must
     * support construction (see {@link #nonEmptyConstructor} and {@link #emptyConstructor}).
     */
    public OptionClassName withElementType(TypeName eltType);

    /**
     * Produce an expression for constructing a non-empty instance of this type.
     * @param eltExpression  An expression producing an element of type {@code elementType()}.
     *                       Must appear exactly once in the result.
     * @throws UnsupportedOperationException  If such an expression can't be created.
     */
    public String nonEmptyConstructor(String eltExpression);

    /**
     * Produce an empty instance of this type.
     * @throws UnsupportedOperationException  If such an expression can't be created.
     */
    public String emptyConstructor();

    /** Produce an expression testing whether the given variable of this type is an empty variant. */
    public String emptyTester(String var);

    /**
     * Produce an expression getting the element of the given non-empty variant variable.  Result
     * must have type {@code elementType()}.
     */
    public String nonEmptyGetter(String var);
  }

  /** A class type wrapping a fixed number of elements. */
  public static interface TupleClassName extends ClassName {

    public List<TypeName> elementTypes();

    /**
     * Produce a similar type name with different element types.  {@code eltTypes} must have
     * the same length as {@code elementTypes()}.  The result must support construction
     * (see {@link #constructor}).
     */
    public TupleClassName withElementTypes(Iterable<TypeName> eltTypes);

    /**
     * Produce an expression for constructing an instance of this type.
     * @param eltExpressions  Expressions producing elements with types corresponding to
     *                        {@code elementTypes()} (must have the same length).  Each
     *                        expression must appear exactly once in the result.
     * @throws UnsupportedOperationException  If such an expression can't be created.
     */
    public String constructor(Iterable<String> eltExpressions);

    /**
     * Produce an expression getting the ith element of the given variable of this type.
     * Result must have type {@code elementsTypes().get(i)}.
     */
    public String getter(String var, int i);
  }

  /** A type representing an arbitrary-length sequence of elements. */
  public static interface SequenceName extends TypeName {

    public TypeName elementType();

    /** Produce a similar type name with a different element type. */
    public SequenceName withElementType(TypeName eltType);

    /**
     * Produce an equivalent SequenceName that (optionally) takes advantage of the fact that
     * newly-allocated instances will be mapped from some original instance.  The final size
     * of the new instance may thus be known from the outset.
     * @param origVar  A variable referencing a sequence of this type that will be mapped from.
     *                 If the mapped-to type is different, this method should be called before
     *                 invoking {@link #withElementType} to ensure that the type of {@code origVar}
     *                 is known.
     */
    public SequenceName withMappedOriginal(String origVar);

    /**
     * A declaration to bind the given variable to an object that can accumulate elements of type
     * {@code elementType()} (and later produce a value of this type with {@link #constructor}).
     * @throws UnsupportedOperationException  If construction isn't supported.
     */
    public String accumulator(String accumVar);

    /**
     * A statement to add an element to an accumulator object.
     * @param accumVar  A variable declared by {@link #accumulator} or {@link #accumulatorForMapping}.
     * @param eltExpression  An expression producing an element of type {@code elementType()}. 
     *                       Must appear exactly once in the result.
     * @throws UnsupportedOperationException  If construction isn't supported.
     */
    public String addToAccumulator(String accumVar, String eltExpression);

    /** 
     * An expression to convert an accumulator to an object of this type.
     * @param accumVar  A variable declared by {@link #accumulator} or {@link #accumulatorForMapping}.
     * @throws UnsupportedOperationException  If construction isn't supported.
     */
    public String constructor(String accumVar);

    /** An expression converting a variable of this type to value that can appear in a for-each loop. */
    public String iterable(String var);
  }

  private static class UnboundedWildcardName implements ExtendsWildcardName {

    public static final ExtendsWildcardName INSTANCE = new UnboundedWildcardName();

    public String name() {
      return "?";
    }

    public String identifierName() {
      return "Wildcard";
    }

    public TypeName upperBound() {
      return ConcreteClassName.OBJECT;
    }
  }

  private static class ConcreteExtendsWildcardName implements ExtendsWildcardName {

    private final TypeName _bound;

    public ConcreteExtendsWildcardName(TypeName bound) {
      _bound = bound;
    }

    public String name() {
      return "? extends " + _bound.name();
    }

    public String identifierName() {
      return "WildcardExtends" + _bound.identifierName();
    }

    public TypeName upperBound() {
      return _bound;
    }
  }

  private static class ConcreteSuperWildcardName implements SuperWildcardName {

    private final TypeName _bound;

    public ConcreteSuperWildcardName(TypeName bound) {
      _bound = bound;
    }

    public String name() {
      return "? super " + _bound.name();
    }

    public String identifierName() {
      return "WildcardSuper" + _bound.identifierName();
    }

    public TypeName lowerBound() {
      return _bound;
    }
  }

  private static class ConcretePrimitiveName implements PrimitiveName {

    public static final Map<String, PrimitiveName> INSTANCES = new HashMap<String, PrimitiveName>();

    static {
      INSTANCES.put("boolean", new ConcretePrimitiveName("boolean", ConcreteClassName.BOOLEAN));
      INSTANCES.put("char", new ConcretePrimitiveName("char", ConcreteClassName.CHARACTER));
      INSTANCES.put("byte", new ConcretePrimitiveName("byte", ConcreteClassName.BYTE));
      INSTANCES.put("short", new ConcretePrimitiveName("short", ConcreteClassName.SHORT));
      INSTANCES.put("int", new ConcretePrimitiveName("int", ConcreteClassName.INTEGER));
      INSTANCES.put("long", new ConcretePrimitiveName("long", ConcreteClassName.LONG));
      INSTANCES.put("float", new ConcretePrimitiveName("float", ConcreteClassName.FLOAT));
      INSTANCES.put("double", new ConcretePrimitiveName("double", ConcreteClassName.DOUBLE));
    }
      
    private final String _name;
    private final TypeName _boxed;

    public ConcretePrimitiveName(String name, TypeName boxed) {
      _name = name;
      _boxed = boxed;
    }

    public String name() {
      return _name;
    }

    public String identifierName() {
      return "Primitive" + CodeGenerator.upperCaseFirst(_name);
    }

    public String erasedName() {
      return _name;
    }

    public <T> T accept(TypeNameVisitor<? extends T> v) {
      return v.forPrimitive(this);
    }

    public TypeName boxed() {
      return _boxed;
    }

    public String toString() {
      return Types.toString(this);
    }

    public boolean equals(Object o) {
      return Types.equals(this, o);
    }

    public int hashCode() {
      return Types.hashCode(this);
    }
  }

  private static abstract class AbstractArrayName implements ArrayName {

    private final TypeName _elt;

    public AbstractArrayName(TypeName elt) {
      _elt = elt;
    }

    public String name() {
      return _elt.name() + "[]";
    }

    public String identifierName() {
      return "ArrayOf" + _elt.identifierName();
    }

    public String erasedName() {
      return _elt.erasedName() + "[]";
    }

    public TypeName elementType() {
      return _elt;
    }

    public String toString() {
      return Types.toString(this);
    }

    public boolean equals(Object o) {
      return Types.equals(this, o);
    }

    public int hashCode() {
      return Types.hashCode(this);
    }
  }

  private static class ConcretePrimitiveArrayName extends AbstractArrayName implements PrimitiveArrayName {

    public ConcretePrimitiveArrayName(PrimitiveName elt) {
      super(elt);
    }

    public <T> T accept(TypeNameVisitor<? extends T> v) {
      return v.forPrimitiveArray(this);
    }

    public PrimitiveName elementType() {
      return (PrimitiveName) elementType();
    }
  }

  private static class ConcreteReferenceArrayName extends AbstractArrayName implements ReferenceArrayName {

    private final String _baseType; // base element type of multi-dim arrays
    private final boolean _generic;
    private final int _dims;

    public ConcreteReferenceArrayName(TypeName elementType) {
      this(elementType, false);
    }

    protected ConcreteReferenceArrayName(TypeName elementType, boolean forceGeneric) {
      super(elementType);
      Triple<String, Boolean, Integer> data =
              elementType.accept(new TypeNameVisitor<Triple<String, Boolean, Integer>>() {

        public Triple<String, Boolean, Integer> forPrimitive(PrimitiveName t) {
          throw new IllegalArgumentException();
        }

        public Triple<String, Boolean, Integer> forTreeNode(ClassName t) {
          return handleClass(t);
        }

        public Triple<String, Boolean, Integer> forString(ClassName t) {
          return handleClass(t);
        }

        public Triple<String, Boolean, Integer> forSequenceClass(SequenceClassName t) {
          return handleClass(t);
        }

        public Triple<String, Boolean, Integer> forOptionClass(OptionClassName t) {
          return handleClass(t);
        }

        public Triple<String, Boolean, Integer> forTupleClass(TupleClassName t) {
          return handleClass(t);
        }

        public Triple<String, Boolean, Integer> forGeneralClass(ClassName t) {
          return handleClass(t);
        }

        private Triple<String, Boolean, Integer> handleClass(ClassName t) {
          return Triple.make(t.className(), !t.typeArguments().isEmpty(), 1);
        }

        public Triple<String, Boolean, Integer> forPrimitiveArray(PrimitiveArrayName t) {
          return Triple.make(t.elementType().name(), false, 2);
        }

        public Triple<String, Boolean, Integer> forReferenceArray(ReferenceArrayName t) {
          Triple<String, Boolean, Integer> forElt = t.elementType().accept(this);
          return Triple.make(forElt.first(), forElt.second(), forElt.third() + 1);
        }
      });
      _baseType = data.first();
      _generic = forceGeneric || data.second();
      _dims = data.third();
    }

    public <T> T accept(TypeNameVisitor<? extends T> v) {
      return v.forReferenceArray(this);
    }

    public ConcreteReferenceArrayName withElementType(TypeName eltType) {
      return new ConcreteReferenceArrayName(eltType);
    }

    public ReferenceArrayName withMappedOriginal(String origVar) {
      return new MappedReferenceArrayName(this, origVar);
    }

    public ConcreteReferenceArrayName withSeed(String seed) {
      return new SeededReferenceArrayName(elementType(), seed);
    }

    public String accumulator(String accumVar) {
      String result =
              "java.util.List<" + elementType().name() + "> " + accumVar + " = "
              + "new java.util.LinkedList<" + elementType().name() + ">();";
      // have to declare a seed here for generics, because it has to be a declaration
      // in order to suppress warnings, and "constructor" just produces an expression
      if (_generic) {
        result += allocationDeclaration(accumVar + "_seed", "0");
      }
      return result;
    }

    public String addToAccumulator(String accumVar, String eltExpression) {
      return accumVar + ".add(" + eltExpression + ");";
    }

    public String constructor(String accumVar) {
      if (_generic) {
        return accumVar + ".toArray(" + accumVar + "_seed" + ")";
      } else {
        return accumVar + ".toArray(" + allocation("0") + ")";
      }
    }

    public String iterable(String var) {
      return var;
    }

    protected String allocation(String length) {
      StringBuilder result = new StringBuilder();
      if (_generic) {
        result.append("(" + name() + ") ");
      }
      result.append("new " + _baseType + "[" + length + "]");
      result.append(TextUtil.repeat("[]", _dims - 1));
      return result.toString();
    }

    protected String allocationDeclaration(String var, String length) {
      StringBuilder result = new StringBuilder();
      if (_generic) {
        result.append("@SuppressWarnings(\"unchecked\") ");
      }
      result.append(name() + " " + var + " = ");
      result.append(allocation(length));
      result.append(";");
      return result.toString();
    }
    
  }

  private static class SeededReferenceArrayName extends ConcreteReferenceArrayName {

    private final String _seedClass;

    public SeededReferenceArrayName(TypeName elementType, String seedClass) {
      // we assume that since a seed was provided, allocations produce unchecked warnings
      super(elementType, true);
      _seedClass = seedClass;
    }

    @Override
    protected String allocation(String length) {
      return "(" + name() + ") java.lang.reflect.Array.newInstance("
              + _seedClass + ", " + length + ")";
    }
  }

  private static class MappedReferenceArrayName extends AbstractArrayName implements ReferenceArrayName {

    private final ConcreteReferenceArrayName _wrappedArray;
    private final String _origVar;

    public MappedReferenceArrayName(ConcreteReferenceArrayName wrappedArray, String origVar) {
      super(wrappedArray.elementType());
      _wrappedArray = wrappedArray;
      _origVar = origVar;
    }

    public <T> T accept(TypeNameVisitor<? extends T> v) {
      return v.forReferenceArray(this);
    }

    public ReferenceArrayName withElementType(TypeName eltType) {
      return new MappedReferenceArrayName(_wrappedArray.withElementType(eltType), _origVar);
    }

    public ReferenceArrayName withMappedOriginal(String origVar) {
      return new MappedReferenceArrayName(_wrappedArray, origVar);
    }

    public ReferenceArrayName withSeed(String seed) {
      return new MappedReferenceArrayName(_wrappedArray.withSeed(seed), _origVar);
    }

    public String accumulator(String accumVar) {
      return _wrappedArray.allocationDeclaration(accumVar, _origVar + ".length")
              + " int " + accumVar + "_i = 0;";
    }

    public String addToAccumulator(String accumVar, String eltExpression) {
      return accumVar + "[" + accumVar + "_i++] = " + eltExpression + ";";
    }

    public String constructor(String accumVar) {
      return accumVar;
    }

    public String iterable(String var) {
      return var;
    }
  }

  private static class ConcreteClassName implements ClassName {

    private static final ClassName OBJECT = new ConcreteClassName("java.lang.Object");
    private static final ClassName BOOLEAN = new ConcreteClassName("java.lang.Boolean");
    private static final ClassName CHARACTER = new ConcreteClassName("java.lang.Character");
    private static final ClassName BYTE = new ConcreteClassName("java.lang.Byte");
    private static final ClassName SHORT = new ConcreteClassName("java.lang.Short");
    private static final ClassName INTEGER = new ConcreteClassName("java.lang.Integer");
    private static final ClassName LONG = new ConcreteClassName("java.lang.Long");
    private static final ClassName FLOAT = new ConcreteClassName("java.lang.Float");
    private static final ClassName DOUBLE = new ConcreteClassName("java.lang.Double");
    
    private final String _cName;
    private final List<TypeArgumentName> _targs;

    public ConcreteClassName(String cName) {
      _cName = cName;
      _targs = Collections.<TypeArgumentName>emptyList();
    }

    public ConcreteClassName(String cName, Iterable<? extends TypeArgumentName> targs) {
      _cName = cName;
      _targs = Collections.<TypeArgumentName>unmodifiableList(CollectUtil.makeArrayList(targs));
    }

    public String name() {
      if (_targs.isEmpty()) {
        return _cName;
      } else {
        return _cName + IterUtil.toString(IterUtil.map(_targs, GET_NAME), "<", ", ", ">");
      }
    }

    public String identifierName() {
      StringBuilder result = new StringBuilder();
      if (_cName.indexOf('.') >= 0) {
        result.append('_');
        result.append(_cName.replace('.', '_'));
      } else {
        result.append(_cName);
      }
      boolean first = true;
      for (TypeArgumentName arg : _targs) {
        if (first) {
          result.append("Of");
        } else {
          first = false;
          result.append("And");
        }
        result.append(arg.identifierName());
      }
      return result.toString();
    }

    public String erasedName() {
      return _cName;
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      return visitor.forGeneralClass(this);
    }

    public String className() {
      return _cName;
    }

    public List<TypeArgumentName> typeArguments() {
      return _targs;
    }

    public String toString() {
      return Types.toString(this);
    }

    public boolean equals(Object o) {
      return Types.equals(this, o);
    }

    public int hashCode() {
      return Types.hashCode(this);
    }
  }

  private static class StringClassName extends ConcreteClassName {

    public StringClassName(String cName) {
      super(cName);
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      return visitor.forString(this);
    }
  }

  /** Can't decide if something is a tree node at creation time, due to possible forward references. */
  private static class PossibleTreeNodeName extends ConcreteClassName {

    private final ASTModel _ast;

    public PossibleTreeNodeName(String cName, ASTModel ast) {
      super(cName);
      _ast = ast;
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      if (_ast.definesName(name())) {
        return visitor.forTreeNode(this);
      } else {
        return visitor.forGeneralClass(this);
      }
    }
  }

  private static class PLTOptionClassName extends ConcreteClassName implements OptionClassName {

    private final TypeName _eltT;

    public PLTOptionClassName(String cName, TypeArgumentName targ) {
      super(cName, Collections.singletonList(targ));
      _eltT = TYPE_ARG_BOUND.value(targ);
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      return visitor.forOptionClass(this);
    }

    public TypeName elementType() {
      return _eltT;
    }

    public OptionClassName withElementType(TypeName eltType) {
      return new PLTOptionClassName(className(), eltType);
    }

    public String nonEmptyConstructor(String eltExpression) {
      return className() + ".<" + _eltT.name() + ">some(" + eltExpression + ")";
    }

    public String emptyConstructor() {
      return className() + ".<" + _eltT.name() + ">none()";
    }

    public String emptyTester(String var) {
      return var + ".isNone()";
    }

    public String nonEmptyGetter(String var) {
      return var + ".unwrap()";
    }
  }

  private static class PLTTupleClassName extends ConcreteClassName implements TupleClassName {

    public static final Set<String> CLASSES = new HashSet<String>();

    static {
      CLASSES.add("Wrapper");
      CLASSES.add("edu.rice.cs.plt.tuple.Wrapper");
      CLASSES.add("Pair");
      CLASSES.add("edu.rice.cs.plt.tuple.Pair");
      CLASSES.add("Triple");
      CLASSES.add("edu.rice.cs.plt.tuple.Triple");
      CLASSES.add("Quad");
      CLASSES.add("edu.rice.cs.plt.tuple.Quad");
      CLASSES.add("Quint");
      CLASSES.add("edu.rice.cs.plt.tuple.Quint");
      CLASSES.add("Sextet");
      CLASSES.add("edu.rice.cs.plt.tuple.Sextet");
      CLASSES.add("Septet");
      CLASSES.add("edu.rice.cs.plt.tuple.Septet");
      CLASSES.add("Octet");
      CLASSES.add("edu.rice.cs.plt.tuple.Octet");
    }
    
    private final List<TypeName> _eltTs;

    public PLTTupleClassName(String cName, Iterable<? extends TypeArgumentName> targs) {
      super(cName, targs);
      _eltTs = Collections.unmodifiableList(CollectUtil.makeArrayList(IterUtil.map(targs, TYPE_ARG_BOUND)));
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      return visitor.forTupleClass(this);
    }

    public List<TypeName> elementTypes() {
      return _eltTs;
    }

    public TupleClassName withElementTypes(Iterable<TypeName> eltTypes) {
      return new PLTTupleClassName(className(), eltTypes);
    }

    public String constructor(Iterable<String> eltExpressions) {
      String targs = IterUtil.toString(IterUtil.map(_eltTs, GET_NAME), "<", ", ", ">");
      String args = IterUtil.toString(eltExpressions, "(", ", ", ")");
      return "new " + className() + targs + args;
    }
    private static final String[] getters = {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth"};

    public String getter(String var, int i) {
      return var + "." + getters[i] + "()";
    }
  }

  /** A sequence class for which the element type corresponds to the type argument. */
  private static class ConcreteSequenceClassName extends ConcreteClassName implements SequenceClassName {
    // supported classes and their corresponding concrete accumulator classes

    public static Map<String, String> CLASSES = new HashMap<String, String>();
    // classes that have a size() method
    public static Set<String> SIZE_CLASSES = new HashSet<String>();
    // accumulator classes that support a size argument in their constructor
    public static Set<String> SIZE_CONS_CLASSES = new HashSet<String>();

    static {
      CLASSES.put("Iterable", "java.util.ArrayList");
      CLASSES.put("java.lang.Iterable", "java.util.ArrayList");
      CLASSES.put("Collection", "java.util.ArrayList");
      CLASSES.put("java.util.Collection", "java.util.ArrayList");
      CLASSES.put("List", "java.util.ArrayList");
      CLASSES.put("java.util.List", "java.util.ArrayList");
      CLASSES.put("ArrayList", "ArrayList");
      CLASSES.put("java.util.ArrayList", "java.util.ArrayList");
      CLASSES.put("LinkedList", "LinkedList");
      CLASSES.put("java.util.LinkedList", "java.util.LinkedList");
      CLASSES.put("Queue", "java.util.LinkedList");
      CLASSES.put("java.util.Queue", "java.util.LinkedList");
      CLASSES.put("Set", "java.util.HashSet");
      CLASSES.put("java.util.Set", "java.util.HashSet");
      CLASSES.put("HashSet", "HashSet");
      CLASSES.put("java.util.HashSet", "java.util.HashSet");
      CLASSES.put("SortedSet", "java.util.TreeSet");
      CLASSES.put("java.util.SortedSet", "java.util.TreeSet");

      SIZE_CLASSES.addAll(CLASSES.keySet());
      SIZE_CLASSES.remove("Iterable");
      SIZE_CLASSES.remove("java.lang.Iterable");

      SIZE_CONS_CLASSES.add("java.util.ArrayList");
    }
    
    protected final TypeName _eltT;
    protected final String _accumClass;
    protected final String _sizeExpr; // may be an empty string (no size available) or null (size not supported)

    public ConcreteSequenceClassName(String cName, TypeArgumentName targ, String accumClass, boolean useSize) {
      this(cName, targ, accumClass, (useSize ? "" : null));
    }

    protected ConcreteSequenceClassName(String cName, TypeArgumentName targ, String accumClass, String sizeExpr) {
      super(cName, Collections.singletonList(targ));
      _eltT = TYPE_ARG_BOUND.value(targ);
      _accumClass = accumClass;
      _sizeExpr = sizeExpr;
    }

    public <T> T accept(TypeNameVisitor<? extends T> visitor) {
      return visitor.forSequenceClass(this);
    }

    public TypeName elementType() {
      return _eltT;
    }

    public SequenceClassName withElementType(TypeName elementType) {
      return new ConcreteSequenceClassName(className(), elementType, _accumClass, _sizeExpr);
    }

    public SequenceClassName withMappedOriginal(String origVar) {
      if (_sizeExpr == null) {
        return this;
      } else {
        return new ConcreteSequenceClassName(className(), _eltT, _accumClass, origVar + ".size()");
      }
    }

    /**
     * An expression producing an accumulator.  Default is {@code new accumClass<eltT>()}, with a 
     * possible size expression.
     */
    protected String accumulatorAllocation() {
      String size = (_sizeExpr == null) ? "" : _sizeExpr;
      return "new " + _accumClass + "<" + _eltT.name() + ">(" + size + ")";
    }

    /** Assign accumulatorAllocation() to a var of type {@code accumClass<eltT>}. */
    public String accumulator(String accumVar) {
      return _accumClass + "<" + _eltT.name() + "> " + accumVar + " = "
              + accumulatorAllocation() + ";";
    }

    /** Default behavior is to invoke an "add" method. */
    public String addToAccumulator(String accumVar, String eltExpression) {
      return accumVar + ".add(" + eltExpression + ");";
    }

    /** Default behavior returns the accumulator. */
    public String constructor(String accumVar) {
      return accumVar;
    }

    /** Default behavior returns the variable. */
    public String iterable(String var) {
      return var;
    }
  }

  /** No allocators of this type exist, so they have to be converted. */
  private static class PLTSizedIterableClassName extends ConcreteSequenceClassName {

    public PLTSizedIterableClassName(String cName, TypeArgumentName targ) {
      super(cName, targ, "java.util.ArrayList", true);
    }

    protected PLTSizedIterableClassName(String cName, TypeArgumentName targ, String sizeExpr) {
      super(cName, targ, "java.util.ArrayList", sizeExpr);
    }

    @Override
    public SequenceClassName withElementType(TypeName elementType) {
      return new PLTSizedIterableClassName(className(), elementType, _sizeExpr);
    }

    @Override
    public SequenceClassName withMappedOriginal(String origVar) {
      return new PLTSizedIterableClassName(className(), _eltT, origVar + ".size()");
    }

    public String constructor(String accumVar) {
      return "edu.rice.cs.plt.iter.IterUtil.asSizedIterable(" + accumVar + ")";
    }
  }

  public static TypeName parse(String name, ASTModel ast) {
    if (ConcretePrimitiveName.INSTANCES.containsKey(name)) {
      return ConcretePrimitiveName.INSTANCES.get(name);
    } else if (name.endsWith("[]")) {
      String eltName = name.substring(0, name.length() - 2);
      if (ConcretePrimitiveName.INSTANCES.containsKey(eltName)) {
        return new ConcretePrimitiveArrayName(ConcretePrimitiveName.INSTANCES.get(eltName));
      } else {
        return new ConcreteReferenceArrayName(parse(eltName, ast));
      }
    } else if (name.endsWith(">")) {
      int leftBracket = name.indexOf('<');
      String className = name.substring(0, leftBracket);

      // parse targs
      List<TypeArgumentName> targs = new LinkedList<TypeArgumentName>();
      int argStart = leftBracket + 1;
      int depth = 1;
      for (int argEnd = argStart; argEnd < name.length(); argEnd++) {
        boolean add = false;
        switch (name.charAt(argEnd)) {
          case ',':
            if (depth == 1) {
              add = true;
            }
            break;
          case '<':
            depth++;
            break;
          case '>':
            depth--;
            if (depth == 0) {
              add = true;
            }
            break;
          default:
            break;
        }
        if (add) {
          String argName = name.substring(argStart, argEnd).trim();
          TypeArgumentName targ;
          if (argName.equals("?")) {
            targ = UnboundedWildcardName.INSTANCE;
          } else if (argName.startsWith("? extends")) {
            String boundName = argName.substring("? extends".length()).trim();
            targ = new ConcreteExtendsWildcardName(parse(boundName, ast));
          } else if (argName.startsWith("? super")) {
            String boundName = argName.substring("? super".length()).trim();
            targ = new ConcreteSuperWildcardName(parse(boundName, ast));
          } else {
            targ = parse(argName, ast);
          }
          targs.add(targ);
          argStart = argEnd + 1;
        }
      }

      if (ConcreteSequenceClassName.CLASSES.containsKey(className)) {
        String accumClass = ConcreteSequenceClassName.CLASSES.get(className);
        boolean useSize = ConcreteSequenceClassName.SIZE_CLASSES.contains(className)
                && ConcreteSequenceClassName.SIZE_CONS_CLASSES.contains(accumClass);
        return new ConcreteSequenceClassName(className, targs.get(0), accumClass, useSize);
      } else if (ast.options().usePLT && PLTTupleClassName.CLASSES.contains(className)) {
        return new PLTTupleClassName(className, targs);
      } else if (ast.options().usePLT
              && (className.equals("Option") || className.equals("edu.rice.cs.plt.tuple.Option"))) {
        return new PLTOptionClassName(className, targs.get(0));
      } else if (ast.options().usePLT
              && (className.equals("SizedIterable") || className.equals("edu.rice.cs.plt.iter.SizedIterable"))) {
        return new PLTSizedIterableClassName(className, targs.get(0));
      } else {
        return new ConcreteClassName(className, targs);
      }
    } else if (name.equals("java.lang.String") || name.equals("String")) {
      return new StringClassName(name);
    } else {
      return new PossibleTreeNodeName(name, ast);
    }
  }
}
