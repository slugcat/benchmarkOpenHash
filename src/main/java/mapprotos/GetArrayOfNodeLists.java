/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiFunction;

public class GetArrayOfNodeLists extends ArrayOfNodeListsBase {
    BiFunction<Integer, Integer, ArrayOfNodeLists> arrayOfNodeListsSupplier;
    ArrayOfNodeLists nodeLists;
    Integer[] mixed;
    int numLists;
    int numNodesPerList;

    /* TODO remove these notes
       add jmh parameters for numLists, numNodesPerList
       setupIteration:
         make keys where number is numLists*numNodesPerList
         init cursor for keys

       test:
         current row is something like currKeyIndex / numNodesPerList
         current getterKey is something like currKeyIndex + numNodesPerList - 1
         fetch getterKey
     */

    @Setup(Level.Iteration)
    public void setupIteration() {
        final String[] split = numListsAndNodesPerList.split(":");
        assert split.length == 2;
        numLists = Integer.parseInt(split[0]);
        numNodesPerList = Integer.parseInt(split[1]);
        super.initIteration(numLists * numNodesPerList);
        try {
            Class<?> arrayOfNodeListsClass = Class.forName(arrayOfNodeListsType);
            System.out.println(" numLists:" + numLists + " numNodesPerList" + numNodesPerList);
            arrayOfNodeListsSupplier = (Integer l,Integer n) -> newInstance(arrayOfNodeListsClass, l, n);
        } catch (Exception ex) {
            System.out.printf("%s: %s%n", arrayOfNodeListsType, ex.getMessage());
            return;
        }

        nodeLists = arrayOfNodeListsSupplier.apply(numLists, numNodesPerList);
        for (int l = 0; l < numLists; l++) {
            // Backwards since linked list will be backwards
            for (int n = numNodesPerList-1; n >= 0; n--) {
                final Integer key = keys[l * numNodesPerList + n];
                nodeLists.addNode(l, n, key.hashCode(), key, key);
            }
        }
        Collections.shuffle(Arrays.asList(keys), rnd);

//        mixed = new Integer[size];
//        System.arraycopy(keys, 0, mixed, 0, size / 2);
//        System.arraycopy(nonKeys, 0, mixed, size / 2, size / 2);
//        Collections.shuffle(Arrays.asList(mixed), rnd);
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        super.TearDown(nodeLists);
    }

    ArrayOfNodeLists newInstance(Class<?> arrayOfNodeLists, int numLists, int numNodesPerList) {
        try {
            return (ArrayOfNodeLists) arrayOfNodeLists.getConstructor(int.class, int.class).newInstance(numLists, numNodesPerList);
        } catch (Exception ex) {
            throw new RuntimeException("failed", ex);
        }
    }

    @Benchmark
    public void getHit(Blackhole bh) {
        Integer[] keys = this.keys;
        ArrayOfNodeLists nodeLists = this.nodeLists;
        // The first key added to a row will be the last key in the row since keys are added to the beginning of a row
        // TODO debug this^ to make sure it is correct.
        for (int l = 0; l < numLists; l++) {
            for (int n = 0; n < numNodesPerList; n++) {
                final Integer key = keys[l*numNodesPerList + n];
                bh.consume(nodeLists.get(l, key, key));
            }
        }
    }

//    @Benchmark
//    public void getMix(Blackhole bh) {
//        Integer[] keys = this.mixed;
//        ArrayOfNodeLists map = this.nodeLists;
//        for (Integer k : keys) {
//            bh.consume(map.get(k));
//        }
//    }

}
