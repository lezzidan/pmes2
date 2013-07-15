#!/bin/sh

#/*------------------------------------------------------------------------*/
#/*                                                                        */
#/*                BSC - Barcelona Supercomputing Center                   */     
#/*                           COMPS Superscalar                            */      
#/*                                                                        */ 
#/*                            INSTALL SCRIPT                              */      
#/*                                                                        */
#/*   More information at COMP Superscalar Website: www.bsc.es/compss      */
#/*                                                                        */
#/*------------------------------------------------------------------------*/


path=$1
working_dir=`pwd`

echo " "
echo "Installing COMPS Superscalar on -> "$path"/COMPSs"
echo " "

#Creating base installation directory
mkdir $path/COMPSs
mkdir $path/COMPSs/bindinglib

echo " "
echo "Installing C Binding Library..."
echo " "

cd bindinglib
make clean
./configure --prefix=$path/COMPSs/bindinglib
make
make install

cd $working_dir
cd gsbuilder
make clean
./configure --prefix=$path/COMPSs/bindinglib
make
make install

cd $working_dir
cd gsstubgen
make clean
./configure --prefix=$path/COMPSs/bindinglib
make
make install

echo " "
echo "Copying COMP Superscalar Runtime..."

cd $working_dir
cp -Rf runtime/ $path/COMPSs

echo " "
echo "Installing COMP Superscalar... "
cd $path/COMPSs/runtime
ant install

echo " "
echo "Copying C Example Applications..."
cd $working_dir
cp -Rf c_example_apps/ $path/COMPSs

echo " "
echo " "
echo "Don't forget to add the following variables on your .bashrc file! "
echo " "
echo "*------------------------------------------------------------------------*"
echo "*     echo "export IT_HOME="$path"/COMPSs/runtime"       *"
echo "*     echo "export GS_HOME="$path"/COMPSs/bindinglib"    *"           
echo "*------------------------------------------------------------------------*"

echo " "
echo "More information at COMP Superscalar Website: www.bsc.es/compss"
echo " "
echo " "
