
# To debug a test uncomment the debug args in the @Fork in ArrayOfNodeListsBase.java

# Just GetX NO Primitive classes
mvn clean && mvn package && $JAVA_HOME/bin/java -jar /home/d/repo/hash/benchmark/target/benchmarks.jar GetX.* |& (d=$(date "+%m%d-%H:%M"); tee /d/tmp/benchmark.$(date "+%m%d-%H:%M"); echo $d)

# Just GetX WITH Primitive classes
#mvn clean && mvn package && $JAVA_HOME/bin/java -XX:+EnablePrimitiveClasses -jar /home/d/repo/hash/benchmark/target/benchmarks.jar GetX.* |& (d=$(date "+%m%d-%H:%M"); tee /d/tmp/benchmark.$(date "+%m%d-%H:%M"); echo $d)

# Suppresses nothing
#mvn clean && mvn package && $JAVA_HOME/bin/java -XX:+EnablePrimitiveClasses -jar /home/d/repo/hash/benchmark/target/benchmarks.jar |& (d=$(date "+%m%d-%H:%M"); tee /d/tmp/benchmark.$(date "+%m%d-%H:%M"); echo $d)

# Suppresses all but array-of-lists tests
#mvn clean && mvn package && $JAVA_HOME/bin/java -XX:+EnablePrimitiveClasses -jar /home/d/repo/hash/benchmark/target/benchmarks.jar -e mapprotos.HashMapBench.put -e mapprotos.HashMapBench.putAllWithBigMapToEmptyMap -e mapprotos.HashMapBench.putAllWithBigMapToNonEmptyMap -e mapprotos.HashMapToArray.testKeySetToArray -e mapprotos.HashMapToArray.testKeySetToArrayTyped -e mapprotos.HashMapToArray.testValuesToArray -e mapprotos.HashMapToArray.testValuesToArrayTyped -e mapprotos.GetX.get -e mapprotos.PutX.put -e mapprotos.PutX.putSized -e mapprotos.ReplX.replace -e mapprotos.WalkX.sumIterator -e mapprotos.WalkX.sumIteratorHidden -e mapprotos.HashMapJustPutGet.\* |& (d=$(date "+%m%d-%H:%M"); tee /d/tmp/benchmark.$(date "+%m%d-%H:%M"); echo $d)


# Includes jvm bytecode and assembly output, also stops func inlining so can more easily see the assembly lines pertaining the get()
#sudo sh -c 'echo -1 >/proc/sys/kernel/perf_event_paranoid' mvn clean && mvn package && $JAVA_HOME/bin/java -XX:-Inline -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -XX:+EnablePrimitiveClasses -jar /home/d/repo/hash/benchmark/target/benchmarks.jar -e mapprotos.HashMapBench.put -e mapprotos.HashMapBench.putAllWithBigMapToEmptyMap -e mapprotos.HashMapBench.putAllWithBigMapToNonEmptyMap -e mapprotos.HashMapToArray.testKeySetToArray -e mapprotos.HashMapToArray.testKeySetToArrayTyped -e mapprotos.HashMapToArray.testValuesToArray -e mapprotos.HashMapToArray.testValuesToArrayTyped -e mapprotos.GetX.get -e mapprotos.PutX.put -e mapprotos.PutX.putSized -e mapprotos.ReplX.replace -e mapprotos.WalkX.sumIterator -e mapprotos.WalkX.sumIteratorHidden -e mapprotos.HashMapJustPutGet.\* -prof perfasm |& (d=$(date "+%m%d-%H:%M"); tee /d/tmp/benchmark.$(date "+%m%d-%H:%M"); echo $d) ; sudo sh -c 'echo 4 >/proc/sys/kernel/perf_event_paranoid'

