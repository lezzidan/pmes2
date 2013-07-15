#include <stdio.h>
#include <errno.h>
#include "Simple.h"


void increment(char *filename)
{
  int counterValue=0;

   FILE *fp;

   fp = fopen(filename, "r");
   fscanf (fp,"%d",&counterValue);
   fclose(fp);

   counterValue++;

   fp = fopen(filename, "w");
   fprintf(fp, "%d", counterValue);
   fclose(fp);

}

