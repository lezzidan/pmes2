#!/bin/sh

if [ $# -lt 3 ]
then
echo
echo "Usage:"
echo "./guapp.sh execution_mode project_file resources_file historical_file loader sla_id full_application_name params"
exit 127
fi

userDir=~
scriptDir=`pwd`

mode=$1
projFile=$2
resFile=$3
histFile=$4
loader=$5
slaId=$6
fullAppPath=$7
#appName=`echo $fullAppPath | cut -d . -f 3`

if [ $mode != sequential ] && [ $mode != IT ]
then
echo
echo "Execution mode $mode is not valid"
echo "Please, choose either IT or sequential"
exit 127
fi

echo -e "\n----------------- Executing $fullAppPath in $mode mode --------------------------\n"


workingDir=$userDir/IT/$fullAppPath
mkdir -p $workingDir
cd $workingDir

export projFile
export resFile
export histFile
export fullAppPath
. $scriptDir/env.sh

shift 5 # $* == loader + sla_id + full_app_name + params  

if [ $mode = sequential ]
then
shift 1
#$JAVA_HOME/bin/java -Xint $*
$JAVA_HOME/bin/java $*
else
$JAVACMD integratedtoolkit.loader.ITAppLoaderSLA $*
#$JAVACMD $*
fi


echo
echo ------------------------------------------------------------
