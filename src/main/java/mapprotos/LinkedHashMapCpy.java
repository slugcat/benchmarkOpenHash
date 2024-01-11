/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package mapprotos;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.io.IOException;


public class LinkedHashMapCpy<K,V>
    extends HashMapCpy<K,V>
    implements Map<K,V>
{

  /*
   * Implementation note.  A previous version of this class was
   * internally structured a little differently. Because superclass
   * HashMapCpy now uses trees for some of its nodes, class
   * LinkedHashMapCpy.Entry is now treated as intermediary node class
   * that can also be converted to tree form. The name of this
   * class, LinkedHashMapCpy.Entry, is confusing in several ways in its
   * current context, but cannot be changed.  Otherwise, even though
   * it is not exported outside this package, some existing source
   * code is known to have relied on a symbol resolution corner case
   * rule in calls to removeEldestEntry that suppressed compilation
   * errors due to ambiguous usages. So, we keep the name to
   * preserve unmodified compilability.
   *
   * The changes in node classes also require using two fields
   * (head, tail) rather than a pointer to a header node to maintain
   * the doubly-linked before/after list. This class also
   * previously used a different style of callback methods upon
   * access, insertion, and removal.
   */

  /**
   * HashMapCpy.Node subclass for normal LinkedHashMapCpy entries.
   */
  static class Entry<K,V> extends HashMapCpy.Node<K,V> {
    LinkedHashMapCpy.Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
      super(hash, key, value, next);
    }
  }

  @java.io.Serial
  private static final long serialVersionUID = 3801124242820219131L;

  /**
   * The head (eldest) of the doubly linked list.
   */
  transient LinkedHashMapCpy.Entry<K,V> head;

  /**
   * The tail (youngest) of the doubly linked list.
   */
  transient LinkedHashMapCpy.Entry<K,V> tail;

  /**
   * The iteration ordering method for this linked hash map: {@code true}
   * for access-order, {@code false} for insertion-order.
   *
   * @serial
   */
  final boolean accessOrder;

  // internal utilities

  // link at the end of list
  private void linkNodeLast(LinkedHashMapCpy.Entry<K,V> p) {
    LinkedHashMapCpy.Entry<K,V> last = tail;
    tail = p;
    if (last == null)
      head = p;
    else {
      p.before = last;
      last.after = p;
    }
  }

  // apply src's links to dst
  private void transferLinks(LinkedHashMapCpy.Entry<K,V> src,
                             LinkedHashMapCpy.Entry<K,V> dst) {
    LinkedHashMapCpy.Entry<K,V> b = dst.before = src.before;
    LinkedHashMapCpy.Entry<K,V> a = dst.after = src.after;
    if (b == null)
      head = dst;
    else
      b.after = dst;
    if (a == null)
      tail = dst;
    else
      a.before = dst;
  }

  // overrides of HashMapCpy hook methods

  void reinitialize() {
    super.reinitialize();
    head = tail = null;
  }

  Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    LinkedHashMapCpy.Entry<K,V> p =
        new LinkedHashMapCpy.Entry<>(hash, key, value, e);
    linkNodeLast(p);
    return p;
  }

  Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
    LinkedHashMapCpy.Entry<K,V> q = (LinkedHashMapCpy.Entry<K,V>)p;
    LinkedHashMapCpy.Entry<K,V> t =
        new LinkedHashMapCpy.Entry<>(q.hash, q.key, q.value, next);
    transferLinks(q, t);
    return t;
  }

  TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
    TreeNode<K,V> p = new TreeNode<>(hash, key, value, next);
    linkNodeLast(p);
    return p;
  }

  TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
    LinkedHashMapCpy.Entry<K,V> q = (LinkedHashMapCpy.Entry<K,V>)p;
    TreeNode<K,V> t = new TreeNode<>(q.hash, q.key, q.value, next);
    transferLinks(q, t);
    return t;
  }

  void afterNodeRemoval(Node<K,V> e) { // unlink
    LinkedHashMapCpy.Entry<K,V> p =
        (LinkedHashMapCpy.Entry<K,V>)e, b = p.before, a = p.after;
    p.before = p.after = null;
    if (b == null)
      head = a;
    else
      b.after = a;
    if (a == null)
      tail = b;
    else
      a.before = b;
  }

  void afterNodeInsertion(boolean evict) { // possibly remove eldest
    LinkedHashMapCpy.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
      K key = first.key;
      removeNode(hash(key), key, null, false, true);
    }
  }

  void afterNodeAccess(Node<K,V> e) { // move node to last
    LinkedHashMapCpy.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
      LinkedHashMapCpy.Entry<K,V> p =
          (LinkedHashMapCpy.Entry<K,V>)e, b = p.before, a = p.after;
      p.after = null;
      if (b == null)
        head = a;
      else
        b.after = a;
      if (a != null)
        a.before = b;
      else
        last = b;
      if (last == null)
        head = p;
      else {
        p.before = last;
        last.after = p;
      }
      tail = p;
      ++modCount;
    }
  }

  void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after) {
      s.writeObject(e.key);
      s.writeObject(e.value);
    }
  }

  /**
   * Constructs an empty insertion-ordered {@code LinkedHashMapCpy} instance
   * with the specified initial capacity and load factor.
   *
   * @apiNote
   * To create a {@code LinkedHashMapCpy} with an initial capacity that accommodates
   * an expected number of mappings, use {@link #newLinkedHashMapCpy(int) newLinkedHashMapCpy}.
   *
   * @param  initialCapacity the initial capacity
   * @param  loadFactor      the load factor
   * @throws IllegalArgumentException if the initial capacity is negative
   *         or the load factor is nonpositive
   */
  public LinkedHashMapCpy(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
  }

  /**
   * Constructs an empty insertion-ordered {@code LinkedHashMapCpy} instance
   * with the specified initial capacity and a default load factor (0.75).
   *
   * @apiNote
   * To create a {@code LinkedHashMapCpy} with an initial capacity that accommodates
   * an expected number of mappings, use {@link #newLinkedHashMapCpy(int) newLinkedHashMapCpy}.
   *
   * @param  initialCapacity the initial capacity
   * @throws IllegalArgumentException if the initial capacity is negative
   */
  public LinkedHashMapCpy(int initialCapacity) {
    super(initialCapacity);
    accessOrder = false;
  }

  /**
   * Constructs an empty insertion-ordered {@code LinkedHashMapCpy} instance
   * with the default initial capacity (16) and load factor (0.75).
   */
  public LinkedHashMapCpy() {
    super();
    accessOrder = false;
  }

  /**
   * Constructs an insertion-ordered {@code LinkedHashMapCpy} instance with
   * the same mappings as the specified map.  The {@code LinkedHashMapCpy}
   * instance is created with a default load factor (0.75) and an initial
   * capacity sufficient to hold the mappings in the specified map.
   *
   * @param  m the map whose mappings are to be placed in this map
   * @throws NullPointerException if the specified map is null
   */
  public LinkedHashMapCpy(Map<? extends K, ? extends V> m) {
    super();
    accessOrder = false;
    putMapEntries(m, false);
  }

  /**
   * Constructs an empty {@code LinkedHashMapCpy} instance with the
   * specified initial capacity, load factor and ordering mode.
   *
   * @param  initialCapacity the initial capacity
   * @param  loadFactor      the load factor
   * @param  accessOrder     the ordering mode - {@code true} for
   *         access-order, {@code false} for insertion-order
   * @throws IllegalArgumentException if the initial capacity is negative
   *         or the load factor is nonpositive
   */
  public LinkedHashMapCpy(int initialCapacity,
                       float loadFactor,
                       boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
  }


  /**
   * Returns {@code true} if this map maps one or more keys to the
   * specified value.
   *
   * @param value value whose presence in this map is to be tested
   * @return {@code true} if this map maps one or more keys to the
   *         specified value
   */
  public boolean containsValue(Object value) {
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after) {
      V v = e.value;
      if (v == value || (value != null && value.equals(v)))
        return true;
    }
    return false;
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   *
   * <p>A return value of {@code null} does not <i>necessarily</i>
   * indicate that the map contains no mapping for the key; it's also
   * possible that the map explicitly maps the key to {@code null}.
   * The {@link #containsKey containsKey} operation may be used to
   * distinguish these two cases.
   */
  public V get(Object key) {
    Node<K,V> e;
    if ((e = getNode(key)) == null)
      return null;
    if (accessOrder)
      afterNodeAccess(e);
    return e.value;
  }

  /**
   * {@inheritDoc}
   */
  public V getOrDefault(Object key, V defaultValue) {
    Node<K,V> e;
    if ((e = getNode(key)) == null)
      return defaultValue;
    if (accessOrder)
      afterNodeAccess(e);
    return e.value;
  }

  /**
   * {@inheritDoc}
   */
  public void clear() {
    super.clear();
    head = tail = null;
  }

  /**
   * Returns {@code true} if this map should remove its eldest entry.
   * This method is invoked by {@code put} and {@code putAll} after
   * inserting a new entry into the map.  It provides the implementor
   * with the opportunity to remove the eldest entry each time a new one
   * is added.  This is useful if the map represents a cache: it allows
   * the map to reduce memory consumption by deleting stale entries.
   *
   * <p>Sample use: this override will allow the map to grow up to 100
   * entries and then delete the eldest entry each time a new entry is
   * added, maintaining a steady state of 100 entries.
   * <pre>
   *     private static final int MAX_ENTRIES = 100;
   *
   *     protected boolean removeEldestEntry(Map.Entry eldest) {
   *        return size() &gt; MAX_ENTRIES;
   *     }
   * </pre>
   *
   * <p>This method typically does not modify the map in any way,
   * instead allowing the map to modify itself as directed by its
   * return value.  It <i>is</i> permitted for this method to modify
   * the map directly, but if it does so, it <i>must</i> return
   * {@code false} (indicating that the map should not attempt any
   * further modification).  The effects of returning {@code true}
   * after modifying the map from within this method are unspecified.
   *
   * <p>This implementation merely returns {@code false} (so that this
   * map acts like a normal map - the eldest element is never removed).
   *
   * @param    eldest The least recently inserted entry in the map, or if
   *           this is an access-ordered map, the least recently accessed
   *           entry.  This is the entry that will be removed it this
   *           method returns {@code true}.  If the map was empty prior
   *           to the {@code put} or {@code putAll} invocation resulting
   *           in this invocation, this will be the entry that was just
   *           inserted; in other words, if the map contains a single
   *           entry, the eldest entry is also the newest.
   * @return   {@code true} if the eldest entry should be removed
   *           from the map; {@code false} if it should be retained.
   */
  protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false;
  }

  /**
   * Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own {@code remove} operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear}
   * operations.  It does not support the {@code add} or {@code addAll}
   * operations.
   * Its {@link Spliterator} typically provides faster sequential
   * performance but much poorer parallel performance than that of
   * {@code HashMapCpy}.
   *
   * @return a set view of the keys contained in this map
   */
  private Set<K> keySet; // TODO remove this if this class is moved to java.util
  public Set<K> keySet() {
    Set<K> ks = keySet;
    if (ks == null) {
      ks = new LinkedHashMapCpy.LinkedKeySet();
      keySet = ks;
    }
    return ks;
  }

  @Override
  final <T> T[] keysToArray(T[] a) {
    Object[] r = a;
    int idx = 0;
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after) {
      r[idx++] = e.key;
    }
    return a;
  }

  @Override
  final <T> T[] valuesToArray(T[] a) {
    Object[] r = a;
    int idx = 0;
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after) {
      r[idx++] = e.value;
    }
    return a;
  }

  final class LinkedKeySet extends AbstractSet<K> {
    public final int size()                 { return size; }
    public final void clear()               { LinkedHashMapCpy.this.clear(); }
    public final Iterator<K> iterator() {
      return new LinkedHashMapCpy.LinkedKeyIterator();
    }
    public final boolean contains(Object o) { return containsKey(o); }
    public final boolean remove(Object key) {
      return removeNode(hash(key), key, null, false, true) != null;
    }
    public final Spliterator<K> spliterator()  {
      return Spliterators.spliterator(this, Spliterator.SIZED |
          Spliterator.ORDERED |
          Spliterator.DISTINCT);
    }

    public Object[] toArray() {
      return keysToArray(new Object[size]);
    }

    public <T> T[] toArray(T[] a) {
      return keysToArray(prepareArray(a));
    }

    public final void forEach(Consumer<? super K> action) {
      if (action == null)
        throw new NullPointerException();
      int mc = modCount;
      for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after)
        action.accept(e.key);
      if (modCount != mc)
        throw new ConcurrentModificationException();
    }
  }

  /**
   * Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own {@code remove} operation),
   * the results of the iteration are undefined.  The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the {@code Iterator.remove},
   * {@code Collection.remove}, {@code removeAll},
   * {@code retainAll} and {@code clear} operations.  It does not
   * support the {@code add} or {@code addAll} operations.
   * Its {@link Spliterator} typically provides faster sequential
   * performance but much poorer parallel performance than that of
   * {@code HashMapCpy}.
   *
   * @return a view of the values contained in this map
   */
  private Collection<V> values; // TODO remove this if this class is moved to java.util
  public Collection<V> values() {
    Collection<V> vs = values;
    if (vs == null) {
      vs = new LinkedHashMapCpy.LinkedValues();
      values = vs;
    }
    return vs;
  }

  final class LinkedValues extends AbstractCollection<V> {
    public final int size()                 { return size; }
    public final void clear()               { LinkedHashMapCpy.this.clear(); }
    public final Iterator<V> iterator() {
      return new LinkedHashMapCpy.LinkedValueIterator();
    }
    public final boolean contains(Object o) { return containsValue(o); }
    public final Spliterator<V> spliterator() {
      return Spliterators.spliterator(this, Spliterator.SIZED |
          Spliterator.ORDERED);
    }

    public Object[] toArray() {
      return valuesToArray(new Object[size]);
    }

    public <T> T[] toArray(T[] a) {
      return valuesToArray(prepareArray(a));
    }

    public final void forEach(Consumer<? super V> action) {
      if (action == null)
        throw new NullPointerException();
      int mc = modCount;
      for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after)
        action.accept(e.value);
      if (modCount != mc)
        throw new ConcurrentModificationException();
    }
  }

  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own {@code remove} operation, or through the
   * {@code setValue} operation on a map entry returned by the
   * iterator) the results of the iteration are undefined.  The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the {@code Iterator.remove},
   * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
   * {@code clear} operations.  It does not support the
   * {@code add} or {@code addAll} operations.
   * Its {@link Spliterator} typically provides faster sequential
   * performance but much poorer parallel performance than that of
   * {@code HashMapCpy}.
   *
   * @return a set view of the mappings contained in this map
   */
  public Set<Map.Entry<K,V>> entrySet() {
    Set<Map.Entry<K,V>> es;
    return (es = entrySet) == null ? (entrySet = new LinkedHashMapCpy.LinkedEntrySet()) : es;
  }

  final class LinkedEntrySet extends AbstractSet<Map.Entry<K,V>> {
    public final int size()                 { return size; }
    public final void clear()               { LinkedHashMapCpy.this.clear(); }
    public final Iterator<Map.Entry<K,V>> iterator() {
      return new LinkedHashMapCpy.LinkedEntryIterator();
    }
    public final boolean contains(Object o) {
      if (!(o instanceof Map.Entry<?, ?> e))
        return false;
      Object key = e.getKey();
      Node<K,V> candidate = getNode(key);
      return candidate != null && candidate.equals(e);
    }
    public final boolean remove(Object o) {
      if (o instanceof Map.Entry<?, ?> e) {
        Object key = e.getKey();
        Object value = e.getValue();
        return removeNode(hash(key), key, value, true, true) != null;
      }
      return false;
    }
    public final Spliterator<Map.Entry<K,V>> spliterator() {
      return Spliterators.spliterator(this, Spliterator.SIZED |
          Spliterator.ORDERED |
          Spliterator.DISTINCT);
    }
    public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
      if (action == null)
        throw new NullPointerException();
      int mc = modCount;
      for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after)
        action.accept(e);
      if (modCount != mc)
        throw new ConcurrentModificationException();
    }
  }

  // Map overrides

  public void forEach(BiConsumer<? super K, ? super V> action) {
    if (action == null)
      throw new NullPointerException();
    int mc = modCount;
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after)
      action.accept(e.key, e.value);
    if (modCount != mc)
      throw new ConcurrentModificationException();
  }

  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    if (function == null)
      throw new NullPointerException();
    int mc = modCount;
    for (LinkedHashMapCpy.Entry<K,V> e = head; e != null; e = e.after)
      e.value = function.apply(e.key, e.value);
    if (modCount != mc)
      throw new ConcurrentModificationException();
  }

  // Iterators

  abstract class LinkedHashIterator {
    LinkedHashMapCpy.Entry<K,V> next;
    LinkedHashMapCpy.Entry<K,V> current;
    int expectedModCount;

    LinkedHashIterator() {
      next = head;
      expectedModCount = modCount;
      current = null;
    }

    public final boolean hasNext() {
      return next != null;
    }

    final LinkedHashMapCpy.Entry<K,V> nextNode() {
      LinkedHashMapCpy.Entry<K,V> e = next;
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
      if (e == null)
        throw new NoSuchElementException();
      current = e;
      next = e.after;
      return e;
    }

    public final void remove() {
      Node<K,V> p = current;
      if (p == null)
        throw new IllegalStateException();
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
      current = null;
      removeNode(p.hash, p.key, null, false, false);
      expectedModCount = modCount;
    }
  }

  final class LinkedKeyIterator extends LinkedHashMapCpy<K,V>.LinkedHashIterator
      implements Iterator<K> {
    public final K next() { return nextNode().getKey(); }
  }

  final class LinkedValueIterator extends LinkedHashMapCpy<K,V>.LinkedHashIterator
      implements Iterator<V> {
    public final V next() { return nextNode().value; }
  }

  final class LinkedEntryIterator extends LinkedHashMapCpy.LinkedHashIterator
      implements Iterator<Map.Entry<K,V>> {
    public final Map.Entry<K,V> next() { return nextNode(); }
  }

  /**
   * Creates a new, empty, insertion-ordered LinkedHashMapCpy suitable for the expected number of mappings.
   * The returned map uses the default load factor of 0.75, and its initial capacity is
   * generally large enough so that the expected number of mappings can be added
   * without resizing the map.
   *
   * @param numMappings the expected number of mappings
   * @param <K>         the type of keys maintained by the new map
   * @param <V>         the type of mapped values
   * @return the newly created map
   * @throws IllegalArgumentException if numMappings is negative
   * @since 19
   */
  public static <K, V> LinkedHashMapCpy<K, V> newLinkedHashMapCpy(int numMappings) {
    if (numMappings < 0) {
      throw new IllegalArgumentException("Negative number of mappings: " + numMappings);
    }
    return new LinkedHashMapCpy<>(HashMapCpy.calculateHashMapCpyCapacity(numMappings));
  }

}
