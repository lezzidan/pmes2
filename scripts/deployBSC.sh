#!/usr/bin/expect -f
# This script requires expect

stty -echo
send_user -- "Enter Password: "
expect_user -re "(.*)\n"
send_user "\n"
stty echo
set password $expect_out(1,string)

log_user 0 

# Copy pmes.war to bsccv02
send_user -- "Copying pmes.war to bsccv02...\n"
spawn scp ../trunk/target/pmes.war bsccv02:
expect "Password:"
send "$password\r"
expect {
	"Password:" {
		send_user -- "Wrong password!\n"
		exit
	}	
}

# Copy pmes.war to PMES VM
send_user -- "Copying pmes.war to 192.168.122.114..."
spawn ssh bsccv02 scp pmes.war root@192.168.122.114:
expect "Password:"
send "$password\r"
interact

# Change owner of war file
spawn ssh bsccv02 ssh root@192.168.122.114 chown pmes:pmes pmes.war
expect "Password:"
send "$password\r"
interact

spawn ssh bsccv02 ssh root@192.168.122.114 mv pmes.war /home/pmes/
expect "Password:"
send "$password\r"
interact

# Restart tomcat and replace pmes.war
send_user -- "Restarting tomcat..."
spawn ssh bsccv02 ssh root@192.168.122.114 /home/pmes/pmes/scripts/redeploy.sh
expect "Password:"
send "$password\r"
interact

