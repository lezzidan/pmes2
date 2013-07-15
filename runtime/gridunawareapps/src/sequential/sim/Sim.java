package sequential.sim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import worker.sim.SimImpl;

public class Sim {

  private static boolean debug;

 public static void main(String args[]) throws Exception {
     	
		/* Parameters:
		 * - 0: debug
		 * - 1: Sim binary location
		 * - 2: models file name
		*/

		debug = Boolean.parseBoolean(args[0]);
		String simBinary = args[1];
		String modelsFileName = args[2];
		
		BufferedReader bf = new BufferedReader(new FileReader(modelsFileName));

                String line = null;
		String result_spec, result1, result2, result3 = null;
		List<String> models = new ArrayList();
		String previousLine = null;
               
		print_header();

		if (debug) {
		   System.out.println("Parameters: ");
		   System.out.println("- Debug: Enabled");
		   System.out.println("- Sim binary: " + simBinary);
                   System.out.println("- Models File: " + modelsFileName);
		   System.out.println(" ");
		}

		System.out.println("Processing Models:\n");

		while ((line = bf.readLine())!=null) {

		 //If empty line readed on models file...
		 if(line.length() == 0){

		   //Submitting the model to the Cloud      
		   if(models.size() == 4){

		   //Splitting the files model path string using a forward slash as delimiter
                   StringTokenizer st = new StringTokenizer(models.get(2), "/");
                   String token = null;

                   while (st.hasMoreElements()){
                    token = st.nextToken();
                   }
 
		    //Modelname will be the base name of result files, for instance: modelName.prog.spec
		    String modelName = token.substring(0,(token.length()-5));
                    modelName = modelName+".prog";

		    
                    //Parsing the simulator run mode
                    st = new StringTokenizer(previousLine, " ");
                    String running_mode = st.nextToken();

                    String commandArgs = null;

                    //Parsing command line arguments
                    if(st.hasMoreElements()){
                     commandArgs = st.nextToken()+" ";

                     while (st.hasMoreElements()){
                      commandArgs += st.nextToken()+" ";
                     }
                    }

		    //If empty command line arguments -> string none
		    if(commandArgs == null) commandArgs = "none";

		    if(debug){
                     System.out.println("\nRunning Mode -> "+running_mode);
                     System.out.println("Command Line Arguments -> "+commandArgs);
		    }

		    //Default Initialization
		    result_spec = modelName+".spec";
                    result1 = modelName+".E.out";
                    result2 = modelName+".C.out";
                    result3 = modelName+".V.out";

		    //Setting the results file names
                    if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){

                      result_spec = modelName+".spec";
                      result1 = modelName+".E.out";
                      result2 = modelName+".C.out";
                      result3 = modelName+".V.out";
                    }

                    if(running_mode.equals("-r=TS") || running_mode.equals("--running_mode=TS")){

                      result_spec = modelName+".spec";
                      result1 = modelName+".sts";
                      result2 = modelName+".nodes";
                      result3 = modelName+".ctmc";
                    }

                    if(running_mode.equals("-r=SBML") || running_mode.equals("--running_mode=SBML")){

                      result_spec = modelName+".spec";
                      result1 = modelName+".or.dot";
                      result2 = modelName+".re.dot";
                      result3 = modelName+".xml";
                    }

		    SimImpl.simBin(models.get(0), models.get(1), models.get(2), modelName ,result_spec, result1, result2, result3, simBinary, running_mode, commandArgs);
		   }
		   else{

		    //Splitting the files model path string using a forward slash as delimiter
                    StringTokenizer st = new StringTokenizer(models.get(1), "/");
                    String token = null;

                    while (st.hasMoreElements()){
                     token = st.nextToken();
                    }

		    String modelName = token.substring(0,(token.length()-6));
                    modelName = modelName+".prog";

		    //Parsing the simulator run mode
		    st = new StringTokenizer(previousLine, " ");
                    String running_mode = st.nextToken();

		    String commandArgs = null;

		    //Parsing command line arguments
		    if(st.hasMoreElements()){
		     commandArgs = st.nextToken()+" ";

                     while (st.hasMoreElements()){
                      commandArgs += st.nextToken()+" ";
                     }
		    }
			
	            //If empty command line arguments -> string none
		    if(commandArgs == null) commandArgs = "none";

		    if(debug){
                     System.out.println("\nRunning Mode -> "+running_mode);
                     System.out.println("Command Line Arguments -> "+commandArgs);
                    }

                    //Default Initialization
                    result_spec = modelName+".spec";
                    result1 = modelName+".E.out";
                    result2 = modelName+".C.out";
                    result3 = modelName+".V.out";

		    //Setting the results file names
		    if(running_mode.equals("-r=STOCHASTIC") || running_mode.equals("--running_mode=STOCHASTIC")){

		      result_spec = modelName+".spec";
                      result1 = modelName+".E.out";
                      result2 = modelName+".C.out";
                      result3 = modelName+".V.out";
                    }

		    if(running_mode.equals("-r=TS") || running_mode.equals("--running_mode=TS")){
			
		      result_spec = modelName+".spec";
		      result1 = modelName+".sts";
                      result2 = modelName+".nodes";
                      result3 = modelName+".ctmc";
		    }

		    if(running_mode.equals("-r=SBML") || running_mode.equals("--running_mode=SBML")){

		      result_spec = modelName+".spec";
                      result1 = modelName+".or.dot";
                      result2 = modelName+".re.dot";
                      result3 = modelName+".xml";
                    }

		    SimImpl.simBin(models.get(0), models.get(1), modelName, result_spec ,result1, result2, result3, simBinary, running_mode, commandArgs);
		   }

		   System.out.println(" ");

                   //Cleaning model arguments in order to read a new line
		   models.clear();
		 }
	         else{
		     //If is not a comment
                     if(!(line.substring(0,1)).equals("#")){
		       //Adding arguments of model -> *.prog, *. types, *.func
		       models.add(line);
		       previousLine = line;
		       System.out.println("Processing -> "+line);
		     }
		 } 
	    }//End while

}

 private static void  print_header(){
  System.out.println("BlenX Model Simulator:\n");
 } 
}
