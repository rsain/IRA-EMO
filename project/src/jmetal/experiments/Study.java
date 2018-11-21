/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmetal.experiments;

import com.panayotis.gnuplot.GNUPlotException;
import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.dataset.FileDataSet;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.swing.JPlot;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.experiments.settings.IRAEMO_Settings;
import jmetal.metaheuristics.iraemo.IRAEMO;
import jmetal.problems.ProblemFactory;
import jmetal.qualityIndicator.util.MetricsUtil;
import jmetal.util.AchievementScalarizingFunction;
import jmetal.util.JMException;
import jmetal.util.ReferencePoint;
//import jmetal.util.kmeans.Cluster;
import jmetal.util.kmeans.KMeans;
import jmetal.util.kmeans.Point;
import jmetal.needed.ValuePath;
import jmetal.util.Distance;
import jmetal.util.Cluster_set;
import jmetal.core.Solution;
import jmetal.util.ParetoDominance;
import jmetal.experiments.settings.RNSGAII_Settings;
import jmetal.metaheuristics.RnsgaII.RNSGAII;

/**
 *
 * @author ruben
 */
public class Study extends javax.swing.JFrame {

    final char SEPARATOR_INDEX_IN_NAME = '-';
    final String[] FILE_OF_WEIGHTS_FOR_KMEANS = new String[]{"300W-KSIMPLEX", "364W-KSIMPLEX", "330W-KSIMPLEX", "462W-KSIMPLEX"};
    final String OUTPUT_FOLDER = "./IRA-Study";
    final String PARETO_FRONTS_FOLDER = "data/paretoFronts/";
    final int DEFAULT_POPULATION_SIZE_FOR_2D = 200;
    final int DEFAULT_GENERATIONS_NUMBER_FOR_2D = 300;
    final int DEFAULT_POPULATION_SIZE_FOR_3D = 300;
    final int DEFAULT_GENERATIONS_NUMBER_FOR_3D = 400;
    private int DECIMALS = 4;

    private Map<String, JLabel> jLabelAspirationLevel = new HashMap<>();
    private Map<String, MyComponents.FloatJSlider> floatJSliderAspirationLevel = new HashMap<>();
    private Map<String, JLabel> jLabelReservationLevel = new HashMap<>();
    private Map<String, MyComponents.FloatJSlider> floatJSliderReservationLevel = new HashMap<>();
    private SolutionSet[] solutions;
    private double[] idealPoint, nadirPoint;
    private Algorithm algorithm;    

    JavaPlot javaPlot1;
        
    private boolean initExperiment(String problemName, String paretoFrontFilePath, String folderForOutputFiles) throws JMException, CloneNotSupportedException {
        String soltype;
        if (problemName.compareTo("AuxiliaryServicesProblem")== 0){
            soltype = "ArrayRealAndBinary";
            DECIMALS = 2;
        }
        else
            soltype = "Real";
        Object[] problemParams = {soltype};
        Problem problem = (new ProblemFactory()).getProblem(problemName, problemParams);
        MetricsUtil paretoFrontInformation = new MetricsUtil();
        boolean result = false;
        int objectivesNumber = ((Integer) jSpinnerObjectivesNumber.getValue()).intValue();
        
        if (new File(paretoFrontFilePath).exists()) {
            double[][] paretoFront = paretoFrontInformation.readFront(paretoFrontFilePath);

            idealPoint = paretoFrontInformation.getMinimumValues(paretoFront, objectivesNumber);
            nadirPoint = paretoFrontInformation.getMaximumValues(paretoFront, objectivesNumber);
            
            result = true;
        } else if (problemName.contains("DTLZ")) {
            if (problemName.equals("DTLZ1")) {
                idealPoint = new double[objectivesNumber];
                nadirPoint = new double[objectivesNumber];

                for (int i = 0; i < objectivesNumber; i++) {
                    idealPoint[i] = 0;
                    nadirPoint[i] = 0.5;
                }

                result = true;
            } else if (problemName.equals("DTLZ2")) {
                idealPoint = new double[objectivesNumber];
                nadirPoint = new double[objectivesNumber];

                for (int i = 0; i < objectivesNumber; i++) {
                    idealPoint[i] = 0;
                    nadirPoint[i] = 1;
                }

                result = true;
            }
        }
        
        HashMap parameters = new HashMap();
        //Common configuration        
        parameters.put("populationSize_", ((Integer) jSpinnerPopulationSize.getValue()).intValue());
        parameters.put("generations_", ((Integer) jSpinnerGenerationsNumber.getValue()).intValue());
        parameters.put("folderForOutputFiles_", folderForOutputFiles);
        if (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")){
             String weightsDirectory = new String("data/weights");
            //String weightsFileName = new String(((Integer) jSpinnerSolutionsNumber.getValue()).intValue() + "W." + objectivesNumber + "D");
            String weightsFileName = new String(((Integer) jSpinnerSolutionsNumber.getValue()).intValue()*2 + "W." + objectivesNumber + "D");
            parameters.put("numberOfWeights_", ((Integer) jSpinnerSolutionsNumber.getValue()).intValue()*2);
            parameters.put("weightsDirectory_", weightsDirectory);
            parameters.put("weightsFileName_", weightsFileName);
            parameters.put("allowRepetitions_", true);
            parameters.put("normalization_", true);
            parameters.put("estimatePoints_", false);
            
            if (result)
            {
                parameters.put("aspirationLevel_", new ReferencePoint(idealPoint));
                parameters.put("asfAspirationLevel_", new AchievementScalarizingFunction(null, nadirPoint, idealPoint));

                parameters.put("reservationLevel_", new ReferencePoint(nadirPoint));
                parameters.put("asfReservationLevel_", new AchievementScalarizingFunction(null, nadirPoint, idealPoint));
                
                try {
                    algorithm = (IRAEMO) (new IRAEMO_Settings(problemName, ((Integer) jSpinnerObjectivesNumber.getValue()).intValue()).configure(parameters));
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        else if (jComboBoxAlgorithm.getSelectedItem().equals("RNSGAII")){
            parameters.put("normalization_", true);
            parameters.put("estimateObjectivesBounds_", true);
            parameters.put("epsilon_", 0.001);
            
            if (result)
            {
                ReferencePoint[] referencePoints = new ReferencePoint[2];
                referencePoints[0] = new ReferencePoint(idealPoint);
                referencePoints[1] = new ReferencePoint(nadirPoint);
                
                parameters.put("referencePoints_", referencePoints);
                
                try {
                    algorithm = (RNSGAII) (new RNSGAII_Settings(problemName, objectivesNumber)).configure(parameters);            
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                }
            }                        
        }

        return result;
    }

    /**
     * Creates new form Ventana
     */
    public Study() {
        beforeInitComponents();

        initComponents();
        
        //To scroll text area
        DefaultCaret caret = (DefaultCaret)jTextAreaLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        this.setLocationRelativeTo(null);

        this.jPanelAspirationLevel.removeAll();
        this.jPanelReservationLevel.removeAll();

        try {
            initExperiment((String) jComboBoxProblemName.getSelectedItem(), PARETO_FRONTS_FOLDER + jComboBoxProblemName.getSelectedItem() + "." + jSpinnerObjectivesNumber.getValue().toString() + "D.pf", OUTPUT_FOLDER);
        } catch (JMException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        }

        jSpinnerSolutionsNumber.setValue(((Integer) jSpinnerObjectivesNumber.getValue()).intValue() * 2);
        jSpinnerPopulationSize.setValue(200);
        jSpinnerGenerationsNumber.setValue(300);

        try {
            loadReferenceLevelsUI(Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()), ((Integer) jSpinnerSolutionsNumber.getValue()).intValue(), idealPoint, nadirPoint);
        } catch (JMException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")){
            algorithm.setInputParameter("aspirationLevel", getAspirationLevel());
            algorithm.setInputParameter("reservationLevel", getReservationLevel());
        }
        else if (jComboBoxAlgorithm.getSelectedItem().equals("RNSGAII")){
            ReferencePoint[] referencePoints = new ReferencePoint[2];
            referencePoints[0] = getAspirationLevel();
            referencePoints[1] = getReservationLevel();                
            algorithm.setInputParameter("referencePoints", referencePoints);
        }

        //Load problem's pareto optimal front
        javaPlot1.set("term", "png size " + (jPlot1.getWidth() - 1) + ", " + (jPlot1.getHeight() - 1));
        javaPlot1.set("grid", "");

        plotParetoFront(jPlot1);
        plotAspirationLevel(jPlot1);
        plotReservationLevel(jPlot1);
        repaintPlot(jPlot1);

        jTextAreaLog.append("Application started successfully ;-)");
        redirectSystemStreams();
    }

    private void beforeInitComponents() {
        try {
            InputStream is;
            is = new FileInputStream("data/IRA.config");
            Properties prop = new Properties();
            prop.load(is);

            String parameter = prop.getProperty("GNUPLOT_PATH");
            if (new File(parameter).exists()) {
                javaPlot1 = new com.panayotis.gnuplot.JavaPlot(parameter);
                javaPlot1.setGNUPlotPath(parameter);
            } else {
                try {
                    javaPlot1 = new com.panayotis.gnuplot.JavaPlot("c:\\gnuplot\\bin\\gnuplot.exe");
                    javaPlot1.setGNUPlotPath("c:\\gnuplot\\bin\\gnuplot.exe");
                } catch (GNUPlotException ex) {
                    JOptionPane.showMessageDialog(null, "It is not possible to find GNUPlot in the system."
                            + "\n\n"
                            + "You have to check if it is installed and the information in the configuration file 'data\\IRA.config' is correct."
                            + "\n\n"
                            + "If you need help you can read the 'README.txt' file.", "IRA: Problems with GNUPLot.", JOptionPane.ERROR_MESSAGE);
                    System.exit(-1);
                }

                JOptionPane.showMessageDialog(null,
                        "The file '" + parameter + "' does not exist."
                        + "\n\n"
                        + "If GNUPlot.exe is not in the default path ('C:\\gnuplot\\bin\\gnuplot.exe'), the application will fail."
                        + "\n"
                        + "In this case, you must install GNUPlot and modify adequately the file 'data\\IRA.config' using your favourite text editor.",
                        "IRA: Problems with GNUPLot?", JOptionPane.WARNING_MESSAGE);
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "The configuration file 'data\\IRA.config' does not exist.\n\nGNUPlot.exe must be in the default path (c:\\gnuplot\\bin\\gnuplot.exe).", "IRA: Problems with the configuration file.", JOptionPane.WARNING_MESSAGE);
            try {
                javaPlot1 = new com.panayotis.gnuplot.JavaPlot("c:\\gnuplot\\bin\\gnuplot.exe");
                //javaPlot1.setGNUPlotPath("c:\\gnuplot\\bin\\gnuplot.exe");
            } catch (GNUPlotException ex1) {
                JOptionPane.showMessageDialog(null, "It is not possible to find GNUPlot in the system."
                        + "\n\n"
                        + "You have to check if it is installed and the information in the configuration file 'data\\IRA.config' is correct."
                        + "\n\n"
                        + "If you need help you can read the 'README.txt' file.", "IRA: Problems with GNUPLot.", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        } catch (IOException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        }

        javaPlot1.setPersist(false);
        javaPlot1.setTerminal(null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelAspirationLevel = new javax.swing.JPanel();
        jLabel0 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        floatJSlider2 = new MyComponents.FloatJSlider();
        jLabel6 = new javax.swing.JLabel();
        jPanelReservationLevel = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        floatJSlider3 = new MyComponents.FloatJSlider();
        jLabel22 = new javax.swing.JLabel();
        jPanelActions = new javax.swing.JPanel();
        jButtonStart = new javax.swing.JButton();
        jButtonNextIteration = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jScrollPaneSolutions = new javax.swing.JScrollPane();
        jTableSolutions = new javax.swing.JTable();
        jScrollPaneLog = new javax.swing.JScrollPane();
        jTextAreaLog = new javax.swing.JTextArea();
        jPanelPlot = new javax.swing.JPanel();
        jPlot1 = new com.panayotis.gnuplot.swing.JPlot(javaPlot1);
        jPanelAlgorithmConfiguration = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jSpinnerSolutionsNumber = new javax.swing.JSpinner();
        jSpinnerGenerationsNumber = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jSpinnerPopulationSize = new javax.swing.JSpinner();
        jLabel23 = new javax.swing.JLabel();
        jComboBoxAlgorithm = new javax.swing.JComboBox<>();
        jPanelProblemConfiguration = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jComboBoxProblemName = new javax.swing.JComboBox();
        jSpinnerObjectivesNumber = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Interactive Optimization using Reservation and Aspiration Levels");
        setIconImages(null);
        setResizable(false);

        jPanelAspirationLevel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Aspiration level", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanelAspirationLevel.setMaximumSize(new java.awt.Dimension(100, 100));
        jPanelAspirationLevel.setMinimumSize(new java.awt.Dimension(50, 50));
        jPanelAspirationLevel.setPreferredSize(new java.awt.Dimension(100, 325));
        jPanelAspirationLevel.setLayout(new java.awt.GridLayout(2, 4));

        jLabel0.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel0.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel0.setText("f");
        jLabel0.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel0.setName("jLabel0"); // NOI18N
        jPanelAspirationLevel.add(jLabel0);

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel13.setText("value");
        jPanelAspirationLevel.add(jLabel13);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("ideal");
        jPanelAspirationLevel.add(jLabel1);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("-");
        jPanelAspirationLevel.add(jLabel2);

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("nadir");
        jPanelAspirationLevel.add(jLabel3);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("jLabel4");
        jPanelAspirationLevel.add(jLabel4);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("jLabel4");
        jPanelAspirationLevel.add(jLabel14);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("jLabel5");
        jPanelAspirationLevel.add(jLabel5);
        jPanelAspirationLevel.add(floatJSlider2);

        jLabel6.setText("jLabel6");
        jPanelAspirationLevel.add(jLabel6);

        jPanelReservationLevel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Reservation level", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanelReservationLevel.setMaximumSize(new java.awt.Dimension(100, 100));
        jPanelReservationLevel.setMinimumSize(new java.awt.Dimension(50, 50));
        jPanelReservationLevel.setPreferredSize(new java.awt.Dimension(100, 325));
        jPanelReservationLevel.setLayout(new java.awt.GridLayout(2, 4));

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("f");
        jLabel12.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel12.setName("jLabel0"); // NOI18N
        jPanelReservationLevel.add(jLabel12);

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("value");
        jPanelReservationLevel.add(jLabel15);

        jLabel16.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("ideal");
        jPanelReservationLevel.add(jLabel16);

        jLabel17.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("-");
        jPanelReservationLevel.add(jLabel17);

        jLabel18.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel18.setText("nadir");
        jPanelReservationLevel.add(jLabel18);

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("jLabel4");
        jPanelReservationLevel.add(jLabel19);

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("jLabel4");
        jPanelReservationLevel.add(jLabel20);

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setText("jLabel5");
        jPanelReservationLevel.add(jLabel21);
        jPanelReservationLevel.add(floatJSlider3);

        jLabel22.setText("jLabel6");
        jPanelReservationLevel.add(jLabel22);

        jPanelActions.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Solution process", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanelActions.setLayout(new java.awt.GridLayout(1, 0));

        jButtonStart.setText("Start");
        jButtonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartActionPerformed(evt);
            }
        });
        jPanelActions.add(jButtonStart);

        jButtonNextIteration.setText("Next iteration");
        jButtonNextIteration.setEnabled(false);
        jButtonNextIteration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextIterationActionPerformed(evt);
            }
        });
        jPanelActions.add(jButtonNextIteration);

        jButton2.setLabel("Exit");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanelActions.add(jButton2);

        jScrollPaneSolutions.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Solutions", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jScrollPaneSolutions.setAutoscrolls(true);
        jScrollPaneSolutions.setMaximumSize(new java.awt.Dimension(100, 100));
        jScrollPaneSolutions.setPreferredSize(new java.awt.Dimension(300, 200));

        jTableSolutions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableSolutions.setEnabled(false);
        jTableSolutions.setMaximumSize(new java.awt.Dimension(2147483647, 100000076));
        jTableSolutions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTableSolutions.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTableSolutionsMousePressed(evt);
            }
        });
        jScrollPaneSolutions.setViewportView(jTableSolutions);

        jScrollPaneLog.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));

        jTextAreaLog.setEditable(false);
        jTextAreaLog.setColumns(20);
        jTextAreaLog.setForeground(new java.awt.Color(0, 0, 102));
        jTextAreaLog.setLineWrap(true);
        jTextAreaLog.setRows(5);
        jScrollPaneLog.setViewportView(jTextAreaLog);

        jPanelPlot.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot"));

        jPlot1.setBackground(new java.awt.Color(255, 255, 255));
        jPlot1.setAutoscrolls(true);
        jPlot1.setJavaPlot(javaPlot1);

        javax.swing.GroupLayout jPlot1Layout = new javax.swing.GroupLayout(jPlot1);
        jPlot1.setLayout(jPlot1Layout);
        jPlot1Layout.setHorizontalGroup(
            jPlot1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPlot1Layout.setVerticalGroup(
            jPlot1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanelPlotLayout = new javax.swing.GroupLayout(jPanelPlot);
        jPanelPlot.setLayout(jPanelPlotLayout);
        jPanelPlotLayout.setHorizontalGroup(
            jPanelPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPlot1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanelPlotLayout.setVerticalGroup(
            jPanelPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPlot1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanelAlgorithmConfiguration.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Algorithm's configuration", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Number of solutions:");
        jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel8.setName("jLabel0"); // NOI18N

        jSpinnerSolutionsNumber.setModel(new javax.swing.SpinnerNumberModel(4, 1, 50, 1));

        jSpinnerGenerationsNumber.setModel(new javax.swing.SpinnerNumberModel(100, 100, 10000, 100));
        jSpinnerGenerationsNumber.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerGenerationsNumberStateChanged(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText("Number of generations:");
        jLabel9.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel9.setName("jLabel0"); // NOI18N

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel11.setText("Population size:");
        jLabel11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel11.setName("jLabel0"); // NOI18N

        jSpinnerPopulationSize.setModel(new javax.swing.SpinnerNumberModel(50, 50, 1000, 50));
        jSpinnerPopulationSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerPopulationSizeStateChanged(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel23.setText("Algorithm");
        jLabel23.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel23.setName("jLabel0"); // NOI18N

        jComboBoxAlgorithm.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "IRAEMO", "RNSGAII" }));
        jComboBoxAlgorithm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxAlgorithmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelAlgorithmConfigurationLayout = new javax.swing.GroupLayout(jPanelAlgorithmConfiguration);
        jPanelAlgorithmConfiguration.setLayout(jPanelAlgorithmConfigurationLayout);
        jPanelAlgorithmConfigurationLayout.setHorizontalGroup(
            jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAlgorithmConfigurationLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jSpinnerPopulationSize)
                        .addComponent(jSpinnerSolutionsNumber)
                        .addComponent(jSpinnerGenerationsNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jComboBoxAlgorithm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanelAlgorithmConfigurationLayout.setVerticalGroup(
            jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAlgorithmConfigurationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBoxAlgorithm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSpinnerSolutionsNumber))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jSpinnerPopulationSize)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelAlgorithmConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel9)
                    .addComponent(jSpinnerGenerationsNumber))
                .addContainerGap())
        );

        jComboBoxAlgorithm.getAccessibleContext().setAccessibleName("");

        jPanelProblemConfiguration.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Problem's configuration", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel7.setText("Problem name:");
        jLabel7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel7.setName("jLabel0"); // NOI18N

        jComboBoxProblemName.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6", "DTLZ1", "DTLZ2", "DTLZ3", "DTLZ4", "DTLZ5", "DTLZ6", "DTLZ7", "WFG1", "WFG2", "WFG3", "WFG4", "WFG5", "WFG6", "WFG7", "WFG8", "WFG9", "AuxiliaryServicesProblem" }));
        jComboBoxProblemName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxProblemNameActionPerformed(evt);
            }
        });

        jSpinnerObjectivesNumber.setModel(new javax.swing.SpinnerNumberModel(2, 2, 6, 1));
        jSpinnerObjectivesNumber.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerObjectivesNumberStateChanged(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Objectives number:");
        jLabel10.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel10.setName("jLabel0"); // NOI18N

        javax.swing.GroupLayout jPanelProblemConfigurationLayout = new javax.swing.GroupLayout(jPanelProblemConfiguration);
        jPanelProblemConfiguration.setLayout(jPanelProblemConfigurationLayout);
        jPanelProblemConfigurationLayout.setHorizontalGroup(
            jPanelProblemConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelProblemConfigurationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSpinnerObjectivesNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jComboBoxProblemName, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelProblemConfigurationLayout.setVerticalGroup(
            jPanelProblemConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelProblemConfigurationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelProblemConfigurationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxProblemName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinnerObjectivesNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneSolutions, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                    .addComponent(jPanelActions, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelAlgorithmConfiguration, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelAspirationLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelReservationLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelPlot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelProblemConfiguration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addComponent(jScrollPaneLog, javax.swing.GroupLayout.DEFAULT_SIZE, 1012, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelAlgorithmConfiguration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelAspirationLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanelReservationLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPaneSolutions, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelActions, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelProblemConfiguration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelPlot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneLog, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE))
        );

        jPanelReservationLevel.getAccessibleContext().setAccessibleName("Reserve level");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonNextIterationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextIterationActionPerformed
        final Study study = this;

        Thread thr = new Thread() {
            @Override
            public void run() {
                if (new File((PARETO_FRONTS_FOLDER + jComboBoxProblemName.getSelectedItem() + "." + jSpinnerObjectivesNumber.getValue().toString() + "D.pf")).exists()
                        || jComboBoxProblemName.getSelectedItem().equals("DTLZ1")
                        || jComboBoxProblemName.getSelectedItem().equals("DTLZ2")) {
                    //algorithm.setInputParameter("populationSize_", ((Integer)jSpinnerPopulationSize.getValue()).intValue());
                    //algorithm.setInputParameter("numberOfWeights_", ((Integer)jSpinnerSolutionsNumber.getValue()).intValue());            
                    //algorithm.setInputParameter("referencePoint", getReferencePoint());

                    if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) > 2 &&  (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO"))) {
                        //String weightsFileName = jSpinnerSolutionsNumber.getValue() + "W." + jSpinnerObjectivesNumber.getValue() + "D";
                        String weightsFileName = ((int) jSpinnerSolutionsNumber.getValue())*2 + "W." + jSpinnerObjectivesNumber.getValue() + "D";
                        algorithm.setInputParameter("weightsFileName", weightsFileName);
                        generateWeightsVectorsWithKmeans((String) algorithm.getInputParameter("weightsDirectory"), (String) algorithm.getInputParameter("weightsFileName"));
                    }

                    if (!SwingUtilities.isEventDispatchThread()) {
                        SwingUtilities.invokeLater(new Runnable() {
                          @Override
                          public void run() {
                             jTextAreaLog.append("\n- Iteration run using Aspiration level " + getAspirationLevel().toString(DECIMALS) + " and Reservation level " + getReservationLevel().toString(DECIMALS) + ".");
                          }
                        });                        
                    }
                    //jTextAreaLog.append("\n- Iteration run using Aspiration level " + getAspirationLevel().toString(DECIMALS) + " and Reservation level " + getReservationLevel().toString(DECIMALS) + ".");

                    try {        
                         if (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")){
                            solutions = ((IRAEMO) algorithm).doIteration(solutions[0], getAspirationLevel(), getReservationLevel(), ((Integer) jSpinnerSolutionsNumber.getValue()).intValue()*2);
                         }
                         else if (jComboBoxAlgorithm.getSelectedItem().equals("RNSGAII")){
                             solutions = ((RNSGAII) algorithm).doIteration(solutions[0], getAspirationLevel(), getReservationLevel());
                         }
                    } catch (JMException ex) {
                        Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //readSolutions(solutions[1]);
                    SolutionSet solutions_to_show = filtersolutions(solutions[1]);    
                    solutions_to_show.printObjectivesToFile(OUTPUT_FOLDER + java.io.File.separator + jComboBoxProblemName.getSelectedItem().toString() + java.io.File.separator + jComboBoxAlgorithm.getSelectedItem().toString() + java.io.File.separator + "ROI.txt");
                    readSolutions(solutions_to_show);
                    
                    jPlot1.getJavaPlot().getPlots().clear();
                    if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) < 3) {
                        plotParetoFront(jPlot1);
                        plotSolutions(solutions_to_show, jPlot1);
                        plotAspirationLevel(jPlot1);
                        plotReservationLevel(jPlot1);
                    } else {
                        try {
                            plotValuePath(solutions_to_show, true);
                        } catch (JMException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    repaintPlot(jPlot1);
                    //setTickLabels();
                } else {
                    JOptionPane.showMessageDialog(study, "The " + jSpinnerObjectivesNumber.getValue().toString() + " objectives " + jComboBoxProblemName.getSelectedItem() + " problem does not exist.");
                }
            }
        };
        thr.start();
        thr.yield();
    }//GEN-LAST:event_jButtonNextIterationActionPerformed

    private ReferencePoint getAspirationLevel() {
        ReferencePoint result;

        double[] values = new double[floatJSliderAspirationLevel.size()];
        for (int i = 0; i < floatJSliderAspirationLevel.size(); i++) {
            values[i] = floatJSliderAspirationLevel.get("floatJSliderAspirationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue();
        }

        result = new ReferencePoint(values);

        return result;
    }
    
    private ReferencePoint getReservationLevel() {
        ReferencePoint result;

        double[] values = new double[floatJSliderReservationLevel.size()];
        for (int i = 0; i < floatJSliderReservationLevel.size(); i++) {
            values[i] = floatJSliderReservationLevel.get("floatJSliderReservationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue();
        }

        result = new ReferencePoint(values);

        return result;
    }

    private double roundWithPrecision(double number, int numberOfDigits) {
        return (Math.rint(number * Math.pow(10, numberOfDigits)) / Math.pow(10, numberOfDigits));
    }

    private ReferencePoint loadReferenceLevelsUI(int numberOfObjectives, int numberOfSolutions, double[] idealPoint, double[] nadirPoint) throws JMException{
        //We show the information for the solutions in a table                     
        ReferencePoint result = new ReferencePoint(numberOfObjectives);
        String[] names;
        //double referenceValue;
        Double[][] data;
        int i;
        JLabel lbl;
        MyComponents.FloatJSlider fs;
        DefaultTableModel tm;
        String soltype;
        String problemName = (String) jComboBoxProblemName.getSelectedItem();
        if (problemName.compareTo("AuxiliaryServicesProblem")== 0){
            soltype = "ArrayRealAndBinary";
            DECIMALS = 2;
        }
        else
            soltype = "Real";
        Object[] problemParams = {soltype};
        Problem problem = (new ProblemFactory()).getProblem(problemName, problemParams);

        data = new Double[numberOfSolutions][numberOfObjectives + 1];
        
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 100;
        c.weighty = 100;

        Font newLabelFont = new Font(jLabel0.getFont().getName(), Font.BOLD, jLabel0.getFont().getSize());

        //Aspiration Level code
        floatJSliderAspirationLevel.clear();
        jPanelAspirationLevel.removeAll();        
        GridBagLayout gbl = new GridBagLayout();
        this.jPanelAspirationLevel.setLayout(gbl);        
        //this.jPanelReferencePoint.setLayout(new GridLayout(1 + numberOfObjectives, 4));
        lbl = new JLabel("f");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelAspirationObjective");
        lbl.setFont(newLabelFont);
        c.gridx = 0;
        c.gridy = 0;
        jPanelAspirationLevel.add(lbl, c);
        lbl = new JLabel("Value");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelAspirationSelecteValue");
        lbl.setFont(newLabelFont);
        c.gridx = 1;
        jPanelAspirationLevel.add(lbl, c);
        lbl = new JLabel("Ideal");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelAspirationIdeal");
        lbl.setFont(newLabelFont);
        c.gridx = 2;
        jPanelAspirationLevel.add(lbl, c);
        lbl = new JLabel("");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelAspirationValue");
        lbl.setFont(newLabelFont);
        c.gridx = 3;
        jPanelAspirationLevel.add(lbl, c);
        lbl = new JLabel("Nadir");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelAspirationNadir");
        lbl.setFont(newLabelFont);
        c.gridx = 4;
        jPanelAspirationLevel.add(lbl, c);
        
        //Reservation Level code
        floatJSliderReservationLevel.clear();
        jPanelReservationLevel.removeAll();
        gbl = new GridBagLayout();
        this.jPanelReservationLevel.setLayout(gbl);
        lbl = new JLabel("f");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelReservationObjective");
        lbl.setFont(newLabelFont);
        c.gridx = 0;
        c.gridy = 0;
        jPanelReservationLevel.add(lbl, c);
        lbl = new JLabel("Value");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelReservationSelecteValue");
        lbl.setFont(newLabelFont);
        c.gridx = 1;
        jPanelReservationLevel.add(lbl, c);
        lbl = new JLabel("Ideal");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelReservationIdeal");
        lbl.setFont(newLabelFont);
        c.gridx = 2;
        jPanelReservationLevel.add(lbl, c);
        lbl = new JLabel("");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelReservationValue");
        lbl.setFont(newLabelFont);
        c.gridx = 3;
        jPanelReservationLevel.add(lbl, c);
        lbl = new JLabel("Nadir");
        lbl.setVisible(true);
        lbl.setEnabled(true);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setName("jLabelReservationNadir");
        lbl.setFont(newLabelFont);
        c.gridx = 4;
        jPanelReservationLevel.add(lbl, c);

        c.gridy++;
        names = new String[numberOfObjectives + 1];
        names[0] = "Solution";
        for (i = 0; i < numberOfObjectives; i++) {
            names[i + 1] = "f " + (i + 1);

            //referenceValue = ((Math.abs(nadirPoint[i]) - idealPoint[i]) / 2) + idealPoint[i];

            //We show the information for the aspiration level
            lbl = new JLabel("f" + (i + 1));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelAspirationObjective" + SEPARATOR_INDEX_IN_NAME + i);
            c.gridx = 0;            
            c.weightx = 1;
            jPanelAspirationLevel.add(lbl, c);

            ////lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(referenceValue), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(idealPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
             
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelAspirationLevel.put(lbl.getName(), lbl);
            c.gridx = 1;
            jPanelAspirationLevel.add(lbl, c);

            //lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(idealPoint[i]), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(idealPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelAspirationMinimum" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelAspirationLevel.put(lbl.getName(), lbl);
            c.gridx = 2;
            jPanelAspirationLevel.add(lbl, c);

            fs = new MyComponents.FloatJSlider();

            fs.setName("floatJSliderAspirationLevel" + SEPARATOR_INDEX_IN_NAME + i);
            fs.setFloatPrecision(DECIMALS);
//            if(problem.getObjectiveOpt() == null){
//                fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());    
//                fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
//                fs.setFloatValue((float) roundWithPrecision(idealPoint[i], DECIMALS));
//            }
//            else{
//                if (Double.valueOf(idealPoint[i]).equals(0.0)){
//                    fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());
//                    fs.setFloatValue((float) roundWithPrecision(idealPoint[i], DECIMALS));
//                }
//                else{           
//                    fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue() * Double.valueOf(problem.getObjectiveOpt()[i]).floatValue());
//                    fs.setFloatValue((float) roundWithPrecision(idealPoint[i]* problem.getObjectiveOpt()[i], DECIMALS));
//                }
//                if (Double.valueOf(nadirPoint[i]).equals(0.0))
//                    fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
//                else                    
//                    fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue() * Double.valueOf(problem.getObjectiveOpt()[i]).floatValue());
//            } 
                
            fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());
            fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
            fs.setFloatValue((float) roundWithPrecision(idealPoint[i], DECIMALS));
            //fs.setFloatValue((float) roundWithPrecision(Math.abs(idealPoint[i]), DECIMALS));
            
            /*fs.setPaintLabels(true);
             Hashtable tickLabel = new Hashtable();
             tickLabel.put(new Integer(scaleFloatToIntValue(fs.getFloatValue(), fs.getMinimum(), fs.getMaximum(), fs.getFloatMinimum(), fs.getFloatMaximum())), new JLabel("|"));// + jTable1.getValueAt(jTable1.getSelectedRow(), i)));
             fs.setLabelTable(tickLabel);*/
            fs.setVisible(true);
            fs.setEnabled(true);
            floatJSliderAspirationLevel.put(fs.getName(), fs);
            fs.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    String name = new String(((MyComponents.FloatJSlider) e.getSource()).getName());
                    String indexOfComponent = name.substring(name.lastIndexOf('-') + 1);
                    double current_reservation = Float.parseFloat(jLabelReservationLevel.get("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).getText());
                    if(current_reservation <= ((MyComponents.FloatJSlider) e.getSource()).getFloatValue())
                    {
                        JOptionPane.showMessageDialog(null, "The aspiration value for objective " + 
                                                            (Integer.parseInt(indexOfComponent) + 1) + 
                                                            " must be lower than its reservation value. \n We will set it to the ideal value.");  
                        //double maximum = Float.parseFloat(jLabelAspirationLevel.get("jLabelAspirationMaximum" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).getText());
                        //double minimum = Float.parseFloat(jLabelAspirationLevel.get("jLabelAspirationMinimum" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).getText());
                        //double new_value = current_reservation - 0.01 *(maximum - minimum);
                        //floatJSliderAspirationLevel.get(name).setFloatValue((float) new_value);
                        ////((MyComponents.FloatJSlider) e.getSource()).setFloatValue((float) current_reservation);
                        ////jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(roundWithPrecision(Math.abs(current_reservation), DECIMALS)));
                        //jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(roundWithPrecision(Math.abs(new_value), DECIMALS)));                        
                        //floatJSliderAspirationLevel.get(name).setFloatValue((float) idealPoint[Integer.parseInt(indexOfComponent)]);
                        jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(idealPoint[Integer.parseInt(indexOfComponent)]));
                        ((MyComponents.FloatJSlider) e.getSource()).setFloatValue((float) idealPoint[Integer.parseInt(indexOfComponent)]);
                    }
                    else
                    {
                        //jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Float.toString(floatJSliderAspirationLevel.get(name).getFloatValue()));
                        jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(roundWithPrecision(((double)floatJSliderAspirationLevel.get(name).getFloatValue()), DECIMALS)));
                    }
                }
            });
            
            c.gridx = 3;
            c.weightx=1000;
            c.fill=GridBagConstraints.BOTH;
            jPanelAspirationLevel.add(fs, c);

            //lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(nadirPoint[i]), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(nadirPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelAspirationMaximum" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelAspirationLevel.put(lbl.getName(), lbl);
            c.gridx = 4;
            c.weightx=1;
            jPanelAspirationLevel.add(lbl, c);
            c.gridy++;

            //We show the information for the reservation level
            lbl = new JLabel("f" + (i + 1));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelReservationObjective" + SEPARATOR_INDEX_IN_NAME + i);
            c.gridx = 0;
            c.weightx = 1;
            jPanelReservationLevel.add(lbl, c);

            ////lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(referenceValue), DECIMALS)));
            //lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(nadirPoint[i]), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(nadirPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelReservationLevel.put(lbl.getName(), lbl);
            c.gridx = 1;
            jPanelReservationLevel.add(lbl, c);

            //lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(idealPoint[i]), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(idealPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(idealPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelReservationMinimum" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelReservationLevel.put(lbl.getName(), lbl);
            c.gridx = 2;
            jPanelReservationLevel.add(lbl, c);

            fs = new MyComponents.FloatJSlider();

            fs.setName("floatJSliderReservationLevel" + SEPARATOR_INDEX_IN_NAME + i);
            fs.setFloatPrecision(DECIMALS);
            fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());
            fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
            //fs.setFloatValue((float) roundWithPrecision(Math.abs(nadirPoint[i]), DECIMALS));
            fs.setFloatValue((float) roundWithPrecision(nadirPoint[i], DECIMALS));
//            if(problem.getObjectiveOpt() == null){
//                fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());
//                fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
//                fs.setFloatValue((float) roundWithPrecision(nadirPoint[i], DECIMALS));
//            }
//            else{
//                if (Double.valueOf(idealPoint[i]).equals(0.0))
//                    fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue());                    
//                else       
//                    fs.setFloatMinimum(Double.valueOf(idealPoint[i]).floatValue() * Double.valueOf(problem.getObjectiveOpt()[i]).floatValue());
//                    
//                if (Double.valueOf(nadirPoint[i]).equals(0.0)){
//                    fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue());
//                    fs.setFloatValue((float) roundWithPrecision(nadirPoint[i], DECIMALS));
//                }
//                else{
//                    fs.setFloatMaximum(Double.valueOf(nadirPoint[i]).floatValue() * Double.valueOf(problem.getObjectiveOpt()[i]).floatValue());
//                    fs.setFloatValue((float) roundWithPrecision(nadirPoint[i]* problem.getObjectiveOpt()[i], DECIMALS));
//                }
//            }
            /*fs.setPaintLabels(true);
             Hashtable tickLabel = new Hashtable();
             tickLabel.put(new Integer(scaleFloatToIntValue(fs.getFloatValue(), fs.getMinimum(), fs.getMaximum(), fs.getFloatMinimum(), fs.getFloatMaximum())), new JLabel("|"));// + jTable1.getValueAt(jTable1.getSelectedRow(), i)));
             fs.setLabelTable(tickLabel);*/
            fs.setVisible(true);
            fs.setEnabled(true);
            floatJSliderReservationLevel.put(fs.getName(), fs);
            fs.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    String name = new String(((MyComponents.FloatJSlider) e.getSource()).getName());
                    String indexOfComponent = name.substring(name.lastIndexOf('-') + 1);
                    //jLabelReservationLevel.get("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Float.toString(floatJSliderReservationLevel.get(name).getFloatValue()));
                    // Check that the resservation value is lower than the current aspiration level:
                    double current_aspiration = Float.parseFloat(jLabelAspirationLevel.get("jLabelAspirationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).getText());
                    if(current_aspiration >= ((MyComponents.FloatJSlider) e.getSource()).getFloatValue())
                    {
                        JOptionPane.showMessageDialog(null, "The reservation value for objective " + 
                                                            (Integer.parseInt(indexOfComponent) + 1) + 
                                                            " must be higher than its aspiration value. \n It will be set to the nadir value.");  
                        ((MyComponents.FloatJSlider) e.getSource()).setFloatValue((float) nadirPoint[Integer.parseInt(indexOfComponent)]);//                        
                        jLabelReservationLevel.get("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(roundWithPrecision(nadirPoint[Integer.parseInt(indexOfComponent)], DECIMALS)));
                    
                    }
                    else
                    {
                        //jLabelReservationLevel.get("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Float.toString(floatJSliderAspirationLevel.get(name).getFloatValue()));
                        jLabelReservationLevel.get("jLabelReservationSelectedValue" + SEPARATOR_INDEX_IN_NAME + indexOfComponent).setText(Double.toString(roundWithPrecision(((double)floatJSliderReservationLevel.get(name).getFloatValue()), DECIMALS)));
                    }
                    
                }
            });
            c.gridx = 3;
            c.weightx=1000;
            c.fill=GridBagConstraints.BOTH;
            jPanelReservationLevel.add(fs, c);

            //lbl = new JLabel(Double.toString(roundWithPrecision(Math.abs(nadirPoint[i]), DECIMALS)));
            lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            if(problem.getObjectiveOpt() == null || Double.valueOf(nadirPoint[i]).equals(0.0))
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i], DECIMALS)));
//            else 
//                lbl = new JLabel(Double.toString(roundWithPrecision(nadirPoint[i]* problem.getObjectiveOpt()[i], DECIMALS)));
            lbl.setVisible(true);
            lbl.setEnabled(true);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setName("jLabelReservationMaximum" + SEPARATOR_INDEX_IN_NAME + i);
            jLabelReservationLevel.put(lbl.getName(), lbl);
            c.gridx = 4;
            c.weightx=1;
            jPanelReservationLevel.add(lbl, c);
            c.gridy++;
            
            result.set(i, Float.valueOf(fs.getFloatValue()).doubleValue());
        }

        tm = new DefaultTableModel(data, names);
        jTableSolutions.setModel(tm);
        jTableSolutions.repaint();

        jPanelAspirationLevel.revalidate();
        jPanelAspirationLevel.repaint();
        
        jPanelReservationLevel.revalidate();
        jPanelReservationLevel.repaint();

        return result;
    }

    private void readSolutions(SolutionSet ss) {
        //We show the information for the solutions in a table                               
        String[] names;
        Object[][] data;
        int i, j;
        DefaultTableModel tm;

        names = new String[ss.get(0).numberOfObjectives() + 1];
        names[0] = "Solution";
        for (i = 0; i < ss.get(0).numberOfObjectives(); i++) {
            names[i + 1] = "f " + (i + 1);
        }

        data = new Object[ss.size()][ss.get(0).numberOfObjectives() + 1];
        for (i = 0; i < ss.size(); i++) {
            for (j = 0; j < ss.get(i).numberOfObjectives() + 1; j++) {
                if (j == 0) {
                    data[i][j] = "S" + (i + 1);
                } else {
                    data[i][j] = roundWithPrecision(ss.get(i).getObjective(j - 1), DECIMALS);
                }
            }
        }

        /*
         minimum = new Double[data[0].length];
         maximum = new Double[data[0].length];

         for (i = 0; i < data[0].length; i++) {
         maximum[i] = Double.MIN_VALUE;
         minimum[i] = Double.MAX_VALUE;
         }

         for (i = 0; i < ss.size(); i++) {
         for (j = 0; j < ss.get(i).numberOfObjectives(); j++) {
         data[i][j] = ss.get(i).getObjective(j);

         if (data[i][j] < minimum[j]) {
         minimum[j] = data[i][j];
         }

         if (data[i][j] > maximum[j]) {
         maximum[j] = data[i][j];
         }
         }
         }*/
        tm = new DefaultTableModel(data, names);
        jTableSolutions.setModel(tm);
        jTableSolutions.repaint();

        jPanelAspirationLevel.revalidate();
        jPanelAspirationLevel.repaint();
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jTableSolutionsMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableSolutionsMousePressed
        // TODO add your handling code here:
        /*
         Hashtable tickLabel;
         JLabel jLabelTick;
         Integer minIntValue, maxIntValue;
         Float floatNumber, minFloatValue, maxFloatValue;

         if (jTableSolutions.getValueAt(jTableSolutions.getSelectedRow(), jTableSolutions.getSelectedColumn()) != null) {
         for (int i = 1; i < jTableSolutions.getColumnCount(); i++) {
         minIntValue = floatJSlider.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + (i-1)).getMinimum();
         maxIntValue = floatJSlider.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + (i-1)).getMaximum();
         floatNumber = Float.valueOf(jTableSolutions.getValueAt(jTableSolutions.getSelectedRow(), i).toString());
         minFloatValue = floatJSlider.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + (i-1)).getFloatMinimum();
         maxFloatValue = floatJSlider.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + (i-1)).getFloatMaximum();

         //Double value = roundWithPrecision((Double) jTableSolutions.getValueAt(jTableSolutions.getSelectedRow(), i), DECIMALS);                
         jLabelTick = new JLabel(jTableSolutions.getValueAt(jTableSolutions.getSelectedRow(), 0).toString());
         jLabelTick.setFont(new Font("", ITALIC, 11));
         tickLabel = new Hashtable();
         tickLabel.put(new Integer(scaleFloatToIntValue(floatNumber, minIntValue, maxIntValue, minFloatValue, maxFloatValue)), jLabelTick);                
                
         floatJSlider.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + (i-1)).setLabelTable(tickLabel);
         }
         }
         */
    }//GEN-LAST:event_jTableSolutionsMousePressed

    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStartActionPerformed
        final Study study = this;      

        Thread thr = new Thread() {
            boolean isPossibleToExecute = true;
            
            @Override
            public void run() {
                isPossibleToExecute = true;

                try {
                    isPossibleToExecute = initExperiment((String) jComboBoxProblemName.getSelectedItem(), PARETO_FRONTS_FOLDER + jComboBoxProblemName.getSelectedItem() + "." + jSpinnerObjectivesNumber.getValue().toString() + "D.pf", OUTPUT_FOLDER);
                } catch (JMException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (isPossibleToExecute) {
                    if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) > 2 && jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")) {
                        generateWeightsVectorsWithKmeans((String) algorithm.getInputParameter("weightsDirectory"), (String) algorithm.getInputParameter("weightsFileName"));
                    }

                    if (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")){
                        algorithm.setInputParameter("aspirationLevel", getAspirationLevel());
                        algorithm.setInputParameter("reservationLevel", getReservationLevel());
                        
                        //loadReferencePointUI((int)jSpinnerObjectivesNumber.getValue(), ((Integer)jSpinnerSolutionsNumber.getValue()).intValue(), idealPoint, nadirPoint);                       
                        try {
                            solutions = ((IRAEMO) algorithm).executeFirstIteration();
                        } catch (JMException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else if (jComboBoxAlgorithm.getSelectedItem().equals("RNSGAII")){
                        ReferencePoint[] referencePoints = new ReferencePoint [2];
                        referencePoints[0] = getAspirationLevel();
                        referencePoints[1] = getReservationLevel();
                        algorithm.setInputParameter("referencePoints", referencePoints);
                        
                        //loadReferencePointUI((int)jSpinnerObjectivesNumber.getValue(), ((Integer)jSpinnerSolutionsNumber.getValue()).intValue(), idealPoint, nadirPoint);                       
                        try {
                            solutions = ((RNSGAII) algorithm).executeFirstIteration();
                        } catch (JMException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }                    
                    
                    if (!SwingUtilities.isEventDispatchThread()) {
                        SwingUtilities.invokeLater(new Runnable() {
                          @Override
                          public void run() {
                             jTextAreaLog.append("\n- " + jComboBoxAlgorithm.getSelectedItem().toString() + " executed using Aspiration level " + getAspirationLevel().toString(DECIMALS) + " and Reservation level " + getReservationLevel().toString(DECIMALS)+ ".");
                          }
                        });                        
                    }
                    //jTextAreaLog.append("\n- " + jComboBoxAlgorithm.getSelectedItem().toString() + " executed using Aspiration level " + getAspirationLevel().toString(DECIMALS) + " and Reservation level " + getReservationLevel().toString(DECIMALS)+ ".");

                                        
                    SolutionSet solutions_to_show = filtersolutions(solutions[1]);   
                    solutions_to_show.printObjectivesToFile(OUTPUT_FOLDER + java.io.File.separator + jComboBoxProblemName.getSelectedItem().toString() + java.io.File.separator + jComboBoxAlgorithm.getSelectedItem().toString() + java.io.File.separator + "ROI.txt");

                    //readSolutions(solutions[1]);
                    readSolutions(solutions_to_show);
                    jPlot1.getJavaPlot().getPlots().clear();
                    if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) < 3) {
                        plotParetoFront(jPlot1);
                        //plotSolutions(solutions[1], jPlot1);
                        plotSolutions(solutions_to_show, jPlot1);
                        plotAspirationLevel(jPlot1);
                        plotReservationLevel(jPlot1);
                    } else {
                        try {
                            //plotValuePath(solutions[1], true);
                            plotValuePath(solutions_to_show, true);
                        } catch (JMException ex) {
                            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    //changeSelection(0, 0, false, false);
                    repaintPlot(jPlot1);

                    //setTickLabels();
                    jButtonNextIteration.setEnabled(true);
                } else {
                    JOptionPane.showMessageDialog(study, "The " + jSpinnerObjectivesNumber.getValue().toString() + " objectives " + jComboBoxProblemName.getSelectedItem() + " problem does not exist.");
                }
            }
        };
                                
        thr.start();
        thr.yield();                
    }//GEN-LAST:event_jButtonStartActionPerformed

    /** 
     * Filter a set solutions to select only those whose objective function values
     * are within the aspiration and reservation values. Later, select only the 
     * number of solutions the DM wants to see:
     * @param solutions Set of solutions to be filtered
     * @return  The subset of solutions verifying this condition
     */  
    private SolutionSet filtersolutions(SolutionSet solutions){
        
        SolutionSet solutions_in_ROIs = new SolutionSet(solutions.size());
        SolutionSet solutions_not_in_ROIs = new SolutionSet(solutions.size());
        SolutionSet final_solutions = new SolutionSet(((int) jSpinnerSolutionsNumber.getValue()));
        double[] asp_values = new double[(Integer) jSpinnerObjectivesNumber.getValue()];
        double[] res_values = new double[(Integer) jSpinnerObjectivesNumber.getValue()];
        
        // Get the aspiration and reservation points:
        Solution aspiration_point = new Solution(solutions.get(0).numberOfObjectives());
        Solution reservation_point = new Solution(solutions.get(0).numberOfObjectives());
        for (int i = 0; i < floatJSliderAspirationLevel.size(); i++) {
            aspiration_point.setObjective(i,((double) floatJSliderAspirationLevel.get("floatJSliderAspirationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue()));
            reservation_point.setObjective(i,((double) floatJSliderReservationLevel.get("floatJSliderReservationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue()));
        }
        
//        // Get the aspiration and reservation values:
//        for(int i = 0; i < ((Integer) jSpinnerObjectivesNumber.getValue()); i++){
//                asp_values[i] = (double) (floatJSliderAspirationLevel.get("floatJSliderAspirationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue());
//                res_values[i] = (double) (floatJSliderReservationLevel.get("floatJSliderReservationLevel" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue());
//        }
        // Check every solution in order to select only whose objective function values are 
        // within the aspiration and reservation values:
        for (int solutionIndex = 0; solutionIndex < solutions.size(); solutionIndex++) {
            boolean solution_selected = true;
            for(int i = 0; i < solutions.get(0).numberOfObjectives(); i++){
                if(aspiration_point.getObjective(i) > solutions.get(solutionIndex).getObjective(i) || solutions.get(solutionIndex).getObjective(i) > reservation_point.getObjective(i)){
                    solution_selected = false;
                    break;
                }
            }
            if(solution_selected)
                solutions_in_ROIs.add(solutions.get(solutionIndex));
            else
                solutions_not_in_ROIs.add(solutions.get(solutionIndex));
        }
        
        if(solutions_in_ROIs.size() == 0){
            SolutionSet[] two_subsets;
            two_subsets = check_dominance_asp_res_points(solutions, aspiration_point, reservation_point);
            solutions_in_ROIs = two_subsets[0];
            solutions_not_in_ROIs = two_subsets[1];
        
        }
        if(solutions_in_ROIs.size() == ((int) jSpinnerSolutionsNumber.getValue())){
            final_solutions = solutions_in_ROIs;
        }
        else if(solutions_in_ROIs.size() > ((int) jSpinnerSolutionsNumber.getValue())){
            final_solutions = cluster_representatives(solutions_in_ROIs);
        }
        else if(solutions_in_ROIs.size() < ((int) jSpinnerSolutionsNumber.getValue())){
            final_solutions = select_more_sol(solutions_in_ROIs, solutions_not_in_ROIs);  
        }
        
        return final_solutions;
    }
    
    /** 
     * Check if the aspiration point is achievable or if the reservation point is 
     * unachievable. In the first case, the ROI defined by both points is the ROI 
     * associated to the aspiration point; in the second case, the ROI defined 
     * by both points is the ROI associated to the reservation point.
     * @params solutions    the set of solutions 
     * @params asp_point    aspiration point 
     * @params res_point    reservation point 
     * @return two subset of solutions, first one with the solutions in ROI, and 
     *         second one with the solutions out of ROI.                
     */  
    private SolutionSet[] check_dominance_asp_res_points(SolutionSet solutions, Solution asp_point, Solution res_point){

        SolutionSet[] solutions_in_out_ROI = new SolutionSet[2];    
                                                                    
        solutions_in_out_ROI[0] = new SolutionSet(solutions.size());// solutions_in_out_ROI[0] = solutions_inROI; 
        solutions_in_out_ROI[1] = new SolutionSet(solutions.size());// solutions_in_out_ROI[1] = solutions_outROI;
        
        ParetoDominance dominance = new ParetoDominance();
        // Check if there is any solution which dominates the aspiration point of
        // if there is any solution which is dominated by the reservation point:
        int dom_flag = 0;   // -1 if asp_point is dominated by one solution (achievable); 
                            //  1 if reservation point dominates one solutions (unachievable); 
                            //  0 otherwise
        for(int i = 0; i < solutions.size(); i ++){
            if(dominance.checkParetoDominance(solutions.get(i), asp_point) == -1){ // solution.get(i) dominates asp_point
                dom_flag = -1;
                break;
            }
            else if (dominance.checkParetoDominance(solutions.get(i), res_point) == 1){ // solution.get(i) is dominated by res_point
                dom_flag = 1;
                break;
            }
        }
        // Check every solution in order to select only whose objective function values are 
        // within the aspiration and reservation values:
        if (dom_flag == -1){ // the aspiration point is achievable
            for (int solutionIndex = 0; solutionIndex < solutions.size(); solutionIndex++) {
                boolean solution_selected = true;
                for(int i = 0; i < solutions.get(0).numberOfObjectives(); i++){
                    if(asp_point.getObjective(i) < solutions.get(solutionIndex).getObjective(i)){
                        solution_selected = false;
                        break;
                    }
                }
                if(solution_selected)// this solution is in the ROI
                    solutions_in_out_ROI[0].add(solutions.get(solutionIndex));
                else // this solution is out of the ROI
                    solutions_in_out_ROI[1].add(solutions.get(solutionIndex));
            }
        }
        else if (dom_flag == 1){ // the reservation point is unachievable
            for (int solutionIndex = 0; solutionIndex < solutions.size(); solutionIndex++) {
                boolean solution_selected = true;
                for(int i = 0; i < solutions.get(0).numberOfObjectives(); i++){
                    if(res_point.getObjective(i) > solutions.get(solutionIndex).getObjective(i)){
                        solution_selected = false;
                        break;
                    }
                }
                if(solution_selected)// this solution is in the ROI
                    solutions_in_out_ROI[0].add(solutions.get(solutionIndex));
                else // this solution is out of the ROI
                    solutions_in_out_ROI[1].add(solutions.get(solutionIndex));
            }
        }
       return solutions_in_out_ROI;
    }
/** 
     * Complete solution_inROI with solutions from solution_outROI until having 
     * N_s solutions, taking into account those with the minimal distance to any
     * solution in solution_inROI:
     * @param solutions Set of solutions to be clustered
     * @return The subset of clusters' representatives
     */  
    private SolutionSet select_more_sol(SolutionSet solution_inROI, SolutionSet solution_outROI){
        
        
        SolutionSet final_sol = new SolutionSet(((int) jSpinnerSolutionsNumber.getValue()));
        // Number of solutions to be added:
        int N = ((int) jSpinnerSolutionsNumber.getValue()) - solution_inROI.size();
        if (solution_outROI.size() <= N){
            final_sol = solution_inROI.union(solution_outROI);
            JOptionPane.showMessageDialog(null, "The aspiration and reservation levels are very restrictive: \n 1) There were not enough generated solutions. \n 2) Some of the objective function values of the last " + solution_outROI.size() + " solutions are out of the specified bounds." );
        }
        else{
            // Matrix with the distance of each solution in solution_outROI to each 
            // solution in solution_inROI:
            Distance distance = new Distance();
            double[][] dist_matrix = new double[solution_outROI.size()][solution_inROI.size()];
            for(int j = 0; j < solution_inROI.size(); j++){
                final_sol.add(solution_inROI.get(j)); // solution_inROI added to the final_sol
                for(int i = 0; i < solution_outROI.size(); i++){
                  dist_matrix[i][j] = distance.distanceBetweenObjectives(solution_outROI.get(i), solution_inROI.get(j));
              }          
            }
            JOptionPane.showMessageDialog(null, "The aspiration and reservation levels are very restrictive: \n Some of the objective function values of the last " + N + " solutions are out of the specified bounds." );
            // Add N solutions from solution_outROI to final_sol, looking for those
            // with the minimal distance:
            while (N > 0){
                // Find the minimun distance in dist_matrix:
                double min_dist = 1e10;
                int i_index_min_dist = 0, j_index_min_dist = 0;
                for(int i = 0; i < solution_outROI.size(); i++){
                  for(int j = 0; j < solution_inROI.size(); j++){
                      if(dist_matrix[i][j] < min_dist){
                          min_dist = dist_matrix[i][j];
                          i_index_min_dist = i;
                          j_index_min_dist = j;
                      }
                  }    
                }
                dist_matrix[i_index_min_dist][j_index_min_dist] = 1e100;
                final_sol.add(solution_outROI.get(i_index_min_dist));
                N--;
            }
            
        }
        return final_sol;
    }
        
    /** 
     * Cluster a set of solutions into N_s clusters and return a set with the
     * representatives of the clusters:
     * @param solutions Set of solutions to be clustered
     * @return The subset of clusters' representatives
     */  
    private SolutionSet cluster_representatives(SolutionSet solutions){
        
        LinkedList<Cluster_set> clusters_list = new LinkedList<>();
        SolutionSet representatives;
        
         // Calculate the distance between each pair of individuals:
        Distance dist = new Distance();
        double[][] distances = dist.distanceMatrix(solutions);
        double[][] dist_ind = new double[solutions.size() + 1][solutions.size() + 1];
        for (int i = 0; i < solutions.size(); i++){
                for (int j = 0; j < solutions.size(); j++){
                    dist_ind[i][j] = distances[i][j];
                }
        }
        
        // Initially, each solution belongs to one cluster:
        Cluster_set cluster;
        for (int i = 0; i < solutions.size(); i++){
            cluster  = new Cluster_set(solutions.size());
            cluster.add(solutions.get(i));   
            clusters_list.add(cluster);
        }
        
        // Set up array of cluster sizes:
        int[] c_size = new int[solutions.size() + 1];
        for (int i = 0; i < solutions.size(); i++)
            c_size[i] = 1;
        
        int N = solutions.size();
        //double[][] dist_ind = new double[N+1][];
        while( N > ((int) jSpinnerSolutionsNumber.getValue()))
        {
            // Find C_i and C_j with the smallest distance:
            double min_d = 1e10;
            int min_i = 0;
            int min_j = 0;
            for (int i = 0; i < N-1; i++){
                for (int j = i + 1; j < N; j++){
                    if (dist_ind[i][j] < min_d){
                        min_d = dist_ind[i][j];
                        min_i = i;
                        min_j = j;
                    }
                }
            }

            // Join clusters C_i and C_j in cluster C_i:
            Cluster_set c_i = clusters_list.get(min_i);
            Cluster_set c_j = clusters_list.get(min_j);
            for(int r = 0; r < c_j.size(); r++)
                c_i.add(c_j.get(r));
            // Add the new cluster at the end of the list:
            clusters_list.add(N, c_i);
            c_size[N] = c_i.size();
            
            // Compute the distance from new cluster to every other cluster:
            double w_i = ((double)c_size[min_i]) / ((double) c_size[N]);
            double w_j = ((double)c_size[min_j]) / ((double) c_size[N]);
            for (int l = 0; l < N; l++){
                dist_ind[N][l] = w_i * dist_ind[l][min_i] + w_j * dist_ind[l][min_j];
                dist_ind[l][N] = dist_ind[N][l];
            }   
            // Swap row <N> in 'dist_ind' with row <min_i> and swap row <N-1> with row <min_j>, 
            // thus removing rows <min_i> and <min_j> from 'dist_ind', 'list_clusters', and 'size_c':
            double[] swap_dist = dist_ind[min_i];
            dist_ind[min_i] = dist_ind[N];
            dist_ind[N] = swap_dist;
            swap_dist = dist_ind[min_j];
            dist_ind[min_j] = dist_ind[N - 1];
            dist_ind[N - 1] = swap_dist;
            
            Cluster_set swap_cluster;
            swap_cluster = clusters_list.get(min_i);
            clusters_list.set(min_i, clusters_list.get(N));
               // list_clusters[min_i] = list_clusters[N].clone();
            clusters_list.set(N, swap_cluster);
                //list_clusters[N] = swap_cluster.clone();
            swap_cluster = clusters_list.get(min_j);
            clusters_list.set(min_j, clusters_list.get(N - 1));
                //list_clusters[min_j] = list_clusters[N - 1].clone();
            clusters_list.set(N - 1, swap_cluster);
                //list_clusters[N - 1] = swap_cluster.clone();

            int swap_size = c_size[min_i];
            c_size[min_i] = c_size[N];
            c_size[N] = swap_size;
            swap_size = c_size[min_j];
            c_size[min_j] = c_size[N - 1];
            c_size[N - 1] = swap_size;

            // Swap column <N> with column <min_i> and swap column <N-1> with
            // column <min_j>, thus removing columns <min_i> and <min_j> from 'dist_ind':
            for (int i = 0; i <= N; i++)
            {
                double[] D_i = dist_ind[i];
                double swap_column = D_i[min_i];
                D_i[min_i] = D_i[N];
                D_i[N] = swap_column;
                swap_column = D_i[min_j];
                D_i[min_j] = D_i[N - 1];
                D_i[N - 1] = swap_column;
            }

            // Take away two trees and added one.
            dist_ind[N] = null;
            clusters_list.set(N, null);
            N--;
        }
        
         // 4. Finally, a representative from each cluster is found:
        representatives = new SolutionSet(N);
        for (int i = 0; i < N; i++)
            representatives.add(clusters_list.get(i).getRespresentative());
        
        return representatives;
        
    }
    
    private void jSpinnerObjectivesNumberStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerObjectivesNumberStateChanged
        boolean result;

        Integer objectivesNumber = (Integer) jSpinnerObjectivesNumber.getValue();
        String selectedProblem = (String) jComboBoxProblemName.getSelectedItem();
        ComboBoxModel<String> cbm;

        switch (objectivesNumber) {
            case 2:
                cbm = new DefaultComboBoxModel<>(new String[]{
                    "ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6",
                    "DTLZ1", "DTLZ2", "DTLZ3", "DTLZ4", "DTLZ5", "DTLZ6", "DTLZ7",
                    "WFG1", "WFG2", "WFG3", "WFG4", "WFG5", "WFG6", "WFG7", "WFG8", "WFG9"
                });

                jSpinnerPopulationSize.setValue(DEFAULT_POPULATION_SIZE_FOR_2D);
                jSpinnerGenerationsNumber.setValue(DEFAULT_GENERATIONS_NUMBER_FOR_2D);
                break;

            case 3:
                cbm = new DefaultComboBoxModel<>(new String[]{
                    "DTLZ1", "DTLZ2", "DTLZ3", "DTLZ4", "DTLZ5", "DTLZ6", "DTLZ7",
                    "WFG1", "WFG2", "WFG3", "WFG4", "WFG5", "WFG6", "WFG7", "WFG8", "WFG9",
                    "AuxiliaryServicesProblem"
                });

                jSpinnerPopulationSize.setValue(DEFAULT_POPULATION_SIZE_FOR_3D);
                jSpinnerGenerationsNumber.setValue(DEFAULT_GENERATIONS_NUMBER_FOR_3D);
                break;

            case 4:
            case 5:
            case 6:
                cbm = new DefaultComboBoxModel<>(new String[]{
                    "DTLZ1", "DTLZ2"
                });
                break;

            default:
                cbm = new DefaultComboBoxModel<>(new String[]{});
        }

        jComboBoxProblemName.setModel(cbm);
        jComboBoxProblemName.setSelectedIndex(0);

        result = setExperimentForNewProblem();

        /*result = setExperimentForNewProblem();

         if (!result)
         {
         if ((Integer)jSpinnerObjectivesNumber.getValue() == 3)
         jSpinnerObjectivesNumber.setValue(2);
         }
         else
         {
         switch (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()))
         {
         case 2:
         jSpinnerPopulationSize.setValue(DEFAULT_POPULATION_SIZE_FOR_2D);
         jSpinnerGenerationsNumber.setValue(DEFAULT_GENERATIONS_NUMBER_FOR_2D);
         break;
         case 3:
         jSpinnerPopulationSize.setValue(DEFAULT_POPULATION_SIZE_FOR_3D);
         jSpinnerGenerationsNumber.setValue(DEFAULT_GENERATIONS_NUMBER_FOR_3D);
         break;
         }
         }*/
    }//GEN-LAST:event_jSpinnerObjectivesNumberStateChanged

    private void jComboBoxProblemNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxProblemNameActionPerformed

        setExperimentForNewProblem();
    }//GEN-LAST:event_jComboBoxProblemNameActionPerformed

    private void jSpinnerPopulationSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerPopulationSizeStateChanged
        // TODO add your handling code here:
        int previousValue = ((Integer) jSpinnerSolutionsNumber.getValue()).intValue();

        if (previousValue <= ((Integer) jSpinnerPopulationSize.getValue()).intValue()) {
            jSpinnerSolutionsNumber.setModel(new SpinnerNumberModel(previousValue, 1, ((Integer) jSpinnerPopulationSize.getValue()).intValue(), 1));
        } else {
            jSpinnerSolutionsNumber.setModel(new SpinnerNumberModel(((Integer) jSpinnerObjectivesNumber.getValue()).intValue() * 2, 1, ((Integer) jSpinnerPopulationSize.getValue()).intValue(), 1));
        }

        jButtonNextIteration.setEnabled(false);
    }//GEN-LAST:event_jSpinnerPopulationSizeStateChanged

    private void jSpinnerGenerationsNumberStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerGenerationsNumberStateChanged
        // TODO add your handling code here:
        jButtonNextIteration.setEnabled(false);
    }//GEN-LAST:event_jSpinnerGenerationsNumberStateChanged

    private void jComboBoxAlgorithmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxAlgorithmActionPerformed
        // TODO add your handling code here:
        setExperimentForNewProblem();
    }//GEN-LAST:event_jComboBoxAlgorithmActionPerformed

    private void showDialog(final Component component, final String title, final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(component, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void setTickLabels() {
        for (int i = 0; i < (int) jSpinnerObjectivesNumber.getValue(); i++) {
            Hashtable tickLabel = new Hashtable();
            tickLabel.put(new Integer(scaleFloatToIntValue(floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).getFloatValue(),
                    floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).getMinimum(),
                    floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).getMaximum(),
                    floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).getFloatMinimum(),
                    floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).getFloatMaximum())),
                    new JLabel("|"));

            floatJSliderAspirationLevel.get("floatJSlider" + SEPARATOR_INDEX_IN_NAME + i).setLabelTable(tickLabel);
        }
    }

    private void plotSolutions(SolutionSet ss, JPlot jplot) {
        PlotStyle myPlotStyle = new PlotStyle();
        myPlotStyle.setStyle(Style.POINTS);
        myPlotStyle.setPointType(6);
        myPlotStyle.setPointSize(2);
        myPlotStyle.setLineWidth(2);
        myPlotStyle.set("linecolor", "1");

        DataSetPlot dataSetPlot = new DataSetPlot(ss.writeObjectivesToMatrix());
        dataSetPlot.setTitle("Solutions");
        dataSetPlot.setPlotStyle(myPlotStyle);

        jplot.getJavaPlot().addPlot(dataSetPlot);
    }

    private void plotValuePath(SolutionSet ss, boolean plotReferenceLevels) throws JMException{
        int numberOfObjectives = ss.get(0).numberOfObjectives();
        double minimumValue = Double.MAX_VALUE, maximumValue = Double.MIN_VALUE;
        double[] idealPointArtifitial = new double[numberOfObjectives];
        double[] nadirPointArtifitial = new double[numberOfObjectives];
        String problemName = (String) jComboBoxProblemName.getSelectedItem();
        String soltype;
        if (problemName.compareTo("AuxiliaryServicesProblem")== 0){
            soltype = "ArrayRealAndBinary";
            DECIMALS = 2;
        }
        else
            soltype = "Real";
        Object[] problemParams = {soltype};
        Problem problem = (new ProblemFactory()).getProblem(problemName, problemParams);
        //double[][] data = ValuePath.getSolutionsForPlot(ss);
        double[][] data = ValuePath.getSolutionsForPlot(ss, idealPoint, nadirPoint,problem);
        /*for (int i = 0; i < numberOfObjectives; i++) {
            if (idealPoint[i] < minimumValue) {
                minimumValue = idealPoint[i];
            }

            if (nadirPoint[i] > maximumValue) {
                maximumValue = nadirPoint[i];
            }
        }*/
        if(problem.getObjectiveOpt() == null) // all objectives are to be minimized
        {
            idealPointArtifitial = idealPoint;
            nadirPointArtifitial = nadirPoint;
            for (int i = 0; i < numberOfObjectives; i++) {
                if (idealPoint[i] < minimumValue) {
                    minimumValue = idealPoint[i]/(nadirPoint[i] - idealPoint[i]);
                }
                if (nadirPoint[i] > maximumValue) {
                    maximumValue =  nadirPoint[i]/(nadirPoint[i] - idealPoint[i]);
                }
            }
        }
        else{ // at least, one objectives is to be maximized
            for (int i = 0; i < numberOfObjectives; i++) {
                if (problem.getObjectiveOpt()[i] == 1){ // the objective i must be minimized
                    idealPointArtifitial[i] = idealPoint[i];
                    nadirPointArtifitial[i] = nadirPoint[i];
                }
                else{ // the objective i must be maximized
                    idealPointArtifitial[i] = nadirPoint[i] * problem.getObjectiveOpt()[i] + 0.0;
                    nadirPointArtifitial[i] = idealPoint[i] * problem.getObjectiveOpt()[i] + 0.0;
                }
            }
            
            for (int i = 0; i < numberOfObjectives; i++) {
                if (idealPointArtifitial[i] < minimumValue) {
                    minimumValue =  idealPointArtifitial[i]/(nadirPointArtifitial[i] - idealPointArtifitial[i]);
                }
                if (nadirPointArtifitial[i] > maximumValue) {
                    maximumValue = nadirPointArtifitial[i]/(nadirPointArtifitial[i] - idealPointArtifitial[i]);
                }
            }
        }

        //Plot information
        jPlot1.getJavaPlot().set("xlabel", "'objectives' font ',9'");
        jPlot1.getJavaPlot().set("ylabel", "'values' font ',9'");
        // Labels in the X axis, with the minimum value of each objetive:
        String aux = "(";
        for(int i=0; i < numberOfObjectives; i++){
           aux = aux + "'" + Double.toString(idealPointArtifitial[i] ) + "' " + Integer.toString(i + 1); //add ("Pi" 3.14159)
           if ( i != numberOfObjectives -1 )
               aux = aux + ", ";
           else
               aux = aux + ")";
        }
        jPlot1.getJavaPlot().set("xtics", aux);
        //jPlot1.getJavaPlot().set("xtics", "1");
        // No labels in the Y axis:
        jPlot1.getJavaPlot().set("ytics", "1");
        // font sizes:
        jPlot1.getJavaPlot().set("tics", "font ',9'");
        jPlot1.getJavaPlot().set("key", "samplen 2 inside bottom font ',9'");

        //It shows a vertical box for each objective                
        jPlot1.getJavaPlot().set("xrange", "[0.5 to " + (numberOfObjectives + 1.5) + "]");
        jPlot1.getJavaPlot().set("yrange", "[" + (minimumValue - 0.1) + " to " + (maximumValue + 0.4) + "]");
        //jPlot1.getJavaPlot().set("key", "outside bottom");

        /*for (int i = 1; i <= numberOfObjectives; i++) {
            jPlot1.getJavaPlot().set("object " + i, "rect from " + (i - 0.1) + "," + idealPoint[i - 1]/(nadirPoint[i-1] - idealPoint[i-1]) + " to " + (i + 0.1) + "," + nadirPoint[i - 1] /(nadirPoint[i-1] - idealPoint[i-1]));
        }*/
        if(problem.getObjectiveOpt() == null){
            for (int j = 1; j <= numberOfObjectives; j++) {
               jPlot1.getJavaPlot().set("object " + j, "rect from " + (j - 0.1) + "," + idealPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]) + " to " + (j + 0.1) + "," + nadirPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1])); 
               jPlot1.getJavaPlot().set("label " + j, " at " + Double.toString(j) + " , " + Double.toString(maximumValue + 0.1) + " '" + Double.toString(roundWithPrecision(nadirPoint[j-1],DECIMALS)) + "' font ',9' front center");
            }            
        }
        else{
            for (int j = 1; j <= numberOfObjectives; j++) {
                jPlot1.getJavaPlot().set("object " + j, "rect from " + (j - 0.1) + "," + problem.getObjectiveOpt()[j-1] * idealPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]) + " to " + (j + 0.1) + "," + problem.getObjectiveOpt()[j-1] * nadirPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]));
                jPlot1.getJavaPlot().set("label " + j, " at " + Double.toString(j) + " , " + Double.toString(maximumValue + 0.1) + " '" + Double.toString(roundWithPrecision(nadirPointArtifitial[j-1],DECIMALS))+ "' font ',9' front center");
            }
        }
        jPlot1.getJavaPlot().set("arrow ", "from " + 0.4 + "," + minimumValue + " to " + 0.4 + "," + maximumValue);
        jPlot1.getJavaPlot().set("label " + numberOfObjectives+1, " at " + 0.4 + "," + (minimumValue -0.05)+ " 'Min' font ',9' front center");
        jPlot1.getJavaPlot().set("label " + (numberOfObjectives +2), " at " + 0.4 + "," + (maximumValue +0.05)+ " 'Max' font ',9' front center");
        

        //It shows the solutions
        int solutionIndex = 0;
        for (int i = 0; i < data.length; i = i + numberOfObjectives) {
            double[][] localData = new double[numberOfObjectives][2];
            for (int j = 0; j < numberOfObjectives; j++) {
                localData[j][0] = data[i + j][0];
                localData[j][1] = data[i + j][1];
            }

            //It paints each objective in each vertical box                       
            PlotStyle myPlotStyle = new PlotStyle();
            myPlotStyle.setStyle(Style.LINESPOINTS);
            myPlotStyle.set("linecolor", String.valueOf(solutionIndex));
            //myPlotStyle.set("tit", "'s" + String.valueOf(i) + "'"); 
            myPlotStyle.setPointType(6);
            myPlotStyle.setPointSize(2);
            myPlotStyle.setLineWidth(1);

            DataSetPlot dataSetPlot = new DataSetPlot(localData);
            dataSetPlot.setPlotStyle(myPlotStyle);
            dataSetPlot.setTitle("s" + String.valueOf(solutionIndex + 1));

            jPlot1.getJavaPlot().addPlot(dataSetPlot);

            solutionIndex++;
        }

        //It shows the aspiration level
        if (plotReferenceLevels) {
            PlotStyle myPlotStyle2 = new PlotStyle();
            myPlotStyle2.setStyle(Style.LINESPOINTS);
            myPlotStyle2.set("linecolor", "rgb 'black'");
            myPlotStyle2.setPointType(8);
            myPlotStyle2.setPointSize(2);
            myPlotStyle2.setLineWidth(1);
            myPlotStyle2.setLineType(10);
            //DataSetPlot dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getReferencePoint()));
            DataSetPlot dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getAspirationLevel(), idealPoint, nadirPoint, problem));
            dataSetPlot.setPlotStyle(myPlotStyle2);
            dataSetPlot.setTitle("Aspir. Level");
            jPlot1.getJavaPlot().addPlot(dataSetPlot);
        }
        
        //It shows the reservation level
        if (plotReferenceLevels) {
            PlotStyle myPlotStyle2 = new PlotStyle();
            myPlotStyle2.setStyle(Style.LINESPOINTS);
            myPlotStyle2.set("linecolor", "rgb 'black'");
            myPlotStyle2.setPointType(2);
            myPlotStyle2.setPointSize(2);
            myPlotStyle2.setLineWidth(1);
            myPlotStyle2.setLineType(10);
            //DataSetPlot dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getReferencePoint()));
            DataSetPlot dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getReservationLevel(), idealPoint, nadirPoint, problem));
            dataSetPlot.setPlotStyle(myPlotStyle2);
            dataSetPlot.setTitle("Reser. Level");
            jPlot1.getJavaPlot().addPlot(dataSetPlot);
        }

        //System.out.println(jplot.getJavaPlot().getCommands());
    }

    private void plotValuePath() throws JMException {
        int numberOfObjectives = Integer.valueOf(this.jSpinnerObjectivesNumber.getValue().toString());
        double minimumValue = Double.MAX_VALUE, maximumValue = Double.MIN_VALUE;
        double[] idealPointArtifitial = new double[numberOfObjectives];
        double[] nadirPointArtifitial = new double[numberOfObjectives];
        String problemName = (String) jComboBoxProblemName.getSelectedItem();
        String soltype;
        if (problemName.compareTo("AuxiliaryServicesProblem")== 0){
            soltype = "ArrayRealAndBinary";
            DECIMALS = 2;
        }
        else
            soltype = "Real";
        Object[] problemParams = {soltype};
        Problem problem = (new ProblemFactory()).getProblem(problemName, problemParams);

        jPlot1.getJavaPlot().getPlots().clear();

        javaPlot1 = new JavaPlot(null, javaPlot1.getGNUPlotPath(), javaPlot1.getTerminal(), false);
        javaPlot1.set("term", "png size " + (jPlot1.getWidth() - 1) + ", " + (jPlot1.getHeight() - 1));
        javaPlot1.set("grid", "");
        javaPlot1.setPersist(false);
        jPlot1.setJavaPlot(javaPlot1);
        jPanelPlot.setBorder(new TitledBorder("Plot for " + jComboBoxProblemName.getSelectedItem() + " problem"));
        
        
        if(problem.getObjectiveOpt() == null) // all objectives are to be minimized
        {
            idealPointArtifitial = idealPoint;
            nadirPointArtifitial = nadirPoint;
            for (int i = 0; i < numberOfObjectives; i++) {
                if (idealPoint[i] < minimumValue) {
                    minimumValue = idealPoint[i]/(nadirPoint[i] - idealPoint[i]);
                }
                if (nadirPoint[i] > maximumValue) {
                    maximumValue =  nadirPoint[i]/(nadirPoint[i] - idealPoint[i]);
                }
            }
        }
        else{ // at least, one objectives is to be maximized
            for (int i = 0; i < numberOfObjectives; i++) {
                if (problem.getObjectiveOpt()[i] == 1){ // the objective i must be minimized
                    idealPointArtifitial[i] = idealPoint[i];
                    nadirPointArtifitial[i] = nadirPoint[i];
                }
                else{ // the objective i must be maximized
                    idealPointArtifitial[i] = nadirPoint[i] * problem.getObjectiveOpt()[i] + 0.0;
                    nadirPointArtifitial[i] = idealPoint[i] * problem.getObjectiveOpt()[i] + 0.0;
                }
            }
            
            for (int i = 0; i < numberOfObjectives; i++) {
                if (idealPointArtifitial[i] < minimumValue) {
                    minimumValue =  idealPointArtifitial[i]/(nadirPointArtifitial[i] - idealPointArtifitial[i]);
                }
                if (nadirPointArtifitial[i] > maximumValue) {
                    maximumValue = nadirPointArtifitial[i]/(nadirPointArtifitial[i] - idealPointArtifitial[i]);
                }
            }
        }

        //Plot information
        jPlot1.getJavaPlot().set("xlabel", "'objectives' font ',9'");
        jPlot1.getJavaPlot().set("ylabel", "'values' font ',9'");
        String aux = "(";
        for(int i=0; i < numberOfObjectives; i++){
           aux = aux + "'" + Double.toString(roundWithPrecision(idealPointArtifitial[i], DECIMALS)) + "' " + Integer.toString(i + 1); //add ("Pi" 3.14159)
           if ( i != numberOfObjectives -1 )
               aux = aux + ", ";
           else
               aux = aux + ")";
        }
        jPlot1.getJavaPlot().set("xtics", aux);
        //jPlot1.getJavaPlot().set("xtics", "1");
        jPlot1.getJavaPlot().set("ytics", "1");
        jPlot1.getJavaPlot().set("tics", "font ',9'");
        jPlot1.getJavaPlot().set("key", "samplen 2 inside bottom font ',9'");

        //It shows a vertical box for each objective                
        jPlot1.getJavaPlot().set("xrange", "[0.5 to " + (numberOfObjectives + 1.5) + "]");  
        jPlot1.getJavaPlot().set("yrange", "[" + (minimumValue - 0.1) + " to " + (maximumValue + 0.4) + "]");
        if(problem.getObjectiveOpt() == null){
            for (int j = 1; j <= numberOfObjectives; j++) {
               jPlot1.getJavaPlot().set("object " + j, "rect from " + (j - 0.1) + "," + idealPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]) + " to " + (j + 0.1) + "," + nadirPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1])); 
               jPlot1.getJavaPlot().set("label " + j, " at " + Double.toString(j) + " , " + Double.toString(maximumValue + 0.1) + " '" + Double.toString(roundWithPrecision(nadirPoint[j-1],DECIMALS)) + "' font ',9' front center");
            }    
        }
        else{
            for (int j = 1; j <= numberOfObjectives; j++) {
                jPlot1.getJavaPlot().set("object " + j, "rect from " + (j - 0.1) + "," + problem.getObjectiveOpt()[j-1] * idealPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]) + " to " + (j + 0.1) + "," + problem.getObjectiveOpt()[j-1] * nadirPoint[j - 1]/(nadirPoint[j-1] - idealPoint[j-1]));
                jPlot1.getJavaPlot().set("label " + j, " at " + Double.toString(j) + " , " + Double.toString(maximumValue + 0.1) + " '" + Double.toString(roundWithPrecision(nadirPointArtifitial[j-1],DECIMALS))+ "' font ',9' front center");
            }
        }
        jPlot1.getJavaPlot().set("arrow ", "from " + 0.4 + "," + minimumValue + " to " + 0.4 + "," + maximumValue);
        jPlot1.getJavaPlot().set("label " + numberOfObjectives+1, " at " + 0.4 + "," + (minimumValue -0.05)+ " 'Min' font ',9' front center");
        jPlot1.getJavaPlot().set("label " + (numberOfObjectives +2), " at " + 0.4 + "," + (maximumValue +0.05)+ " 'Max' font ',9' front center");
        

        //It shows the aspiration level
        PlotStyle myPlotStyle2 = new PlotStyle();
        myPlotStyle2.setStyle(Style.LINESPOINTS);
        myPlotStyle2.set("linecolor", "rgb 'black'");
        myPlotStyle2.setPointType(8);
        myPlotStyle2.setPointSize(2);
        myPlotStyle2.setLineWidth(1);
        myPlotStyle2.setLineType(10);        
        DataSetPlot dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getAspirationLevel(), idealPoint, nadirPoint, problem));  
        dataSetPlot.setPlotStyle(myPlotStyle2);
        dataSetPlot.setTitle("Aspir. level");
        jPlot1.getJavaPlot().addPlot(dataSetPlot);
        
        //It shows the reservation level
        myPlotStyle2 = new PlotStyle();
        myPlotStyle2.setStyle(Style.LINESPOINTS);
        myPlotStyle2.set("linecolor", "rgb 'black'");
        myPlotStyle2.setPointType(2);
        myPlotStyle2.setPointSize(2);
        myPlotStyle2.setLineWidth(1);
        myPlotStyle2.setLineType(10);        
        dataSetPlot = new DataSetPlot(ValuePath.getReferencePointForPlot(getReservationLevel(), idealPoint, nadirPoint, problem));  
        dataSetPlot.setPlotStyle(myPlotStyle2);
        dataSetPlot.setTitle("Reser. level");
        jPlot1.getJavaPlot().addPlot(dataSetPlot);
    }

    private void plotAspirationLevel(JPlot jplot) {
        double[][] rp = new double[1][(int) jSpinnerObjectivesNumber.getValue()];
        rp[0] = getAspirationLevel().toDouble();

        PlotStyle myPlotStyle = new PlotStyle();
        myPlotStyle.setStyle(Style.POINTS);
        myPlotStyle.setPointType(8);
        myPlotStyle.setPointSize(2);
        myPlotStyle.setLineWidth(1);
        myPlotStyle.set("linecolor", "rgb 'black'");

        DataSetPlot dataSetPlot = new DataSetPlot(rp);
        dataSetPlot.setTitle("Aspiration level");
        dataSetPlot.setPlotStyle(myPlotStyle);

        jplot.getJavaPlot().addPlot(dataSetPlot);
    }

    private void plotReservationLevel(JPlot jplot) {
        double[][] rp = new double[1][(int) jSpinnerObjectivesNumber.getValue()];
        rp[0] = getReservationLevel().toDouble();

        PlotStyle myPlotStyle = new PlotStyle();
        myPlotStyle.setStyle(Style.POINTS);
        myPlotStyle.setPointType(2);
        myPlotStyle.setPointSize(2);
        myPlotStyle.setLineWidth(1);
        myPlotStyle.set("linecolor", "rgb 'black'");

        DataSetPlot dataSetPlot = new DataSetPlot(rp);
        dataSetPlot.setTitle("Reservation level");
        dataSetPlot.setPlotStyle(myPlotStyle);

        jplot.getJavaPlot().addPlot(dataSetPlot);
    }
    
    private void generateWeightsVectorsWithKmeans(String weightsDirectory, String weightsFileName) {
        if (!(new File(weightsDirectory + java.io.File.separator + weightsFileName).exists())) {
            FileWriter fw = null;
            try {
                fw = new FileWriter(weightsDirectory + java.io.File.separator + weightsFileName, false);
                KMeans kMeans = new KMeans(weightsDirectory + java.io.File.separator + FILE_OF_WEIGHTS_FOR_KMEANS[((Integer) (jSpinnerObjectivesNumber.getValue())).intValue() - 3] + "." + Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) + "D", ((Integer) jSpinnerSolutionsNumber.getValue()).intValue()*2);
                java.util.List<Point> centroids = kMeans.getCentroids();

                Collections.sort(centroids);

                for (int i = 0; i < ((Integer) jSpinnerSolutionsNumber.getValue()).intValue()*2; i++) {
                    fw.write(centroids.get(i).toStringForFileFormat() + "\n");
                }
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean setExperimentForNewProblem() {
        boolean isPossibleToExecute = true;

        try {
            isPossibleToExecute = initExperiment((String) jComboBoxProblemName.getSelectedItem(), PARETO_FRONTS_FOLDER + jComboBoxProblemName.getSelectedItem() + "." + jSpinnerObjectivesNumber.getValue().toString() + "D.pf", OUTPUT_FOLDER);
        } catch (JMException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (isPossibleToExecute) {
            try {
                loadReferenceLevelsUI(Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()), ((Integer) jSpinnerSolutionsNumber.getValue()).intValue(), idealPoint, nadirPoint);
            } catch (JMException ex) {
                Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (jComboBoxAlgorithm.getSelectedItem().equals("IRAEMO")){
                algorithm.setInputParameter("aspirationLevel", getAspirationLevel());
                algorithm.setInputParameter("reservationLevel", getReservationLevel());
            }
            else if (jComboBoxAlgorithm.getSelectedItem().equals("RNSGAII")){
                ReferencePoint[] referencePoints = new ReferencePoint[2];
                referencePoints[0] = getAspirationLevel();
                referencePoints[1] = getReservationLevel();                
                algorithm.setInputParameter("referencePoints", referencePoints);
            }

            jPlot1.getJavaPlot().getPlots().clear();

            if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) < 3) {
                plotParetoFront(jPlot1);
                plotAspirationLevel(jPlot1);
                plotReservationLevel(jPlot1);
            } else {
                try {
                    plotValuePath();
                } catch (JMException ex) {
                    Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            repaintPlot(jPlot1);

            jButtonNextIteration.setEnabled(false);
        } else {
            showDialog(this, "Problem's configuration error:", "The " + jSpinnerObjectivesNumber.getValue().toString() + " objectives " + jComboBoxProblemName.getSelectedItem() + " problem does not exist.");
        }

        return isPossibleToExecute;
    }

    private void plotParetoFront(JPlot jplot) {
        PlotStyle myPlotStyle = new PlotStyle();
        myPlotStyle.setStyle(Style.POINTS);
        myPlotStyle.setPointType(6);
        myPlotStyle.setPointSize(1);
        myPlotStyle.set("linecolor", "0");

        try {
            FileDataSet fds = new FileDataSet(new File(PARETO_FRONTS_FOLDER + jComboBoxProblemName.getSelectedItem() + "." + jSpinnerObjectivesNumber.getValue().toString() + "D.pf"));
            DataSetPlot testDataSetPlot = new DataSetPlot(fds);
            testDataSetPlot.setTitle("Pareto front");
            testDataSetPlot.setPlotStyle(myPlotStyle);

            if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) == 2) {
                javaPlot1 = new JavaPlot(null, javaPlot1.getGNUPlotPath(), javaPlot1.getTerminal(), false);
                javaPlot1.set("term", "png size " + (jplot.getWidth() - 1) + ", " + (jplot.getHeight() - 1));
                javaPlot1.set("grid", "");
                javaPlot1.setPersist(false);
                jplot.setJavaPlot(javaPlot1);
                jplot.getJavaPlot().addPlot(testDataSetPlot);

                jplot.getJavaPlot().set("xlabel", "'f1'");
                jplot.getJavaPlot().set("ylabel", "'f2'");
                jPanelPlot.setBorder(new TitledBorder("Plot for " + jComboBoxProblemName.getSelectedItem() + " problem"));
            } else if (Integer.valueOf(jSpinnerObjectivesNumber.getValue().toString()) == 3) {
                javaPlot1 = new JavaPlot(null, javaPlot1.getGNUPlotPath(), javaPlot1.getTerminal(), true);
                javaPlot1.set("term", "png size " + (jplot.getWidth() - 1) + ", " + (jplot.getHeight() - 1));
                javaPlot1.set("grid", "");
                javaPlot1.setPersist(false);
                jplot.setJavaPlot(javaPlot1);
                jplot.getJavaPlot().addPlot(testDataSetPlot);

                jplot.getJavaPlot().set("xlabel", "'f1'");
                jplot.getJavaPlot().set("ylabel", "'f2'");
                jplot.getJavaPlot().set("zlabel", "'f3'");
                jPanelPlot.setBorder(new TitledBorder("Plot for " + jComboBoxProblemName.getSelectedItem() + " problem"));
            } else {
                jPanelPlot.setBorder(new TitledBorder("Plot not available"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NumberFormatException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ArrayIndexOutOfBoundsException ex) {
            Logger.getLogger(Study.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void repaintPlot(JPlot jplot) {
        jplot.plot();
        jplot.repaint();
    }

    private Integer scaleFloatToIntValue(Float floatNumber, Integer minIntValue, Integer maxIntValue, Float minFloatValue, Float maxFloatValue) {
        Integer result;
        result = new Integer(minIntValue + (int) (((maxIntValue - minIntValue) * ((floatNumber - minFloatValue) * 100.0) / (maxFloatValue - minFloatValue)) / 100));

        return result;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Study.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Study().setVisible(true);
            }
        });
    }

    //The following codes set where the text get redirected. In this case, jTextArea1    
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                jTextAreaLog.append(text);
            }
        });
    }

    //Followings are The Methods that do the Redirect, you can simply Ignore them. 
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private MyComponents.FloatJSlider floatJSlider2;
    private MyComponents.FloatJSlider floatJSlider3;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButtonNextIteration;
    private javax.swing.JButton jButtonStart;
    private javax.swing.JComboBox<String> jComboBoxAlgorithm;
    private javax.swing.JComboBox jComboBoxProblemName;
    private javax.swing.JLabel jLabel0;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanelActions;
    private javax.swing.JPanel jPanelAlgorithmConfiguration;
    private javax.swing.JPanel jPanelAspirationLevel;
    private javax.swing.JPanel jPanelPlot;
    private javax.swing.JPanel jPanelProblemConfiguration;
    private javax.swing.JPanel jPanelReservationLevel;
    private com.panayotis.gnuplot.swing.JPlot jPlot1;
    private javax.swing.JScrollPane jScrollPaneLog;
    private javax.swing.JScrollPane jScrollPaneSolutions;
    private javax.swing.JSpinner jSpinnerGenerationsNumber;
    private javax.swing.JSpinner jSpinnerObjectivesNumber;
    private javax.swing.JSpinner jSpinnerPopulationSize;
    private javax.swing.JSpinner jSpinnerSolutionsNumber;
    private javax.swing.JTable jTableSolutions;
    private javax.swing.JTextArea jTextAreaLog;
    // End of variables declaration//GEN-END:variables

    private void elseif(boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
