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

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Objects;
//import jdk.internal.access.SharedSecrets;

public class ArrayOfArraysOfPointer implements ArrayOfNodeLists {

    static class Node {
        final int intKey;
        final Object objKey;
        final Object value;

        Node(int intKey, Object objKey, Object value) {
            this.intKey = intKey;
            this.objKey = objKey;
            this.value = value;
        }
    }

    transient Object[][] table;




    /* ---------------- Public operations -------------- */


    public ArrayOfArraysOfPointer(int tableLen, int binLen) {
        table = new Node[tableLen][];
        for (int b = 0; b < table.length; b++) {
            table[b] = new Node[binLen];
        }
    }

    @Override
    public void addNode(int tableIndex, int binIndex, int intKey, Object objKey, Object value) { // TODO binIndex should be in decreasing order so that bin order is same as linked list
        table[tableIndex][binIndex] = new Node(intKey, objKey, value);
    }

    @Override
    public Object get(int intTableIndex, int intKey, Object objKey) {
        // not calling getNode and wrapping the Node in a NodeRef saves a huge amount of time.
        @SuppressWarnings("unchecked")
        ArrayOfArraysOfPointer.Node[] nodes = (ArrayOfArraysOfPointer.Node[]) table[intTableIndex];
        for (int i = 0; i < nodes.length; i++) {
            final Node node = nodes[i];
            Object k;
            if (node.intKey == intKey &&
                ((k = node.objKey) == objKey || (k != null && k.equals(objKey)))) {
                return node.value;
            }
        }
        return null;
    }

    public void dumpStats(PrintStream out) {
        long numElements = ((long) this.table.length * this.table[0].length);
        out.printf("%s instance: numElements: %d%n", this.getClass().getName(), numElements);
        long heapSize = heapSize();
        long bytesPer = heapSize / numElements;
        out.printf("    heap heapSize: %d(bytes), avg bytes per entry: %d, table len: %d%n",
            heapSize, bytesPer, table.length);
        long[] types = entryTypes();
        out.printf("    values: %d, empty: %d%n", types[0], types[1]);
    }

    private long[] entryTypes() {
        long[] counts = new long[3];
        for (Object te : table) {
            counts[te!=null ? 1 : 0]++; // TODO changed but not verified
        }
        return counts;
    }

    private long heapSize() {
        long acc = objectSizeMaybe(this);
        acc += objectSizeMaybe(table);

        Object[] tab = table;
        for (Object bin : tab) {
            if (bin != null) {
                acc += objectSizeMaybe(bin);
            }
        }
        return acc;
    }

    private static final Method mObjectSize = getObjectSizeMethod();

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
}
