#!/usr/bin/env bash

source $(dirname $0)/common.sh
# This will start 1 instance of zookeeper for development environment.
# create myid file. 
if [ ! -d /tmp/zookeeper/zoo ]; then
  echo creating zookeeper data dir...
  mkdir -p /tmp/zookeeper/zoo
  echo 1 > /tmp/zookeeper/zoo/myid
fi
zk_tmp=/tmp/zookeeper/zoo.cfg
cat > $zk_tmp <<- EOM
tickTime=2000
dataDir=/tmp/zookeeper/zoo
clientPort=2181
initLimit=5
syncLimit=2
dataDir=/tmp/zookeeper/zoo
server.1=${node_ip}:2888:3888
EOM
# echo starting zookeeper
$kafka_home/bin/zookeeper-server-start.sh $zk_tmp > /tmp/zookeeper/zoo/zookeeper.log &
