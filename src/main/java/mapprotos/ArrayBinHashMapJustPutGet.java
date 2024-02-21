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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class ArrayBinHashMapJustPutGet<K,V> extends AbstractMap<K,V> // TODO maybe rename to have maybe Iterate
    implements Map<K,V>, Cloneable, Serializable {

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMapCpy for its Entry subclass.)
     */
    static class Node<K,V> implements Entry<K,V> {
        final int hash;
        final K key;
        final V value;

        // TODO use (or add) an internal only mechanism to create an instance of a class that lacks a no-arg constructor.
        //  The resulting Object would be invalid, since it is not properly initialized, but if it were only ever used
        //  as a sentinel like this constant, and it did not leak out of its java OOB class, it seems harmless.
        //  Using this facility Node can be generic, since ABSENT would be in the proper class.  The alternative used here
        //  requires any returned key to be cast (at some runtime overhead) whenever it is returned :-(.
        static final Object NO_KEY = new Object();
        static final Node EMPTY = new Node(0, null, null) /* TODO uncomment MaskedHashKeyValue.default*/;

        Node(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            throw new UnsupportedOperationException("not implemented");
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;

            return o instanceof Map.Entry<?, ?> e
                    && Objects.equals(key, e.getKey())
                    && Objects.equals(value, e.getValue());
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     */
    transient Node<K,V>[][] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty {@code HashMap} with the specified initial
     * capacity and load factor.
     *
     * @apiNote
     * To create a {@code HashMap} with an initial capacity that accommodates
     * an expected number of mappings, use {@link #newHashMap(int) newHashMap}.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public ArrayBinHashMapJustPutGet(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            throw new IllegalArgumentException("initialCapacity:" + initialCapacity + " too big");
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty {@code HashMap} with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @apiNote
     * To create a {@code HashMap} with an initial capacity that accommodates
     * an expected number of mappings, use {@link #newHashMap(int) newHashMap}.
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public ArrayBinHashMapJustPutGet(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }


    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    public V get(Object key) {

        // not calling getNode and wrapping the Node in a NodeRef saves a huge amount of time.
        Object[] tab; Object binObj, e; int n, hash; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (binObj = tab[(n - 1) & (hash = hash(key))]) != null) {
            Node<K,V>[] nodes = (Node<K, V>[]) binObj;
            for (int b = 0; b < nodes.length; b++) {
                if (nodes[b].hash == hash &&
                    ((k = nodes[b].key) == key || (key != null && key.equals(k))))
                    return nodes[b].value;
            }
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[][] tab;
        int n;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        for (;;) {
            int i;
            Node<K, V>[] nodes;
            if ((nodes = tab[i = (n - 1) & hash]) == null) {
                nodes = tab[i] = new Node[1];
                nodes[0] = new Node(hash, key, value);
                ++modCount;
                ++size;
                return null;
            }
            Object k;
            @SuppressWarnings("unchecked")
            int binLen = nodes.length;
            for (int b = 0; b < binLen; ++b) {
                if (nodes[b].hash == hash &&
                    ((k = nodes[b].key) == key || (key != null && key.equals(k)))) {
                    ++modCount;
                    V oldValue = nodes[b].value;
                    if (!onlyIfAbsent || oldValue == null)
                        nodes[b] = newNode(hash, key, value);
                    return oldValue;
                }
            }
            // Key not present
            Node<K,V>[] newBin = new Node[binLen+1];
            System.arraycopy(nodes, 0, newBin, 0, binLen); //
            newBin[binLen] = newNode(hash, key, value);
            tab[i] = newBin;

            ++modCount;
            if (++size > threshold) {
                tab = resize();
                // Since Entrys can't simply be moved (as they can for HashMap), it makes sense to only add the new Entry after resize.
                // This is like IdentityHashMap.
                continue;
            }
            return null;
        }
    }

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[][] resize() {
        Node<K,V>[][] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings("unchecked")
        Node<K,V>[][] newTab = (Node<K,V>[][])new Node[newCap][];
        table = newTab;
        if (oldTab != null) {
            for (int ot = 0; ot < oldCap; ++ot) { // 'ot' old table
                Node<K,V>[]oldBin;
                int movingTabIndex = ot + oldCap;
                if ((oldBin = oldTab[ot]) != null) {
                    oldTab[ot] = null;

                }
            }
        }
        table = newTab;
        return newTab;
    }


    @SuppressWarnings("rawtypes")
    private static <K, V> void rehashArrayBinOneOfManyHasDifferentIndex(int oldBinLen, Node[] oldArrayBin, int oldBinIndexForOne, int newTabIndexForOne, int newTabIndexForMany, Object[] newTab) {
        Node[] newManyArrayBin = new Node[oldBinLen-1];
        newTab[newTabIndexForOne] = new NodeRef(oldArrayBin[oldBinIndexForOne]);
        System.arraycopy(oldArrayBin, 0, newManyArrayBin, 0, oldBinIndexForOne);
        System.arraycopy(oldArrayBin, oldBinIndexForOne+1, newManyArrayBin, oldBinIndexForOne, oldBinLen-oldBinIndexForOne-1);
        newTab[newTabIndexForMany] = newManyArrayBin;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Object[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
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
     *
     * @return a set view of the keys contained in this map
     */
    private Set<K> keySet; // TODO remove this if this class is moved to java.util
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    /**
     * Prepares the array for {@link Collection#toArray(Object[])} implementation.
     * If supplied array is smaller than this map size, a new array is allocated.
     * If supplied array is bigger than this map size, a null is written at size index.
     *
     * @param a an original array passed to {@code toArray()} method
     * @param <T> type of array elements
     * @return an array ready to be filled and returned from {@code toArray()} method.
     */
    @SuppressWarnings("unchecked")
    final <T> T[] prepareArray(T[] a) {
        int size = this.size;
        if (a.length < size) {
            return (T[]) java.lang.reflect.Array
                    .newInstance(a.getClass().getComponentType(), size);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    /**
     * Fills an array with this map keys and returns it. This method assumes
     * that input array is big enough to fit all the keys. Use
     * {@link #prepareArray(Object[])} to ensure this.
     *
     * @param a an array to fill
     * @param <T> type of array elements
     * @return supplied array
     */
    <T> T[] keysToArray(T[] a) {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//        Object[] r = a;
//        Node<K,V>[] tab;
//        int idx = 0;
//        if (size > 0 && (tab = table) != null) {
//            for (Node<K,V> e : tab) {
//                for (; e != null; e = e.next) {
//                    r[idx++] = e.key;
//                }
//            }
//        }
//        return a;
    }

    /**
     * Fills an array with this map values and returns it. This method assumes
     * that input array is big enough to fit all the values. Use
     * {@link #prepareArray(Object[])} to ensure this.
     *
     * @param a an array to fill
     * @param <T> type of array elements
     * @return supplied array
     */
    <T> T[] valuesToArray(T[] a) {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//        Object[] r = a;
//        Node<K,V>[] tab;
//        int idx = 0;
//        if (size > 0 && (tab = table) != null) {
//            for (Node<K,V> e : tab) {
//                for (; e != null; e = e.next) {
//                    r[idx++] = e.value;
//                }
//            }
//        }
//        return a;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { ArrayBinHashMapJustPutGet.this.clear(); }
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(ArrayBinHashMapJustPutGet.this, 0, -1, 0, 0);
        }

        public Object[] toArray() {
            return keysToArray(new Object[size]);
        }

        public <T> T[] toArray(T[] a) {
            return keysToArray(prepareArray(a));
        }

        public final void forEach(Consumer<? super K> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            Node<K,V>[] tab;
//            if (action == null)
//                throw new NullPointerException();
//            if (size > 0 && (tab = table) != null) {
//                int mc = modCount;
//                for (Node<K,V> e : tab) {
//                    for (; e != null; e = e.next)
//                        action.accept(e.key);
//                }
//                if (modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
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
     *
     * @return a view of the values contained in this map
     */
    private Collection<V> values; // TODO remove this if this class is moved to java.util
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { ArrayBinHashMapJustPutGet.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(ArrayBinHashMapJustPutGet.this, 0, -1, 0, 0);
        }

        public Object[] toArray() {
            return valuesToArray(new Object[size]);
        }

        public <T> T[] toArray(T[] a) {
            return valuesToArray(prepareArray(a));
        }

        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//                int mc = modCount;
//                for (Node<K,V> e : tab) {
//                    for (; e != null; e = e.next)
//                        action.accept(e.value);
//                }
//                if (modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
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
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Entry<K,V>> entrySet() {
        Set<Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { ArrayBinHashMapJustPutGet.this.clear(); }
        public final Iterator<Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            if (!(o instanceof Map.Entry<?, ?> e))
//                return false;
//            Object key = e.getKey();
//            Node<K,V> candidate = getNode(key);
//            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//                Object key = e.getKey();
//                Object value = e.getValue();
//                return removeNode(hash(key), key, value, true, true) != null;
//            }
//            return false;
        }
        public final Spliterator<Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(ArrayBinHashMapJustPutGet.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            if (size > 0 && (tab = table) != null) {
//                int mc = modCount;
//                for (Node<K,V> e : tab) {
//                    for (; e != null; e = e.next)
//                        action.accept(e);
//                }
//                if (modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
        }
    }

    // Overrides of JDK8 Map extension methods

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//        Node<K,V> e;
//        return (e = getNode(key)) == null ? defaultValue : e.value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException("not done yet");
//        Node<K,V> e; V v;
//        if ((e = getNode(key)) != null &&
//            ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
//            e.value = newValue;
//            afterNodeAccess(e);
//            return true;
//        }
//        return false;
    }

    @Override
    public V replace(K key, V value) {
        throw new UnsupportedOperationException("not done yet");
//        Node<K,V> e;
//        if ((e = getNode(key)) != null) {
//            V oldValue = e.value;
//            e.value = value;
//            afterNodeAccess(e);
//            return oldValue;
//        }
//        return null;
    }






    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            expectedModCount = modCount;
//            Node<K,V>[] t = table;
//            current = next = null;
//            index = 0;
//            if (t != null && size > 0) { // advance to first entry
//                do {} while (index < t.length && (next = t[index++]) == null);
//            }
        }

        public final boolean hasNext() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            return next != null;
        }

        final Node<K,V> nextNode() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            Node<K,V>[] t;
//            Node<K,V> e = next;
//            if (modCount != expectedModCount)
//                throw new ConcurrentModificationException();
//            if (e == null)
//                throw new NoSuchElementException();
//            if ((next = (current = e).next) == null && (t = table) != null) {
//                do {} while (index < t.length && (next = t[index++]) == null);
//            }
//            return e;
        }

        public final void remove() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            Node<K,V> p = current;
//            if (p == null)
//                throw new IllegalStateException();
//            if (modCount != expectedModCount)
//                throw new ConcurrentModificationException();
//            current = null;
//            removeNode(p.hash, p.key, null, false, false);
//            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends HashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends HashIterator
        implements Iterator<Entry<K,V>> {
        public final Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K,V> {
        final ArrayBinHashMapJustPutGet<K,V> map;
        Node<K,V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(ArrayBinHashMapJustPutGet<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int hi;
//            if ((hi = fence) < 0) {
//                ArrayBucketHashMap<K,V> m = map;
//                est = m.size;
//                expectedModCount = m.modCount;
//                Node<K,V>[] tab = m.table;
//                hi = fence = (tab == null) ? 0 : tab.length;
//            }
//            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(ArrayBinHashMapJustPutGet<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
//            return (lo >= mid || current != null) ? null :
//                new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
//                                        expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int i, hi, mc;
//            if (action == null)
//                throw new NullPointerException();
//            ArrayBucketHashMap<K,V> m = map;
//            Node<K,V>[] tab = m.table;
//            if ((hi = fence) < 0) {
//                mc = expectedModCount = m.modCount;
//                hi = fence = (tab == null) ? 0 : tab.length;
//            }
//            else
//                mc = expectedModCount;
//            if (tab != null && tab.length >= hi &&
//                (i = index) >= 0 && (i < (index = hi) || current != null)) {
//                Node<K,V> p = current;
//                current = null;
//                do {
//                    if (p == null)
//                        p = tab[i++];
//                    else {
//                        action.accept(p.key);
//                        p = p.next;
//                    }
//                } while (p != null || i < hi);
//                if (m.modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int hi;
//            if (action == null)
//                throw new NullPointerException();
//            Node<K,V>[] tab = map.table;
//            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
//                while (current != null || index < hi) {
//                    if (current == null)
//                        current = tab[index++];
//                    else {
//                        K k = current.key;
//                        current = current.next;
//                        action.accept(k);
//                        if (map.modCount != expectedModCount)
//                            throw new ConcurrentModificationException();
//                        return true;
//                    }
//                }
//            }
//            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(ArrayBinHashMapJustPutGet<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
//            return (lo >= mid || current != null) ? null :
//                new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
//                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int i, hi, mc;
//            if (action == null)
//                throw new NullPointerException();
//            ArrayBucketHashMap<K,V> m = map;
//            Node<K,V>[] tab = m.table;
//            if ((hi = fence) < 0) {
//                mc = expectedModCount = m.modCount;
//                hi = fence = (tab == null) ? 0 : tab.length;
//            }
//            else
//                mc = expectedModCount;
//            if (tab != null && tab.length >= hi &&
//                (i = index) >= 0 && (i < (index = hi) || current != null)) {
//                Node<K,V> p = current;
//                current = null;
//                do {
//                    if (p == null)
//                        p = tab[i++];
//                    else {
//                        action.accept(p.value);
//                        p = p.next;
//                    }
//                } while (p != null || i < hi);
//                if (m.modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int hi;
//            if (action == null)
//                throw new NullPointerException();
//            Node<K,V>[] tab = map.table;
//            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
//                while (current != null || index < hi) {
//                    if (current == null)
//                        current = tab[index++];
//                    else {
//                        V v = current.value;
//                        current = current.next;
//                        action.accept(v);
//                        if (map.modCount != expectedModCount)
//                            throw new ConcurrentModificationException();
//                        return true;
//                    }
//                }
//            }
//            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<Entry<K,V>> {
        EntrySpliterator(ArrayBinHashMapJustPutGet<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if put & get jmh tests have promising results
//            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
//            return (lo >= mid || current != null) ? null :
//                new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
//                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Entry<K,V>> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int i, hi, mc;
//            if (action == null)
//                throw new NullPointerException();
//            ArrayBucketHashMap<K,V> m = map;
//            Node<K,V>[] tab = m.table;
//            if ((hi = fence) < 0) {
//                mc = expectedModCount = m.modCount;
//                hi = fence = (tab == null) ? 0 : tab.length;
//            }
//            else
//                mc = expectedModCount;
//            if (tab != null && tab.length >= hi &&
//                (i = index) >= 0 && (i < (index = hi) || current != null)) {
//                Node<K,V> p = current;
//                current = null;
//                do {
//                    if (p == null)
//                        p = tab[i++];
//                    else {
//                        action.accept(p);
//                        p = p.next;
//                    }
//                } while (p != null || i < hi);
//                if (m.modCount != mc)
//                    throw new ConcurrentModificationException();
//            }
        }

        public boolean tryAdvance(Consumer<? super Entry<K,V>> action) {
            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            int hi;
//            if (action == null)
//                throw new NullPointerException();
//            Node<K,V>[] tab = map.table;
//            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
//                while (current != null || index < hi) {
//                    if (current == null)
//                        current = tab[index++];
//                    else {
//                        Node<K,V> e = current;
//                        current = current.next;
//                        action.accept(e);
//                        if (map.modCount != expectedModCount)
//                            throw new ConcurrentModificationException();
//                        return true;
//                    }
//                }
//            }
//            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMapCpy support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMapCpy, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMapCpy, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node
    Node<K,V> newNode(int hash, K key, V value) {
        return new Node<>(hash, key, value);
    }

    // For conversion from TreeNodes to plain nodes
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
// TODO uncomment if jmh get/put is promising
//    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
//        return new TreeNode<>(hash, key, value, next);
//    }
//
//    // For treeifyBin
//    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
//        return new TreeNode<>(p.hash, p.key, p.value, next);
//    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMapCpy post-actions
// TODO remove these?
    void afterNodeAccess(Node<K,V> p) { }
//    void afterNodeInsertion(boolean evict) { }
//    void afterNodeRemoval(Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(ObjectOutputStream s) throws IOException {
        throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//        Node<K,V>[] tab;
//        if (size > 0 && (tab = table) != null) {
//            for (Node<K,V> e : tab) {
//                for (; e != null; e = e.next) {
//                    s.writeObject(e.key);
//                    s.writeObject(e.value);
//                }
//            }
//        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * Entry for Tree bins. Extends LinkedHashMapCpy.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
//    static final class TreeNode<K,V> extends LinkedHashMapCpy.Entry<K,V> {
//        TreeNode<K,V> parent;  // red-black tree links
//        TreeNode<K,V> left;
//        TreeNode<K,V> right;
//        TreeNode<K,V> prev;    // needed to unlink next upon deletion
//        boolean red;
//        TreeNode(int hash, K key, V val, Node<K,V> next) {
//            throw new UnsupportedOperationException("Bad Thing!");// TODO uncomment if jmh get/put is promising
//            super(hash, key, val, next);
//        }
//
//        /**
//         * Returns root of tree containing this node.
//         */
//        final TreeNode<K,V> root() {
//            for (TreeNode<K,V> r = this, p;;) {
//                if ((p = r.parent) == null)
//                    return r;
//                r = p;
//            }
//        }
//
//        /**
//         * Ensures that the given root is the first node of its bin.
//         */
//        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
//            int n;
//            if (root != null && tab != null && (n = tab.length) > 0) {
//                int index = (n - 1) & root.hash;
//                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
//                if (root != first) {
//                    Node<K,V> rn;
//                    tab[index] = root;
//                    TreeNode<K,V> rp = root.prev;
//                    if ((rn = root.next) != null)
//                        ((TreeNode<K,V>)rn).prev = rp;
//                    if (rp != null)
//                        rp.next = rn;
//                    if (first != null)
//                        first.prev = root;
//                    root.next = first;
//                    root.prev = null;
//                }
//                assert checkInvariants(root);
//            }
//        }
//
//        /**
//         * Finds the node starting at root p with the given hash and key.
//         * The kc argument caches comparableClassFor(key) upon first use
//         * comparing keys.
//         */
//        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
//            TreeNode<K,V> p = this;
//            do {
//                int ph, dir; K pk;
//                TreeNode<K,V> pl = p.left, pr = p.right, q;
//                if ((ph = p.hash) > h)
//                    p = pl;
//                else if (ph < h)
//                    p = pr;
//                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
//                    return p;
//                else if (pl == null)
//                    p = pr;
//                else if (pr == null)
//                    p = pl;
//                else if ((kc != null ||
//                          (kc = comparableClassFor(k)) != null) &&
//                         (dir = compareComparables(kc, k, pk)) != 0)
//                    p = (dir < 0) ? pl : pr;
//                else if ((q = pr.find(h, k, kc)) != null)
//                    return q;
//                else
//                    p = pl;
//            } while (p != null);
//            return null;
//        }
//
//        /**
//         * Calls find for root node.
//         */
//        final TreeNode<K,V> getTreeNode(int h, Object k) {
//            return ((parent != null) ? root() : this).find(h, k, null);
//        }
//
//        /**
//         * Tie-breaking utility for ordering insertions when equal
//         * hashCodes and non-comparable. We don't require a total
//         * order, just a consistent insertion rule to maintain
//         * equivalence across rebalancings. Tie-breaking further than
//         * necessary simplifies testing a bit.
//         */
//        static int tieBreakOrder(Object a, Object b) {
//            int d;
//            if (a == null || b == null ||
//                (d = a.getClass().getName().
//                 compareTo(b.getClass().getName())) == 0)
//                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
//                     -1 : 1);
//            return d;
//        }
//
//        /**
//         * Forms tree of the nodes linked from this node.
//         */
//        final void treeify(Node<K,V>[] tab) {
//            TreeNode<K,V> root = null;
//            for (TreeNode<K,V> x = this, next; x != null; x = next) {
//                next = (TreeNode<K,V>)x.next;
//                x.left = x.right = null;
//                if (root == null) {
//                    x.parent = null;
//                    x.red = false;
//                    root = x;
//                }
//                else {
//                    K k = x.key;
//                    int h = x.hash;
//                    Class<?> kc = null;
//                    for (TreeNode<K,V> p = root;;) {
//                        int dir, ph;
//                        K pk = p.key;
//                        if ((ph = p.hash) > h)
//                            dir = -1;
//                        else if (ph < h)
//                            dir = 1;
//                        else if ((kc == null &&
//                                  (kc = comparableClassFor(k)) == null) ||
//                                 (dir = compareComparables(kc, k, pk)) == 0)
//                            dir = tieBreakOrder(k, pk);
//
//                        TreeNode<K,V> xp = p;
//                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
//                            x.parent = xp;
//                            if (dir <= 0)
//                                xp.left = x;
//                            else
//                                xp.right = x;
//                            root = balanceInsertion(root, x);
//                            break;
//                        }
//                    }
//                }
//            }
//            moveRootToFront(tab, root);
//        }
//
//        /**
//         * Returns a list of non-TreeNodes replacing those linked from
//         * this node.
//         */
//        final Node<K,V> untreeify(ArrayBucketHashMap<K,V> map) {
//            Node<K,V> hd = null, tl = null;
//            for (Node<K,V> q = this; q != null; q = q.next) {
//                Node<K,V> p = map.replacementNode(q, null);
//                if (tl == null)
//                    hd = p;
//                else
//                    tl.next = p;
//                tl = p;
//            }
//            return hd;
//        }
//
//        /**
//         * Tree version of putVal.
//         */
//        final TreeNode<K,V> putTreeVal(ArrayBucketHashMap<K,V> map, Node<K,V>[] tab,
//                                       int h, K k, V v) {
//            Class<?> kc = null;
//            boolean searched = false;
//            TreeNode<K,V> root = (parent != null) ? root() : this;
//            for (TreeNode<K,V> p = root;;) {
//                int dir, ph; K pk;
//                if ((ph = p.hash) > h)
//                    dir = -1;
//                else if (ph < h)
//                    dir = 1;
//                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
//                    return p;
//                else if ((kc == null &&
//                          (kc = comparableClassFor(k)) == null) ||
//                         (dir = compareComparables(kc, k, pk)) == 0) {
//                    if (!searched) {
//                        TreeNode<K,V> q, ch;
//                        searched = true;
//                        if (((ch = p.left) != null &&
//                             (q = ch.find(h, k, kc)) != null) ||
//                            ((ch = p.right) != null &&
//                             (q = ch.find(h, k, kc)) != null))
//                            return q;
//                    }
//                    dir = tieBreakOrder(k, pk);
//                }
//
//                TreeNode<K,V> xp = p;
//                if ((p = (dir <= 0) ? p.left : p.right) == null) {
//                    Node<K,V> xpn = xp.next;
//                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
//                    if (dir <= 0)
//                        xp.left = x;
//                    else
//                        xp.right = x;
//                    xp.next = x;
//                    x.parent = x.prev = xp;
//                    if (xpn != null)
//                        ((TreeNode<K,V>)xpn).prev = x;
//                    moveRootToFront(tab, balanceInsertion(root, x));
//                    return null;
//                }
//            }
//        }
//
//        /**
//         * Removes the given node, that must be present before this call.
//         * This is messier than typical red-black deletion code because we
//         * cannot swap the contents of an interior node with a leaf
//         * successor that is pinned by "next" pointers that are accessible
//         * independently during traversal. So instead we swap the tree
//         * linkages. If the current tree appears to have too few nodes,
//         * the bin is converted back to a plain bin. (The test triggers
//         * somewhere between 2 and 6 nodes, depending on tree structure).
//         */
//        final void removeTreeNode(ArrayBucketHashMap<K,V> map, Node<K,V>[] tab,
//                                  boolean movable) {
//            int n;
//            if (tab == null || (n = tab.length) == 0)
//                return;
//            int index = (n - 1) & hash;
//            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
//            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
//            if (pred == null)
//                tab[index] = first = succ;
//            else
//                pred.next = succ;
//            if (succ != null)
//                succ.prev = pred;
//            if (first == null)
//                return;
//            if (root.parent != null)
//                root = root.root();
//            if (root == null
//                || (movable
//                    && (root.right == null
//                        || (rl = root.left) == null
//                        || rl.left == null))) {
//                tab[index] = first.untreeify(map);  // too small
//                return;
//            }
//            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
//            if (pl != null && pr != null) {
//                TreeNode<K,V> s = pr, sl;
//                while ((sl = s.left) != null) // find successor
//                    s = sl;
//                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
//                TreeNode<K,V> sr = s.right;
//                TreeNode<K,V> pp = p.parent;
//                if (s == pr) { // p was s's direct parent
//                    p.parent = s;
//                    s.right = p;
//                }
//                else {
//                    TreeNode<K,V> sp = s.parent;
//                    if ((p.parent = sp) != null) {
//                        if (s == sp.left)
//                            sp.left = p;
//                        else
//                            sp.right = p;
//                    }
//                    if ((s.right = pr) != null)
//                        pr.parent = s;
//                }
//                p.left = null;
//                if ((p.right = sr) != null)
//                    sr.parent = p;
//                if ((s.left = pl) != null)
//                    pl.parent = s;
//                if ((s.parent = pp) == null)
//                    root = s;
//                else if (p == pp.left)
//                    pp.left = s;
//                else
//                    pp.right = s;
//                if (sr != null)
//                    replacement = sr;
//                else
//                    replacement = p;
//            }
//            else if (pl != null)
//                replacement = pl;
//            else if (pr != null)
//                replacement = pr;
//            else
//                replacement = p;
//            if (replacement != p) {
//                TreeNode<K,V> pp = replacement.parent = p.parent;
//                if (pp == null)
//                    (root = replacement).red = false;
//                else if (p == pp.left)
//                    pp.left = replacement;
//                else
//                    pp.right = replacement;
//                p.left = p.right = p.parent = null;
//            }
//
//            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);
//
//            if (replacement == p) {  // detach
//                TreeNode<K,V> pp = p.parent;
//                p.parent = null;
//                if (pp != null) {
//                    if (p == pp.left)
//                        pp.left = null;
//                    else if (p == pp.right)
//                        pp.right = null;
//                }
//            }
//            if (movable)
//                moveRootToFront(tab, r);
//        }
//
//        /**
//         * Splits nodes in a tree bin into lower and upper tree bins,
//         * or untreeifies if now too small. Called only from resize;
//         * see above discussion about split bits and indices.
//         *
//         * @param map the map
//         * @param tab the table for recording bin heads
//         * @param index the index of the table being split
//         * @param bit the bit of hash to split on
//         */
//        final void split(ArrayBucketHashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
//            TreeNode<K,V> b = this;
//            // Relink into lo and hi lists, preserving order
//            TreeNode<K,V> loHead = null, loTail = null;
//            TreeNode<K,V> hiHead = null, hiTail = null;
//            int lc = 0, hc = 0;
//            for (TreeNode<K,V> e = b, next; e != null; e = next) {
//                next = (TreeNode<K,V>)e.next;
//                e.next = null;
//                if ((e.hash & bit) == 0) {
//                    if ((e.prev = loTail) == null)
//                        loHead = e;
//                    else
//                        loTail.next = e;
//                    loTail = e;
//                    ++lc;
//                }
//                else {
//                    if ((e.prev = hiTail) == null)
//                        hiHead = e;
//                    else
//                        hiTail.next = e;
//                    hiTail = e;
//                    ++hc;
//                }
//            }
//
//            if (loHead != null) {
//                if (lc <= UNTREEIFY_THRESHOLD)
//                    tab[index] = loHead.untreeify(map);
//                else {
//                    tab[index] = loHead;
//                    if (hiHead != null) // (else is already treeified)
//                        loHead.treeify(tab);
//                }
//            }
//            if (hiHead != null) {
//                if (hc <= UNTREEIFY_THRESHOLD)
//                    tab[index + bit] = hiHead.untreeify(map);
//                else {
//                    tab[index + bit] = hiHead;
//                    if (loHead != null)
//                        hiHead.treeify(tab);
//                }
//            }
//        }
//
//        /* ------------------------------------------------------------ */
//        // Red-black tree methods, all adapted from CLR
//
//        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
//                                              TreeNode<K,V> p) {
//            TreeNode<K,V> r, pp, rl;
//            if (p != null && (r = p.right) != null) {
//                if ((rl = p.right = r.left) != null)
//                    rl.parent = p;
//                if ((pp = r.parent = p.parent) == null)
//                    (root = r).red = false;
//                else if (pp.left == p)
//                    pp.left = r;
//                else
//                    pp.right = r;
//                r.left = p;
//                p.parent = r;
//            }
//            return root;
//        }
//
//        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
//                                               TreeNode<K,V> p) {
//            TreeNode<K,V> l, pp, lr;
//            if (p != null && (l = p.left) != null) {
//                if ((lr = p.left = l.right) != null)
//                    lr.parent = p;
//                if ((pp = l.parent = p.parent) == null)
//                    (root = l).red = false;
//                else if (pp.right == p)
//                    pp.right = l;
//                else
//                    pp.left = l;
//                l.right = p;
//                p.parent = l;
//            }
//            return root;
//        }
//
//        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
//                                                    TreeNode<K,V> x) {
//            x.red = true;
//            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
//                if ((xp = x.parent) == null) {
//                    x.red = false;
//                    return x;
//                }
//                else if (!xp.red || (xpp = xp.parent) == null)
//                    return root;
//                if (xp == (xppl = xpp.left)) {
//                    if ((xppr = xpp.right) != null && xppr.red) {
//                        xppr.red = false;
//                        xp.red = false;
//                        xpp.red = true;
//                        x = xpp;
//                    }
//                    else {
//                        if (x == xp.right) {
//                            root = rotateLeft(root, x = xp);
//                            xpp = (xp = x.parent) == null ? null : xp.parent;
//                        }
//                        if (xp != null) {
//                            xp.red = false;
//                            if (xpp != null) {
//                                xpp.red = true;
//                                root = rotateRight(root, xpp);
//                            }
//                        }
//                    }
//                }
//                else {
//                    if (xppl != null && xppl.red) {
//                        xppl.red = false;
//                        xp.red = false;
//                        xpp.red = true;
//                        x = xpp;
//                    }
//                    else {
//                        if (x == xp.left) {
//                            root = rotateRight(root, x = xp);
//                            xpp = (xp = x.parent) == null ? null : xp.parent;
//                        }
//                        if (xp != null) {
//                            xp.red = false;
//                            if (xpp != null) {
//                                xpp.red = true;
//                                root = rotateLeft(root, xpp);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
//                                                   TreeNode<K,V> x) {
//            for (TreeNode<K,V> xp, xpl, xpr;;) {
//                if (x == null || x == root)
//                    return root;
//                else if ((xp = x.parent) == null) {
//                    x.red = false;
//                    return x;
//                }
//                else if (x.red) {
//                    x.red = false;
//                    return root;
//                }
//                else if ((xpl = xp.left) == x) {
//                    if ((xpr = xp.right) != null && xpr.red) {
//                        xpr.red = false;
//                        xp.red = true;
//                        root = rotateLeft(root, xp);
//                        xpr = (xp = x.parent) == null ? null : xp.right;
//                    }
//                    if (xpr == null)
//                        x = xp;
//                    else {
//                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
//                        if ((sr == null || !sr.red) &&
//                            (sl == null || !sl.red)) {
//                            xpr.red = true;
//                            x = xp;
//                        }
//                        else {
//                            if (sr == null || !sr.red) {
//                                if (sl != null)
//                                    sl.red = false;
//                                xpr.red = true;
//                                root = rotateRight(root, xpr);
//                                xpr = (xp = x.parent) == null ?
//                                    null : xp.right;
//                            }
//                            if (xpr != null) {
//                                xpr.red = (xp == null) ? false : xp.red;
//                                if ((sr = xpr.right) != null)
//                                    sr.red = false;
//                            }
//                            if (xp != null) {
//                                xp.red = false;
//                                root = rotateLeft(root, xp);
//                            }
//                            x = root;
//                        }
//                    }
//                }
//                else { // symmetric
//                    if (xpl != null && xpl.red) {
//                        xpl.red = false;
//                        xp.red = true;
//                        root = rotateRight(root, xp);
//                        xpl = (xp = x.parent) == null ? null : xp.left;
//                    }
//                    if (xpl == null)
//                        x = xp;
//                    else {
//                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
//                        if ((sl == null || !sl.red) &&
//                            (sr == null || !sr.red)) {
//                            xpl.red = true;
//                            x = xp;
//                        }
//                        else {
//                            if (sl == null || !sl.red) {
//                                if (sr != null)
//                                    sr.red = false;
//                                xpl.red = true;
//                                root = rotateLeft(root, xpl);
//                                xpl = (xp = x.parent) == null ?
//                                    null : xp.left;
//                            }
//                            if (xpl != null) {
//                                xpl.red = (xp == null) ? false : xp.red;
//                                if ((sl = xpl.left) != null)
//                                    sl.red = false;
//                            }
//                            if (xp != null) {
//                                xp.red = false;
//                                root = rotateRight(root, xp);
//                            }
//                            x = root;
//                        }
//                    }
//                }
//            }
//        }
//
//        /**
//         * Recursive invariant check
//         */
//        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
//            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
//                tb = t.prev, tn = (TreeNode<K,V>)t.next;
//            if (tb != null && tb.next != t)
//                return false;
//            if (tn != null && tn.prev != t)
//                return false;
//            if (tp != null && t != tp.left && t != tp.right)
//                return false;
//            if (tl != null && (tl.parent != t || tl.hash > t.hash))
//                return false;
//            if (tr != null && (tr.parent != t || tr.hash < t.hash))
//                return false;
//            if (t.red && tl != null && tl.red && tr != null && tr.red)
//                return false;
//            if (tl != null && !checkInvariants(tl))
//                return false;
//            if (tr != null && !checkInvariants(tr))
//                return false;
//            return true;
//        }
//    }

    /**
     * Calculate initial capacity for HashMap based classes, from expected size and default load factor (0.75).
     *
     * @param numMappings the expected number of mappings
     * @return initial capacity for HashMap based classes.
     * @since 19
     */
    static int calculateHashMapCpyCapacity(int numMappings) {
        return (int) Math.ceil(numMappings / (double) DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new, empty HashMap suitable for the expected number of mappings.
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
    public static <K, V> ArrayBinHashMapJustPutGet<K, V> newHashMap(int numMappings) {
        if (numMappings < 0) {
            throw new IllegalArgumentException("Negative number of mappings: " + numMappings);
        }
        return new ArrayBinHashMapJustPutGet<>(calculateHashMapCpyCapacity(numMappings));
    }

    public void dumpStats(PrintStream out) {
        out.printf("%s instance: size: %d%n", this.getClass().getName(), this.size());
        long size = heapSize();
        long bytesPer = size / this.size();
        out.printf("    heap size: %d(bytes), avg bytes per entry: %d, table len: %d%n",
                size, bytesPer, table.length);
        long[] types = entryTypes();
        out.printf("    values: %d, empty: %d%n", types[1], types[0]);
        int[] rehashes = entryRehashes();
        out.printf("    hash collision histogram: max: %d, %s%n",
                rehashes.length - 1, Arrays.toString(rehashes));
    }

    private long[] entryTypes() {
        long[] counts = new long[3];
        for (Object te : table) {
            counts[te!=null ? 1 : 0]++; // TODO changed but not verified
        }
        return counts;
    }

    // Returns a histogram array of the number of rehashs needed to find each key.
    private int[] entryRehashes() {
        int[] counts = new int[size];
        Node<K,V>[][] tab = table;
        for (Node<K,V>[] bin : tab) {
            if (bin == null) { // TODO changed but not verified
                counts[0]++;
            } else {
                counts[bin.length]++;
            }
        }

        int i;
        for (i = counts.length - 1; i >= 0 && counts[i] == 0; i--) {
        }
        counts = Arrays.copyOf(counts, i + 1);
        return counts;
    }

    private long heapSize() {
        long acc = objectSizeMaybe(this);
        acc += objectSizeMaybe(table);

        Node<K,V>[][] tab = table;
        for (Node<K,V>[] bin : tab) {
            // TODO doesn't handle TreeNodes
            if (bin != null)
                for(Node<K,V> node : bin) {
                    acc += objectSizeMaybe(node);
                }
        }
        return acc;
    }

    private static Method mObjectSize = getObjectSizeMethod();

    private static Method getObjectSizeMethod() {
        try {
            Method m = Objects.class.getDeclaredMethod("getObjectSize", Object.class);
            return m;
        } catch (NoSuchMethodException nsme) {
            return null;
        }
    }

    private long objectSizeMaybe(Object o) {
        try {
            return (mObjectSize != null)
                    ? (long)mObjectSize.invoke(null, o)
                    : 0L;
        } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            return 0L;
        }
    }

    private static class NodeRef<K,V> {
        Node<K,V> node; // TODO not sure if this is the best way to do this.  For Value Objects it is possible to have a reference, so maybe SingleNode would just be a reference to Node, but I'm not sure that's what "reference" means for value Objects

        NodeRef(int hash, K key, V value) {
            node = new Node<>(hash, key, value);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        NodeRef(Node node) {
            this.node = node;
        }

    }
}

