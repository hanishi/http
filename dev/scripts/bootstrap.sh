#!/usr/bin/env bash

TIME_ZONE_FILE=/usr/share/zoneinfo/Asia/Tokyo

printLog() {
  printf "[$1-$0] $1\n";
}

install_wget() {
  value=$(rpm -qa | grep -c ^wget)
  if [ $value -eq 0 ]; then
    printLog "Installing wget";
    sudo yum --quiet -y install wget
  fi
}

install_adoptopenjdk_11_hotspot() {
  value=$(rpm -qa | grep -c ^java)
  if [ $value -eq 0 ]; then
    printLog "Installing adoptopenjdk-11-hotspot-11";
    sudo yum --quiet -y install adoptopenjdk-11-hotspot-11.0.3+7-1.$(uname -m)
    echo 'export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-11-hotspot' | sudo tee -a /etc/profile.d/java.sh
    echo 'export JRE_HOME=/usr/lib/jvm/adoptopenjdk-11-hotspot' | sudo tee -a /etc/profile.d/java.sh
    source /etc/profile
  fi
}

install_kafka() {
  FILE=/home/vagrant/kafka
  if [[ ! -f ${FILE} ]]; then
    printLog "Installing Apache Kafka";
    sudo wget -q https://archive.apache.org/dist/kafka/2.1.1/kafka_2.11-2.1.1.tgz
    sudo mkdir -p /home/vagrant/kafka && sudo tar xvf kafka_2.11-2.1.1.tgz -C /home/vagrant/kafka --strip-components=1
    rm -rf kafka_2.11-2.1.1.tgz
  fi
}

install_mariadb() {
  value=$(rpm -qa | grep -c ^MariaDB)
  if [ $value -eq 0 ]; then
    printLog "Installing MariaDB-client MariaDB-server";
    yum --quiet -y install MariaDB-client MariaDB-server
    systemctl enable mariadb.service
    systemctl start mariadb.service
    printLog "Provisioning database";
    mysql -u root -e "CREATE USER 'vagrant'@'localhost' IDENTIFIED BY 'vagrant';"
    mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO vagrant IDENTIFIED BY 'vagrant' WITH GRANT OPTION; FLUSH PRIVILEGES;"
    sudo systemctl restart mariadb.service
  fi
}

node_ip=$1

sudo tee "/vagrant/scripts/common.sh" > /dev/null <<EOF
#!/usr/bin/env bash

node_ip=$node_ip
zk_port=2181
kafka_port=9092

zk=\${node_ip}:\$zk_port
broker=\${node_ip}:\$kafka_port

kafka_home=/home/vagrant/kafka
spark_home=/home/vagrant/spark
EOF

source "/vagrant/scripts/common.sh"

sudo tee "/etc/yum.repos.d/MariaDB.repo" > /dev/null <<EOF
[mariadb]
name = MariaDB
baseurl = http://yum.mariadb.org/10.1/centos7-amd64
gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
gpgcheck=0
EOF

sudo tee "/etc/yum.repos.d/adoptopenjdk.repo" > /dev/null <<EOF
[AdoptOpenJDK]
name=AdoptOpenJDK
baseurl=http://adoptopenjdk.jfrog.io/adoptopenjdk/rpm/centos/7/$(uname -m)
enabled=1
gpgcheck=1
gpgkey=https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public
EOF

install_wget
install_adoptopenjdk_11_hotspot
install_kafka
install_mariadb

chown vagrant:vagrant -R /home/vagrant/
chmod u+x /vagrant/scripts/*.sh