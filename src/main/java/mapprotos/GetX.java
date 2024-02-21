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
import java.util.Map;
import java.util.function.IntFunction;

public class GetX extends MapBase {

    IntFunction<Map<Integer, Integer>> mapSupplier;
    Map<Integer, Integer> map;
    Integer[] mixed;

    @Setup(Level.Iteration)
    public void setupIteration() {
        super.initIteration(size);
        float loadFactor = 8.0f;
        int origSize = (int) (size / loadFactor) + 1;
        try {
            Class<?> mapClass = Class.forName(mapType);
            System.out.println("loadFactor:" + loadFactor + " origSize:" + origSize);
            mapSupplier = (s) -> newInstance(mapClass, s, loadFactor);
        } catch (Exception ex) {
            System.out.printf("%s: %s%n", mapType, ex.getMessage());
            return;
        }

        map = mapSupplier.apply(origSize);
        for (Integer k : keys) {
            map.put(k, k);
        }

        mixed = new Integer[size];
        System.arraycopy(keys, 0, mixed, 0, size / 2);
        System.arraycopy(nonKeys, 0, mixed, size / 2, size / 2);
        Collections.shuffle(Arrays.asList(mixed), rnd);
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        super.TearDown(map);
    }

    Map<Integer, Integer> newInstance(Class<?> mapClass, int size, float loadFactor) {
        try {
            return (Map<Integer, Integer>)mapClass.getConstructor(int.class, float.class).newInstance(size, loadFactor);
        } catch (Exception ex) {
            throw new RuntimeException("failed", ex);
        }
    }

    @Benchmark
    public void getHit(Blackhole bh) {
        Integer[] keys = this.keys;
        Map<Integer, Integer> map = this.map;
        for (Integer k : keys) {
            bh.consume(map.get(k));
        }
    }

    @Benchmark
    public void getMix(Blackhole bh) {
        Integer[] keys = this.mixed;
        Map<Integer, Integer> map = this.map;
        for (Integer k : keys) {
            bh.consume(map.get(k));
        }
    }

}
