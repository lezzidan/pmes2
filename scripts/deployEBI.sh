#!/bin/bash

echo "Copying pmes.war to EBI..."
scp -i $1 ../trunk/target/pmes.war ubuntu@193.62.52.104:
ssh -i $1 -l ubuntu 193.62.52.104 scp -i lcodo.pem pmes.war pmes@192.168.0.27:

#TODO: redeploy pmes.war 
#ssh -i $1 -l ubuntu 193.62.52.104 ssh -i lcodo.pem pmes@192.168.0.27 /home/pmes/pmes/scripts/redeploy.sh

