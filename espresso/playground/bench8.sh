#!/bin/bash

SOURCE=$1
COUNT=$2
ADDITIONAL_ARGS="${*:3}"

NOW=`date +"%s"`
FILENAME="result_bench_java8_$NOW.txt"
JH="/usr/lib/jvm/openjdk8-jvmci-21.1-b03"


for i in $(seq $COUNT); do
    echo "run $i..."
    LD_DEBUG=unused \
    "$JH/bin/javac" "$SOURCE.java" && \
    /home/ivan/graal/sdk/latest_graalvm_home/bin/java -truffle \
     --log.level=FINE \
     --java.NativeBackend=nfi-native \
     --experimental-options \
     "$ADDITIONAL_ARGS" "$SOURCE" \
    >> "$FILENAME" 2>&1 # | tee "$FILENAME"
done



