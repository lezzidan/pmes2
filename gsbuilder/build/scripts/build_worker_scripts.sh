# Copyright 2002-2007 Barcelona Supercomputing Center (www.bsc.es)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

generate_workergs_script_sh_in() {
	{ /bin/cat <<EOF
#!@BASH_noreplace@

. \${PWD}/config.worker

#zeropading of numbers in order to have fixed length callbacks
zeropad ()
{
	number=\$@;
	count=\$((padding_length - \${#number}));

	while [ \$count -ne 0 ]; do
		number=0\$number;
		count=\$((\$count - 1));
	done
	echo \$number;
}

# interrupt handling function. Notify master & then exit
killed ()
{
	echo [GridSs] Worker was signaled;
	notify \$@;
	@MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${workerdir};
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" > workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                @RM_noreplace@ -rf \${TMPTASK}
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
	exit -1;
}

notify ()
{
EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
if [ x"GRID" == x"GRID" ]; then
	{ /bin/cat <<EOF
		notification="\`zeropad \$task_num\` \$1 \$2 \$3";
		command="\$SSH \$master @BASH_noreplace@ -c \"echo \$notification > \$notification_pipe\"";
		execute_with_retries \$command || echo "FATAL ERROR : COULDN'T CONTACT MASTER";
EOF
	} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
elif [ x"GRID" == x"CLUSTER" ]; then
	{ /bin/cat <<EOF
        execute_with_retries \${COMMAND} \$master \$task_num \$1 \$2 \$3 \$notification_port 
        return \$?
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        execute_with_retries \${COMMAND} \$master \$task_num \$1 \$2 \$3 \$notification_port 
        return \$?
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
fi
        { /bin/cat <<EOF
}

create_binary_env ()
{
	echo "[GridSs] Creating execution environment in additional machines";
	j=0;
	while [ \$j -lt \$nAdditMachines ]; do
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
		if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
			ADDMACHINE_TMPDIR=\${pathsAdditMachines[\$j]}/.gs_tmpdir_\${task_num}_\${master_pid};
		else	
			ADDMACHINE_TMPDIR=\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}/.gs_tmpdir_\${task_num}_\${master_pid};
		fi
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	else
	{ /bin/cat <<EOF
		ADDMACHINE_TMPDIR=\${pathsAdditMachines[\$j]}/.gs_tmpdir_\${task_num}_\${master_pid};
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	fi
	{ /bin/cat <<EOF		
		create_dir \${additMachines[\$j]} \${ADDMACHINE_TMPDIR};
		result=\$?;
		if [ \$result -eq 0 ]; then
			command_env="ln -s \${bindirsAdditMachines[\$j]}/\${EXECUTABLE} \${ADDMACHINE_TMPDIR}/\${EXECUTABLE}; mkdir \${ADDMACHINE_TMPDIR}/.gs_\${master_pid}_dir; cd \${ADDMACHINE_TMPDIR};";
			i=0;
                	while [ \$i -lt \$input_files_num ]; do
				res=\`expr \${args[\$i+2]} : '/'\`;
				if [ \$res -eq 0 ]; then 
                        		command_env="\$command_env ln -s \${pathsAdditMachines[\$j]}/\${args[\$i+2]} \${args[\$i+2]};";
				fi
                        	i=\$((\$i+1));
 	               	done
			echo "[GridSs] Creating softlinks in \${additMachines[\$j]}";			
			\$SSH \$SSH_FLAGS \${additMachines[\$j]} \$command_env;
			reslink=\$?;
			if [ \$reslink -ne 0 ]; then
				echo "ERROR: running remote command \$command_env in \${additMachines[\$j]}";
				return -1;
			fi
		elif [ \$result -eq 1 ]; then
                        echo "ERROR: creating temporary execution directory \${ADDMACHINE_TMPDIR} in \${additMachines[\$j]}";
                        return -1;
		fi
	j=\$((\$j+1));
	done
	return 0;	
}
remove_binary_env ()
{
	echo "[GridSs] Removing temporal execution environment in additional machines";
	j=0;
        while [ \$j -lt \$nAdditMachines ]; do
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
                        ADDMACHINE_TMPDIR=\${pathsAdditMachines[\$j]}/.gs_tmpdir_\${task_num}_\${master_pid};
                else
                        ADDMACHINE_TMPDIR=\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}/.gs_tmpdir_\${task_num}_\${master_pid};
                fi
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
                ADDMACHINE_TMPDIR=\${pathsAdditMachines[\$j]}/.gs_tmpdir_\${task_num}_\${master_pid};
EOF
        }| /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
		remove_dir \${additMachines[\$j]} \${ADDMACHINE_TMPDIR};
		if [ \$? -eq 1 ]; then
			echo "ERROR: removing temporay execution directory \${ADDMACHINE_TMPDIR} in \${additMachines[\$j]}";	
			return -1;
		fi
	j=\$((\$j+1));
	done
	return 0;
}

execute_binary ()
{
        tid="task\${zeropadded_task_num}";

        TMPDIRECTORY="\${TMPTASK}/.gs_tmpdir_\${task_num}_\${master_pid}";
	if test -e "\${TMPDIRECTORY}"; then
		echo "[GridSs] \${TMPDIRECTORY} exists from a previous execution. Removing..." 1>>\${TMPTASK}/\${tid}.out;
		@RM_noreplace@ -rf \${TMPDIRECTORY};
	fi
	( @MKDIR_noreplace@ \${TMPDIRECTORY} && create_binary_env && 
                  cd \${TMPDIRECTORY} && @MKDIR_noreplace@ .gs_\${master_pid}_dir && 
                  @LN_noreplace@ -s \${BINDIR}/\${EXECUTABLE} \${EXECUTABLE} &&
                  \${COMMAND} \$@ && 
                  cd - &&
                  @RM_noreplace@ -rf \${TMPDIRECTORY} && remove_binary_env
        ) 1>>\${TMPTASK}/\${tid}.out 2>>\${TMPTASK}/\${tid}.err
                result=\$?;
        return \${result};
}

execute_with_retries ()
{
	retries=0;
	command=\$@;
	while [ \$retries -ne \$max_retries ]; do
		\$command && return 0;
		echo "Command execution failed retrying..."; 
		@SLEEP_noreplace@ \$((5 + \${retries}*\${RANDOM} % \${max_time_before_retry}));
		retries=\$((\$retries+1));
	done
	echo "ERROR Executing command: \$command ";
	return -1;
}

# function to compare is workerdir is the same as masterdir
are_the_same()
{
        A="\$1";
        AA="\$1/";
        B="\$2";
        BB="\$2/";
        if test \${A} == \${B}; then
                return 0;
        elif test \${A} == \${BB}; then
                return 0;
        elif test \${AA} == \${B}; then
                return 0;
        elif test \${AA} == \${BB}; then
                return 0;
        else
                return -1;
        fi
}

create_dir ()
{
	\$SSH \$SSH_FLAGS \$1 test -e \$2;
	result=\$?;
	if [ \$result -eq 1 ]; then # Dir not exists
		\$SSH \$SSH_FLAGS \$1 mkdir \$2
		if [ \$? -ne 0 ]; then
			echo "ERROR creating directory \$2"
                        return 1;
		else
			echo "[GridSs] Directory \$2 crated";
                        return 0;
		fi
	elif [ \$result -eq 0 ]; then # Dir already exists
		echo "[GridSs] Directory \$2 not created. It already exists"
                return 2;	
	else
		echo "ERROR: Checking remote directory existency" 
		return 1;	
	fi
}

remove_dir ()
{
	\$SSH \$SSH_FLAGS \$1 test -e \$2;
	result=\$?;
        if [ \$result -eq 0 ]; then #Dir exists
                \$SSH \$SSH_FLAGS \$1 rm -rf \$2
            	if [ \$? -ne 0 ]; then
                        echo "ERROR removing directory \$2"
                        return 1;
                else
                        echo "[GridSs] Directory \$2 removed";
                        return 0;
                fi
        elif [ \$result -eq 1 ]; then # Dir already exists
                echo "[GridSs] Directory \$2 not removed. It does not exists"
                return 2;
        else
                echo "ERROR: Checking remote directory existency"
                return 1;
        fi 
}

EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;


if [ x"GRID" == x"MN_SCRATCH" ]; then
        echo "gs_exec_env=\$1;" >> workerGS_script.sh.in;
        echo "shift 1;" >> workerGS_script.sh.in;
fi
        { /bin/cat <<EOF
# read arguments
cd \$1;
workerdir=\$1;
mode=\$2;
logs=\$3;
master_src_dir=\$4;
notification_port=\$5;
master_pid=\$6
shift 6;

#setting common GS environment variables
export GS_SHORTCUTS=\$1;
export GS_SOCKETS=\$2;
export GS_MIN_PORT=\$3;
export GS_MAX_PORT=\$4;
export GS_NAMELENGTH=\$5;
export GS_GENLENGTH=\$6;
export GS_MAXMSGSIZE=\$7;
shift 7;

if [ \$mode -eq \$EXECUTE ]; then
	# read GS_ENVIRONMENT
	num_env_var=\$1;
	shift;
	j=0;
        while [ \$j -lt \$num_env_var ]; do
                export \$1;
		shift;
                j=\$((\$j+1));
        done

	# read additional hosts

	ncpus=\$1
	slotsmachine=\$2;
	nAdditMachines=\$3;
	shift 3;
	workermachine=\`hostname\`;
        GS_MACHINELIST="";
	j=0;
        while [ \$j -lt \$slotsmachine ]; do
                GS_MACHINELIST="\$GS_MACHINELIST \$workermachine"
                j=\$((\$j+1));
        done
	i=0;
	while [ \$i -lt \$nAdditMachines ]; do
                additMachines[\$i]=\$1;
		slotsAdditMachines[\$i]=\$2;
		pathsAdditMachines[\$i]=\$3;
		bindirsAdditMachines[\$i]=\$4;
		j=0;
		while [ \$j -lt \$2 ]; do
                        GS_MACHINELIST="\$GS_MACHINELIST \$1"
                        j=\$((\$j+1));
                done
		shift 4;
                i=\$((\$i+1));
        done
        
 
	# read numers of files to be cleaned up and staged in
	nstagein=\$1;
	cleanup_files_num=\$2;
	shift 2;

	# builds the string holding the files to be staged in
	i=0;
	while [ \$i -lt \$nstagein ]; do
		input_files="\$input_files \$1 \$2";
		shift 2;
		i=\$((\$i+1));
	done
	i=0;

	# builds a string holding the files to be cleaned up
	while [ \$i -lt \$cleanup_files_num ]; do
		rmfiles="\$rmfiles \$1";
		shift;
		i=\$((\$i+1));
	done

	# read number of input/output files & master hostname( indexed backwards )
	j=0;
	for i in \$@; do
		args[\$j]=\$i;
		j=\$((\$j+1));
	done
	
	total_args=\$j
        master_pid=\${args[\$j-1]}
        input_files_num=\${args[\$j-6]}
        output_files_num=\${args[\$j-5]}
        master=\${args[\$j-4]}
	task_num=\${args[0]}
	
	zeropadded_task_num="\`zeropad \$task_num\`_\${master_pid}"
	
EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

if [ x"GRID" == x"CLUSTER" ]; then
        echo "	     TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"GRID" ]; then
        echo "	     TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
	if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
        	TMPTASK=\${workerdir};
	else
        	TMPTASK="\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}";
		@MKDIR_noreplace@ \${TMPTASK}; 
		#Create TMPTASK dir for additional Machines ssh mkdir"
		
	fi

EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

fi
        { /bin/cat <<EOF
	if are_the_same \${BINDIR} \${TMPTASK}; then
        	samedir=1;
	else
        	samedir=0;
	fi
	#setting task GS environment variables
	export GS_TASK_NCPUS=\${ncpus};
	export GS_TASKNUM=\${task_num};
	export GS_MASTERPID=\${master_pid};
	if [ x"\${GS_MACHINELIST}" != x"" ]; then
		export GS_MACHINELIST;
	fi
	export GS_WORKER_BINDIR=\${BINDIR};
	export GS_WORKER_WKGDIR=\${workerdir};
	export GS_WORKER_TMPTASKDIR=\${TMPTASK};
	export GS_APPNUM=\${master_pid};
	if [ x"\${gsenv}" != x"" ]; then
		export \${gsenv};
	fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;	
	if [ x"GRID" == x"MN_SCRATCH" ]; then
	{ /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then		
		if test ! -e "\${TMPTASK}/.gs_\${master_pid}_dir"; then
			@MKDIR_noreplace@ \${TMPTASK}/.gs_\${master_pid}_dir 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
		fi
		#create file dirs in all additional machines
	else 
		if test ! -e "\${workerdir}/.gs_\${master_pid}_dir"; then
                        @MKDIR_noreplace@ \${workerdir}/.gs_\${master_pid}_dir 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                fi
		#create file dirs in additional machines workerdirs where it does not exist yet
		k=0
		while [ \$k -lt \$nAdditMachines ]; do
			create_dir \${additMachines[\$k]} \${pathsAdditMachines[\$k]}/.gs_\${master_pid}_dir 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
			if [ \$? -eq 1 ]; then
                                notify \$STAGEIN_ERROR \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
				echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
			        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
	                        if [ \$samedir -eq 0 ]; then
                                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                                        @RM_noreplace@ -rf \${TMPTASK}
                                        #Remove tmp disk in all additional machines
                                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                                exit -1;
                        fi
                	k=\$((\$k+1));
        	done
		 
	fi	
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	else
	{ /bin/cat <<EOF
	if test ! -e "\${workerdir}/.gs_\${master_pid}_dir"; then
        	@MKDIR_noreplace@ \${workerdir}/.gs_\${master_pid}_dir;
        fi
	#create file dirs in additional machines workerdirs where it does not exist yet
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	fi
	{ /bin/cat <<EOF
	# trap SIGHUP, SIGINT, SIGQUIT, SIGTERM, SIGXCPU
        trap "killed \$USER_CANCEL \$TASK_ENDS" 1 2 3 15;
        trap "killed \$QSYS_CANCEL \$TASK_ENDS" 24;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                i=0;
                #TMPTASK=\`@MKTEMP_noreplace@ -d -p\${TMPPREFIX}\`
                while [ \$i -lt \$output_files_num ]; do
                        args[\$j-7-\$i]="\${TMPTASK}/\${args[\$j-7-\$i]}";
                        if [ \$i -eq 0 ]; then
                                output_files="\${args[\$j-7-\$i]}";
                        else
                                output_files="\$output_files \${args[\$j-7-\$i]}";
                        fi
                        i=\$((\$i+1));
                done

                args[\$j-3]="\${workerdir}";
                #creating Temporal directory
                #TMPDIRECTORY=''
                #TMPDIRECTORY=\`@MKTEMP_noreplace@ -d -p\${TMPTASK}\`
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
	#remove logs from previous executions
	if [ \$logs == 1 ]; then
		@RM_noreplace@ -f \${BINDIR}/task\${zeropadded_task_num}.out \${BINDIR}/task\${zeropadded_task_num}.err;
	else
		@RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err;
	fi

	# trap SIGHUP, SIGINT, SIGQUIT, SIGTERM
	# trap "killed \$USER_CANCEL \$TASK_ENDS" 1 2 3 15;

	# stage in files
	echo [GridSs] File Stage in ... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out
	j=0;
	for i in \$input_files; do
		if [ \$j -eq 1 ]; then
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                                files="\$files \${TMPTASK}/\$i"
                        else
                                files="\$files \$i"
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
                        files="\$files \$i";
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF			
			j=0;
			echo [GridSs] STFile: \$SCP \$SCP_FLAGS \$files  1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
			execute_with_retries \$SCP \$SCP_FLAGS \$files 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; 
			if [ \$? -ne 0 ]; then
				notify \$STAGEIN_ERROR \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
				echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
			        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                                if [ \$samedir -eq 0 ]; then
                                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                                        @RM_noreplace@ -rf \${TMPTASK}
					#Remove tmp disk in all additional machines
                                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
				exit -1;
			fi
		else
			files="\$i";
			j=1;
		fi
	done

	# notify for active state
	if ! notify \$TASK_ACTIVE \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                echo ERROR sending Notification task \$task_num active.retrying... 1>>\${TMPTASK}/task\${zeropadded_task_num}.err
                @SLEEP_noreplace@ 5;
                if ! notify \$TASK_ACTIVE \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                        echo ERROR sending task \$task_num active notification.  1>>\${TMPTASK}/task\${zeropadded_task_num}.err
			echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
		        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                        if [ \$samedir -eq 0 ]; then
                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                                @RM_noreplace@ -rf \${TMPTASK}
				#Remove tmp disk in all additional machines
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                        exit -1;
                fi
        fi	

	# check if number of arguments is ok */
	if test ! -e "\${BINDIR}/\${EXECUTABLE}"; then 
		notify \$WORKER_EXEC_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
		echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
	        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK};
			#Remove tmp disk in all additional machines
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
		exit -1;
	fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                i=0;
                while [ \$i -lt \$input_files_num ]; do
                        args[\$i+2]="\${TMPTASK}/\${args[\$i+2]}";
                        i=\$((\$i+1));
                done
        fi
        i=0;
        while [ \$i -lt \$total_args ]; do
                if [ \$i -eq 0 ]; then
                        param="\${args[\$i]}";
                else
                        param="\${param} \${args[\$i]}";
                fi
                i=\$((\$i+1));
        done
        echo [GridSs] Executing binary \${param}... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out
        if ! execute_binary \${param}; then
                notify \$WORKER_ERROR \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
		echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
	        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                fi
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
			#Remove tmp disk in all additional machines
                fi
                exit -1;
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
        echo [GridSs] Executing binary \$@... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out
        if ! execute_binary \$@; then
                notify \$WORKER_ERROR \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
	        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
		if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
			#Remove tmp disk in all additional machines
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                exit -1;
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
	{ /bin/cat <<EOF
        if test ! -f "\${PWD}/destGen_\${master_pid}_.\$task_num"; then
                notify \$DESTGEN_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
        	(@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
		if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
			#Remove tmp disk in all additional machines
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                exit -1;
        fi
        echo [GridSs] Stage Out... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"GRID" ]; then
        { /bin/cat <<EOF
                if ! execute_with_retries "\$SCP \$SCP_FLAGS \${PWD}/destGen_\${master_pid}_.\$task_num \$master:\${master_src_dir}/.gs_\${master_pid}_dir"1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                        notify \$DESTGEN_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
			echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
		        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                        if [ \$samedir -eq 0 ]; then
                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                        fi
                        exit -1;
                else
                        @RM_noreplace@ -f \${PWD}/destGen_\${master_pid}_.\$task_num;
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	elif [ x"GRID" == x"CLUSTER" ]; then
        { /bin/cat <<EOF

                #if ! are_the_same \${PWD} \${master_src_dir}; then
                      # echo "Worker and master directories are not the same destGen file transfer is needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;

                        if ! execute_with_retries "\$SCP \$SCP_FLAGS \${PWD}/destGen_\${master_pid}_.\$task_num \${master_src_dir}/.gs_\${master_pid}_dir";1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err then
                                notify \$DESTGEN_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err:
				echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;

                                if [ \$samedir -eq 0 ]; then
                                       @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                fi
                                exit -1;
                        else
                                @RM_noreplace@ -f \${PWD}/destGen_\${master_pid}_.\$task_num;
                        fi
                #else
                       # echo "Worker and master directories are the same destGen file transfer is not needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                #fi

EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then

                        for i in \$output_files; do
                                echo [GridSs] Stage Out:\$SCP \$SCP_FLAGS \$i \$master:\${master_src_dir}/.gs_\${master_pid}_dir/ 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                if ! execute_with_retries "\$SCP \$SCP_FLAGS \$i \$master:\${master_src_dir}/.gs_\${master_pid}_dir/" 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then

                                        notify \$STAGEOUT_ERROR \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
					echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                                        if [ \$samedir -eq 0 ]; then
                                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                        fi
                        		@RM_noreplace@ -rf \${TMPTASK}
                        		#Remove tmp disk in all additional machines
                                        exit -1;
                                fi
                        done
                        #if ! are_the_same \${workerdir} \${master_src_dir}; then
                                #echo "Worker and master directories are not the same destGen file transfer is needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                if ! execute_with_retries "\$SCP \$SCP_FLAGS \${workerdir}/destGen_\${master_pid}_.\$task_num \$master:\${master_src_dir}/.gs_\${master_pid}_dir/" 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                                        notify \$DESTGEN_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                                	echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
				        if [ \$samedir -eq 0 ]; then
                                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                        fi
                                        @RM_noreplace@ -rf \${TMPTASK}
                                        exit -1;
                                else
                                        @RM_noreplace@ -f \${workerdir}/destGen_\${master_pid}_.\$task_num;
                                fi
                        #else
                                #echo "Worker and master directories are the same destGen file transfer is not needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                        #fi
                else

			#if ! are_the_same \${PWD} \${master_src_dir}; then
                                #echo "Worker and master directories are not the same destGen file transfer is needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                                if ! execute_with_retries "\$SCP \$SCP_FLAGS \${PWD}/destGen_\${master_pid}_.\$task_num \$master:\${master_src_dir}/.gs_\${master_pid}_dir/" 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                                        notify \$DESTGEN_NF \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
                                        echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
				        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
					if [ \$samedir -eq 0 ]; then
                                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                                        fi
                                        exit -1;
                                else
                                        @RM_noreplace@ -f \${PWD}/destGen_\${master_pid}_.\$task_num;
                                fi
                        #else
                                #echo "Worker and master directories are the same destGen file transfer is not needed..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                        #fi
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
	{ /bin/cat <<EOF
	# cleanup phase
	echo "[GridSs] Clean up phase (removing \$rmfiles) ..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
	for i in \${rmfiles}; do
		echo "[GridSs] Removing \$i" ... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
		@RM_noreplace@ -f \$i 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
	done
	echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out; 
	(@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
	echo [GridSs] Task Done ... 1>>\${TMPTASK}/task\${zeropadded_task_num}.out
        if ! notify \$TASK_DONE \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                echo ERROR sending task \$task_num done notification. Retrying... 1>>\${TMPTASK}/task\${zeropadded_task_num}.err
                @SLEEP_noreplace@ 5;
                if ! notify \$TASK_DONE \$TASK_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err; then
                        echo ERROR sending task \$task_num done notification. 1>>\${TMPTASK}/task\${zeropadded_task_num}.err
                        if [ \$samedir -eq 0 ]; then
                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                                @RM_noreplace@ -rf \${TMPTASK}
				#Remove tmp disk in all additional machines
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                        exit -1;
                fi
        fi
        if [ \$logs -eq 0 ]; then
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
			#Remove tmp disk in all additional machines
                else
                        @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err;
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
        else
           if [ \$logs -eq 1 ]; then
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err \${BINDIR};
                fi

EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
			#Remove tmp disk in all additional machines
                else
                        if [ \$samedir -eq 0 ]; then
                                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num}.out \${TMPTASK}/task\${zeropadded_task_num}.err;
                        fi
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
                fi
        fi	

elif [ \$mode -eq \$CLEANUP ]; then
        master=\$1;
        task_num=\$2;
	is_masterdisk=\$3;
        num_of_stageout=\$4;
        i=0;
        zeropadded_task_num="\`zeropad \$task_num\`_\${master_pid}.cleanup"
        shift 4;
EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

if [ x"GRID" == x"CLUSTER" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"GRID" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
                TMPTASK=\${workerdir};
        else
                TMPTASK="\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}";
                @MKDIR_noreplace@ \${TMPTASK};
        fi

EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

fi
        { /bin/cat <<EOF
        if are_the_same \${BINDIR} \${TMPTASK}; then
                samedir=1;
        else
                samedir=0;
        fi
	
	while [ \$i -lt \$num_of_stageout ]; do 
EOF
	} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
		if [ x"GRID" == x"GRID" ]; then
                        echo "			command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$master:\$2\"" >> workerGS_script.sh.in;
		elif [ x"GRID" == x"CLUSTER" ]; then
                        echo "			command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\"" >> workerGS_script.sh.in;
		elif [ x"GRID" == x"MN_SCRATCH" ]; then
                        echo "			if [ \$gs_exec_env -eq \$ENV_GPFS ]; then" >> workerGS_script.sh.in;
                        echo "           		command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\"" >> workerGS_script.sh.in;
			echo "		 		echo \"[GridSs] cleanup:\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\" 1>>\${TMPTASK}/task\${zeropadded_task_num}" >> workerGS_script.sh.in; 		
                        echo "			fi" >> workerGS_script.sh.in;
		fi		
	{ /bin/cat <<EOF
		if ! execute_with_retries \$command 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num}; then
                       	notify \$CLEANUP_ERROR \$RM_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
                        echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num}.out;
                	(@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num}.out 2>>\${TMPTASK}/task\${zeropadded_task_num}.err;
			if [ \$samedir -eq 0 ]; then
                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                        fi
		
                        exit -1;
                fi
		shift 2;
		i=\$((\$i+1));
	done
        for i in \$@; do
        	echo "Deleting file \${workerdir}/.gs_\${master_pid}_dir/\$i" 1>>\${TMPTASK}/task\${zeropadded_task_num};
                @RM_noreplace@ -f \${workerdir}/.gs_\${master_pid}_dir/\$i;
        done
        if [ \$is_masterdisk -eq \$ISNOTMASTER ]; then
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
	if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
            	if [ \$gs_exec_env -eq \$ENV_GPFS ]; then	
               		if test -e "\${workerdir}/.gs_\${master_pid}_dir"; then
                       		@RM_noreplace@ -rf \${workerdir}/.gs_\${master_pid}_dir;
               		fi
		fi
	fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else 
	{ /bin/cat <<EOF
		if test -e "\${workerdir}/.gs_\${master_pid}_dir"; then
        	        @RM_noreplace@ -rf \${workerdir}/.gs_\${master_pid}_dir;
                fi
	fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;	
	fi

        { /bin/cat <<EOF
        
	notify \$CLEANUP_DONE \$RM_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
       	@RM_noreplace@ -f \${workerdir}/.gs_process_\${master_pid};
	
	if [ \$logs -eq 1 ]; then
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                fi
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                @RM_noreplace@ -rf \${TMPTASK}
        else
                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num}; 
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
elif [ \$mode -eq \$STAGEOUT ]; then
        master=\$1;
        task_num=\$2;
        is_masterdisk=\$3;
        num_of_stageout=\$4;
        i=0;
        zeropadded_task_num="\`zeropad \$task_num\`_\${master_pid}.stageout"
        shift 4;
EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

if [ x"GRID" == x"CLUSTER" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"GRID" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
                TMPTASK=\${workerdir};
        else
                TMPTASK="\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}";
                @MKDIR_noreplace@ \${TMPTASK};
        fi

EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

fi
        { /bin/cat <<EOF
        if are_the_same \${BINDIR} \${TMPTASK}; then
                samedir=1;
        else
                samedir=0;
        fi

        while [ \$i -lt \$num_of_stageout ]; do
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
                if [ x"GRID" == x"GRID" ]; then
                        echo "                  command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$master:\$2\"" >> workerGS_script.sh.in;
                elif [ x"GRID" == x"CLUSTER" ]; then
                        echo "                  command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\"" >> workerGS_script.sh.in;
                elif [ x"GRID" == x"MN_SCRATCH" ]; then
                        echo "                  if [ \$gs_exec_env -eq \$ENV_GPFS ]; then" >> workerGS_script.sh.in;
                        echo "                          command=\"\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\"" >> workerGS_script.sh.in;
                        echo "                          echo \"[GridSs] cleanup:\$SCP \$SCP_FLAGS \${PWD}/\$1 \$2\" 1>>\${TMPTASK}/task\${zeropadded_task_num}" >> workerGS_script.sh.in;
                        echo "                  fi" >> workerGS_script.sh.in;
                fi
        { /bin/cat <<EOF
                if ! execute_with_retries \$command 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num}; then
                        notify \$CLEANUP_ERROR \$TRF_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
                        echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num};
                	(@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
			if [ \$samedir -eq 0 ]; then
                                @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                        fi
                        exit -1;
                fi
                shift 2;
                i=\$((\$i+1));
        done
        notify \$CLEANUP_DONE \$TRF_ENDS 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
	echo "[GridSs] Removing task from gs_process file..." 1>>\${TMPTASK}/task\${zeropadded_task_num};
        (@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num}) 1>>\${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
        if [ \$logs -eq 1 ]; then
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                fi
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                @RM_noreplace@ -rf \${TMPTASK}
        else
                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
                @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
elif [ \$mode -eq \$NOTIFY ]; then
        master=\$1;
        task_num=\$2;
	error_notification="\$3 \$4";
        state_notification=\$5;
        zeropadded_task_num="\`zeropad \$task_num\`_\${master_pid}.notify";
EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

if [ x"GRID" == x"CLUSTER" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"GRID" ]; then
        echo "       TMPTASK=\${workerdir}" >> workerGS_script.sh.in;
elif [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ \$gs_exec_env -eq \$ENV_GPFS ]; then
                TMPTASK=\${workerdir};
        else
                TMPTASK="\${TMPPREFIX}/.gs_tmpdir_\${task_num}_\${master_pid}";
                @MKDIR_noreplace@ \${TMPTASK};
        fi

EOF
} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;

fi
        { /bin/cat <<EOF
        if are_the_same \${BINDIR} \${TMPTASK}; then
                samedir=1;
        else
                samedir=0;
        fi
        notify \${error_notification} \${state_notification} 1>> \${TMPTASK}/task\${zeropadded_task_num} 2>>\${TMPTASK}/task\${zeropadded_task_num};
	result=\$?
        if [ \${result} -ne 0 ]; then
		echo Notification result is: \${result} >> \${TMPTASK}/task\${zeropadded_task_num}
                if [ \$samedir -eq 0 ]; then
                         @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
                else
                        @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
        	@RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF
	fi
	if [ \$logs -eq 1 ]; then
                if [ \$samedir -eq 0 ]; then
                        @MV_noreplace@ \${TMPTASK}/task\${zeropadded_task_num} \${BINDIR};
                fi
        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                if [ \$gs_exec_env -eq \$ENV_SCRATCH ]; then
                        @RM_noreplace@ -rf \${TMPTASK}
                else
                        @RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
                fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        else
        { /bin/cat <<EOF
        	@RM_noreplace@ -f \${TMPTASK}/task\${zeropadded_task_num};
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
        fi
        { /bin/cat <<EOF	
        exit \${result};
fi

EOF
	} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS_script.sh.in;
}


generate_workergs_sh_PBS() {
	
	queue_submit_com=$1;

	{ /bin/cat <<EOF
#!@BASH_noreplace@
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" > workerGS.sh.in;

if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
gs_exec_env=\$1;
shift 1;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
task_num=\$1;
queue=\$2;
wc_limit=\$3;
shift 3;
master_pid=\$6
cd \$1;
shift 1;

. \${PWD}/config.worker;

EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
	if [ x\$queue = x"none" ]; then
        	exec \${PWD}/workerGS_script.sh \${gs_exec_env} \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
        { /bin/cat <<EOF
        if [ x\$queue = x"none" ]; then
		exec \${PWD}/workerGS_script.sh \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
	else
	{
		@CAT_noreplace@ <<EOT
			#!@CSH_noreplace@
			## JobName
			#PBS -N GS.\$task_num.\${master_pid}
			## Job is not re-runable
			#PBS -r n
			## Outputs
			#PBS -e GS.\${task_num}_\${master_pid}.err
			#PBS -o GS.\${task_num}_\${master_pid}.out
			# export all my environment variables to the job
			#PBS -V 
			#########

			# change to working directory
			cd \$1
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF

                        ./workerGS_script.sh \${gs_exec_env} \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
        { /bin/cat <<EOF
                        ./workerGS_script.sh \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
EOT
        } | exec $queue_submit_com -q \$queue 1>subtsk\${task_num}_\${master_pid}.out 2>subtsk\${task_num}_\${master_pid}.err
        result=\$?;
                if [ \$result -eq 0 ]; then
                        @RM_noreplace@ -f subtsk\${task_num}_\${master_pid}.out subtsk\${task_num}_\${master_pid}.err;
                else
                        # Notify error in submit

                        mode=\$2;
                        if [ \$mode -eq \$EXECUTE ]; then

                                nstagein=\$7;
                                cleanup_files_num=\$8;
                                let "t=8+(2*\$nstagein)+\$cleanup_files_num";

                                j=0;
                                for i in \$@; do
                                        args[\$j]=\$i;
                                        j=\$((\$j+1));
                                done


                                master=\${args[\$j-4]};
                                task_num=\${args[\$t]};

                        elif [ \$mode -eq \$CLEANUP ]; then
                                master=\$7;
				task_num=\$8;
                                #task_num=0;
                        fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                        exec ./workerGS_script.sh \${gs_exec_env} \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num 2>>subtsk\${task_num_\${master_pid}}.err 1>>subtsk\${task_num}_\${master_pid}.out &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
{ /bin/cat <<EOF
                        exec ./workerGS_script.sh \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num 2>>subtsk\${task_num}_\${master_pid}.err 1>>subtsk\${task_num}_\${master_pid}.out &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
{ /bin/cat <<EOF
                fi
        fi



EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
}

# common functions
generate_workergs_sh_LL() {

	queue_submit_com=$1;

	{ /bin/cat <<EOF
#!@BASH_noreplace@
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" > workerGS.sh.in;

if [ x"GRID" == x"MN_SCRATCH" ]; then
	{ /bin/cat <<EOF
gs_exec_env=\$1;
shift 1;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
	{ /bin/cat <<EOF
task_num=\$1;
queue=\$2;
wc_limit=\$3;
if [ x\$wc_limit != x"unlimited" ]; then
        WC_LIMIT_arg=\`echo \${wc_limit} | @SED_noreplace@ -e 's/:/ /g'\`;
                                        j=0;
                                        for i in \$WC_LIMIT_arg; do
                                                if [ \$j = 0 ]; then
                                                        WC_LIMIT=\$i;
                                                elif [ \$j = 1 ]; then
                                                        min=\$((\$i+1));
                                                        WC_LIMIT=\${WC_LIMIT}:\$min;
                                                elif [ \$j = 2 ]; then
                                                        WC_LIMIT=\${WC_LIMIT}:\$i,\${wc_limit};
                                                else
                                                        echo "Bad definition of walk_limit_clock";
                                                fi
                                                j=\$((\$j+1));
                                        done
fi
shift 3;
master_pid=\$6
cd \$1;
shift 1;

. \${PWD}/config.worker;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
	if [ x\$queue = x"none" ]; then
        	exec \${PWD}/workerGS_script.sh \${gs_exec_env} \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
	{ /bin/cat <<EOF
	if [ x\$queue = x"none" ]; then
		exec \${PWD}/workerGS_script.sh \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi 
	{ /bin/cat <<EOF
	else
		{	
		@CAT_noreplace@ <<EOT
			# @ shell = @BASH_noreplace@
			# @ job_type = serial 
			# @ job_name = GS.\${task_num}.\${master_pid}
			# @ output = GS.\${task_num}_\${master_pid}.out
			# @ error = GS.\${task_num}_\${master_pid}.err
			# @ initialdir = \$1
			# @ class = \${queue}
			# @ wall_clock_limit= \${WC_LIMIT}
			# @ group = \`id -gn\`
			## @ node = 1
			## @ total_tasks = 1 
			# @ queue
		
			cd \$1;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
	
                        ./workerGS_script.sh \${gs_exec_env} \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
        { /bin/cat <<EOF
			./workerGS_script.sh \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
EOT
		} | exec $queue_submit_com - 1>subtsk\${task_num}_\${master_pid}.out 2>subtsk\${task_num}_\${master_pid}.err
        	result=\$?;
        	if [ \$result -eq 0 ]; then
                	@RM_noreplace@ -f subtsk\${task_num}_\${master_pid}.out subtsk\${task_num}_\${master_pid}.err;
        	else
                	# Notify error in submit
			 
                	mode=\$2;
			if [ \$mode -eq \$EXECUTE ]; then

                        	nstagein=\$7;
                        	cleanup_files_num=\$8;
                        	let "t=8+(2*\$nstagein)+\$cleanup_files_num";
                        
                        	j=0;
                        	for i in \$@; do
                                	args[\$j]=\$i;
                                	j=\$((\$j+1));
                        	done

                        
                        	master=\${args[\$j-4]};
                        	task_num=\${args[\$t]};
                        
                	elif [ \$mode -eq \$CLEANUP ]; then
                        	master=\$7;
                        	task_num=\$8;
                	fi
EOF
	} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                	exec ./workerGS_script.sh \${gs_exec_env} \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num 2>>subtsk\${task_num}_\${master_pid}.err 1>>subtsk\${task_num}_\${master_pid}.out &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else 
{ /bin/cat <<EOF
                        exec ./workerGS_script.sh \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num 2>>subtsk\${task_num}_\${master_pid}.err 1>>subtsk\${task_num}_\${master_pid}.out &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi 
{ /bin/cat <<EOF
        	fi
	fi



EOF
	} | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
}

generate_workergs_sh_MN() {

        queue_submit_com=$1;

        { /bin/cat <<EOF
#!@BASH_noreplace@
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" > workerGS.sh.in;

if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
gs_exec_env=\$1;
shift 1;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
task_num=\$1;
queue=\$2;
wc_limit=\$3;
shift 3;
cd \$1;
shift 1;
workerdir=\$1;
mode=\$2;
master_pid=\$6;

. \${PWD}/config.worker;

if [ \${mode} -eq \$CANCEL ]; then
        if [ x"\$queue" == x"none" ]; then
                if test -e "\${workerdir}/.gs_process_\${master_pid}"; then
                        pid_to_kill=\`@CAT_noreplace@ \${workerdir}/.gs_process_\${master_pid} | @GREP_noreplace@ -e "^TASK \${task_num} " | @SED_noreplace@ -e "s/TASK \$task_num //g"\`;
                        if [ x"\${pid_to_kill}" != x"" ]; then
				num_procs=\`echo \${pid_to_kill} | wc -w\`;
                        	if [ \${num_procs} -ne 1 ]; then
                                	echo "ERROR: cancelling task \$task_num. Processes file corrupted. There are more than one processes assigned to one task";
                                	echo "PIDS to check: \${pid_to_kill} Num_procs: \${num_procs}";
                                	exit -1;
                        	fi
                                target1_pid=\`@PS_noreplace@ --ppid \${pid_to_kill} -o pid=\`;
                                if [ x"\${target1_pid}" != x"" ]; then
                                        target_pid=\`@PS_noreplace@ --ppid \${target1_pid} -o pid=\`;
                                        if [ x"\${target_pid}" != x"" ]; then
						target_pids=\`@PS_noreplace@ --ppid \${target_pid} -o pid=\`;
                                                while [ x"\${target_pids}" != x"" ]; do
                                                        newpids="";
                                                        for pids in \${target_pids}
                                                        do
                                                                pid_to_kill="\${pid_to_kill} \${pids}";
                                                                tmppids=\`@PS_noreplace@ --ppid \${pids} -o pid=\`;
                                                                newpids="\${newpids} \${tmppids}";
                                                        done
                                                        target_pids=\${newpids};
                                                done
                                                kill -15 \${target_pid} \${target1_pid} \${pid_to_kill};
                                                if [ \$? -ne 0 ]; then
                                                        echo "ERROR: cancelling task \$task_num. A problem has occurred during killing process \$pid_to_kill";
                                                        exit -1;
						fi
                                        else
                                                kill -15 \${target1_pid} \${pid_to_kill};
                                                if [ \$? -ne 0 ]; then
                                                        echo "ERROR: cancelling task \$task_num. A problem has occurred during killing process \$pid_to_kill";
                                                        exit -1;
						fi
                                        fi
                                else
                                        kill -15 \${pid_to_kill};
                                        if [ \$? -ne 0 ]; then
                                                echo "ERROR: cancelling task \$task_num. A problem has occurred during killing process \$pid_to_kill";
                                                exit -1;
					fi
                                fi
				pid_to_kill=\`@CAT_noreplace@ \${workerdir}/.gs_process_\${master_pid} | @GREP_noreplace@ -e "^TASK \${task_num} " | @SED_noreplace@ -e "s/TASK \$task_num //g"\`;
                                if [ x"\${pid_to_kill}" != x"" ]; then
                                        #echo "[GridSs-worker] Process number not deleted related to task \${task_num}, we must to delete it now to avoid future problems...";
					@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num};
                                fi
                        else
                                #echo "Cancelling task \$task_num. Task not found in process file \${workerdir}/.gs_process_\${master_pid}";
                                exit \$PROCESS_NFND;
                        fi
                else
                        #echo "Cancelling task \$task_num. File \${workerdir}/.gs_process_\${master_pid} not found";
                        exit \$PROCESS_FNFND;
                fi
        else
                echo Queue task cancellation not supported by this version;
                exit 0;
        fi
elif [ \$mode -eq \$ACTIVE ]; then
        if [ x"\$queue" == x"none" ]; then
                if test -e "\${workerdir}/.gs_process_\${master_pid}"; then
                        pid_to_check=\`@CAT_noreplace@ \${workerdir}/.gs_process_\${master_pid} | @GREP_noreplace@ -e "^TASK \${task_num} " | @SED_noreplace@ -e "s/TASK \$task_num //g"\`;
                        if [ x"\${pid_to_check}" != x"" ]; then
				num_procs=\`echo \${pid_to_check} | wc -w\`;
                        	if [ \${num_procs} -ne 1 ]; then
                                	#echo "ERROR: checking task \$task_num. Processes file corrupted. There are more than one processes assigned to one task";
                                	#echo "PIDS to check: \${pid_to_check} Num_procs: \${num_procs}";
                                	exit -1;
                        	fi
                                num_of_procs=\`@PS_noreplace@ x -o pid | grep -e \$pid_to_check | wc -l\`;
                                if [ \${num_of_procs} -ne 1 ]; then
                                        #echo "[GridSs-worker] Checking state of task \$task_num. Process NOT ACTIVE";
					pid_to_kill=\`@CAT_noreplace@ \${workerdir}/.gs_process_\${master_pid} | @GREP_noreplace@ -e "^TASK \${task_num} " | @SED_noreplace@ -e "s/TASK \$task_num //g"\`;
                                	if [ x"\${pid_to_kill}" != x"" ]; then
                                        	#echo "[GridSs-worker] Process number not deleted related to task \${task_num}, we must to delete it now to avoid future problems...";
                                        	@SED_noreplace@ -e "/TASK \${task_num} /d" \${workerdir}/.gs_process_\${master_pid} > .tmp_prcs_\${task_num} && @CAT_noreplace@ .tmp_prcs_\${task_num} > \${workerdir}/.gs_process_\${master_pid} && @RM_noreplace@ -f .tmp_prcs_\${task_num};
                                	fi
                                        exit \$PROCESS_NACT;
                                else
                                        #echo "[GridSs-worker] Checking state of task \$task_num. Process ACTIVE";
                                        exit 0;
                                fi
                        else
                                #echo "[GridSs-worker] Checking state of task \$task_num. Task not found in process file \${workerdir}/.gs_process_\${master_pid}. Sending value \$PROCESS_NFND";
                                exit \$PROCESS_NFND;
                        fi
                else
                        #echo "[GridSs-worker] Checking state of task \$task_num. File \${workerdir}/.gs_process_\${master_pid} not found.Sending value \$PROCESS_NFND";
                        exit \$PROCESS_FNFND;
                fi
        else
                echo Queue active task checking not supported by this version;
                exit 0;
        fi
elif [ \$mode -eq \$TEST ]; then
        hostname=\`hostname\`;
        #echo " [GridSs-worker] Remote shell conection to \$hostname successfully finished. Testing Callback conecctions..."
        \${PWD}/workerGS_script.sh \${gs_exec_env} \$workerdir \$NOTIFY \$3 \$4 \$5 \$6 \$7 \$TEST_TASK \$TEST_NOTIFY \$TEST_ENDS;
        result=\$?;
	if [ \$result -ne 0 ]; then
                echo ERROR checking callbacks;
        fi
        exit \$result;

else
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
        if [ x\$queue = x"none" ]; then
               	exec \${PWD}/workerGS_script.sh \${gs_exec_env} \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
        { /bin/cat <<EOF
        if [ x\$queue = x"none" ]; then
               	exec \${PWD}/workerGS_script.sh \$@ 2>/dev/null 1>/dev/null &
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
		childpid=\$!
               	result=\$?
               	if [ \$result -eq 0 ]; then
                       	@CAT_noreplace@ >> \${workerdir}/.gs_process_\${master_pid} <<EOT
TASK \$task_num \$childpid
EOT
			result=\$?
                	if [ \$result -ne 0 ]; then
				target_pid=\`@PS_noreplace@ --ppid \${childpid} -o pid=\`;
				kill -9 \$childpid \$target_pid;
				echo ERROR: Starting worker script. Writing processes file;
				exit -1;
			fi
               	else
                       	echo ERROR: Starting worker script;
                   	exit -1;
                fi

        else
        {
               	@CAT_noreplace@ > /scratch/tmp/submit_\${task_num}_\${master_pid} <<EOT
#!@BASH_noreplace@
# @ job_type = serial
# @ job_name = GS.\${task_num}.\${master_pid}
# @ output = GS.\${task_num}_\${master_pid}.out
# @ error = GS.\${task_num}_\${master_pid}.err
# @ initialdir = \$PWD
# @ class = \${queue}
# @ wall_clock_limit= \${wc_limit}
# @ queue

	cd \$PWD;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF

	./workerGS_script.sh \${gs_exec_env} \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;

EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
        { /bin/cat <<EOF

	./workerGS_script.sh \$@ && @RM_noreplace@ -f GS.\${task_num}_\${master_pid}.out GS.\${task_num}_\${master_pid}.err;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
        { /bin/cat <<EOF
	# The task identifier deletion from the processes file must be added
EOT
                } | exec $queue_submit_com /scratch/tmp/submit_\${task_num}_\${master_pid} 1>subtsk\${task_num}_\${master_pid}.out 2>subtsk\${task_num}_\${master_pid}.err
                if [ \$? -eq 0 ]; then
                        tsk_err=\` @CAT_noreplace@ subtsk\${task_num}_\${master_pid}.err\`
                        case "\$tsk_err" in
                                "sbatch: Submitted batch job "* )
					#Getting queue system task identifier
                                        #childpid=...
                                        result=0;;
                                * )
					
                                        result=1;;
                        esac
                else
                        result=1;
                fi
                if [ \$result -eq 0 ]; then
			#writing task identifier in the process
                        @CAT_noreplace@ >> \${workerdir}/.gs_process_\${master_pid} <<EOT
TASK \$task_num \$childpid
EOT

                        @RM_noreplace@ -f subtsk\${task_num}_\${master_pid}.out subtsk\${task_num}_\${master_pid}.err /scratch/tmp/submit_${task_num}_${master_pid};
                else
                        # Notify error in submit
			@MV_noreplace@ /scratch/tmp/submit_\${task_num}_\${master_pid} .
                        mode=\$2;
                        if [ \$mode -eq \$EXECUTE ]; then

                                nstagein=\$7;
                                cleanup_files_num=\$8;
                                let "t=8+(2*\$nstagein)+\$cleanup_files_num";

                                j=0;
                                for i in \$@; do
                                        args[\$j]=\$i;
                                        j=\$((\$j+1));
                                done


                                master=\${args[\$j-4]};
                                task_num=\${args[\$t]};

                        elif [ \$mode -eq \$CLEANUP ]; then
                                master=\$7;
                                task_num=\$8;
			fi
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
if [ x"GRID" == x"MN_SCRATCH" ]; then
        { /bin/cat <<EOF
                        exec ./workerGS_script.sh \${gs_exec_env} \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num \$QSUB_ERR \$TASK_ENDS
                        #exit \$?; 
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
else
{ /bin/cat <<EOF
                        exec ./workerGS_script.sh \$1 2 \$3 \$4 \$5 \$6 \$master \$task_num \$QSUB_ERR \$TASK_ENDS 
			#exit \$?;
EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
fi
{ /bin/cat <<EOF
                fi
        fi
fi


EOF
        } | /usr/bin/sed -e "s/_noreplace//g" >> workerGS.sh.in;
}
