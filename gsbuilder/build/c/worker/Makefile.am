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

bin_PROGRAMS = PACKAGE-worker

PACKAGE_worker_SOURCES = \
	PACKAGE-worker.c \
	PACKAGE-functions.c

PACKAGE_worker_LDFLAGS = 


GSSTUBGEN_FLAGS = -n


PACKAGE-worker.c PACKAGE.h: PACKAGE.idl
	$(GSSTUBGEN) $(GSSTUBGEN_FLAGS) PACKAGE.idl


