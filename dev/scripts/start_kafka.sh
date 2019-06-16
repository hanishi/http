#!/usr/bin/env bash

source $(dirname $0)/common.sh

export KAFKA_HEAP_OPTS="-Xmx256m -Xms256m"

$kafka_home/bin/kafka-server-start.sh -daemon $kafka_home/config/server.properties \
      --override zookeeper.connect=$zk --override listeners=PLAINTEXT://0.0.0.0:9092 --override advertised.listeners=PLAINTEXT://127.0.0.1:9092
