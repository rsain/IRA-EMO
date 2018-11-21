//  MOEAD_Settings.java 
//
//  Authors:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.experiments.settings;

import jmetal.metaheuristics.moead.*;
import jmetal.metaheuristics.moead.MOEADSBX.FunctionType;

import java.util.HashMap;
import java.util.Properties;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.encodings.solutionType.ArrayRealAndBinarySolutionType;
import jmetal.experiments.Settings;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.ProblemFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.JMException;

/**
 * Settings class of algorithm MOEA/D
 */
public class MOEADSBX_Settings extends Settings {
	public double CR_ ;
  public double F_  ;
	  
  public int populationSize_ ;
  public int maxEvaluations_ ;
 
  public double mutationProbability_         ;
  public double crossoverProbability_        ;
  public double mutationDistributionIndex_   ;
  public double crossoverDistributionIndex_  ;    

  public String weightsDirectory_  ;

  public FunctionType functionType_;
  
  public int numberOfThreads  ; // Parameter used by the pMOEAD version
  public String moeadVersion  ;
  
  // For binary variables (if any)
  public double mutationProbabilityBinary_         ;
  public double crossoverProbabilityBinary_        ;
  
  /**
   * Constructor
   */
  public MOEADSBX_Settings(String problem) {
    super(problem);
    
    Object [] problemParams = {"Real"};
    String soltype;
    if (problem.compareTo("AuxiliaryServicesProblem")== 0)
    {
        soltype = "ArrayRealAndBinary";
        problemParams = new Object[]{soltype, 3};
    }
    try {
	    problem_ = (new ProblemFactory()).getProblem(problemName_, problemParams);
    } catch (JMException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    }      

    // Default settings
    CR_ = 1.0 ;
    F_  = 0.5 ;
    populationSize_ = 50;
    maxEvaluations_ = 10000;
    functionType_ = FunctionType.TCHE;
   
    if (problem_.getSolutionType().getClass().equals(ArrayRealAndBinarySolutionType.class)){
        mutationProbability_         = 1.0/problem_.getNumberOfVariables() ;
        mutationProbabilityBinary_   = 1.0/((ArrayRealAndBinarySolutionType) problem_.getSolutionType()).getNumberOfBinaryVariables();
        crossoverProbability_        = 0.9   ;   
        crossoverProbabilityBinary_  = 0.9   ;
        mutationDistributionIndex_   = 25.0  ; 
        crossoverDistributionIndex_  = 2.0   ; 
    }
    else{
        mutationProbability_         = 1.0/problem_.getNumberOfVariables() ;
        crossoverProbability_        = 0.9   ;     
        mutationDistributionIndex_   = 20.0  ; 
        crossoverDistributionIndex_  = 20.0  ; 
    }
    
    // Directory with the files containing the weight vectors used in 
    // Q. Zhang,  W. Liu,  and H Li, The Performance of a New Version of MOEA/D 
    // on CEC09 Unconstrained MOP Test Instances Working Report CES-491, School 
    // of CS & EE, University of Essex, 02/2009.
    // http://dces.essex.ac.uk/staff/qzhang/MOEAcompetition/CEC09final/code/ZhangMOEADcode/moead0305.rar
    weightsDirectory_ =  "data" ;

    numberOfThreads = 1 ; // Parameter used by the pMOEAD version
    moeadVersion = "MOEAD" ; // or "pMOEAD"
  } // MOEAD_Settings

  /**
   * Configure the algorithm with the specified parameter settings
   * @return an algorithm object
   * @throws jmetal.util.JMException
   */
  public Algorithm configure() throws JMException {
    Algorithm algorithm;
    Operator crossover;
    Operator mutation;
    Operator selection;

    QualityIndicator indicators ;

    HashMap  parameters ; // Operator parameters

    // Creating the problem
    if (moeadVersion.compareTo("MOEAD") == 0 )
      algorithm = new MOEADSBX(problem_, functionType_);
    else { // pMOEAD
      algorithm = new pMOEAD(problem_); 
      algorithm.setInputParameter("numberOfThreads", numberOfThreads);
    } // else
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize", populationSize_);
    algorithm.setInputParameter("maxEvaluations", maxEvaluations_);
    algorithm.setInputParameter("weightsDirectory", weightsDirectory_) ;
    algorithm.setInputParameter("functionType", functionType_) ;
    
    // Mutation and Crossover:    
    if (problem_.getSolutionType().getClass().equals(ArrayRealAndBinarySolutionType.class)){ // Mutation and Crossover for Real and Binary codification
        parameters = new HashMap() ;
        parameters.put("realCrossoverProbability", crossoverProbability_) ;
        parameters.put("binaryCrossoverProbability", crossoverProbabilityBinary_) ;
        parameters.put("distributionIndex", crossoverDistributionIndex_) ; 
        crossover = CrossoverFactory.getCrossoverOperator("SBXTwoPointsCrossover", parameters);  
        parameters = new HashMap() ; 
        parameters.put("realMutationProbability", mutationProbability_) ;
        parameters.put("binaryMutationProbability", mutationProbabilityBinary_) ;
        parameters.put("distributionIndex", mutationDistributionIndex_) ;
        mutation = MutationFactory.getMutationOperator("PolynomialBitFlipMutation", parameters);  
    }
    else{ // Mutation and Crossover for Real codification
        parameters = new HashMap() ;
        parameters.put("probability", crossoverProbability_) ;
        parameters.put("distributionIndex", crossoverDistributionIndex_) ; 
        crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);  
        parameters = new HashMap() ; 
        parameters.put("probability", mutationProbability_) ;
        parameters.put("distributionIndex", mutationDistributionIndex_) ;
        mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);  
    }
    
    // Mutation and Crossover for Real codification
    parameters = new HashMap() ;
    parameters.put("CR", CR_) ;
    parameters.put("F", F_) ;
 
    // Selection Operator 
    parameters = null ;
    selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;     

    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    algorithm.addOperator("selection",selection);
    
    return algorithm;
  } // configure
} // MOEAD_Settings
