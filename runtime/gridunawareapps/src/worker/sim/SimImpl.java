
package worker.sim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SimImpl {

    // Debug
	private static final boolean debug = true;

    	public static void simBin(String progModel,
                                             String typesModel,
                                             String funcModel,
                                             String resultName,
                                             String resSpec,
                                             String res1,
                                             String res2,
                                             String res3,
                                             String simBinary,
					     String running_mode,
					     String commandArgs) throws IOException, InterruptedException, Exception{

            if (debug) {
			System.out.println("\nRunning Sim with parameters:");
			System.out.println("\t- Binary: " + simBinary);
			System.out.println("\t- Model *.prog: " + progModel);
			System.out.println("\t- Model *.types: " + typesModel);
			System.out.println("\t- Model *.func: " + funcModel);
                        System.out.println("\t- Result spec: " + resSpec);
                        System.out.println("\t- Result 1: " + res1);
                        System.out.println("\t- Result 2: " + res2);
                        System.out.println("\t- Result 3: " + res3);
			System.out.println("\t- Running Mode: " + running_mode);
                        System.out.println("\t- Command Line Arguments: " + commandArgs);
		}


                String cmd = null;

                if(!commandArgs.equals("none")){
		   if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){
                    cmd = simBinary + " " +progModel + " "
                                          + typesModel+" "+funcModel+" "+commandArgs;
		   }
		   else{
		    cmd = simBinary + " " +running_mode+" "+progModel + " "
                                          + typesModel+" "+funcModel+" "+commandArgs;
		   }
                }
                else{
		   if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){
                    cmd = simBinary + " " +progModel + " "
                                          + typesModel+" "+funcModel;
		   }
		   else{
		    cmd = simBinary + " " +running_mode+" "+progModel + " "
                                          + typesModel+" "+funcModel;
		   }
                }

		if(debug){
		 System.out.println("\nSim Cmd -> "+cmd);
		 System.out.println(" ");
		}

		Process simProc = Runtime.getRuntime().exec(cmd);

                byte[] b = new byte[1024];
		int read;

                // Check the proper finalization of the process
		int exitValue = simProc.waitFor();
		if (exitValue != 0) {
			BufferedInputStream bisErr = new BufferedInputStream(simProc.getErrorStream());
			BufferedOutputStream bosErr = new BufferedOutputStream(new FileOutputStream(resultName + ".err"));

			while ((read = bisErr.read(b)) >= 0) {
				bosErr.write(b, 0, read);
			}

			bisErr.close();
			bosErr.close();

			throw new Exception("Error executing Sim, exit value is: " + exitValue);
		}
		
	       //Renaming the simulator results to COMPSs out files
	       result_renaming(running_mode, resultName, resSpec, res1, res2, res3);
      }




    	public static void simBin(String progModel,
                                             String typesModel,
                                             String resultName,
                                             String resSpec,
                                             String res1,
                                             String res2,
                                             String res3,
                                             String simBinary,
					     String running_mode,
                                             String commandArgs) throws IOException, InterruptedException, Exception{

            if (debug) {
			System.out.println("\nRunning Sim with parameters:");
			System.out.println("\t- Binary: " + simBinary);
			System.out.println("\t- Model *.prog: " + progModel);
			System.out.println("\t- Model *.types: " + typesModel);
                        System.out.println("\t- Result spec: " + resSpec);
                        System.out.println("\t- Result 1: " + res1);
                        System.out.println("\t- Result 2: " + res2);
                        System.out.println("\t- Result 3: " + res3);
			System.out.println("\t- Running Mode: " + running_mode);
                        System.out.println("\t- Command Line Arguments: " + commandArgs);

		}
		
		String cmd = null;


		if(!commandArgs.equals("none")){
                   if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){
                    cmd = simBinary + " " +progModel + " "
                                          + typesModel+" "+commandArgs;
                   }
                   else{
                    cmd = simBinary + " " +running_mode+" "+progModel + " "
                                          + typesModel+" "+commandArgs;
                   }
                }
                else{
                   if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){
                    cmd = simBinary + " " +progModel + " "
                                          + typesModel;
                   }
                   else{
                    cmd = simBinary + " " +running_mode+" "+progModel + " "
                                          + typesModel;
                   }
                }


		if(debug)
                System.out.println("\nSim Cmd -> "+cmd);
		System.out.println(" ");

		Process simProc = Runtime.getRuntime().exec(cmd);

                byte[] b = new byte[1024];
		int read;

                // Check the proper finalization of the process
		int exitValue = simProc.waitFor();
		
		if (exitValue != 0) {
			BufferedInputStream bisErr = new BufferedInputStream(simProc.getErrorStream());
			BufferedOutputStream bosErr = new BufferedOutputStream(new FileOutputStream(resultName + ".err"));

			while ((read = bisErr.read(b)) >= 0) {
				bosErr.write(b, 0, read);
			}

			bisErr.close();
			bosErr.close();

			throw new Exception("Error executing Sim, exit value is: " + exitValue);
		}

		//Renaming the simulator results to COMPSs out files
		result_renaming(running_mode, resultName, resSpec, res1, res2, res3);  
      }


 private static void result_renaming(String running_mode, String resultName, String resSpec, String res1, String res2, String res3){

                    if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){

                       // Stochastic Run Mode Simulation File Renaming
		       File resSpecFile = new File(resultName+".spec");
                       File resSpecRen = new File(resSpec);
                       resSpecFile.renameTo(resSpecRen);

                       File resCFile = new File(resultName+".C.out");
                       File resCRen = new File(res1);
                       resCFile.renameTo(resCRen);

                       File resEFile = new File(resultName+".E.out");
                       File resERen = new File(res2);
                       resEFile.renameTo(resERen);

                       File resVFile = new File(resultName+".V.out");
                       File resVRen = new File(res3);
                       resVFile.renameTo(resVRen);
		    }

                    if(running_mode.equals("-r=TS") || running_mode.equals("--running_mode=TS")){

		       //TS Run Mode Simulation File Renaming
		       File resSpecFile = new File(resultName+".spec");
                       File resSpecRen = new File(resSpec);
                       resSpecFile.renameTo(resSpecRen);

                       File resStsFile = new File(resultName+".sts");
                       File resStsRen = new File(res1);
                       resStsFile.renameTo(resStsRen);

                       File resNodesFile = new File(resultName+".nodes");
                       File resNodesRen = new File(res2);
                       resNodesFile.renameTo(resNodesRen);

                       File resCtmcFile = new File(resultName+".ctmc");
                       File resCtmcRen = new File(res3);
                       resCtmcFile.renameTo(resCtmcRen);
		    }

                    if(running_mode.equals("-r=SBML") || running_mode.equals("--running_mode=SBML")){

		       //SBML Run Mode Simulation File Renaming
		       File resSpecFile = new File(resultName+".spec");
                       File resSpecRen = new File(resSpec);
                       resSpecFile.renameTo(resSpecRen);

                       File resOrDotFile = new File(resultName+".or.dot");
                       File resOrDotRen = new File(res1);
                       resOrDotFile.renameTo(resOrDotRen);

                       File resReDotFile = new File(resultName+".re.dot");
                       File resReDotRen = new File(res2);
                       resReDotFile.renameTo(resReDotRen);

                       File resXmlFile = new File(resultName+".xml");
                       File resXmlRen = new File(res3);
                       resXmlFile.renameTo(resXmlRen);

                    }
 }

}

