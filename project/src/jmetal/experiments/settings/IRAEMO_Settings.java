//  IRAEMO_Settings.java 
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
import jmetal.metaheuristics.iraemo.IRAEMO;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.AchievementScalarizingFunction;
import jmetal.util.JMException;
import jmetal.util.ReferencePoint;

/**
 * Settings class of algorithm GWASFGA (real encoding)
 */
public class IRAEMO_Settings extends Settings {
  public int populationSize_                 ; 
  public int generations_                    ;
  // For real variables (if any)
  public double mutationProbability_         ;
  public double crossoverProbability_        ;
  public double mutationDistributionIndex_   ;
  public double crossoverDistributionIndex_  ;
  // For binary variables (if any)
  public double mutationProbabilityBinary_         ;
  public double crossoverProbabilityBinary_        ;
  public double mutationDistributionIndexBinary_   ;
  public double crossoverDistributionIndexBinary_  ;
  
  QualityIndicator convergenceIndicator_     ;
  public int requiredEvaluations_            ;
  public int numberOfWeights_                ;
  //public boolean allowRepetitions_           ;
  public boolean normalization_              ;
  public String weightsDirectory_            ;
  public String weightsFileName_             ;
  public AchievementScalarizingFunction asfAspirationLevel_ ; 
  public AchievementScalarizingFunction asfReservationLevel_ ; 
  public ReferencePoint aspirationLevel_		 ;
  public ReferencePoint reservationLevel_		 ;
  public boolean estimatePoints_             ; 
  public String folderForOutputFiles_	     ;
  
  /**
   * Constructor
   * @throws JMException 
   */
  public IRAEMO_Settings(String problem) throws JMException {
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
    generations_                 = 200 ;
      
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
    numberOfWeights_             = populationSize_;
    weightsDirectory_            = new String("data");   
    weightsFileName_            = new String("weightsFileName");   
    //allowRepetitions_            = false;
    normalization_               = true;
    estimatePoints_              = true;        
    aspirationLevel_	         = new ReferencePoint(problem_.getNumberOfObjectives());        
    reservationLevel_	         = new ReferencePoint(problem_.getNumberOfObjectives());        
    folderForOutputFiles_        = ".";
    requiredEvaluations_          = 0;
  } // GWASFGA_Settings

  /**
   * Constructor
   * @throws JMException 
   */
  public IRAEMO_Settings(String problem, int objectivesNumber) throws JMException {
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
    generations_                 = 200 ;
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
    numberOfWeights_             = populationSize_;
    weightsDirectory_            = new String("weightsDirectory");   
    weightsFileName_            =  new String("weightsFileName"); 
    //allowRepetitions_            = false;
    normalization_               = true;
    estimatePoints_              = true;        
    aspirationLevel_	         = new ReferencePoint(problem_.getNumberOfObjectives());        
    reservationLevel_	         = new ReferencePoint(problem_.getNumberOfObjectives());        
    folderForOutputFiles_        = ".";
    requiredEvaluations_          = 0;
  } // IRAEMO_Settings
  
  /**
   * Configure GWASFGA with user-defined parameter settings
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
    
    // Creating the algorithm.
    algorithm = new IRAEMO(problem_) ;    
    
    // Algorithm parameters
    algorithm.setInputParameter("populationSize",populationSize_);
    algorithm.setInputParameter("generations",generations_);
    algorithm.setInputParameter("numberOfWeights",numberOfWeights_);
    algorithm.setInputParameter("weightsDirectory",weightsDirectory_);
    algorithm.setInputParameter("weightsFileName",weightsFileName_);    
    algorithm.setInputParameter("indicators",convergenceIndicator_);    
    //algorithm.setInputParameter("allowRepetitions",allowRepetitions_);
    algorithm.setInputParameter("normalization",normalization_);
    algorithm.setInputParameter("asfAspirationLevel",asfAspirationLevel_);
    algorithm.setInputParameter("asfReservationLevel",asfReservationLevel_);
    algorithm.setInputParameter("estimatePoints",estimatePoints_);
    algorithm.setInputParameter("aspirationLevel",aspirationLevel_);
    algorithm.setInputParameter("reservationLevel",reservationLevel_);
    algorithm.setInputParameter("folderForOutputFiles", folderForOutputFiles_);

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
   
    return algorithm ;
  } // configure
} // IRAEMO_Settings