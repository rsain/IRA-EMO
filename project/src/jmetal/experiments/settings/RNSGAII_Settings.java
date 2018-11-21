//  RNSGAII_Settings.java 
//
//  Authors:
//       Rubén Saborido Infantes <rsain@uma.es>
//
//  Copyright (c) 2013 Rubén Saborido Infantes
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

import jmetal.metaheuristics.RnsgaII.RNSGAII;
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
import jmetal.util.JMException;
import jmetal.util.ReferencePoint;

/**
 * Settings class of algorithm RNSGAII (real encoding)
 */
public class RNSGAII_Settings extends Settings {
  public int populationSize_                 ; 
  public int generations_                 ;
  public double mutationProbability_         ;
  public double crossoverProbability_        ;
  public double mutationDistributionIndex_   ;
  public double crossoverDistributionIndex_  ;
  // For binary variables (if any)
  public double mutationProbabilityBinary_         ;
  public double crossoverProbabilityBinary_        ;
  public double mutationDistributionIndexBinary_   ;
  public double crossoverDistributionIndexBinary_  ;
  public int requiredEvaluations_            ;  
  public ReferencePoint[] referencePoints_	 ;  
  public String folderForOutputFiles_		 ;
  public boolean estimateObjectivesBounds_   ; 
  public boolean normalization_              ;
  public double[] lowerBounds_				 ;
  public double[] upperBounds_				 ;
  public double epsilon_	 ;				 ;
  
  /**
   * Constructor
   * @throws JMException 
   */
  public RNSGAII_Settings(String problem) throws JMException {
    super(problem) ;    
    String soltype;
    
    if (problem.compareTo("AuxiliaryServicesProblem")== 0)
        soltype = "ArrayRealAndBinary";
    else
        soltype = "Real";
    
    Object [] problemParams = {soltype};
    problem_ = (new ProblemFactory()).getProblem(problemName_, problemParams);        
    
    // Default settings
    populationSize_              = 100   ; 
    generations_              = 200; 
    estimateObjectivesBounds_	 = true  ; 
    normalization_               = true  ;
    epsilon_ 			 = 0.0045;
    
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
    
    folderForOutputFiles_        = ".";
    requiredEvaluations_          = 0;
  } // RNSGAII_Settings
  
  /**
   * Constructor
   * @throws JMException 
   */
  public RNSGAII_Settings(String problem, int objectivesNumber) throws JMException {
    super(problem) ;    
    String soltype;
        if (problem.compareTo("AuxiliaryServicesProblem")== 0)
            soltype = "ArrayRealAndBinary";
        else
            soltype = "Real";
    Object [] problemParams = {soltype, objectivesNumber};
    problem_ = (new ProblemFactory()).getProblem(problemName_, problemParams);        
    
    // Default settings
    populationSize_              = 100   ; 
    generations_              = 200 ;
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
    
    estimateObjectivesBounds_	 = true  ; 
    normalization_               = true  ;
    epsilon_ 			 = 0.0045;      
    folderForOutputFiles_        = ".";
    requiredEvaluations_          = 0;
  } // RNSGAII_Settings

  
  /**
   * Configure RNSGAII with user-defined parameter settings
   * @return A RNSGAII algorithm object
   * @throws jmetal.util.JMException
   */
  public Algorithm configure() throws JMException {
    Algorithm algorithm ;    
    Crossover  crossover ;
    Mutation   mutation  ;
    Selection  selection ;

    HashMap  parameters ; // Operator parameters  
        
    // Creating the algorithm.
    algorithm = new RNSGAII(problem_) ;    
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize",populationSize_);
    algorithm.setInputParameter("generations",generations_);           
    algorithm.setInputParameter("referencePoints",referencePoints_);
    algorithm.setInputParameter("folderForOutputFiles", folderForOutputFiles_);
    algorithm.setInputParameter("estimateObjectivesBounds", estimateObjectivesBounds_);
    algorithm.setInputParameter("lowerBounds", lowerBounds_);
    algorithm.setInputParameter("upperBounds", upperBounds_);
    algorithm.setInputParameter("normalization", normalization_);
    algorithm.setInputParameter("epsilon", epsilon_);
    
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
    //RNSGAII uses a pesonalized BinaryTournament2 operator, using preference distance (not crowding distance)

    // Add the operators to the algorithm
    algorithm.addOperator("crossover",crossover);
    algorithm.addOperator("mutation",mutation);    
    algorithm.addOperator("selection",selection);
   
    return algorithm ;
  } // configure
} // RNSGAII_Settings