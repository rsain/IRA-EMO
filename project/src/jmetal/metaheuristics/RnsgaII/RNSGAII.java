//  RNSGAII.java 
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

package jmetal.metaheuristics.RnsgaII;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import jmetal.core.*;
import jmetal.operators.selection.BinaryPreferenceTournament2;
import jmetal.util.*;

public class RNSGAII extends Algorithm {

    ReferencePoint[] referencePoints;
    String folderForOutputFiles;
    double[] lowerBounds, upperBounds;
    boolean estimateObjectivesBounds, normalization;
    double epsilon;
    int populationSize, generations, executedIterations = 0;

    /**
     * Constructor
     *
     * @param problem Problem to solve
     */
    public RNSGAII(Problem problem) {
        super(problem);
    } // RNSGAII

    /**
     * Runs the RNSGA-II algorithm.
     *
     * @return a <code>SolutionSet</code> that is a set of non dominated
     * solutions as a result of the algorithm execution
     * @throws JMException
     */
    public SolutionSet execute() throws JMException, ClassNotFoundException {   
        final JFrame window = new JFrame("Evaluating ...");
        final JProgressBar progressBar = new JProgressBar();      
        
        /*NumberOfWorkerThreads = 10; // Maximum number of threads
        ExecutorService threadPool = Executors.newFixedThreadPool(NumberOfWorkerThreads);
        SwingWorker<SolutionSet, Integer> worker = null;
        threadPool.submit(worker);*/
        SwingWorker<SolutionSet, Integer> worker = new SwingWorker<SolutionSet, Integer>() {
            @Override
            protected SolutionSet doInBackground() throws Exception {
                progressBar.setStringPainted(true);
                window.setPreferredSize(new Dimension(300, 80));
                window.getContentPane().add(progressBar);
                window.setAlwaysOnTop(true);
                window.setResizable(false);
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);                
        int localGenerations;
        int evaluations;

        SolutionSet population;
        SolutionSet offspringPopulation;
        SolutionSet union;

        Operator mutationOperator;
        Operator crossoverOperator;                

        Distance distance = new Distance();

        //Read the parameters
        populationSize = ((Integer) getInputParameter("populationSize")).intValue();
        generations = ((Integer) getInputParameter("generations")).intValue();
        folderForOutputFiles = (String) getInputParameter("folderForOutputFiles");
        normalization = ((Boolean) getInputParameter("normalization")).booleanValue();

        estimateObjectivesBounds = ((Boolean) getInputParameter("estimateObjectivesBounds")).booleanValue();
        if (estimateObjectivesBounds) {
            initializeBounds();
        }
        else
        {
        	lowerBounds = (double[]) getInputParameter("lowerBounds");
        	upperBounds = (double[]) getInputParameter("upperBounds");
        }

        referencePoints = ((ReferencePoint[]) getInputParameter("referencePoints"));
        epsilon = ((Double) getInputParameter("epsilon")).doubleValue();        

        //Initialize the variables
        population = new SolutionSet(populationSize);
        evaluations = 0;

        //Read the operators
        mutationOperator = operators_.get("mutation");
        crossoverOperator = operators_.get("crossover");        
        BinaryPreferenceTournament2 selectionOperator = new BinaryPreferenceTournament2(null);

        // Create the initial solutionSet
        Solution newSolution;
        for (int i = 0; i < populationSize; i++) {
            newSolution = new Solution(problem_);
            problem_.evaluate(newSolution);
            problem_.evaluateConstraints(newSolution);
            evaluations++;

            population.add(newSolution);

            if (estimateObjectivesBounds) {
                updateBounds(newSolution);
            }
        } //for

        //preferenceDistanceAssignment(population);
        
        // Generations 
        localGenerations = 0;
        progressBar.setMaximum(generations);
        while (localGenerations < generations) //while (requiredEvaluations == 0 && evaluations < maxEvaluations)
        {
            if (estimateObjectivesBounds) {
                initializeBounds();
            }

            // Create the offSpring solutionSet      
            offspringPopulation = new SolutionSet(populationSize);
            Solution[] parents = new Solution[2];
            for (int i = 0; i < (populationSize / 2); i++) {                
                //obtain parents
                parents[0] = (Solution) selectionOperator.execute(population);
                parents[1] = (Solution) selectionOperator.execute(population);
                Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
                mutationOperator.execute(offSpring[0]);
                mutationOperator.execute(offSpring[1]);
                problem_.evaluate(offSpring[0]);
                problem_.evaluateConstraints(offSpring[0]);
                problem_.evaluate(offSpring[1]);
                problem_.evaluateConstraints(offSpring[1]);
                offspringPopulation.add(offSpring[0]);
                offspringPopulation.add(offSpring[1]);
                evaluations += 2;

                if (estimateObjectivesBounds) {
                    updateBounds(offSpring[0]);
                    updateBounds(offSpring[1]);
                }                                           
            } // for		

            // Create the solutionSet union of solutionSet and offSpring
            union = ((SolutionSet) population).union(offspringPopulation);

            // Ranking the union
            Ranking ranking = new Ranking(union);

            int remain = populationSize;
            int index = 0;
            SolutionSet front = null;
            population.clear();

            // Obtain the next front
            front = ranking.getSubfront(index);            
            
            while ((remain > 0) && (remain >= front.size())) {   
                //Assign crowding distance to individuals
                distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
            	//preferenceDistanceAssignment(front);
            	
                //Add the individuals of this front
                for (int k = 0; k < front.size(); k++) {
                    population.add(front.get(k));
                } // for

                //Decrement remain
                remain = remain - front.size();

                //Obtain the next front
                index++;
                if (remain > 0) {
                    front = ranking.getSubfront(index);                                        
                } // if        
            } // while					

            // Remain is less than front(index).size, insert only the best one
            if (remain > 0) {  // front contains individuals to insert                        
                LinkedList<Node> preferenceCrowdingDistanceBySolutions = new LinkedList<RNSGAII.Node>();
                preferenceCrowdingDistanceBySolutions = preferenceCrowdingDistance(front);

                //front.sort(new CrowdingComparator());
                Collections.sort(preferenceCrowdingDistanceBySolutions);                
                for (int k = 0; k < remain; k++) {
                    front.get(preferenceCrowdingDistanceBySolutions.get(k).solutionIndex).setCrowdingDistance(preferenceCrowdingDistanceBySolutions.get(k).distance);
                    population.add(front.get(preferenceCrowdingDistanceBySolutions.get(k).solutionIndex));                    
                } // for

                remain = 0;
            } // if    
            localGenerations++;
            publish(localGenerations);
        } // while

        // Return the first non-dominated front
        Ranking ranking = new Ranking(population);        
        return population;
        }
        
        @Override
            protected void process(List<Integer> chunks) {
                progressBar.setValue(chunks.get(0));
            }

            @Override
            protected void done() {
                progressBar.setValue(progressBar.getMaximum());
                window.dispose();
            }
        };
        if (problem_.getName() == "AuxiliaryServicesProblem")
        {
            int NumberOfWorkerThreads = 1;
            ExecutorService threadPool = Executors.newFixedThreadPool(NumberOfWorkerThreads);
            threadPool.submit(worker);
        }
        worker.execute();

        SolutionSet result = new SolutionSet();

        try {
            result = worker.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(RNSGAII.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(RNSGAII.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    } // execute

    public SolutionSet[] executeFirstIteration() throws JMException, ClassNotFoundException {
        SolutionSet[] result = new SolutionSet[2];

        result[0] = new SolutionSet().union(execute());        
        result[1] = new Ranking(result[0]).getSubfront(0);                    

        //deleteWithChildren(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator);
        try {            
            new File(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName()).mkdirs();
            referencePoints[0].writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "ASPIRATION IN 1.rl");
            referencePoints[1].writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "RESERVATION IN 1.rl");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        result[0].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "POPULATION IN 1.txt");
        result[1].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "SOLUTIONS IN 1.txt");

        executedIterations = 1;

        return result;
    } // execute

    public SolutionSet[] doIteration(final SolutionSet population, final ReferencePoint newAspirationLevel, final ReferencePoint newReservationLevel) throws JMException, ClassNotFoundException {
        final JFrame window = new JFrame("Evaluating ...");
        final JProgressBar progressBar = new JProgressBar();

        SwingWorker<SolutionSet, Integer> worker = new SwingWorker<SolutionSet, Integer>() {
            @Override
            protected SolutionSet doInBackground() throws Exception {
                progressBar.setStringPainted(true);
                window.setPreferredSize(new Dimension(300, 80));
                window.getContentPane().add(progressBar);
                window.setAlwaysOnTop(true);
                window.setResizable(false);
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);

        int evaluations;
        
        SolutionSet offspringPopulation;
        SolutionSet union;

        Operator mutationOperator;
        Operator crossoverOperator; 
        
        Distance distance = new Distance();
        
        referencePoints[0] = newAspirationLevel;        
        referencePoints[1] = newReservationLevel;

        evaluations = 0;
        
        //Read the operators
        mutationOperator = operators_.get("mutation");
        crossoverOperator = operators_.get("crossover");        
        BinaryPreferenceTournament2 selectionOperator = new BinaryPreferenceTournament2(null);

        if (estimateObjectivesBounds) {
            initializeBounds();
            updateLowerBounds(population);
            updateUpperBounds(population);
        }
        else
        {
        	lowerBounds = (double[]) getInputParameter("lowerBounds");
        	upperBounds = (double[]) getInputParameter("upperBounds");
        }      
        
        // Generations 
        int localGenerations = 0;
        progressBar.setMaximum(generations);
        while (localGenerations < generations) //while (requiredEvaluations == 0 && evaluations < maxEvaluations)
        {
            if (estimateObjectivesBounds) {
                initializeBounds();
            }

            // Create the offSpring solutionSet      
            offspringPopulation = new SolutionSet(populationSize);
            Solution[] parents = new Solution[2];
            for (int i = 0; i < (populationSize / 2); i++) {                
                //obtain parents
                parents[0] = (Solution) selectionOperator.execute(population);
                parents[1] = (Solution) selectionOperator.execute(population);
                Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
                mutationOperator.execute(offSpring[0]);
                mutationOperator.execute(offSpring[1]);
                problem_.evaluate(offSpring[0]);
                problem_.evaluateConstraints(offSpring[0]);
                problem_.evaluate(offSpring[1]);
                problem_.evaluateConstraints(offSpring[1]);
                offspringPopulation.add(offSpring[0]);
                offspringPopulation.add(offSpring[1]);
                evaluations += 2;

                if (estimateObjectivesBounds) {
                    updateBounds(offSpring[0]);
                    updateBounds(offSpring[1]);
                }                                           
            } // for		

            // Create the solutionSet union of solutionSet and offSpring
            union = ((SolutionSet) population).union(offspringPopulation);

            // Ranking the union
            Ranking ranking = new Ranking(union);

            int remain = populationSize;
            int index = 0;
            SolutionSet front = null;
            population.clear();

            // Obtain the next front
            front = ranking.getSubfront(index);            
            
            while ((remain > 0) && (remain >= front.size())) {
                //Assign crowding distance to individuals
                distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
            	//preferenceDistanceAssignment(front);
            	
                //Add the individuals of this front
                for (int k = 0; k < front.size(); k++) {
                    population.add(front.get(k));
                } // for

                //Decrement remain
                remain = remain - front.size();

                //Obtain the next front
                index++;
                if (remain > 0) {
                    front = ranking.getSubfront(index);                                        
                } // if        
            } // while					

            // Remain is less than front(index).size, insert only the best one
            if (remain > 0) {  // front contains individuals to insert                                                        
                LinkedList<Node> preferenceCrowdingDistanceBySolutions = new LinkedList<RNSGAII.Node>();
                preferenceCrowdingDistanceBySolutions = preferenceCrowdingDistance(front);

                //front.sort(new CrowdingComparator());
                Collections.sort(preferenceCrowdingDistanceBySolutions);                
                for (int k = 0; k < remain; k++) {
                    front.get(preferenceCrowdingDistanceBySolutions.get(k).solutionIndex).setCrowdingDistance(preferenceCrowdingDistanceBySolutions.get(k).distance);
                    population.add(front.get(preferenceCrowdingDistanceBySolutions.get(k).solutionIndex));                    
                } // for

                remain = 0;
            } // if    
            localGenerations++;
            publish(localGenerations);
        } // while

        // Return the first non-dominated front
        Ranking ranking = new Ranking(population);        
        return population;
            }

            @Override
            protected void process(List<Integer> chunks) {
                progressBar.setValue(chunks.get(0));
            }

            @Override
            protected void done() {
                progressBar.setValue(progressBar.getMaximum());
                window.dispose();
            }
        };

        SolutionSet solutions = new SolutionSet();

        worker.execute();
        try {
            solutions = worker.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(RNSGAII.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(RNSGAII.class.getName()).log(Level.SEVERE, null, ex);
        }

        SolutionSet[] result = new SolutionSet[2];
        result[0] = new SolutionSet().union(solutions);        
        result[1] = new Ranking(solutions).getSubfront(0);                    

        executedIterations++;

        try {
            referencePoints[0].writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "ASPIRATION IN " + executedIterations + ".rl");
            referencePoints[1].writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "RESERVATION IN " + executedIterations + ".rl");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        result[0].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "POPULATION IN " + executedIterations + ".txt");
        result[1].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "SOLUTIONS IN " + executedIterations + ".txt");

        return result;
    }
    
    /**
     * Deletes the given path and, if it is a directory, deletes all its
     * children.
     */
    public boolean deleteWithChildren(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }
        if (!file.isDirectory()) {
            return file.delete();
        }
        return this.deleteChildren(file) && file.delete();
    }

    private boolean deleteChildren(File dir) {
        File[] children = dir.listFiles();
        boolean childrenDeleted = true;
        for (int i = 0; children != null && i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                childrenDeleted = this.deleteChildren(child) && childrenDeleted;
            }
            if (child.exists()) {
                childrenDeleted = child.delete() && childrenDeleted;
            }
        }
        return childrenDeleted;
    }
    
    private class Node implements Comparable<Node> {

        public int solutionIndex;
        public double distance;

        public Node (int solutionIndex, double distance) {
            this.solutionIndex = solutionIndex;
            this.distance = distance;
        }
        
        //@Override
        public boolean equals(Object obj) {
            // TODO Auto-generated method stub
            return this.solutionIndex == ((Node) obj).solutionIndex;
        }
        
        @Override
        public int compareTo(Node n) {
            int result;

            if (this.distance < n.distance)
            	result = -1;
            else if(this.distance > n.distance)
            	result =1;
            else //similar distances
            {
            	result = 0;
            }
                        
            return result;
        }        
    }

    /**
     * Assigns crowding distances based in epsilon-preference to all solutions
     * in a
     * <code>SolutionSet</code>, for each reference point.
     *
     * @param solutionSet The <code>SolutionSet</code>.
     */
    private LinkedList<Node> preferenceCrowdingDistance(SolutionSet solutionSet) {
    	int size = solutionSet.size();
        int numberOfReferencePoints = referencePoints.length;

        Vector<Double> weights = new Vector<Double>();
        for (int indexOfWeight = 0; indexOfWeight < problem_.getNumberOfObjectives(); indexOfWeight++) {
            weights.add(new Double(1.0 / problem_.getNumberOfObjectives()));
        }

        /**
         * STEP 1 in paper:
         * For each reference point, the normalized Euclidean
         * distance of each solution of the front is calculated and
         * the solutions are sorted in ascending order of distance.
         * This way, the solution closest to the reference point is
         * assigned a rank of one.
         **/
        
        //we get preference distances for each solution and reference point	    
        ArrayList<ArrayList<Node>> distancesToReferencePoints = new  ArrayList<ArrayList<RNSGAII.Node>>(numberOfReferencePoints);         
        
        //Loop for each reference point
        for (int indexOfReferencePoint = 0; indexOfReferencePoint < numberOfReferencePoints; indexOfReferencePoint++)
        {                      
            distancesToReferencePoints.add(new ArrayList<RNSGAII.Node>());
            
            //Loop for each solution, regarding the reference point.
            for (int indexOfSolution = 0; indexOfSolution < size; indexOfSolution++) {
                double distance;
                           
                if (normalization) {
                    distance = this.referencePoints[indexOfReferencePoint].normalizedWeightedDistanceFromSolution(solutionSet.get(indexOfSolution), lowerBounds, upperBounds, weights);
                } else {
                    distance = this.referencePoints[indexOfReferencePoint].weightedDistanceFromSolution(solutionSet.get(indexOfSolution), weights);
                }

                Node node = new Node(indexOfSolution,distance);
                distancesToReferencePoints.get(indexOfReferencePoint).add(node);            
            }
            
            Collections.sort(distancesToReferencePoints.get(indexOfReferencePoint));
        }       
        
        //Now we compute the ranks
        int[][] rankBySolutionsAndReferencePoints = new int[numberOfReferencePoints][size];
        //Loop for each reference point
        for (int indexOfReferencePoint = 0; indexOfReferencePoint < numberOfReferencePoints; indexOfReferencePoint++)
        {        
            int rank = 1;
            for (int indexOfNode = 0; indexOfNode < size; indexOfNode++) {
                    rankBySolutionsAndReferencePoints[indexOfReferencePoint][distancesToReferencePoints.get(indexOfReferencePoint).get(indexOfNode).solutionIndex]=rank;                    
                    rank++;        	        
            }    
        }
        
        /**
         * Step 2 in paper: 
         * After such computations are performed for all reference points,
         * the minimum of the assigned ranks is assigned as the crowding 
         * distance to a solution. This way,  solutions closest  to all
         * reference points are assigned the smallest crowding distance of one.
         * The solutions having next-to-smallest Euclidean distance to all 
         * reference points are assigned the next-to-smallest crowding distance
         * of two, and so on. Thereafter, solutions with a smaller crowding
         * distance are preferred.
         **/
        LinkedList<Node> crowdingDistanceBySolutions = new LinkedList<RNSGAII.Node>();
        for (int indexOfSolution = 0; indexOfSolution < size; indexOfSolution++) {
            int minRanking = rankBySolutionsAndReferencePoints[0][indexOfSolution];
            for (int indexOfReferencePoint = 1; indexOfReferencePoint < numberOfReferencePoints; indexOfReferencePoint++)
            {
                if (rankBySolutionsAndReferencePoints[indexOfReferencePoint][indexOfSolution] < minRanking)
                {
                    minRanking = rankBySolutionsAndReferencePoints[indexOfReferencePoint][indexOfSolution];
                }
            }            
            crowdingDistanceBySolutions.add(new Node(indexOfSolution,minRanking));                       
        }          
        
        /**
         * Step 3 in paper:
         * To control the extent of obtained solutions, all solutions
         * having a sum of normalized difference in objective values of epsilon
         * or less between them are grouped. A randomly picked solution from
         * each group is retained and rest all group members are assigned a
         * large crowding distance in order to discourage them to remain in
         * the race.
         */                
        LinkedList<Node> markedPreferenceSolutions = new LinkedList<RNSGAII.Node>();        
        do {                        
            int randomSolutionIndex = PseudoRandom.randInt(0, crowdingDistanceBySolutions.size() - 1);
            
            Solution randomSolution = new Solution(solutionSet.get(crowdingDistanceBySolutions.get(randomSolutionIndex).solutionIndex));
            
            markedPreferenceSolutions.add(crowdingDistanceBySolutions.get(randomSolutionIndex));
            crowdingDistanceBySolutions.remove(randomSolutionIndex);
                        
            for (int indexOfSolution = 0; indexOfSolution < crowdingDistanceBySolutions.size(); indexOfSolution++) {
            	double sum = 0;
            	                                   
                for (int indexOfObjective = 0; indexOfObjective < this.problem_.getNumberOfObjectives(); indexOfObjective++) {
                    if (normalization) {
                        sum = sum + ((Math.abs(randomSolution.getObjective(indexOfObjective) - solutionSet.get(crowdingDistanceBySolutions.get(indexOfSolution).solutionIndex).getObjective(indexOfObjective))) / (upperBounds[indexOfObjective] - lowerBounds[indexOfObjective]));
                    } else {
                        sum = sum + Math.abs(randomSolution.getObjective(indexOfObjective) - solutionSet.get(crowdingDistanceBySolutions.get(indexOfSolution).solutionIndex).getObjective(indexOfObjective));
                    }
                }

                if (sum <= epsilon) {                    
                    crowdingDistanceBySolutions.get(indexOfSolution).distance = Double.MAX_VALUE;
                    markedPreferenceSolutions.add(crowdingDistanceBySolutions.get(indexOfSolution));
                    crowdingDistanceBySolutions.remove(indexOfSolution);
                }                
            }                
        } while (!crowdingDistanceBySolutions.isEmpty());

        return markedPreferenceSolutions;
    } // crowdingDistanceAssing

    /**
     *
     * @param individual
     */
    void updateLowerBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            if (individual.getObjective(n) < this.lowerBounds[n]) {
                this.lowerBounds[n] = individual.getObjective(n);
            }
        }
    } // updateLowerBounds

    /**
     *
     * @param individual
     */
    void updateUpperBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            if (individual.getObjective(n) > this.upperBounds[n]) {
                this.upperBounds[n] = individual.getObjective(n);
            }
        }
    } // updateUpperBounds
    
    /**
     *
     * @param individual
     */
    void updateUpperBounds(SolutionSet population) {
        for (int i = 0; i < population.size(); i++) {
            updateUpperBounds(population.get(i));
        }
    } // updateUpperBounds

    void updateLowerBounds(SolutionSet population) {        
        for (int i = 0; i < population.size(); i++) {
            updateLowerBounds(population.get(i));
        }
    } // updateLowerBounds

    /**
     *
     * @param individual
     */
    void updateBounds(Solution individual) {
        updateLowerBounds(individual);
        updateUpperBounds(individual);
    } // updateBounds

    /**
     *
     * @param individual
     */
    void initializeBounds() {
        lowerBounds = new double[problem_.getNumberOfObjectives()];       
        upperBounds = new double[problem_.getNumberOfObjectives()];        

        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.lowerBounds[n] = Double.MAX_VALUE;
            this.upperBounds[n] = Double.MIN_VALUE;
        }
    } // initializeBounds
} // RNSGA-II
