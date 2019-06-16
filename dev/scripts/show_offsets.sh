#!/usr/bin/env bash

source $(dirname $0)/common.sh

if [ $# -eq 2 ]; then
   $kafka_home/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list $broker --topic $1 --time $2 
else
   echo "Usage: "$(basename $0)" <topic> <time>"
fi
