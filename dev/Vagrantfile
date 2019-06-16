# -*- mode: ruby -*-
# vi: set ft=ruby :

# RequireYAML module
require 'yaml'

# Read YAML file with box details
servers = YAML.load_file('servers.yml')

# create boxes

Vagrant.configure("2") do |config|
  servers.each do |servers|
    config.vm.define servers["name"] do |server|
      server.vm.box = servers["box"]
      server.vm.hostname = servers["name"]
      server.vm.network "private_network", ip: servers["ip"], netmask: servers["netmask"]
      server.vm.synced_folder ".", "/vagrant", type: "nfs"
      if servers["forwarded_ports"] != nil
          servers["forwarded_ports"].each_slice(2){ |ports| server.vm.network "forwarded_port", guest: ports[0], host: ports[1], auto_correct: true }
      end
      server.vm.provision :shell, path: servers["provision"], args: [servers["ip"], servers["name"]]
      server.vm.provider :virtualbox do |vb|
        vb.memory = servers["memory"]
        vb.cpus = servers["cpus"]
      end
    end
  end
  config.vm.synced_folder ".", "/vagrant", type: "virtualbox"
end