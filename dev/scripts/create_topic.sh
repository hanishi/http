#!/usr/bin/env bash

source $(dirname $0)/common.sh

if [ $# -eq 2 ]; then
   $kafka_home/bin/kafka-topics.sh --zookeeper $zk --replication-factor 1 --topic $1 --partition $2 --create
else
    echo "Usage: "$(basename $0)" <topic> <partition>"
fi
