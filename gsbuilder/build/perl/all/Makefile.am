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

BASENAME=PACKAGE
MODULE_NAME = PACKAGE
bin_SCRIPTS = PACKAGE-worker.pl
noinst_LTLIBRARIES = PACKAGE.la
noinst_DATA = PACKAGE.so
if USE_CLASSADS
classads_sources = PACKAGE_constraints_wrapper.cc PACKAGE_constraints.cc
else
classads_sources =
endif
PACKAGE_la_SOURCES = PACKAGE_wrap.c PACKAGE-stubs.c $(classads_sources)
PACKAGE_la_LDFLAGS = -rpath $(PERLEXT_DESTDIR)/auto/PACKAGE -module -avoid-version
PACKAGE_la_LIBADD = @ac_cv_use_gs_prefix@/lib/libGS-master.so
PACKAGE_la_DEPENDENCIES = @ac_cv_use_gs_prefix@/lib/libGS-master.so

PERL_MODULES = PACKAGE.pm

GSSTUBGEN_FLAGS = -n -s -p -P@PERL@



PACKAGE_wrap.c PACKAGE.pm: PACKAGE.i
	$(SWIG) -I@ac_cv_use_gs_prefix@/share/gridsuperscalar/swig -perl5 -module PACKAGE PACKAGE.i

PACKAGE-worker.pl PACKAGE-stubs.c PACKAGE.h PACKAGE.i: PACKAGE.idl
	$(GSSTUBGEN) $(GSSTUBGEN_FLAGS) $(STUBGEN_EXTRA_FLAGS) PACKAGE.idl

PACKAGE.so:
	-ln -s .libs/$(BASENAME).so .

install-exec-local: $(BUILT_SOURCES) $(noinst_LTLIBRARIES) PACKAGE-worker.pl
	@$(NORMAL_INSTALL)
	$(mkinstalldirs) $(PERLEXT_DESTDIR)/auto/PACKAGE
	echo "$(LIBTOOL)  --mode=install $(INSTALL) PACKAGE.la $(PERLEXT_DESTDIR)/auto/PACKAGE/PACKAGE.la"; \
	$(LIBTOOL)  --mode=install $(INSTALL) PACKAGE.la $(PERLEXT_DESTDIR)/auto/PACKAGE/PACKAGE.la;
	for p in $(PERL_MODULES) ; do \
		echo $(INSTALL)  $$p $(PERLEXT_DESTDIR)/$$p ; \
		$(INSTALL)  $$p $(PERLEXT_DESTDIR)/$$p ; \
	done


#PERLEXT_DESTDIR = @PERLEXT_DESTDIR@
