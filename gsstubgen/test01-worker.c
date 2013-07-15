/* This file has been autogenerated from 'test01.idl'. */
/* Generator component: $Id: c-backend.c,v 1.11 2004/11/19 10:00:34 perez Exp $ */
/* CHANGES TO THIS FILE WILL BE LOST */

static const char gs_generator[]="$Id: c-backend.c,v 1.11 2004/11/19 10:00:34 perez Exp $";

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>
#include <gs_base64.h>
#include <GS_worker.h>
#include "test01.h"


int main(int argc, char **argv)
{
   enum operationCode opCod;

   if(argc < 7)
   {
      printf("ERROR: Wrong arguments list passed to the worker\n");
      exit(1);
   }
   opCod = (enum operationCode)atoi(argv[2]);

   IniWorker(argc, argv);

   switch(opCod)
   {
      case SubstOp:
         {
            char *buff_newCFG;
            double seed;
            double newCFG;

            buff_newCFG = (char *)malloc(atoi(getenv("GS_GENLENGTH"))+1);


            seed = strtod(argv[2], NULL);
            Subst(argv[2], seed, &newCFG);

            sprintf(buff_newCFG, "%.20g", newCFG);
            free(buff_newCFG);


         }
         break;
      case DimemOp:
         {



            Dimem(argv[2], argv[2], argv[2]);



         }
         break;
      case PostOp:
         {



            Post(argv[2], argv[2], argv[2]);



         }
         break;
   }

   EndWorker(gs_result, argc, argv);
   return 0;
}
