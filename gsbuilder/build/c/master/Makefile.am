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

bin_PROGRAMS = PACKAGE

DEFINE = -DAPPNAME=\"PACKAGE\" \
	-DPRJ_FILE=\"$(PRJ_FILE)\" \
	-DRES_FILE=\"$(RES_FILE)\" \
	-DHIST_FILE=\"$(HIST_FILE)\"\
	-DMASTER_DIR=\"$(MASTER_DIR)\" \
	-DIT_HOME=\"$(IT_HOME)\" \
	-DGAT_LOCATION=\"$(GAT_LOCATION)\" \
	-DPROACTIVE_HOME=\"$(PROACTIVE_HOME)\"


AM_CPPFLAGS = -I$(JAVA_HOME)/include -I$(GS_HOME)/include $(DEFINE)
AM_LDFLAGS = -L$(GS_HOME)/lib
AM_CFLAGS = -I$(JAVA_HOME)/include -I$(GS_HOME)/include $(DEFINE)


PACKAGE_SOURCES = empty.cc \
	PACKAGE-stubs.cc \
	PACKAGE.cc

PACKAGE_LDFLAGS = -lcbindings -L$(JAVA_HOME)/jre/lib/ARCH/SUBFLDR -ljvm -ldl


GSSTUBGEN_FLAGS =


PACKAGE-stubs.cc PACKAGE.h: PACKAGE.idl
	$(GSSTUBGEN) $(GSSTUBGEN_FLAGS) PACKAGE.idl


