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
#include <limits.h>
#include <string.h>
#include <ctype.h>
#include <libxml/tree.h>
#include "backend.h"
#include "datatypes.h"
#include "semantic.h"
#include "backendlib.h"

static const char cvs_file_id[]="$Id: xml-backend.c,v 1.2 2004/02/04 09:33:51 perez Exp $";


#if 0 /* Unsupported on AIX */
#ifdef DEBUG
#define debug_printf(fmt, args...) printf(fmt, ## args)
#else
#define debug_printf(args...) {}
#endif
#endif

#ifndef DEBUG
#define DEBUG 0
#endif
#define debug_printf if (DEBUG) printf


#define ARGS_OFFSET 2


static char xmlName[PATH_MAX];
static xmlDocPtr doc = NULL;
static xmlNodePtr root = NULL;

static char *xml_return_types[] = { "void", "character", "character", "boolean", "any", "short",
	"long","longlong", "integer", "float", "double", "error", "string", "string", "error" };
static char *xml_arg_types[] = { "error", "character", "character", "boolean", "any", "short",
	"long", "longlong", "integer", "float", "double", "file", "string", "string", "error" };
static char *xml_directions[] = {"in", "out", "inout"};


void generate_xml_prolog()
{
	strncpy(xmlName, get_filename_base(), PATH_MAX);
	strncat(xmlName, ".xml", PATH_MAX);
	rename_if_clash(xmlName);
	
	doc = xmlNewDoc(BAD_CAST XML_DEFAULT_VERSION);
	if (doc == NULL) {
		fprintf(stderr, "Error: cannot create an xml document.\n");
		exit(1);
	}
	
	root = xmlNewNode(NULL, BAD_CAST "interface");
	xmlDocSetRootElement(doc, root);
#if 0
	xmlCreateIntSubset(doc, BAD_CAST "interface", NULL, BAD_CAST "GSIDL.dtd");
#endif
	xmlNewProp(root, BAD_CAST "name" , BAD_CAST get_main_interface()->name);
}


void generate_xml_epilogue(void)
{
	xmlSaveFormatFileEnc(xmlName, doc, "UTF-8", 1);
}


static void generate_prototype(function *current_function)
{
	xmlNodePtr func_node;
	argument *arg;
	xmlNodePtr arg_node;
	
	func_node = xmlNewChild(root, NULL, BAD_CAST "function", NULL);
	xmlNewProp(func_node, BAD_CAST "name", BAD_CAST current_function->name);
	xmlNewProp(func_node, BAD_CAST "type", BAD_CAST xml_return_types[current_function->return_type]);
	
	arg = current_function->first_argument;
	while (arg != NULL) {
		arg_node = xmlNewChild(func_node, NULL, BAD_CAST "argument", NULL);
		xmlNewProp(arg_node, BAD_CAST "name", BAD_CAST arg->name);
		xmlNewProp(arg_node, BAD_CAST "direction", BAD_CAST xml_directions[arg->dir]);
		xmlNewProp(arg_node, BAD_CAST "type", BAD_CAST xml_arg_types[arg->type]);
		arg = arg->next_argument;
	}
}


void generate_xml_body(void)
{
	function *current_function;
	
	current_function = get_first_function();
	while (current_function != NULL) {
		generate_prototype(current_function);		
		current_function = current_function->next_function;
	}
}

