/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(value = 1, jvmArgs = {"-XX:+EnablePrimitiveClasses", /*TODO comment out "-Xms24g", "-Xmx24g", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"*/})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations=5)
@Measurement(iterations=10)
@State(Scope.Thread)
public class MapBase {

    @Param({
//              "11",
//             "767",
//        "1536", // Divisible by 3 fields and 4 bytes per field, per table Object overhead should be negligible. Half way to next resize.
//        "50331600", // the one just below this one seems like capacity is too big.  So try a little less than halfway to see the size & performance consequences
//        "50331648", // this is halfway between two powers of 2
        "131072",
//         "1000000"
//     "100663296", //Used this to fill cpu caches with other data to get() would need to fetch from memory.
//     "201326592", // Didn't use: Too much memory
    })
    public int size;

    // All trials have the same seed for each iteration.  However, each iteration has a different
    // seed than its other iterations.  In this way it is hoped that each trial is more
    // realistically warmed up with a variety of data and also that each trial has the same set
    // of seeds to make fair comparisons at least a bit better.
    private int seed;

    @Param(value = {
//        "newhash.OpenHashMap",
        "mapprotos.ArrayBinHashMapJustPutGet",
        "mapprotos.HashMapJustPutGet",
//        "mapprotos.ArrayBinLessIndexHashMap",
//            "mapprotos.XHashMap",
//            "org.openjdk.bench.valhalla.corelibs.mapprotos.HashMap",
//            "org.openjdk.bench.valhalla.corelibs.mapprotos.XHashMap",
//            "java.util.HashMap0",
    })
    public String mapType;

    public Random rnd;
    public Integer[] keys;
    public Integer[] nonKeys;

    public void initIteration(int size) {
        System.out.println("CALLING MapBase.initIteration seed:" + seed);
        Integer[] all;
        rnd = new Random(seed++);
        all = rnd.ints().distinct().limit(size * 2).boxed().toArray(Integer[]::new);
        Collections.shuffle(Arrays.asList(all), rnd);
        keys = Arrays.copyOfRange(all, 0, size);
        nonKeys = Arrays.copyOfRange(all, size, size * 2);
    }

    void TearDown(Map<Integer, Integer> map) {
        try {
            Method m = map.getClass().getMethod("dumpStats", java.io.PrintStream.class);
            m.invoke(map, System.out);
        } catch (Throwable nsme) {
            System.out.println("Stats not available:");
            throw new IllegalStateException(nsme);
        }
    }
}
