//  Authors:
//       Rubén Saborido Infantes <rsain@uma.es>
//
//  Copyright (c) 2018 Rubén Saborido Infantes
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

package jmetal.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmetal.core.Solution;

import jmetal.core.SolutionSet;
import jmetal.experiments.Experiment;
import jmetal.qualityIndicator.Hypervolume;
import jmetal.qualityIndicator.util.MetricsUtil;

/**
 * @author Rubén Saborido Infantes
 * This class offers different methods to manipulate reference points.
 * A reference point is a vector containing a value for each component of an objective function.
 */
public class ReferencePoint {

	Vector<Double> referencePoint_;

	public enum ReferencePointType {
		ACHIEVABLE, UNACHIEVABLE
	};

	/**
	 * Construct a reference point reading it from a file
	 * @param referencePointFileName File containing a reference point	 
	 */
	public ReferencePoint(String referencePointFileName) throws IOException {
		// Open the aspiration file
		FileInputStream fis = new FileInputStream(referencePointFileName);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);

		referencePoint_ = new Vector<Double>();
		String aux = br.readLine();
		while (aux != null) {
			StringTokenizer st = new StringTokenizer(aux);

			while (st.hasMoreTokens()) {
				Double value = (new Double(st.nextToken()));
				referencePoint_.add(value);
			}
			aux = br.readLine();
		}
		br.close();
	}

	/**
	 * Construct a reference point from a vector
	 * @param referencePoint Vector defining a reference point	 	 
	 */
	public ReferencePoint(double[] referencePoint) {
		this.referencePoint_ = new Vector<Double>();

		for (int indexOfComponent = 0; indexOfComponent < referencePoint.length; indexOfComponent++)
			this.referencePoint_.add(Double
					.valueOf(referencePoint[indexOfComponent]));
	}

	/**
	 * Construct an empty reference point with a dimension given
	 * @param numberOfObjectives The number of components 
	 */
	public ReferencePoint(int numberOfObjectives) {
		this.referencePoint_ = new Vector<Double>(numberOfObjectives);
		referencePoint_.setSize(numberOfObjectives);
	}

	/**
	 * Construct a random reference point from a Pareto front file
	 * @param type The type of the created reference point
	 * @param paretoFrontFileName A Pareto front in a file
	 */
	public ReferencePoint(ReferencePointType type, String paretoFrontFileName)
			throws JMException {
		int randomIndexPoint;
		double[] minimumValues, maximumValues;
		double[][] front;
		int index, numberOfObjectives;
		MetricsUtil metrics = new MetricsUtil();

		front = metrics.readFront(paretoFrontFileName);

		numberOfObjectives = front[0].length;

		minimumValues = metrics.getMinimumValues(front, numberOfObjectives);
		maximumValues = metrics.getMaximumValues(front, numberOfObjectives);

		randomIndexPoint = PseudoRandom.randInt(0, front.length);

		referencePoint_ = new Vector<Double>();

		switch (type) {
		case ACHIEVABLE:
			for (index = 0; index < numberOfObjectives; index++) {
				this.referencePoint_.add(PseudoRandom.randDouble(
						front[randomIndexPoint][index], maximumValues[index]));
			}
			break;

		case UNACHIEVABLE:
			for (index = 0; index < numberOfObjectives; index++) {
				this.referencePoint_.add(PseudoRandom.randDouble(
						minimumValues[index], front[randomIndexPoint][index]));
			}
			break;
		}
	}

        
	/**
	 * Returns the distance between a solution and the reference point in
	 * objective space.
	 * 
	 * @param solutionI
	 *            The first <code>Solution</code>.
	 * @return the distance between a solution and the reference point in
	 *         objective space.
	 */
	public double normalizedWeightedDistanceFromSolution(Solution solution,
			double[] lowerBounds, double[] upperBounds,
			Vector<Double> weights) {
		double normalizedDiff; // Auxiliar var
		double distance = 0.0;
		// -> Calculate the euclidean distance
		for (int nObj = 0; nObj < solution.numberOfObjectives(); nObj++) {
			normalizedDiff = (solution.getObjective(nObj) - (this.referencePoint_
					.get(nObj)))
					/ (upperBounds[nObj] - lowerBounds[nObj]);
			distance += weights.get(nObj) * Math.pow(normalizedDiff, 2.0);
		} // for

		// Return the euclidean distance
		return Math.sqrt(distance);
	} // distanceBetweenObjectives.
        
	/**
	 * Returns the distance between a solution and the reference point in
	 * objective space.
	 * 
	 * @param solutionI
	 *            The first <code>Solution</code>.
	 * @return the distance between a solution and the reference point in
	 *         objective space.
	 */
	public double weightedDistanceFromSolution(Solution solution,
			Vector<Double> weights) {
		double diff; // Auxiliar var
		double distance = 0.0;
		// -> Calculate the euclidean distance
		for (int nObj = 0; nObj < solution.numberOfObjectives(); nObj++) {
			diff = solution.getObjective(nObj) - this.referencePoint_.get(nObj);
			distance += weights.get(nObj) * Math.pow(diff, 2.0);
		} // for

		// Return the euclidean distance
		return Math.sqrt(distance);
	} // distanceBetweenObjectives.
        
	/**
	 * Get a component of the reference point
	 * @param indexOfObjective The index of the component 
	 * @return The value of the selected component
	 */
	public Double get(int indexOfObjective) {
		return this.referencePoint_.get(indexOfObjective);
	}

	/**
	 * Set the value of a component of the reference point
	 * @param indexOfObjective The index of the component
	 * @param valueOfObjective The new value of the component 
	 * @return The value of the selected component
	 */
	public void set(int indexOfObjective, Double valueOfObjective) {
		this.referencePoint_.set(indexOfObjective, valueOfObjective);
	}
        
	/**
	 * Get the size of the reference point
	 * @return The number of components of the reference point
	 */
	public int size() {
		return this.referencePoint_.size();
	}

	/**
	 * Convert the reference point in a vector of double
	 * @return A vector of double containing the values of the reference point
	 */
	public double[] toDouble() {
		double[] result = new double[this.referencePoint_.size()];
		for (int indexOfObjective = 0; indexOfObjective < this.referencePoint_
				.size(); indexOfObjective++) {
			result[indexOfObjective] = referencePoint_.get(indexOfObjective)
					.doubleValue();
		}

		return result;
	}

	/**
	 * Return the solutions Pareto-dominated by the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions Pareto-dominated by the reference point
	 */
	public double[][] getDominatedSolutionsByMe(double[][] solutions) {
		double[][] result;
		ArrayList<Integer> indexsOfDominatedSolutions = new ArrayList<Integer>();

		for (int indexOfSolution = 0; indexOfSolution < solutions.length; indexOfSolution++) {
			if (ParetoDominance.checkParetoDominance(this.toDouble(),
					solutions[indexOfSolution]) == -1) {
				indexsOfDominatedSolutions
						.add(Integer.valueOf(indexOfSolution));
			}
		}

		result = new double[indexsOfDominatedSolutions.size()][referencePoint_
				.size()];
		for (int indexOfSolution = 0; indexOfSolution < indexsOfDominatedSolutions
				.size(); indexOfSolution++) {
			result[indexOfSolution] = solutions[indexsOfDominatedSolutions
					.get(indexOfSolution)].clone();
		}

		return result;
	}

	/**
	 * Return the solutions which Pareto-dominate to the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions which Pareto-dominate to the reference point
	 */
	public double[][] getDominantSolutions(double[][] solutions) {
		double[][] result;
		ArrayList<Integer> indexsOfDominatedSolutions = new ArrayList<Integer>();

		for (int indexOfSolution = 0; indexOfSolution < solutions.length; indexOfSolution++) {
			if (ParetoDominance.checkParetoDominance(this.toDouble(),
					solutions[indexOfSolution]) == 1) {
				indexsOfDominatedSolutions
						.add(Integer.valueOf(indexOfSolution));
			}
		}

		result = new double[indexsOfDominatedSolutions.size()][referencePoint_
				.size()];
		for (int indexOfSolution = 0; indexOfSolution < indexsOfDominatedSolutions
				.size(); indexOfSolution++) {
			result[indexOfSolution] = solutions[indexsOfDominatedSolutions
					.get(indexOfSolution)].clone();
		}

		return result;
	}

	/**
	 * Return the solutions greater of equal than the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions greater of equal than the reference point
	 */
	public double[][] getSolutionsGreaterOrEqualThanMe(double[][] solutions) {
		double[][] result;

		ArrayList<Integer> indexsOfSolutions = new ArrayList<Integer>();

		for (int indexOfSolution = 0; indexOfSolution < solutions.length; indexOfSolution++) {
			boolean isGreater = true;
			int indexOfObjective = 0;

			while (isGreater
					&& indexOfObjective < solutions[indexOfSolution].length) {
				isGreater = solutions[indexOfSolution][indexOfObjective] >= this.referencePoint_
						.get(indexOfObjective);
				indexOfObjective++;
			}

			if (isGreater) {
				indexsOfSolutions.add(Integer.valueOf(indexOfSolution));
			}
		}

		result = new double[indexsOfSolutions.size()][referencePoint_.size()];
		for (int indexOfSolution = 0; indexOfSolution < indexsOfSolutions
				.size(); indexOfSolution++) {
			result[indexOfSolution] = solutions[indexsOfSolutions
					.get(indexOfSolution)].clone();
		}

		return result;
	}

	/**
	 * Return the solutions greater of equal than the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions greater of equal than the reference point
	 */
	public SolutionSet getSolutionsGreaterOrEqualThanMe(SolutionSet solutions) {
		ArrayList<Integer> indexsOfSolutions = new ArrayList<Integer>();

		for (int indexOfSolution = 0; indexOfSolution < solutions.size(); indexOfSolution++) {
			boolean isGreater = true;
			int indexOfObjective = 0;

			while (isGreater
					&& indexOfObjective < solutions.get(indexOfSolution)
							.numberOfObjectives()) {
				isGreater = solutions.get(indexOfSolution).getObjective(
						indexOfObjective) >= this.referencePoint_
						.get(indexOfObjective);
				indexOfObjective++;
			}

			if (isGreater) {
				indexsOfSolutions.add(Integer.valueOf(indexOfSolution));		
			}
		}

		SolutionSet result = new SolutionSet(indexsOfSolutions.size());
		for (int indexOfSolution = 0; indexOfSolution < indexsOfSolutions.size(); indexOfSolution++)
		{
			result.add(solutions.get(indexsOfSolutions.get(indexOfSolution)));
		}

		return result;
	}

	/**
	 * Return the solutions lower of equal than the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions lower of equal than the reference point
	 */
	public double[][] getSolutionsLowerOrEqualThanMe(double[][] solutions) {
		double[][] result;

		ArrayList<Integer> indexsOfSolutions = new ArrayList<Integer>();

		for (int indexOfSolution = 0; indexOfSolution < solutions.length; indexOfSolution++) {
			boolean isLower = true;
			int indexOfObjective = 0;

			while (isLower
					&& indexOfObjective < solutions[indexOfSolution].length) {
				isLower = solutions[indexOfSolution][indexOfObjective] <= this.referencePoint_
						.get(indexOfObjective);
				indexOfObjective++;
			}

			if (isLower) {
				indexsOfSolutions.add(Integer.valueOf(indexOfSolution));
			}
		}

		result = new double[indexsOfSolutions.size()][referencePoint_.size()];
		for (int indexOfSolution = 0; indexOfSolution < indexsOfSolutions
				.size(); indexOfSolution++) {
			result[indexOfSolution] = solutions[indexsOfSolutions
					.get(indexOfSolution)].clone();
		}

		return result;
	}

	/**
	 * Return the solutions lower of equal than the reference point
	 * @param solutions A set of solutions	
	 * @return The solutions lower of equal than the reference point
	 */
	public SolutionSet getSolutionsLowerOrEqualThanMe(SolutionSet solutions) {		
		ArrayList<Integer> indexsOfSolutions = new ArrayList<Integer>();
		
		for (int indexOfSolution = 0; indexOfSolution < solutions.size(); indexOfSolution++) {
			boolean isLower = true;
			int indexOfObjective = 0;

			while (isLower
					&& indexOfObjective < solutions.get(indexOfSolution).numberOfObjectives()) {
				isLower = solutions.get(indexOfSolution).getObjective(indexOfObjective) <= this.referencePoint_
						.get(indexOfObjective);
				indexOfObjective++;
			}

			if (isLower) {
				indexsOfSolutions.add(Integer.valueOf(indexOfSolution));		
			}
		}

		SolutionSet result = new SolutionSet(indexsOfSolutions.size());
		for (int indexOfSolution = 0; indexOfSolution < indexsOfSolutions.size(); indexOfSolution++)
		{
			result.add(solutions.get(indexsOfSolutions.get(indexOfSolution)));
		}
		
		return result;
	}
        
        /**
         * 
         * @param ReferencePointFileName
         * @throws IOException 
         */
	public void appendInFile(String ReferencePointFileName) throws IOException {
		int index;

		FileWriter ReferencePointFile = new FileWriter(ReferencePointFileName,
				true);

                ReferencePointFile.write("\n");
                
		for (index = 0; index < referencePoint_.size(); index++) {
			ReferencePointFile.write(referencePoint_.get(index).toString()
					+ " ");
		}                                

		ReferencePointFile.close();
	}
        
        /**
         * 
         * @param ReferencePointFileName
         * @throws IOException 
         */
	public void writeInFile(String ReferencePointFileName) throws IOException {
		int index;

		FileWriter ReferencePointFile = new FileWriter(ReferencePointFileName,
				false);

		for (index = 0; index < referencePoint_.size(); index++) {
			ReferencePointFile.write(referencePoint_.get(index).toString()
					+ " ");
		}

		ReferencePointFile.close();
	}
        
        public static void writeInFile(ReferencePoint[] referencePoints,
			String ReferencePointFileName) throws IOException {
		FileWriter ReferencePointFile = new FileWriter(ReferencePointFileName,
				false);

		for (int indexOfReferencePoint = 0; indexOfReferencePoint < referencePoints.length; indexOfReferencePoint++) {
			for (int indexOfComponent = 0; indexOfComponent < referencePoints[indexOfReferencePoint]
					.size(); indexOfComponent++) {
				ReferencePointFile.write(String
						.valueOf(referencePoints[indexOfReferencePoint]
								.get(indexOfComponent))
						+ " ");
			}
			ReferencePointFile.write("\n");
		}

		ReferencePointFile.close();
	}
        
        @Override
        public String toString() {
            String result = new String("[");       
            for (int i = 0; i < referencePoint_.size()-1; i++)
            {
                result = result + Double.toString(referencePoint_.get(i)) + ", ";            
            }
            result = result + Double.toString(referencePoint_.get(referencePoint_.size()-1)) + "]";
            
            return result;
        }
                
        public String toString(int numberOfDigitsByComponent) {
            String result = new String("[");       
            for (int i = 0; i < referencePoint_.size()-1; i++)
            {
                result = result + Math.rint(referencePoint_.get(i) * Math.pow(10, numberOfDigitsByComponent)) / Math.pow(10, numberOfDigitsByComponent) + ", ";                
            }
            result = result + Math.rint(referencePoint_.get(referencePoint_.size()-1) * Math.pow(10, numberOfDigitsByComponent)) / Math.pow(10, numberOfDigitsByComponent) + "]";                            
            
            return result;
        }    
        
        public static void writeROIForProblem(Experiment exp,
			ReferencePoint.ReferencePointType rpt) throws IOException {
		for (int algorithm = 0; algorithm < exp.algorithmNameList_.length; algorithm++) {
			for (int problem = 0; problem < exp.problemList_.length; problem++) {
				// Read values from data files
				String directory = exp.experimentBaseDirectory_;
				directory += "/data";
				directory += "/" + exp.algorithmNameList_[algorithm];
				directory += "/" + exp.problemList_[problem];

				ReferencePoint rp = new ReferencePoint(directory
						+ "/REFERENCE_POINTS.rl");

				MetricsUtil mu = new MetricsUtil();
				SolutionSet solutions = mu.readNonDominatedSolutionSet(exp.paretoFrontDirectory_+ "/" + exp.paretoFrontFile_[problem]);

				String fileName = directory + "/ROI.rl";
				FileWriter outputFile = new FileWriter(fileName, false);

				for (int solution = 0; solution < solutions.size(); solution++) {
					int objective = 0;

					boolean isInROI;

					if (rpt == ReferencePoint.ReferencePointType.ACHIEVABLE) {
						isInROI = solutions.get(solution).getObjective(
								objective) <= rp.get(objective).doubleValue();
					} else {
						isInROI = solutions.get(solution).getObjective(
								objective) >= rp.get(objective).doubleValue();
					}

					objective = 1;
					while (objective < solutions.get(solution)
							.numberOfObjectives() && isInROI) {

						if (rpt == ReferencePoint.ReferencePointType.ACHIEVABLE) {
							isInROI = solutions.get(solution).getObjective(
									objective) <= rp.get(objective)
									.doubleValue();
						} else {
							isInROI = solutions.get(solution).getObjective(
									objective) >= rp.get(objective)
									.doubleValue();
						}
						objective++;
					}

					if (isInROI) {
						for (objective = 0; objective < solutions.get(solution)
								.numberOfObjectives(); objective++) {
							outputFile.write(solutions.get(solution)
									.getObjective(objective) + " ");
						}
						outputFile.write("\n");
					}
				} // for

				outputFile.close();
				System.out.println("File " + fileName
						+ " created successfully.");
			} // for
		}
	}
        
        public static void generateFilesHVROI(Experiment exp,
	    ReferencePoint[] referencePoints, ReferencePointType rpt) {
		MetricsUtil mu = new MetricsUtil();

		for (int algorithmIndex = 0; algorithmIndex < exp.algorithmNameList_.length; algorithmIndex++) {
			String algorithmDirectory;
			algorithmDirectory = exp.experimentBaseDirectory_ + "/data/"
					+ exp.algorithmNameList_[algorithmIndex] + "/";

			for (int indexOfProblem = 0; indexOfProblem < exp.problemList_.length; indexOfProblem++) {
				String problemDirectory = algorithmDirectory
						+ exp.problemList_[indexOfProblem];
				String paretoFrontPath = exp.paretoFrontDirectory_ + "/" + exp.paretoFrontFile_[indexOfProblem];
				String qualityIndicatorFile = problemDirectory + "/HVROI";
				
				FileWriter os;				
				try {
					os = new FileWriter(qualityIndicatorFile.toString(), false);
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				
				for (int numRun = 0; numRun < exp.independentRuns_; numRun++) {
					String outputParetoFrontFilePath;
					outputParetoFrontFilePath = problemDirectory + "/FUN."
							+ numRun;
					String solutionFrontFile = outputParetoFrontFilePath;					
					double value = 0;

					Hypervolume indicators = new Hypervolume();

					double[][] solutionFront, trueFront;

					if (rpt == ReferencePoint.ReferencePointType.UNACHIEVABLE) {
						solutionFront = referencePoints[indexOfProblem]
								.getDominatedSolutionsByMe(mu.readFront(solutionFrontFile));

						double[][] a= mu.readFront(paretoFrontPath);
						trueFront = referencePoints[indexOfProblem]
								.getDominatedSolutionsByMe(mu.readFront(paretoFrontPath));
						solutionFront = new ReferencePoint(mu.getMaximumValues(
								trueFront, trueFront[0].length))
								.getDominantSolutions(solutionFront);
					} else {
						solutionFront = referencePoints[indexOfProblem]
								.getDominantSolutions(mu.readFront(solutionFrontFile));
						
						trueFront = referencePoints[indexOfProblem]
								.getDominantSolutions(mu.readFront(paretoFrontPath));						
					}				
														
					//Se usa como valor máximo y mínimo de HV los del frente pareto óptimo en la ROI
					value = indicators.hypervolume(solutionFront, trueFront,
							trueFront[0].length);
					
					try {
						os = new FileWriter(
								qualityIndicatorFile.toString(), true);
						os.write("" + value + "\n");
						os.close();
					} catch (IOException ex) {
						Logger.getLogger(Experiment.class.getName()).log(
								Level.SEVERE, null, ex);
					}					
				} // for
			}
		}
	}
        
        public static void writeNumberOfSolutionsInROIForExperiment(Experiment exp,
			ReferencePoint.ReferencePointType rpt) throws IOException {
		MetricsUtil mu = new MetricsUtil();
		
		for (int algorithm = 0; algorithm < exp.algorithmNameList_.length; algorithm++) {
			for (int problem = 0; problem < exp.problemList_.length; problem++) {
				// Read values from data files
				String directory = exp.experimentBaseDirectory_;
				directory += "/data";
				directory += "/" + exp.algorithmNameList_[algorithm];
				directory += "/" + exp.problemList_[problem];

				ReferencePoint rp = new ReferencePoint(directory
						+ "/REFERENCE_POINTS.rl");
				String fileName = directory + "/SolutionsInROI.txt";
				FileWriter outputFile = new FileWriter(fileName, false);
				double[][] trueFront = rp.getDominatedSolutionsByMe(mu.readFront(exp.paretoFrontDirectory_ + "/" + exp.paretoFrontFile_[problem]));
				
				int countTotalNonDominatedSolutions = 0, countNonDominatedSolutionsInROI = 0;
				int countTotalSolutions = 0, countSolutionsInROI = 0;

				for (int independentRun = 0; independentRun < exp.independentRuns_; independentRun++) {					
					String solutionFrontFile = directory + "/" + "FUN."	+ independentRun;			
					
					if (rpt == ReferencePoint.ReferencePointType.UNACHIEVABLE)
					{
						SolutionSet nonDominatedSolutions = mu.readNonDominatedSolutionSet(solutionFrontFile);
						countTotalNonDominatedSolutions = countTotalNonDominatedSolutions + nonDominatedSolutions.size();						
						nonDominatedSolutions = rp.getSolutionsGreaterOrEqualThanMe(nonDominatedSolutions);																	
						nonDominatedSolutions = new ReferencePoint(mu.getMaximumValues(trueFront, trueFront[0].length))
													.getSolutionsLowerOrEqualThanMe(nonDominatedSolutions);
						countNonDominatedSolutionsInROI = countNonDominatedSolutionsInROI + nonDominatedSolutions.size();	
						
						double[][] solutions = mu.readFront(solutionFrontFile);
						countTotalSolutions = countTotalSolutions + solutions.length;
						solutions = rp.getSolutionsGreaterOrEqualThanMe(solutions);																
						solutions = new ReferencePoint(mu.getMaximumValues(trueFront, trueFront[0].length))
													.getSolutionsLowerOrEqualThanMe(solutions);
						countSolutionsInROI = countSolutionsInROI + solutions.length;
					}
					else if (rpt == ReferencePoint.ReferencePointType.ACHIEVABLE)
					{						
						SolutionSet nonDominatedSolutions = mu.readNonDominatedSolutionSet(solutionFrontFile);
						countTotalNonDominatedSolutions =  countTotalNonDominatedSolutions + nonDominatedSolutions.size();
						nonDominatedSolutions = rp.getSolutionsLowerOrEqualThanMe(nonDominatedSolutions);												 
						countNonDominatedSolutionsInROI = countNonDominatedSolutionsInROI + nonDominatedSolutions.size();
						
						double[][] solutions = mu.readFront(solutionFrontFile);
						countTotalSolutions = countTotalSolutions +  solutions.length;
						solutions = rp.getSolutionsLowerOrEqualThanMe(solutions);						
						countSolutionsInROI = countSolutionsInROI + solutions.length;						 
					}
					
				}

				outputFile
						.write("Solutions in ROI: "
								+ countSolutionsInROI
								+ "\n"
								+ "Total number of solutions: "
								+ countTotalSolutions
								+ "\n"
								+ "Average of solutions in ROI (in "
								+ exp.independentRuns_
								+ " runs): "
								+ ((double) countSolutionsInROI / (double) exp.independentRuns_)
								+ "\n"
								+ "\n"
								+ "Non-dominated Solutions in ROI: "
								+ countNonDominatedSolutionsInROI
								+ "\n"
								+ "Total number of non-dominated solutions: "
								+ countTotalNonDominatedSolutions
								+ "\n"
								+ "Average of non-dominated solutions in ROI (in "
								+ exp.independentRuns_
								+ " runs): "
								+ ((double) countNonDominatedSolutionsInROI / (double) exp.independentRuns_));

				outputFile.close();
				System.out.println("File " + fileName
						+ " created successfully.");
			} // for
		}
	}
}
