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

/*------------------------------------------------------------------------*/
/*																		  */
/*                                                                        */
/*      BSC - Barcelona Supercomputing Center                             */
/*                                                                        */
/*      Author: Daniele Lezzi (daniele.lezzi@bsc.es)                      */
/*      Modified by:                                                      */
/*                                                                        */
/*------------------------------------------------------------------------*/

#ifndef GS_COMPSS_H
#define GS_COMPSS_H

#include <jni.h>

/*** ==============> API FUNCTIONS <================= ***/
void GS_On(char *prj_file, char *res_file, char *hist_file, char *master_dir, char *appname);
void GS_Off(int code);
JNIEnv* create_vm(JavaVM ** jvm, char *prj_file, char *res_file, char *hist_file, char *master_dir, char *appname);
char *get_classpath();

#endif /* GS_COMPSS_H */

