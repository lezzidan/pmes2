#!/bin/sh

####################################################################
# SCRIPT FOR SUBMISSION OF APPLICATIONS TO MARENOSTRUM WITH COMPSs #
####################################################################


# Function that converts a cost in minutes to an expression of wall clock limit for slurm
convert_to_wc()
{
        cost=$1
        wc_limit=":00"

        min=`expr $cost % 60`
        if [ $min -lt 10 ]
        then
                wc_limit=":0${min}${wc_limit}"
        else
                wc_limit=":${min}${wc_limit}"
        fi

        hrs=`expr $cost / 60`
        if [ $hrs -gt 0 ]
        then
                if [ $hrs -lt 10 ]
                then
                        wc_limit="0${hrs}${wc_limit}"
                else
                        wc_limit="${hrs}${wc_limit}"
                fi
        else
                wc_limit="00${wc_limit}"
        fi
}


# Parameters: num_procs wc_minutes loader ur_creation app_name app_params
NPROCS=$1
if [ $NPROCS -lt 5 ]
then
echo "Error: at least 5 processors needed, exiting"
exit 1
fi
convert_to_wc $2
LOADER=$3
#UR=$4
shift 3
#APP_NAME=$1
EOT="EOT"

#TMP_SUBMIT_SCRIPT=`mktemp`
TMP_SUBMIT_SCRIPT=`pwd`
echo "Temp submit script is: $TMP_SUBMIT_SCRIPT"
if [ $? -ne 0 ]
then
	echo "Can't create temp file, exiting..."
	exit 1
fi

#TMPPREFIX=COMPSs
#TMPDIR=`mktemp -d`
#if [ $? -ne 0 ]; then
#        echo "Can't create temp dir, exiting..."
#        exit 1
#fi

/bin/cat >> $TMP_SUBMIT_SCRIPT << EOT
#!/bin/bash
#
# @ initialdir = . 
# @ output = compss_${NPROCS}_%j.out
# @ error  = compss_${NPROCS}_%j.err
# @ total_tasks = $NPROCS 
# @ scratch = 1000
# @ wall_clock_limit = $wc_limit 

export IT_HOME=/home/bsc19/bsc19275/compss/c_binding
export GAT_LOCATION=/gpfs/projects/bsc19/COMPSs/JavaGAT
export PROACTIVE_HOME=/gpfs/projects/bsc19/COMPSs/ProActive

WORKER_INSTALL_DIR=/home/bsc19/bsc19275/compss/romina/worker/
WORKER_WORKING_DIR=/home/bsc19/bsc19275/compss/romina/worker/temp/
USER=\`whoami\`
sec=\`/bin/date +%s\`
RESOURCES_FILE=/home/bsc19/bsc19275/compss/romina/master/resources.xml
PROJECT_FILE=/home/bsc19/bsc19275/compss/romina/master/mn.xml


# Begin creating the resources file and the project file

/bin/cat > \$RESOURCES_FILE << EOT
<?xml version="1.0" encoding="UTF-8"?>
<ResourceList>

$EOT

/bin/cat > \$PROJECT_FILE << EOT
<?xml version="1.0" encoding="UTF-8"?>
<Project>

  <!-- list primary, secondary, tertiary... -->
  <RUS Name="http://inexistent.host.es" Port="8088" Path="/usage_records"/>

$EOT

 
# Get node list
ASSIGNED_LIST=\`/usr/local/bin/sl_get_machine_list | /usr/bin/sed -e 's/\.[^\ ]*//g'\`
echo "Node list assigned is:"
echo "\$ASSIGNED_LIST"
# Remove the 4 processors of the master node from the list
MASTER_NODE=\`hostname\`;
echo "Master will run in \$MASTER_NODE"
WORKER_LIST=\`echo \$ASSIGNED_LIST | /usr/bin/sed -e "s/\$MASTER_NODE//g"\`;
# To remove only once: WORKER_LIST=\`echo \$ASSIGNED_LIST | /usr/bin/sed -e "s/\$MASTER_NODE//"\`;
echo "List of workers:"
echo "\$WORKER_LIST"
AUX_LIST=\`echo \$WORKER_LIST\`;

# Find the number of tasks to be executed on each node
for node in \$WORKER_LIST
do
        ntasks=\`echo \$AUX_LIST | /usr/bin/sed -e 's/\ /\n/g' | grep \$node | wc -l\`
        if [ \$ntasks -ne 0 ]
        then
		/bin/cat >> \$RESOURCES_FILE << EOT
  <Resource Name="\${node}-gigabit1">
    <Capabilities>
      <Host>
        <TaskCount>0</TaskCount>
      </Host>
      <Processor>
        <Architecture>PPC</Architecture>
        <Speed>2.3</Speed>
        <CPUCount>4</CPUCount>
      </Processor>
      <OS>
        <OSType>Linux</OSType>
      </OS>
      <StorageElement>
        <Size>36</Size>
      </StorageElement>
      <Memory>
        <PhysicalSize>8</PhysicalSize>
      </Memory>
      <ApplicationSoftware>
        <Software>COMPSs</Software>
        <Software>JavaGAT</Software>
	<Software>ProActive</Software>
      </ApplicationSoftware>
      <FileSystem/>
      <NetworkAdaptor/>
    </Capabilities>
    <Requirements/>
  </Resource>

$EOT

	/bin/cat >> \$PROJECT_FILE << EOT
  <Worker Name="\${node}-gigabit1">
    <InstallDir>\$WORKER_INSTALL_DIR</InstallDir>
    <WorkingDir>\$WORKER_WORKING_DIR</WorkingDir>
    <User>\$USER</User>
    <LimitOfTasks>\$ntasks</LimitOfTasks>
  </Worker>

$EOT

        fi
        AUX_LIST=\`echo \$AUX_LIST | /usr/bin/sed -e "s/\$node//g"\`
done


# Finish the resources file and the project file 

/bin/cat >> \$RESOURCES_FILE << EOT
</ResourceList>
$EOT

/bin/cat >> \$PROJECT_FILE << EOT
</Project>
$EOT


echo "Generation of resources and project file finished"


# Launch the application with COMPSs

export JAVA_HOME=/opt/ibm/java-ppc64-60
export projFile=\$PROJECT_FILE
export resFile=\$RESOURCES_FILE
#export fullAppPath=$APP_NAME
. \$IT_HOME/gridunawareapps/env.sh
EOT

/bin/cat >> $TMP_SUBMIT_SCRIPT << EOT
time $LOADER $*
EOT

/bin/cat >> $TMP_SUBMIT_SCRIPT << EOT

echo "Application finished"


EOT


# Check if the creation of the script failed
result=$?
if [ $result -ne 0 ]
then
	echo "Error creating the submit script" >&2
        exit -1
fi

# Submit the job to the queue
/usr/local/bin/mnsubmit $TMP_SUBMIT_SCRIPT 1>$TMP_SUBMIT_SCRIPT.out 2>$TMP_SUBMIT_SCRIPT.err
result=$?

# Cleanup
submit_err=`/bin/cat $TMP_SUBMIT_SCRIPT.err`
/bin/rm -rf $TMP_SUBMIT_SCRIPT.*

# Check if submission failed
if [ $result -ne 0 ]
then
	echo "Error submitting the job" >&2
	echo $submit_err >&2
        exit -1 
fi

