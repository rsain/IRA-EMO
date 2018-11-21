//  NSGAII_Settings.java 
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

import java.util.HashMap;

import jmetal.metaheuristics.nsgaII.*;
import jmetal.operators.crossover.Crossover;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.Mutation;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.ProblemFactory;
import jmetal.core.*;
import jmetal.encodings.solutionType.ArrayRealAndBinarySolutionType;
import jmetal.experiments.Settings;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.JMException;

/**
 * Settings class of algorithm NSGA-II (real encoding)
 */
public class NSGAII_Settings extends Settings {
  public int populationSize_                 ; 
  public int maxEvaluations_                 ;
  public double mutationProbability_         ;
  public double crossoverProbability_        ;
  public double mutationDistributionIndex_   ;
  public double crossoverDistributionIndex_  ;
  // For binary variables (if any)
  public double mutationProbabilityBinary_         ;
  public double crossoverProbabilityBinary_        ;
  
  /**
   * Constructor
   * @throws JMException 
   */
  public NSGAII_Settings(String problem) throws JMException {
    super(problem) ;
    
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
    populationSize_              = 50   ; 
    maxEvaluations_              = 10000 ;
    if (problem_.getSolutionType().getClass().equals(ArrayRealAndBinarySolutionType.class)){
        mutationProbability_         = 1.0/problem_.getNumberOfVariables() ;
        mutationProbabilityBinary_   = 1.0/((ArrayRealAndBinarySolutionType) problem_.getSolutionType()).getNumberOfBinaryVariables();
        crossoverProbability_        = 0.9   ;   
        crossoverProbabilityBinary_  = 0.9   ;
        mutationDistributionIndex_   = 2.0  ; 
        crossoverDistributionIndex_  = 25.0   ; 
    }
    else{
        mutationProbability_         = 1.0/problem_.getNumberOfVariables() ;
        crossoverProbability_        = 0.9   ;     
        mutationDistributionIndex_   = 20.0  ; 
        crossoverDistributionIndex_  = 20.0  ; 
    }
//    mutationProbability_         = 1.0/problem_.getNumberOfVariables() ;
//    crossoverProbability_        = 0.9   ;
//    mutationDistributionIndex_   = 20.0  ;
//    crossoverDistributionIndex_  = 20.0  ;
  } // NSGAII_Settings

  
  /**
   * Configure NSGAII with user-defined parameter settings
   * @return A NSGAII algorithm object
   * @throws jmetal.util.JMException
   */
  public Algorithm configure() throws JMException {
    Algorithm algorithm ;
    Selection  selection ;
    Crossover  crossover ;
    Mutation   mutation  ;

    HashMap  parameters ; // Operator parameters

    QualityIndicator indicators ;
    
    // Creating the algorithm. There are two choices: NSGAII and its steady-
    // state variant ssNSGAII
    algorithm = new NSGAII(problem_) ;
    //algorithm = new ssNSGAII(problem_) ;
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize",populationSize_);
    algorithm.setInputParameter("maxEvaluations",maxEvaluations_);

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

    // Selection Operator 
    parameters = null ;
    selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;     

    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);
    algorithm.addOperator("selection",selection);
    
    
    /* Deleted since jMetal 4.2
   // Creating the indicator object
   if ((paretoFrontFile_!=null) && (!paretoFrontFile_.equals(""))) {
      indicators = new QualityIndicator(problem_, paretoFrontFile_);
      algorithm.setInputParameter("indicators", indicators) ;  
   } // if
   */
    return algorithm ;
  } // configure
} // NSGAII_Settings
