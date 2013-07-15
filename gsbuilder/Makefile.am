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

SUBDIRS = etc
ACLOCAL_AMFLAGS=-I m4

bin_SCRIPTS = gsbuild gsjavabuild gsperlbuild
nobase_dist_pkgdata_DATA = \
	build/c/master/configure.in \
	build/c/master/Makefile.am \
	build/c/worker/configure.in \
	build/c/worker/Makefile.am \
	build/c/all/configure.in \
	build/c/all/Makefile.am \
	build/java/master/configure.in \
	build/java/master/Makefile.am \
	build/java/worker/configure.in \
	build/java/worker/Makefile.am \
	build/java/all/configure.in \
	build/java/all/Makefile.am \
	build/perl/master/configure.in \
	build/perl/master/Makefile.am \
	build/perl/master/acinclude.m4 \
	build/perl/worker/configure.in \
	build/perl/worker/Makefile.am \
	build/perl/worker/acinclude.m4 \
	build/perl/all/configure.in \
        build/perl/all/Makefile.am \
        build/perl/all/acinclude.m4 \
	build/scripts/config_script.sh.in \
	build/scripts/build_worker_scripts.sh

BUILT_SOURCES = gsbuild gsjavabuild gsperlbuild build/scripts/build_worker_scripts.sh

install-exec-local: build/scripts/build_worker_scripts.sh
	$(mkinstalldirs) $(pkgdatadir)/build/c/master
	$(mkinstalldirs) $(pkgdatadir)/build/c/worker
	$(mkinstalldirs) $(pkgdatadir)/build/c/all
	$(mkinstalldirs) $(pkgdatadir)/build/java/master
	$(mkinstalldirs) $(pkgdatadir)/build/java/worker
	$(mkinstalldirs) $(pkgdatadir)/build/java/all
	$(mkinstalldirs) $(pkgdatadir)/build/perl/master
	$(mkinstalldirs) $(pkgdatadir)/build/perl/worker
	$(mkinstalldirs) $(pkgdatadir)/build/perl/all
	$(mkinstalldirs) $(pkgdatadir)/build/scripts
