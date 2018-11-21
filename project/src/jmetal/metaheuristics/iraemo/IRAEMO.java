//  IRAEMO.java 
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

package jmetal.metaheuristics.iraemo;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
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
import jmetal.util.*;

public class IRAEMO extends Algorithm {

    Vector<double[][]> weights_;
    double[][] oddWeights_; 
    double[][] pairWeights_;  
    AchievementScalarizingFunction asfAspirationLevel,asfReservationLevel;
    ReferencePoint aspirationLevel, reservationLevel;
    boolean estimatePoints, normalization;
    String folderForOutputFiles;
    int populationSize, numberOfWeights, generations, evaluations;
    int executedIterations = 0;

    /**
     * Constructor
     *
     * @param problem Problem to solve
     */
    public IRAEMO(Problem problem) {
        super(problem);
    } // NSGAII

    /**
     * Runs the IRA-EMO algorithm.
     *
     * @return a <code>SolutionSet</code> that is a set of non dominated
     * solutions as a result of the algorithm execution
     * @throws JMException
     */
    public SolutionSet execute() throws JMException, ClassNotFoundException, IndexOutOfBoundsException {
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

                String weightsDirectory, weightsFileName;                                      

                SolutionSet population;
                SolutionSet offspringPopulation;
                SolutionSet union;

                Operator mutationOperator;
                Operator crossoverOperator;
                Operator selectionOperator;

                //Read the parameters
                populationSize = ((Integer) getInputParameter("populationSize")).intValue();
                numberOfWeights = ((Integer) getInputParameter("numberOfWeights")).intValue();
                generations = ((Integer) getInputParameter("generations")).intValue();                               
                folderForOutputFiles = (String) getInputParameter("folderForOutputFiles");
                normalization = ((Boolean) getInputParameter("normalization")).booleanValue();
                estimatePoints = ((Boolean) getInputParameter("estimatePoints")).booleanValue();

                if (estimatePoints) {
                    asfAspirationLevel = new AchievementScalarizingFunction(problem_.getNumberOfObjectives());
                    asfReservationLevel = new AchievementScalarizingFunction(problem_.getNumberOfObjectives());
                } else {
                    asfAspirationLevel = (AchievementScalarizingFunction) getInputParameter("asfAspirationLevel");
                    asfReservationLevel = (AchievementScalarizingFunction) getInputParameter("asfReservationLevel");
                }
                aspirationLevel = ((ReferencePoint) getInputParameter("aspirationLevel"));
                reservationLevel = ((ReferencePoint) getInputParameter("reservationLevel"));
                try {
                    asfAspirationLevel.setReferencePoint(aspirationLevel);
                    asfReservationLevel.setReferencePoint(reservationLevel);
                } catch (CloneNotSupportedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                weightsDirectory = getInputParameter("weightsDirectory").toString();
                weightsFileName = getInputParameter("weightsFileName").toString();

                //Initialize the variables
                population = new SolutionSet(populationSize);
                evaluations = 0;                

                //Read the operators
                mutationOperator = operators_.get("mutation");
                crossoverOperator = operators_.get("crossover");
                selectionOperator = operators_.get("selection");

                if (problem_.getNumberOfObjectives() == 2) {
                    weights_ = Weights.initUniformPairAndOddWeights2D(0.01, numberOfWeights);
                } else {                    
                    weights_ = Weights.getPairAndOddWeightsFromFile(weightsDirectory + java.io.File.separator + weightsFileName);
                }
                pairWeights_ = Weights.invertWeights(weights_.get(0), true);        
	        oddWeights_ = Weights.invertWeights(weights_.get(1), true);

                // Create the initial solutionSet
                if (estimatePoints) {
                    initializeBounds();
                }

                Solution newSolution;
                for (int i = 0; i < populationSize; i++) {
                    newSolution = new Solution(problem_);
                    problem_.evaluate(newSolution);
                    problem_.evaluateConstraints(newSolution);
                    evaluations++;
                    population.add(newSolution);

                    if (estimatePoints) {
                        updateBounds(newSolution);
                    }
                } //for

                // Generations 
                int localGenerations = 0;
                progressBar.setMaximum(generations);
                while (localGenerations < generations) //while (requiredEvaluations == 0 && evaluations < maxEvaluations)
                {
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

                        if (estimatePoints) {
                            updateBounds(offSpring[0]);
                            updateBounds(offSpring[1]);
                        }
                    } // for                     

                    // Create the solutionSet union of solutionSet and offSpring
                    union = ((SolutionSet) population).union(offspringPopulation);
                    
                    // Ranking the union
                    RankingASFs ranking = new RankingASFs(union, asfAspirationLevel, pairWeights_, asfReservationLevel, oddWeights_, normalization);                    

                    int remain = populationSize;
                    int index = 0;
                    SolutionSet front;
                    population.clear();

                    // Obtain the next front
                    front = ranking.getSubfront(index);

                    while ((remain > 0) && (remain >= front.size())) {
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
                        for (int k = 0; k < remain; k++) {
                            population.add(front.get(k));
                        } // for

                        remain = 0;
                    } // if                               

                    localGenerations++;
                    publish(localGenerations);
                } // while

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
            Logger.getLogger(IRAEMO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(IRAEMO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public SolutionSet[] executeFirstIteration() throws JMException, ClassNotFoundException {
        SolutionSet[] result = new SolutionSet[2];

        result[0] = new SolutionSet().union(execute());        
        result[1] = new RankingASFs(result[0], asfAspirationLevel, pairWeights_, asfReservationLevel, oddWeights_, normalization).getSubfront(0);                    

        //deleteWithChildren(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator);
        try {
            new File(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName()).mkdirs();
            aspirationLevel.writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "ASPIRATION IN 1.rl");
            reservationLevel.writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "RESERVATION IN 1.rl");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        result[0].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "POPULATION IN 1.txt");
        result[1].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "SOLUTIONS IN 1.txt");

        executedIterations = 1;

        return result;
    } // execute

    public SolutionSet[] doIteration(final SolutionSet population, final ReferencePoint newAspirationLevel, final ReferencePoint newReservationLevel, final int numberOfSolutions) throws JMException, ClassNotFoundException {
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
                Operator selectionOperator;

                if (numberOfWeights != numberOfSolutions) {
                    numberOfWeights = numberOfSolutions;
                    if (problem_.getNumberOfObjectives() == 2) {
                        weights_ = Weights.initUniformPairAndOddWeights2D(0.01, numberOfWeights);                        
                    } else {
                        String weightsDirectory = getInputParameter("weightsDirectory").toString();
                        String weightsFileName = getInputParameter("weightsFileName").toString();
                        weights_ = Weights.getPairAndOddWeightsFromFile(weightsDirectory + java.io.File.separator + weightsFileName);
                    }
                }
                pairWeights_ = Weights.invertWeights(weights_.get(0), true);        
	        oddWeights_ = Weights.invertWeights(weights_.get(1), true);
                    
                aspirationLevel = newAspirationLevel;
                reservationLevel = newReservationLevel;
                try {
                    asfAspirationLevel.setReferencePoint(aspirationLevel);
                    asfReservationLevel.setReferencePoint(reservationLevel);
                } catch (CloneNotSupportedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                //Initialize the variables
                //population = new SolutionSet(populationSize);
                evaluations = 0;

                //Read the operators
                mutationOperator = operators_.get("mutation");
                crossoverOperator = operators_.get("crossover");
                selectionOperator = operators_.get("selection");

                // Create the initial solutionSet
                if (estimatePoints) {
                    initializeBounds();
                    updateLowerBounds(population);
                    updateUpperBounds(population);
                }

                // Generations 
                int localGenerations = 0;
                progressBar.setMaximum(generations);
                while (localGenerations < generations) {
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

                        if (estimatePoints) {
                            updateBounds(offSpring[0]);
                            updateBounds(offSpring[1]);
                        }
                    } // for                     

                    // Create the solutionSet union of solutionSet and offSpring
                    union = ((SolutionSet) population).union(offspringPopulation);

                    // Ranking the union                    
                    RankingASFs ranking = new RankingASFs(union, asfAspirationLevel, pairWeights_, asfReservationLevel, oddWeights_, normalization);
                    int remain = populationSize;
                    int index = 0;
                    SolutionSet front;
                    population.clear();

                    // Obtain the next front
                    front = ranking.getSubfront(index);

                    while ((remain > 0) && (remain >= front.size())) {
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
                        for (int k = 0; k < remain; k++) {
                            population.add(front.get(k));
                        } // for

                        remain = 0;
                    } // if                               

                    localGenerations++;
                    publish(localGenerations);
                } // while
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
            Logger.getLogger(IRAEMO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(IRAEMO.class.getName()).log(Level.SEVERE, null, ex);
        }

        SolutionSet[] result = new SolutionSet[2];
        result[0] = new SolutionSet().union(solutions);        
        result[1] = new RankingASFs(solutions, asfAspirationLevel, pairWeights_, asfReservationLevel, oddWeights_, normalization).getSubfront(0);                    

        executedIterations++;

        try {
            aspirationLevel.writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "ASPIRATION IN " + executedIterations + ".rl");
            reservationLevel.writeInFile(this.folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "RESERVATION IN " + executedIterations + ".rl");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        result[0].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "POPULATION IN " + executedIterations + ".txt");
        result[1].printObjectivesToFile(folderForOutputFiles + java.io.File.separator + problem_.getName() + java.io.File.separator + this.getClass().getSimpleName() + java.io.File.separator + "SOLUTIONS IN " + executedIterations + ".txt");

        return result;
    }

    /**
     *
     * @param individual
     */
    void updateLowerBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            if (individual.getObjective(n) < this.asfAspirationLevel.getIdeal()[n]) {
                this.asfAspirationLevel.setIdeal(n, individual.getObjective(n));
            }
        }
    } // updateLowerBounds

    /**
     *
     * @param individual
     */
    void updateUpperBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            if (individual.getObjective(n) > this.asfAspirationLevel.getNadir()[n]) {
                this.asfAspirationLevel.setNadir(n, individual.getObjective(n));
            }
        }
    } // updateUpperBounds

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
    void updateUpperBounds(SolutionSet population) {
        initializeUpperBounds(population.get(0));

        for (int i = 1; i < population.size(); i++) {
            updateUpperBounds(population.get(i));
        }
    } // updateNadirPoint

    void updateLowerBounds(SolutionSet population) {
        initializeLowerBounds(population.get(0));

        for (int i = 1; i < population.size(); i++) {
            updateLowerBounds(population.get(i));
        }
    } // updateNadirPoint

    /**
     *
     * @param individual
     */
    void initializeUpperBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.asfAspirationLevel.setNadir(n, individual.getObjective(n));
        }
    } // initializeUpperBounds

    /**
     *
     * @param individual
     */
    void initializeUpperBounds() {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.asfAspirationLevel.setNadir(n, Double.MIN_VALUE);
        }
    } // initializeUpperBounds

    /**
     *
     * @param individual
     */
    void initializeLowerBounds() {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.asfAspirationLevel.setIdeal(n, Double.MAX_VALUE);
        }
    } // initializeUpperBounds

    /**
     *
     * @param individual
     */
    void initializeLowerBounds(Solution individual) {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.asfAspirationLevel.setIdeal(n, individual.getObjective(n));
        }
    } // initializeUpperBounds

    /**
     *
     * @param individual
     */
    void initializeBounds() {
        for (int n = 0; n < problem_.getNumberOfObjectives(); n++) {
            this.asfAspirationLevel.setIdeal(n, Double.MAX_VALUE);
            this.asfAspirationLevel.setNadir(n, Double.MIN_VALUE);
        }
    } // initializeBounds

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
} // GWASFGA
