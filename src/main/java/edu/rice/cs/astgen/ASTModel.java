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

import edu.rice.cs.astgen.Types.ClassName;
import edu.rice.cs.astgen.Types.KindTag;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.collect.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A data structure modeling the hierarchy defined by an ASTGen source file. 
 *
 * @version $Id: ASTModel.java,v 1.2 2008/07/16 15:45:13 dlsmith Exp $
 */
public class ASTModel {
  private final Options _options;
  private final OneToOneRelation<String, NodeType> _types;
  private final Set<NodeType> _tops;
  private final InjectiveRelation<NodeType, NodeType> _nodeChildren;
  private final Relation<NodeType, NodeType> _rootDescendents;
  
  public ASTModel(Options options) {
    _options = options;
    _types = IndexedOneToOneRelation.makeLinkedHashBased();
    _tops = new LinkedHashSet<NodeType>();
    _nodeChildren = IndexedInjectiveRelation.makeLinkedHashBased();
    _rootDescendents = IndexedRelation.makeLinkedHashBased();
  }
  
  // ACCESSOR METHODS:
  
  public Options options() { return _options; }

  /** Whether the given name is defined in this tree. */
  public boolean definesName(String name) { return _types.containsFirst(name); }
  
  /** Get all names of node types in the tree (in declaration order). */
  public PredicateSet<String> names() { return CollectUtil.immutable(_types.firstSet()); }
  
  /** Get the class or interface with the given name, if it exists. */
  public Option<NodeType> typeForName(String name) { return Option.wrap(_types.value(name)); }
  
  /**
   * Get the declaration corresponding to the given name, or {@code none()} if the name is not a tree node.
   * Throws an exception if the name is designated as a tree node but doesn't appear in the tree.
   */
  public Option<NodeType> typeForName(TypeName name) {
    if (Types.kind(name).equals(KindTag.TREE_NODE)) {
      NodeType result = _types.value(((ClassName) name).className());
      if (result == null) { throw new IllegalArgumentException("Unrecognized tree node: " + name.name()); }
      return Option.some(result);
    }
    else { return Option.none(); }
  }
  
  /** Get all classes and interfaces defined in the tree (in declaration order). */
  public PredicateSet<NodeType> types() { return CollectUtil.immutable(_types.secondSet()); }
  
  /** Get all interfaces defined in the tree (in declaration order). */
  public Iterable<NodeInterface> interfaces() {
    return IterUtil.filterInstances(_types.secondSet(), NodeInterface.class);
  }
  
  /** Get all classes defined in the tree (in declaration order). */
  public Iterable<NodeClass> classes() {
    return IterUtil.filterInstances(_types.secondSet(), NodeClass.class);
  }
  
  /** Whether the given type is a top type. */
  public boolean isTop(NodeType t) { return _tops.contains(t); }
  
  /**
   * Whether the given class has no NodeClass parent -- it is either a top type, or its parent
   * is an interface.
   */
  public boolean isTopClass(NodeClass c) {
    NodeType parent = _nodeChildren.antecedent(c);
    return (parent == null) || !(parent instanceof NodeClass);
  }
  
  /** Get the set of top types -- types that don't have a parent in the tree (in declaration order). */
  public PredicateSet<NodeType> topTypes() { return CollectUtil.immutable(_tops); }
  
  /** Whether the given type is a root type. */
  public boolean isRoot(NodeType t) { return _rootDescendents.containsFirst(t); }
  
  /** Get the set of root types -- types that have been designated as "roots" (in declaration order). */
  public PredicateSet<NodeType> rootTypes() { return CollectUtil.immutable(_rootDescendents.firstSet()); }
  
  /** Get the direct children of the given parent (in declaration order). */
  public PredicateSet<NodeType> children(NodeType parent) {
    return CollectUtil.immutable(_nodeChildren.matchFirst(parent));
  }
  
  /** Get the parent of the given node type.  For top types, the result is {@code none()}. */
  public Option<NodeType> parent(NodeType child) { return Option.wrap(_nodeChildren.antecedent(child)); }
  
  /** Whether {@code t} is a descendent of root node {@code root}. */
  public boolean isDescendent(NodeType root, NodeType t) {
    return _rootDescendents.contains(root, t);
  }
  
  /**
   * Get the descendents of the given root (in declaration order).  This is the reflexive, transitive
   * closure of the "child" relation for root types.  For non-roots, the result is empty.
   */
  public PredicateSet<NodeType> descendents(NodeType root) {
    return CollectUtil.immutable(_rootDescendents.matchFirst(root));
  }
  
  /**
   * Get all ancestors of the given node type that are designated as "roots" (in declaration order).
   * If {@code t} is a root, it is included in the result.
   */
  public PredicateSet<NodeType> ancestorRoots(NodeType t) {
    return CollectUtil.immutable(_rootDescendents.matchSecond(t));
  }
  
  // MUTATION METHODS:
  
  public void addTopType(NodeType t, boolean root) {
    _types.add(t.name(), t);
    _tops.add(t);
    if (root) { _rootDescendents.add(t, t); }
  }
  
  public void addType(NodeType t, boolean root, NodeType parent) {
    _types.add(t.name(), t);
    if (root) { _rootDescendents.add(t, t); }
    _nodeChildren.add(parent, t);
    for (NodeType ancestor : _rootDescendents.matchSecond(parent)) {
      _rootDescendents.add(ancestor, t);
    }
  }
  
  public void removeType(NodeType t) {
    if (!_types.containsSecond(t)) { throw new IllegalArgumentException("Unknown type"); }
    if (_nodeChildren.containsFirst(t)) { throw new IllegalArgumentException("Type has children"); }
    _types.remove(t.name(), t);
    _tops.remove(t);
    _nodeChildren.matchSecond(t).clear();
    _rootDescendents.matchSecond(t).clear();
  }
  
  public void removeTypeWithChildren(NodeType t) {
    for (NodeType child : _nodeChildren.matchFirst(t)) {
      removeTypeWithChildren(child);
    }
    removeType(t);
  }
    
}
