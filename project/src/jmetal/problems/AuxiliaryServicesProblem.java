package jmetal.problems;

import jmetal.core.*;
import jmetal.encodings.solutionType.ArrayRealAndBinarySolutionType;
import jmetal.util.JMException;
import jmetal.encodings.variable.ArrayReal;
import jmetal.encodings.variable.Binary;

import java.io.*;

/**
 * @author Ana Belen Ruiz - abruiz@uma.es - Universidad de Málaga
 * 
 *  This class represents the problem modelled in Ana Belen Ruiz's thesis, for the optimization 
 *  of the operation of the auxiliary services in a power plant. In this multiobjective optimization problem is formulated as
 *  	MAX energy savings (MWh)
 *  	MIN economic investment (million €)
 *  	MAX IRR (%)
 *  In the problem we have:
 *  	- 1 continuous variable per transformer.
 *  	- 1 continuous variable per group of drives (of MV or LV).
 *  	- 2 binary variables per group of drives (of MV or LV).
 *  and, in the case study considered, there are:
 *  	- 1 HV-MV transformer
 *  	- 2 MV-LV transformer
 *  	- 8 MV groups of drives
 *  	- 2 LV groups of drives
 *  Therefore, the resulting multiobjective optimization problem has the following variables:
 *  - 13 continuous variables:
 *   HV-MV transformer 		-> Q-TMV-1
 *   MV-LV transformer 1	-> Q-TLV-1
 *   MV-LV transformer 2	-> Q-TLV-2
 *   MV group 1		-> Q-GMV-1
 *   MV group 2		-> Q-GMV-2
 *   MV group 3		-> Q-GMV-3
 *   MV group 4		-> Q-GMV-4
 *   MV group 5		-> Q-GMV-5
 *   MV group 6		-> Q-GMV-6
 *   MV group 7		-> Q-GMV-7
 *   MV group 8		-> Q-GMV-8
 *   LV group 1		-> Q-GLV-1
 *   LV group 2		-> Q-GLV-2
 *   - 20 binary variables associated to the groups of drives:
 *   MV group 1		-> M-GMV-1 and V-GMV-1
 *   MV group 2		-> M-GMV-2 and V-GMV-2
 *   MV group 3		-> M-GMV-3 and V-GMV-3
 *   MV group 4		-> M-GMV-4 and V-GMV-4
 *   MV group 5		-> M-GMV-5 and V-GMV-5
 *   MV group 6		-> M-GMV-6 and V-GMV-6
 *   MV group 7		-> M-GMV-7 and V-GMV-7
 *   MV group 8		-> M-GMV-8 and V-GMV-8
 *   LV group 1		-> M-GLV-1 and V-GLV-1
 *   LV group 2		-> M-GLV-2 and V-GLV-2
 *   
 *   Real Variables Bounds	
 *   0 <= Q-TMV-1 <= 	56302
 *   0 <= Q-TLV-1 <=	1211
 *   0 <= Q-TLV-2 <=	1211
 *   0 <= Q-GMV-1 <=	1127
 *   0 <= Q-GMV-2 <=	1150
 *   0 <= Q-GMV-3 <=	421
 *   0 <= Q-GMV-4 <=	3653
 *   0 <= Q-GMV-5 <=	3547
 *   0 <= Q-GMV-6 <=	520
 *   0 <= Q-GMV-7 <=	2634
 *   0 <= Q-GMV-8 <=	1055
 *   0 <= Q-GLV-1 <=	222
 *   0 <= Q-GLV-2 <=  	105
 *
 *   Binary Variables Bounds
 *   0 <= M-GMV-1 <=	1
 *   0 <= M-GMV-2 <=	1
 *   0 <= M-GMV-3 <=	1
 *   0 <= M-GMV-4 <=	1
 *   0 <= M-GMV-5 <=	1
 *   0 <= M-GMV-6 <=	1
 *   0 <= M-GMV-7 <=	1
 *   0 <= M-GMV-8 <=	1
 *   0 <= M-GLV-1 <=	1
 *   0 <= M-GLV-2 <=	1
 *   0 <= V-GMV-1 <=	1
 *   0 <= V-GMV-2 <=	1
 *   0 <= V-GMV-3 <=	0
 *   0 <= V-GMV-4 <=	1
 *   0 <= V-GMV-5 <=	1
 *   0 <= V-GMV-6 <=	0
 *   0 <= V-GMV-7 <=	1
 *   0 <= V-GMV-8 <=	1
 *   0 <= V-GLV-1 <=	1
 *   0 <= V-GLV-2 <=	1
 *   
 *   
 *   CONDITION 1:
 *   
 *   The variables V-GMV-i and Q-GMV-i (for i = 1,...8) are related. The following condition must be checked
 *   in the code before executing the black box simulator:
 *   	FOR i = 1, ..., 8
 *   		IF (V-GMV-i == 1)
 *   			Q-GMV-i = 0;
 *   		ENDIF
 *   	ENDFOR
 *   In the same way, the variables V-GLV-i and Q-GLV-i (for i = 1, 2) are related and the following condition
 *   must be checked in the code before executing the black box simulator:
 *    	FOR i = 1, 2		
 *    		IF (V-GLV-i == 1)
 *   			Q-GLV-i = 0;
 *   		ENDIF
 *   	ENDFOR
 *   
 *   CONDITION 2:
 *   
 *   After calling the black blox simulator, the constraint violations must be normalized:
 *   	V_QTMV = V_QTMV / (upper bound - lower bound);
 *   	V_QTLV-1 = V_QTLV-1 / (upper_bound - lower_bound);
 *     	V_QTLV-2 = V_QTLV-2 / (upper_bound - lower_bound);
 *
 *  	FOR i = 1, ..., 8
 *  		V_QGMV-i = V_QGMV-i / (upper_bound - lower_bound);
 *  	ENDFOR
 *
 *  	FOR i = 1, 2
 *  		V_QGLV-i = V_QGLV-i / (upper_bound - lower_bound);
 *  	ENDFOR
 *     
 *   The final constraint violation is:
 *   	OverallContraintViolation= V_QTMV + V_QTLV-1 + V_QTLV-2 +
 *   							   V_QGMV-1 + ..... + V_QGMV-8 + V_QGLV-1 + V_QGLV-2 
 *      (only positive values must be considered, as the constraints are V_Q* <= 0)

 *
 * */
public class AuxiliaryServicesProblem extends Problem{
        private static String FOLDER = "C:\\AuxiliaryServicesProblem\\";

        private String decision_var_filepath = FOLDER + "decision_vars.dat";
	private String exogenous_var_filepath =FOLDER + "variables_exogenas.xml";
	private String simulator_path= FOLDER + "Caja_negra.exe";
        private String result_filepath = FOLDER + "objectives_constraints.dat";
	private int[] binaryLowerLimit;
	private int[] binaryUpperLimit;
	
	/** 
	  * Constructor.
	  * Creates a default instance of Ana Belen's thesis problem with 33 variables
	  * (13 continuous decision variables and 20 binary decision variables - 1 TMV, 2 TLV, 8 GMV and 2 GLV)
	  * @param solutionType The solution type must "ArrayRealAndBinary".
	  */
        
          public AuxiliaryServicesProblem(String solutionType, int objectivesNumber) throws ClassNotFoundException {
            this(solutionType); 
          }
          
	  public AuxiliaryServicesProblem(String solutionType) throws ClassNotFoundException {
		  super();
		  numberOfVariables_  =		13; // 13 continuous variables (by default) in this order: Q-TMV-1, Q-TLV-1, Q-TLV-2, Q-GMV-1, ..., Q-GMV-8, Q-GLV-1, Q-GLV-2 
		  numberOfObjectives_ =  	3;
		  numberOfConstraints_=  	13;
		  problemName_        = 	"AuxiliaryServicesProblem";
		  
		  lowerLimit_ = new double[numberOfVariables_];
		  upperLimit_ = new double[numberOfVariables_]; 
		  binaryLowerLimit = new int[20];
		  binaryUpperLimit = new int[20];
                  if (solutionType.compareTo("ArrayRealAndBinary") == 0)
                      solutionType_ = 	new ArrayRealAndBinarySolutionType(this, numberOfVariables_, 20) ; // 20 binary variables in this order: M-GMV-1, ..., M-GMV-8, M-GLV-1, M-GLV-2, V-GMV-1, ..., V-GMV-8, V-GLV-1, V-GLV-2
                  // Establishes the optimization of each objective function:
                  objective_opt_ = new int[numberOfObjectives_];
                  objective_opt_[0] = -1;
                  objective_opt_[1] = 1;
                  objective_opt_[2] = -1;
		  // Establishes upper and lower limits for the variables
		  lowerLimit_[0] = 0.0;
		  upperLimit_[0] = 56302.0;
		  lowerLimit_[1] = 0.0;
		  upperLimit_[1] = 1211.0;
		  lowerLimit_[2] = 0.0;
		  upperLimit_[2] = 1211.0;
		  lowerLimit_[3] = 0.0;
		  upperLimit_[3] = 1127.0;
		  lowerLimit_[4] = 0.0;
		  upperLimit_[4] = 1150.0;
		  lowerLimit_[5] = 0.0;
		  upperLimit_[5] = 421.0;
		  lowerLimit_[6] = 0.0;
		  upperLimit_[6] = 3653.0;
		  lowerLimit_[7] = 0.0;
		  upperLimit_[7] = 3547.0;
		  lowerLimit_[8] = 0.0;
		  upperLimit_[8] = 520.0;
		  lowerLimit_[9] = 0.0;
		  upperLimit_[9] = 2634.0;
		  lowerLimit_[10] = 0.0;
		  upperLimit_[10] = 1055.0;
		  lowerLimit_[11] = 0.0;
		  upperLimit_[11] = 222.0;
		  lowerLimit_[12] = 0.0;
		  upperLimit_[12] = 105.0;
		  for(int i= 0; i < 20; i++)
		  {
			  binaryLowerLimit[i] = 0;
			  binaryUpperLimit[i] = 1;
		  }
		  binaryUpperLimit[12] = 0;
		  binaryUpperLimit[15] = 0;			  
	  }
	    
	  /** 
	   * Evaluates a solution.
	   * @param solution The solution to evaluate.
	   * @throws JMException 
	   */
	  public void evaluate(Solution solution) throws JMException {

		  Variable [] variables_ = solution.getDecisionVariables();
		  ArrayReal real_variables = (ArrayReal) variables_[0];
		  Binary binary_variables = (Binary) variables_[1];
		  boolean values_changed = false;
		  
		  // Check that binary variables are within their lower and upper limits:
		  int aux = 0;
		  for(int i = 0; i < binary_variables.getNumberOfBits(); i++)
		  {
			  if(binary_variables.getIth(i))
				  aux = 1;
			  else
				  aux = 0;
			  if(aux < binaryLowerLimit[i] || aux > binaryUpperLimit[i])
			  {
				  binary_variables.setIth(i, !binary_variables.getIth(i));	
				  values_changed = true;
			  }
		  }
		  		  
		  // Check CONDITION 1:
		  int index = 3; // Index of continuous variables associated to groups of drives. The first one is at position 3.
		  for(int i = 10; i <= 19; i++)
		  {
			  if(binary_variables.getIth(i) == true)//IF (V-GMV-i or V-GLV-i  == 1 )  
			  {
				  real_variables.setValue(index, 0.0); //Q-GMV-i or Q-GLV-i = 0;
				  values_changed = true;
			  }
			  index++;
		  }
		  
		  // If a decision variable value has changed, it must be updated in solution:
		  if(values_changed)
		  {
			  variables_[0] = real_variables;
			  variables_[1] = binary_variables;
			  solution.setDecisionVariables(variables_);
		  }
			  		  
		  // Write the decision variables values in the file:
		  this.write_decision_variables(real_variables,binary_variables);
                  
                  // WARNING: BE SURE THAT THE FOLDER AuxiliaryServicesProblem EXISTS IN C: AND CONTAINS ALL THE FILES NEEDED:
                  // Caja_negra.exe -> black-box simulator
                  // decision_vars.dat -> file with the decision variables
                  // variables_exogenas.xml -> file with exogenous variables (not needed to be updated)
                  // true -> boolean variable for internal use in the black-box simulator
                  String comando = "cmd /c cd c:/AuxiliaryServicesProblem & Caja_negra.exe \"decision_vars.dat\" \"variables_exogenas.xml\" \"true\"";
                  try{                        
                        Process tr = Runtime.getRuntime().exec(comando);
                        tr.waitFor();                        
                    } catch (Exception ex) {
                        System.out.println ("An error ocurred while calling the simulator Caja_negra.exe" + ex.getMessage());
                        return;
                    } 
		  
		  // Finally, read the constraints and objective function values:
		  this.read_results(solution);
		  	  
	  } // evaluate
	  
	  
	  /** 
	   * Read the constraint violation and objective function values in the file 'objectives_constraints.dat' created
           * after executing Caja_negra.exe
	   * @param solution The solution for which Caja_negra.exe has retrieved these values
	   */
	  private void read_results(Solution solution) {

		  FileReader r;
		  BufferedReader b;
			       
		  try {
			  r = new FileReader(result_filepath);
			  b = new BufferedReader(r);
			  String aux = "";
			  double total = 0.0;
			  int number = 0;
			  
			  // Read the objective values, which are in the first three lines:
			  // Objective 1: 
			  aux = b.readLine();
			  solution.setObjective(0, objective_opt_[0] * Double.parseDouble(aux) / 1000); // in MWh
			  // Objective 2: 
			  aux = b.readLine();
			  solution.setObjective(1, objective_opt_[1] * Double.parseDouble(aux) / 1000000); // in million €
			  // Objective 3: 
			  aux = b.readLine();
			  solution.setObjective(2, objective_opt_[2] * Double.parseDouble(aux) * 100); // in %
			  
			  // Read the constraints violation values, which are in the last lines: 
			  total = 0.0;
			  number = 0;
			  for (int i = 0; i < numberOfConstraints_ && aux  != null; i++)
			  {
				  aux = b.readLine();
				  // Only positive constraints violation values are accumulated:
				  if( aux != null && Double.parseDouble(aux) > 0 )
                                  {
                                      // CONDITION 2: Values must be normalized 
                                      total +=  Double.parseDouble(aux)/ (upperLimit_[i] - lowerLimit_[i]); 
                                      number++;
                                  }
				  else if (aux == null) 
                                      break;	  
			  }
			  solution.setOverallConstraintViolation(total);    
			  solution.setNumberOfViolatedConstraint(number); 
                          b.close();
                          r.close();
		  }
		  catch (FileNotFoundException e) {System.out.println(e.getMessage());} 
		  catch (IOException e) {System.out.println(e.getMessage());}
	
	  }

	/** 
	   * Write the values of the decision variables in the file decision_vars.dat needed for executing "Caja_negra.exe"
	   * @param solution The solution with the decision variables
	   */

	private void write_decision_variables(ArrayReal real_variables_,Binary binary_variables_) {
		
		File f = new File(decision_var_filepath);
		
		try{
			
			FileWriter w = new FileWriter(f);
			
			for(int i = 0; i < real_variables_.getLength(); i++)
			{
				w.write(real_variables_.getValue(i) + "\n");
			}
			
			for(int i = 0; i < binary_variables_.getNumberOfBits(); i++)
			{
				if(binary_variables_.getIth(i))
					w.write("1.00\n");
				else
					w.write("0.00\n");
			}
			
			w.close();

		} catch(Exception e){System.out.println("Error while writting variables in the local .tex file: " + e.getMessage());}
		
	}
	
}
