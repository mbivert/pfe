#!/bin/sh
#Compile protobuf message to Java and put the files in the sources folder
cd src/main/proto
DIRS="entropy/configuration/parser/ entropy/plan/parser/ entropy/vjob/builder/protobuf/"
for d in $DIRS; do
    echo $d
    protoc --java_out=../java/ -I. $d/*.proto
done
cd -