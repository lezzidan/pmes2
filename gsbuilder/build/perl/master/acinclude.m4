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

AC_DEFUN([AC_CHECK_PERL],
	[
		AC_MSG_CHECKING([where to look for perl])
		AC_ARG_WITH([perl],
			[  --with-perl=[path]      build perl module using perl binary installed in path)],
			[
				ac_cv_use_perl=$withval
				if test x"$ac_cv_use_perl" != x"yes" ; then
					PERL_PATH=$ac_cv_use_perl
				else
					PERL_PATH=$PATH
				fi
				
			],
			[
				PERL_PATH=$PATH
			]
		)
		AC_MSG_RESULT([$PERL_PATH])
		
		AC_PATH_PROG(PERL, perl, none, $PERL_PATH)
		if test x"$PERL" != x"none" ; then
			DO_PERL_EXT=perlext
			AC_SUBST(DO_PERL_EXT)
			AC_MSG_CHECKING([perl arch dir])
			perl_archdir=$($PERL -MConfig -e 'print $Config{archlib}')
			AC_MSG_RESULT([$perl_archdir])
			CPPFLAGS="$CPPFLAGS -DPERL_POLLUTE -D_GNU_SOURCE -I$perl_archdir/CORE"
			AC_SUBST(perl_archdir)
		else
			AC_MSG_ERROR([perl cannot be found])
		fi
	]
)

AC_DEFUN([AC_CHECK_GSMASTER_PERL],
	[
		AC_ARG_WITH([gssperl-dir],
			[  --with-gssperl-dir=[path]   specify the path of the GRID superscalar perl module)],
			[
				AC_MSG_CHECKING([the location of the GRID superscalar perl module])
				ac_cv_use_gssperl_dir=$withval
				AC_MSG_RESULT($ac_cv_use_gssperl_dir)
				STUBGEN_EXTRA_FLAGS=-Q$ac_cv_use_gssperl_dir
				AC_SUBST(STUBGEN_EXTRA_FLAGS)
			]
		)
		
		AC_MSG_CHECKING([if the GSMaster perl module is installed])
		if test x"$ac_cv_use_gssperl_dir" != x"" ; then
			echo "use lib '$ac_cv_use_gssperl_dir';" > conftest.pl
		fi
		echo "use GSMaster; 1;" >> conftest.pl
		$PERL conftest.pl > /dev/null 2>&1
		if test $? = 0 ; then
			AC_MSG_RESULT([yes])
		else
			AC_MSG_ERROR([could not find GSMaster perl module])
		fi
		rm conftest.pl
		PERLEXT_DESTDIR=$ac_cv_use_gssperl_dir
                AC_SUBST(PERLEXT_DESTDIR)
	]
)



