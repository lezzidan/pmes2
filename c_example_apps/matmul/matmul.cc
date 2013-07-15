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

#include <time.h>
#include <stdio.h>
#include <errno.h>
#include "matmul.h"
#include "GS_compss.h"

#include "parameters.h"


int main(int argc, char **argv)
{
	long int t = time(NULL);
	char f1[15], f2[15], f3[15], file[15];
	FILE *fp;
	char c;
	int i, j, k, ii, jj;

	//GS_On();
	//GS_On(PRJ_FILE, RES_FILE, HIST_FILE, MASTER_DIR, APPNAME);
	GS_On(PRJ_FILE, RES_FILE, MASTER_DIR, APPNAME);
	for (c = 'A'; c < 'D'; c++) {
		for (i = 0; i < MSIZE; i++) {
			for (j = 0; j < MSIZE; j++) {
				sprintf(file, "%c.%d.%d", c, i, j);
				fp = fopen(file, "w");
				for (ii = 0; ii < BSIZE; ii++) {
					for (jj = 0; jj < BSIZE; jj++) {
						if (c == 'C') {
							fprintf(fp, "%lf ", 0.0);
						} else {
							fprintf(fp, "%lf ", 2.0);
						}
					}
					fprintf(fp, " \n");
				}
				fclose(fp);
			}
		}
	}

	for (i = 0; i < MSIZE; i++) {
		for (j = 0; j < MSIZE; j++) {
			for (k = 0; k < MSIZE; k++) {
				sprintf(f1, "A.%d.%d", i, k);
				sprintf(f2, "B.%d.%d", k, j);
				sprintf(f3, "C.%d.%d", i, j);
				matmul(f1, f2, f3);
			}
		}
	}

	GS_Off(0);

	printf("Total time:\n");
	t = time(NULL) - t;
	printf("%li Hours, %li Minutes, %li Seconds\n", t / 3600, (t % 3600) / 60,
			 (t % 3600) % 60);

	return 0;
}
