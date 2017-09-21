#!/bin/bash

echo "Copying pmes.war to bsccv02..."
scp ../trunk/target/pmes.war bsccv02:

echo "Copying pmes.war to 192.168.122.114..."
ssh bsccv02 scp pmes.war root@192.168.122.114:
ssh bsccv02 ssh root@192.168.122.114 chown pmes:pmes pmes.war
ssh bsccv02 ssh root@192.168.122.114 mv pmes.war /home/pmes/

echo "Restarting tomcat..."
ssh bsccv02 ssh root@192.168.122.114 /home/pmes/pmes/scripts/redeploy.sh
