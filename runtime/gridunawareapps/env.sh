#!/bin/sh

 
## Check needed environment variables

IT_HOME=${IT_HOME-NULL};
if [ "$IT_HOME" = "NULL" ]
then
echo
echo "Environment variable IT_HOME not set"
echo "Please use:"
echo " export IT_HOME=<Integrated Toolkit installation directory>"
exit 127
fi

export IT_LIB=$IT_HOME/integratedtoolkit/lib
if [ ! -d $IT_LIB ]
then
echo
echo "Cannot find IT libraries"
echo "Please check if $IT_LIB exists"
exit 127
fi

APP_LIB=$IT_HOME/gridunawareapps/lib
APP_CLASS=$IT_HOME/gridunawareapps/build

JAVA_HOME=${JAVA_HOME-NULL};
if [ "$JAVA_HOME" = "NULL" ]
then
echo
echo "Environment variable JAVA_HOME not set"
echo "Please use:"
echo " export JAVA_HOME=<JDK installation directory>"
exit 127
fi

PROACTIVE_HOME=${PROACTIVE_HOME-NULL};
if [ "$PROACTIVE_HOME" = "NULL" ]
then
echo
echo "Environment variable PROACTIVE_HOME not set"
echo "Please use:"
echo " export PROACTIVE_HOME=<ProActive installation directory>"
exit 127
fi

GAT_LOCATION=${GAT_LOCATION-NULL};
if [ "$GAT_LOCATION" = "NULL" ]
then
echo
echo "Environment variable GAT_LOCATION not set"
echo "Please use:"
echo " export GAT_LOCATION=<GAT installation directory>"
exit 127
fi



## Set up the classpath

CLASSPATH=${CLASSPATH-NULL};
if [ "$CLASSPATH" = "NULL" ]
then
CLASSPATH=.
else
CLASSPATH=$CLASSPATH:.
fi

# Integrated Toolkit

if [ -f $IT_LIB/IT.jar ]
then
    CLASSPATH=$CLASSPATH:$IT_LIB/IT.jar					# IT classes
fi
if [ -d $IT_HOME/xml ]
then
    CLASSPATH=$CLASSPATH:$IT_HOME/xml      				# XML files
fi
if [ -d $IT_HOME/xml/adl ]
then
    CLASSPATH=$CLASSPATH:$IT_HOME/xml/adl  				# ADL files
fi
if [ -d $APP_CLASS ]
then
    CLASSPATH=$CLASSPATH:$APP_CLASS 			       		# Sample applications classes
fi
if [ -f $APP_LIB/guapp.jar ]
then
    CLASSPATH=$CLASSPATH:$APP_LIB/guapp.jar 			      	# Sample applications jar
fi

# ProActive

CLASSPATH=$CLASSPATH:$PROACTIVE_HOME/dist/lib/ProActive.jar

# Xalan and Xerces

if [ -f $IT_LIB/xalan/xalan.jar ]
then
    CLASSPATH=$CLASSPATH:$IT_LIB/xalan/xalan.jar
fi
if [ -f $IT_LIB/xalan/xml-apis.jar ]
then
    CLASSPATH=$CLASSPATH:$IT_LIB/xalan/xml-apis.jar
fi

# GAT

if [ -f $GAT_LOCATION/lib/GAT-API.jar ]
then
    CLASSPATH=$CLASSPATH:$GAT_LOCATION/lib/GAT-API.jar
fi
if [ -f $GAT_LOCATION/lib/GAT-engine.jar ]
then
    CLASSPATH=$CLASSPATH:$GAT_LOCATION/lib/GAT-engine.jar
fi

# Log4j

if [ -f $IT_LIB/log4j/log4j-1.2.15.jar ]
then
    CLASSPATH=$CLASSPATH:$IT_LIB/log4j/log4j-1.2.15.jar
fi

# Trove High Performance HashMaps

if [ -f $IT_LIB/trove/trove-3.0.0a5.jar ]
then
    CLASSPATH=$CLASSPATH:$IT_LIB/trove/trove-3.0.0a5.jar
fi


export CLASSPATH

## Set up the JVM properties
#echo "PATH: $fullAppPath"

JAVACMD=$JAVA_HOME/bin/java"\
	-Xms128m -Xmx256m \
	-Djava.security.manager \
	-Djava.security.policy=$PROACTIVE_HOME/examples/proactive.java.policy \
	-Dlog4j.configuration=$IT_HOME/log/it-log4j \
	-Dit.deployment=$IT_HOME/xml/deployment/ITdeployment.xml \
	-Dit.project.file=$projFile \
	-Dit.resources.file=$resFile \
	-Dit.hist.file=$histFile \
	-Dit.appName=$fullAppPath \
	-Dit.lang=java
	-Dit.locations=true
	-Dit.gat.broker.adaptor=sshtrilead \
	-Dit.gat.file.adaptor=sshtrilead"

#-Dlog4j.configuration=file:$GAT_LOCATION/log4j.properties
#-Dlog4j.configuration=file:$PROACTIVE_HOME/examples/proactive-log4j

export JAVACMD

