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
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author Rubén Saborido Infantes This class offers different methods to
 *         manipulate weight vectors.
 */
public class Weights {
	/**
	 * Generate uniform weight vectors in two dimension
	 * 
	 * @param epsilon
	 *            Distance between each component of the weight vector
	 * @param numberOfWeights
	 *            Number of weight vectors to generate
	 * @return A set of weight vectors
	 */
	public static double[][] initUniformWeights2D(double epsilon, int numberOfWeights) {
		double[][] weights = new double[numberOfWeights][2];

		int indexOfWeight;
		double w, jump;

		jump = (1 - (2 * epsilon)) / (numberOfWeights - 1);
		indexOfWeight = 0;

		w = epsilon;

		while (w <= (1 - epsilon)+(1e-10)) {
			weights[indexOfWeight][0] = w;
			weights[indexOfWeight][1] = 1 - w;

			w = w + jump;

			indexOfWeight = indexOfWeight + 1;
		}

		return weights;
	}

	/**
	 * Generate uniform weight vectors in two dimension (separating pair and odd
	 * weight vectors)
	 * 
	 * @param epsilon
	 *            Distance between each component of the weight vector
	 * @param numberOfWeights
	 *            Number of weight vectors to generate
	 * @return A vector with 2 components, containing the pair and odd weight
	 *         vectors generated, respectively.
	 */
	public static Vector<double[][]> initUniformPairAndOddWeights2D(double epsilon, int numberOfWeights) {
		Vector<double[][]> result = new Vector<double[][]>();
		double[][] weights = new double[numberOfWeights][2];

		int indexOfWeight;
		double w, jump;

		jump = (1 - (2 * epsilon)) / (numberOfWeights - 1);
		indexOfWeight = 0;

		w = epsilon;

		while (w <= (1 - epsilon)+(1e-10)) {
			weights[indexOfWeight][0] = w;
			weights[indexOfWeight][1] = 1 - w;

			w = w + jump;

			indexOfWeight = indexOfWeight + 1;
		}

		double[][] pairWeights = new double[numberOfWeights / 2][2];
		double[][] oddWeights = new double[numberOfWeights / 2][2];

		int indexPairWeight = 0, indexOddWeight = 0;
		for (indexOfWeight = 0; indexOfWeight < weights.length; indexOfWeight++) {
			if (indexOfWeight % 2 == 0) {
				pairWeights[indexPairWeight][0] = weights[indexOfWeight][0];
				pairWeights[indexPairWeight][1] = weights[indexOfWeight][1];
				indexPairWeight++;
			} else {
				oddWeights[indexOddWeight][0] = weights[indexOfWeight][0];
				oddWeights[indexOddWeight][1] = weights[indexOfWeight][1];
				indexOddWeight++;
			}
		}

		result.add(pairWeights);
		result.add(oddWeights);

		return result;
	}

	public static void main (String[] args)
	{
		double[][] weights;
		
		weights=initUniformWeights(0.001, 3);				
	}
	
	public static double[][] initUniformWeights(double epsilon, int numberOfFunctions) {
		int i, j, k;
		int numberOfWeights;
		double[] jump = new double[numberOfFunctions];
		double sum;
		int L;
		double[][] result;

		if ((numberOfFunctions >= 2) && (numberOfFunctions <= 5))
			epsilon = 0.005;
		else if ((numberOfFunctions >= 6) && (numberOfFunctions <= 10))
			epsilon = 0.01;
		else if ((numberOfFunctions >= 11) && (numberOfFunctions <= 15))
			epsilon = 0.1;

		if (numberOfFunctions == 3)
			L = 60;
		else if (numberOfFunctions == 4)
			L = 25;
		else if (numberOfFunctions == 5)
			L = 16;
		else if (numberOfFunctions == 6)
			L = 12;
		else if (numberOfFunctions == 7)
			L = 10;
		else if (numberOfFunctions == 8)
			L = 9;
		else if (numberOfFunctions == 9)
			L = 8;
		else if (numberOfFunctions == 10)
			L = 7;
		else if (numberOfFunctions == 11)
			L = 6;
		else if (numberOfFunctions == 12)
			L = 6;
		else if (numberOfFunctions >= 13)
			L = 5;
		else
			L= 2;

		//numberOfWeights = factorial(L + numberOfFunctions - 1) / (factorial(L) * factorial(numberOfFunctions - 1));
		result = new double[100000][numberOfFunctions];

		for (i = 0; i < numberOfFunctions; i++) {
			jump[i] = 0;
		}

		j = numberOfFunctions - 1;
		k = 0;

		while (j >= 0) {
			j = numberOfFunctions - 1;

			for (i = 0; i < numberOfFunctions; i++) {
				result[k][i] = epsilon + (jump[i] / L) * (1 - 2 * epsilon);
			}

			k++;

			// SI SE QUITA ESTE BUCLE, LA GENERACIÓN DE PESOS REPITE MUCHOS
			// PESOS
			sum = 0;
			for (i = 0; i < j; i++) {
				sum = sum + jump[i];
			}

			while (jump[j] == L - sum) {
				for (i = j; i < numberOfFunctions; i++) {
					jump[i] = 0;
				}
				j--;

				if (j < 0) {
					break; // FIN DE LA GENERACIÓN DE PESOS
				}

			}// Fin del if(salto[j]==L)

			jump[j]++;
		}// Fin del while (j>=0)

		return result;
	}

	/**
	 * Read a set of weight vector from a file
	 * 
	 * @param filePath
	 *            A file containing the weight vectors
	 * @return A set of weight vectors
	 */
	public static double[][] getWeightsFromFile(String filePath) {
		double[][] weights = new double[0][0];

		Vector<double[]> listOfWeights = new Vector<double[]>();

		try {
			// Open the file
			FileInputStream fis = new FileInputStream(filePath);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);

			int numberOfObjectives = 0;
			int i = 0;
			int j = 0;
			String aux = br.readLine();
			while (aux != null) {
				StringTokenizer st = new StringTokenizer(aux);
				j = 0;
				numberOfObjectives = st.countTokens();
				double[] weight = new double[numberOfObjectives];

				while (st.hasMoreTokens()) {
					weight[j] = (new Double(st.nextToken())).doubleValue();
					j++;
				}

				listOfWeights.add(weight);
				aux = br.readLine();
				i++;
			}
			br.close();

			weights = new double[listOfWeights.size()][numberOfObjectives];
			for (int indexWeight = 0; indexWeight < listOfWeights.size(); indexWeight++) {
				for (int indexOfObjective = 0; indexOfObjective < numberOfObjectives; indexOfObjective++) {
					weights[indexWeight][indexOfObjective] = listOfWeights.get(indexWeight)[indexOfObjective];
				}
			}
		} catch (Exception e) {
			System.out.println("getWeightsFromFile: failed when reading for file: " + filePath);
			e.printStackTrace();			
		}

		return weights;
	}

	/**
	 * Read a set of weight vector from a file (separating pair and odd weight
	 * vectors)
	 * 
	 * @param filePath
	 *            A file containing the weight vectors
	 * @return A vector with 2 components, separating the pair and odd weight
	 *         vectors read from the file
	 */
	public static Vector<double[][]> getPairAndOddWeightsFromFile(String filePath) {
		Vector<double[][]> weights = new Vector<double[][]>();

		double[][] pairWeights = new double[0][0];
		double[][] oddWeights = new double[0][0];

		Vector<double[]> listOfPairWeights = new Vector<double[]>();
		Vector<double[]> listOfOddWeights = new Vector<double[]>();

		try {
			// Open the file
			FileInputStream fis = new FileInputStream(filePath);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);

			int numberOfObjectives = 0;
			int i = 0;
			int j = 0;
			String aux = br.readLine();
			while (aux != null) {
				StringTokenizer st = new StringTokenizer(aux);
				j = 0;
				numberOfObjectives = st.countTokens();
				double[] weight = new double[numberOfObjectives];

				while (st.hasMoreTokens()) {
					weight[j] = (new Double(st.nextToken())).doubleValue();
					j++;
				}

				if (i % 2 == 0)
					listOfPairWeights.add(weight);
				else
					listOfOddWeights.add(weight);

				aux = br.readLine();
				i++;
			}
			br.close();

			pairWeights = new double[listOfPairWeights.size()][numberOfObjectives];
			for (int indexWeight = 0; indexWeight < listOfPairWeights.size(); indexWeight++) {
				for (int indexOfObjective = 0; indexOfObjective < numberOfObjectives; indexOfObjective++) {
					pairWeights[indexWeight][indexOfObjective] = listOfPairWeights.get(indexWeight)[indexOfObjective];
				}
			}

			oddWeights = new double[listOfOddWeights.size()][numberOfObjectives];
			for (int indexWeight = 0; indexWeight < listOfOddWeights.size(); indexWeight++) {
				for (int indexOfObjective = 0; indexOfObjective < numberOfObjectives; indexOfObjective++) {
					oddWeights[indexWeight][indexOfObjective] = listOfOddWeights.get(indexWeight)[indexOfObjective];
				}
			}

			weights.add(pairWeights);
			weights.add(oddWeights);
		} catch (Exception e) {
			System.out.println("getWeightsFromFile: failed when reading for file: " + filePath);
			e.printStackTrace();
		}

		return weights;
	}

	/**
	 * Calculate the inverse of a set of weight vectors
	 * 
	 * @param weights
	 *            A set of weight vectors
	 * @param normalize
	 *            True if the weights should be normalize by the sum of the
	 *            components
	 * @return A set of weight vectors
	 */
	public static double[][] invertWeights(double[][] weights, boolean normalize) {
		double[][] result = new double[weights.length][weights[0].length];

		for (int indexOfWeight = 0; indexOfWeight < weights.length; indexOfWeight++) {
			double sum = 0;

			for (int indexOfComponent = 0; indexOfComponent < weights[indexOfWeight].length; indexOfComponent++) {
				sum = sum + (1.0 / weights[indexOfWeight][indexOfComponent]);
			}

			if (normalize) {
				for (int indexOfComponent = 0; indexOfComponent < weights[indexOfWeight].length; indexOfComponent++)
					result[indexOfWeight][indexOfComponent] = (1.0 / weights[indexOfWeight][indexOfComponent]) / sum;
			} else {
				for (int indexOfComponent = 0; indexOfComponent < weights[indexOfWeight].length; indexOfComponent++)
					result[indexOfWeight][indexOfComponent] = 1.0 / weights[indexOfWeight][indexOfComponent];
			}
		}

		return result;
	}
}
