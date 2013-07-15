#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <string.h>
#include "GS_compss.h"

 #define PATH_SEPARATOR ';' /* define it to be ':' on Solaris */
 #define USER_CLASSPATH "." /* where Prog.class is */

JNIEnv *env;
jobject jobjIT;
jclass clsITimpl;
JavaVM * jvm;

//int execute_compss(JNIEnv *env, jobject jobjIT, jclass clsITimpl);

JNIEnv* create_vm(JavaVM ** jvm, char *prj_file, char *res_file, char *hist_file, char *master_dir, char *appname) {
	
    JNIEnv *env;
    JavaVMInitArgs vm_args;
    JavaVMOption options[16];
	char *it_home;
	it_home = strdup(getenv("IT_HOME"));
	
	options[0].optionString = (char *)malloc(sizeof(char)*(strlen("-Dlog4j.configuration=file://")+strlen(it_home)+strlen("log/it-log4j")+1));
	sprintf(options[0].optionString, "-Dlog4j.configuration=file://%s/log/it-log4j", it_home);
    	options[1].optionString = strdup("-Djava.security.manager");
	options[2].optionString = (char *)malloc(sizeof(char)*(strlen("-Djava.security.policy=")+strlen(it_home)+strlen("/.java.policy")+1));
	sprintf(options[2].optionString, "-Djava.security.policy=%s/.java.policy", it_home);

	options[3].optionString = strdup(get_classpath()); //Path to the java source code
	options[4].optionString = (char 
*)malloc(sizeof(char)*(strlen("-Dit.deployment=")+strlen(it_home)+strlen("/xml/deployment/ITdeployment.xml")+1));
	sprintf(options[4].optionString, "-Dit.deployment=%s/xml/deployment/ITdeployment.xml", it_home);
	options[5].optionString = strdup("-Dit.gat.broker.adaptor=sshtrilead");
	options[6].optionString = strdup("-Dit.gat.file.adaptor=sshtrilead");
	options[7].optionString = (char *)malloc(sizeof(char)*(strlen("-Dit.project.file=")+strlen(prj_file)+1));
	sprintf(options[7].optionString, "-Dit.project.file=%s", prj_file);
	options[8].optionString = (char *)malloc(sizeof(char)*(strlen("-Dit.resources.file=")+strlen(res_file)+1));
	sprintf(options[8].optionString, "-Dit.resources.file=%s", res_file);
	options[9].optionString = (char *)malloc(sizeof(char)*(strlen(appname)+strlen("-Dit.constraints.file=")+strlen(master_dir)+14));
	sprintf(options[9].optionString, "-Dit.constraints.file=%s/%s-constraints", master_dir, appname);
	options[10].optionString = strdup("-Dit.lang=C");
	options[11].optionString = (char *)malloc(sizeof(char)*(strlen("-Dit.appName=")+strlen(appname)+1));
	sprintf(options[11].optionString, "-Dit.appName=%s", appname);										    
	options[12].optionString=strdup("-Dlog4j.debug=false");
	options[13].optionString=strdup("-Dit.presched=true");
	options[14].optionString=strdup("-Dit.locations=true");
	options[15].optionString = (char *)malloc(sizeof(char)*(strlen("-Dit.hist.file=")+strlen(hist_file)+1));
        sprintf(options[15].optionString, "-Dit.hist.file=%s", hist_file);

	vm_args.version = JNI_VERSION_1_4; //JDK version. This indicates version 1.6
    vm_args.nOptions = 16;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = 1;
    
    int ret = JNI_CreateJavaVM(jvm, (void**)&env, &vm_args);
    if(ret < 0)
    	printf("\nUnable to Launch JVM\n");   	
	return env;
}

char *get_classpath()
{
	char *cp;
	char cp_it[1024];
	char cp_gat[1024];
	char cp_proactive[1024];
	char cp_log4j[1024];
	char cp_trove[1024];
	char *it_home;
	char *gat_location;
	char *proactive_home;
	
	it_home = strdup(getenv("IT_HOME"));
	gat_location = strdup(getenv("GAT_LOCATION"));
	proactive_home = strdup(getenv("PROACTIVE_HOME"));
	
	sprintf(cp_it, "%s/gridunawareapps/lib/guapp.jar:%s/integratedtoolkit/build/:%s/integratedtoolkit/lib/xalan/xml-apis.jar:"
			"%s/integratedtoolkit/lib/xalan/xalan.jar:%s/integratedtoolkit/lib/IT.jar:%s/xml/:"
			"%s/xml/adl:%s/build/integratedtoolkit/components/impl/", it_home,it_home, it_home, it_home, it_home, it_home, it_home, it_home);
	
	sprintf(cp_gat, "%s/lib/GAT-engine.jar:%s/lib/GAT-API.jar", gat_location, gat_location);
	
	sprintf(cp_proactive, "%s/dist/lib/fractal-adl.jar:%s/dist/lib/ProActive.jar:%s/dist/lib/fractal.jar:%s/dist/lib/ow_deployment_scheduling.jar",
						proactive_home, proactive_home, proactive_home, proactive_home);
	
	sprintf(cp_log4j, "%s/log/:%s/log/it-log4j:%s/integratedtoolkit/lib/log4j/log4j-1.2.15.jar", it_home, it_home, it_home);

        sprintf(cp_trove, "%s/integratedtoolkit/lib/trove/trove-3.0.0a5.jar", it_home);    
	
	cp = (char *)malloc(sizeof(char)*(strlen(cp_it)+strlen(cp_gat)+strlen(cp_proactive)+strlen(cp_log4j)+strlen(cp_trove))+20);
	
	sprintf(cp, "-Djava.class.path=%s:%s:%s:%s:%s", cp_log4j, cp_it, cp_gat, cp_proactive,cp_trove);
	fprintf(stderr, "%s\n", cp);
	
	return cp;
}


void GS_On(char *prj_file, char *res_file, char *hist_file, char *master_dir, char *appname)
{
	//JNIEnv *env;
	
	env = create_vm(&jvm, prj_file, res_file, hist_file, master_dir, appname);
	if (env == NULL)
	{
	printf("Error creating the JVM\n");
		exit(1);
	}
	
	clsITimpl = NULL;
	
	jmethodID midITImplConst = NULL;

	jmethodID midStartIT = NULL;
     
//    jobject jobjIT = NULL;
    
    //Obtaining Classes
    clsITimpl = env->FindClass("integratedtoolkit/api/impl/IntegratedToolkitImpl");
	if (env->ExceptionOccurred()) {
		env->ExceptionDescribe();
		printf("Error looking for the IntegratedToolkitImpl class\n\n");
	}
	
	if(clsITimpl != NULL)
	{
		//Get constructor ID for IntegratedToolkitImpl
		midITImplConst = env->GetMethodID(clsITimpl, "<init>", "()V");	
		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
		printf("Error looking for the init method\n\n");
			exit(0);
		}
		
		midStartIT = env->GetMethodID(clsITimpl, "startIT", "()V");

		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
		printf("Error looking for the startIT method\n\n");
			exit(0);
		}
	}
	else
    {
    	printf("\nUnable to find the requested class\n");    	
    }	

	
	/************************************************************************/
	/* Now we will call the functions using the their method IDs			*/
	/************************************************************************/
		
	if (midITImplConst!=NULL)
	{
		if(clsITimpl != NULL && midITImplConst != NULL)
		{
						
			//Creating the Object of IT.
			jobjIT = env->NewObject(clsITimpl, midITImplConst);
			if (env->ExceptionOccurred()) {
				env->ExceptionDescribe();
				//exit(0);
			}
		}
		printf("\nGoing to Call startIT\n");

		if(jobjIT != NULL && midStartIT != NULL) {
			env->CallVoidMethod(jobjIT,midStartIT); //Calling the method and passing IT Object as parameter
		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
			exit(0);
		}
		else
			printf("\nApp started\n");  	
		}
		else
			printf("\nUnable to find the startit method\n");  

		}
	else
		printf("\nUnable to find the requested method\n");    
	
  //	execute_compss(env, jobjIT, clsITimpl);

	

}

void GS_Off(int mode)
{
	//Release resources.
	jmethodID midStopIT = NULL;

	if(clsITimpl != NULL)
	{
		midStopIT = env->GetMethodID(clsITimpl, "stopIT", "(Z)V");
		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
			exit(0);
		}
		printf("Stopping app\n");
	}
	else
    {
    	printf("\nUnable to find the requested class\n");    	
    }
	if(jobjIT != NULL && midStopIT != NULL) {
		env->CallVoidMethod(jobjIT,midStopIT, "TRUE"); //Calling the method and passing IT Object as parameter
		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
			exit(0);
		}
		else
			printf("\nApp stopped\n");  	
	}
	else
		printf("\nUnable to find the stopIT method\n"); 
	
	
	//int n = jvm->DestroyJavaVM();
}


