#!/usr/bin/env bash

node_ip=192.168.0.11
zk_port=2181
kafka_port=9092

zk=${node_ip}:$zk_port
broker=${node_ip}:$kafka_port

kafka_home=/home/vagrant/kafka
spark_home=/home/vagrant/spark
