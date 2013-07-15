#include <time.h>
#include <stdio.h>
#include <errno.h>

#include "GS_compss.h"
#include "Simple.h"

int main(int argc, char **argv)
{

	long int t = time(NULL);
	FILE *fp;
	char filename[15]="counter.txt";
	int initialValue = 1;
	int finalValue=0;

	GS_On(PRJ_FILE, RES_FILE, MASTER_DIR, APPNAME);

	fp = fopen(filename, "w");
	
	fprintf(fp, "%d", initialValue);
	printf("Initial Counter Value is: %d \n", initialValue);
	fclose(fp);

	increment(filename);

	GS_Off(0);

	fp = fopen(filename, "r");
        fscanf (fp,"%d",&finalValue);
        printf("Final Counter Value is: %d \n", finalValue);
        fclose(fp);
	
	printf("Total time:\n");
	t = time(NULL) - t;
	printf("%li Hours, %li Minutes, %li Seconds\n", t / 3600, (t % 3600) / 60,
			 (t % 3600) % 60);

	return 0;
}

