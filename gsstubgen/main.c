/* Copyright 2002-2007 Barcelona Supercomputing Center (www.bsc.es)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "backend.h"
#include "semantic.h"
#include "backendlib.h"

static const char cvs_file_id[]="$Id: main.c,v 1.5 2004/11/19 10:00:35 perez Exp $";



extern FILE *yyin;
int yyparse (void);
char *filename;


static void print_help(char *command)
{       
        printf("Usage: %s [-splxn] [-P<perl-binary>] <input file>\n", command);
        printf("   The output files are generated according to the input filename.\n");
        printf("   -s  Generate swig files.\n");
        printf("   -p  Generate perl files.\n");
        printf("   -l  Generate shell script files.\n");
        printf("   -j  Generate java files.\n");
        printf("   -P  Specify which perl interpreter to use (default /usr/bin/perl).\n");
        printf("   -Q  Specify the directory where the GRID superscalar perl modules are installed.\n");
        printf("   -x  Generate XML formatted file.\n");
        printf("   -n  Do not generate backups of generated files.\n");
}       


int main(int argc, char **argv)
{
	char *filename;
	int create_swig = 0;
	int create_perl = 0;
	int create_xml = 0;
	int create_shell = 0;
	int create_java = 0;
	int opt;
	int correct_args = 1;
	
	while ((opt = getopt(argc, argv, "spljP:Q:xn")) != -1) {
		switch ((char)opt) {
			case 's':
				create_swig = 1;
				break;
			case 'p':
				create_perl = 1;
				break;
			case 'l':
				create_shell = 1;
				break;
			case 'j':
				create_java = 1;
				break;
			case 'P':
				set_perl_binary(optarg);
			case 'Q':
				set_perl_extension_dir(optarg);
				break;
			case 'x':
				create_xml = 1;
			case 'n':
				set_no_backups();
				break;
			default:
				correct_args = 0;
				break;
		}
	}
	filename = argv[optind];
	
	if (!filename || !correct_args) {
		print_help(argv[0]);
		exit(1);
	}
	set_filename(filename);
	yyin = fopen(filename, "r");
	if (yyin == NULL) {
		fprintf(stderr, "Error: file not found.\n");
		exit(2);
	}
	yyparse();
	if (can_generate()) {
		if (create_swig) {
			generate_swig_prolog();
			generate_swig_body();
			generate_swig_epilogue();
		}
		if (create_perl) {
			generate_perl_prolog();
			generate_perl_body();
			generate_perl_epilogue();
		}
		if (create_xml) {
			generate_xml_prolog();
			generate_xml_body();
			generate_xml_epilogue();
		}
		if (create_shell)
		{
			generate_shell_prolog();
			generate_shell_body();
			generate_shell_epilogue();
		}
		else if (create_java)
		{
			generate_java_prolog();
			generate_java_body();
			generate_java_epilogue();

			generate_java_constraints_prolog();
			generate_java_constraints_body();
			generate_java_constraints_epilogue();
		}
		else
		{
			generate_prolog();
			generate_body();
			generate_epilogue();
	/*		
			generate_c_constraints_prolog();
			generate_c_constraints_body();
			generate_c_constraints_epilogue();
	*/	
		}	
	} else {
		printf("No code generated.\n");
		return 1;
	}
	return 0;
}


