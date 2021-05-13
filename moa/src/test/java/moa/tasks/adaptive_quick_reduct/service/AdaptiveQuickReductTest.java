package moa.tasks.adaptive_quick_reduct.service;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.lazy.kNN;
import moa.tasks.AdaptiveQuickReductConfig;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.EuclideanDistanceCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.AbstractMap;
import java.util.ArrayList;

import static moa.tasks.adaptive_quick_reduct.utils.Utils.*;

public class AdaptiveQuickReductTest {

  public AdaptiveQuickReductTest() {
  }

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductTest.class);
  private static final int DEFAULT_WINDOWS_SIZE = 100;
  private static final int DEFAULT_WINDOWS_OVERLAP = 90;
  private static final String AQR_CLASSIFICATION = "Aqr Classification run";
  private static final String NO_AQR_CLASSIFICATION = "Standard Classification run";
  private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1;

  public static void main(String[] args) {

    ArrayList<String> datasests = new ArrayList<>(3);
    datasests.add("moa/src/test/resources/adaptive_quick_reduct_datasets/electricity_4.arff");
    datasests.add("moa/src/test/resources/adaptive_quick_reduct_datasets/electricity_10.arff");
    datasests.add("moa/src/test/resources/adaptive_quick_reduct_datasets/electricity_20.arff");

    ArrayList<Integer> windowSizes = new ArrayList<>(10);
    for(int i = 100; i <= 1000; i += 100)
      windowSizes.add(i);

    ArrayList<Float> overlapPercents = new ArrayList<>(3);
    overlapPercents.add(0.1f);
    overlapPercents.add(0.5f);
    overlapPercents.add(0.8f);

    ArrayList<Integer> kNeighbors = new ArrayList<>(4);
    kNeighbors.add(3);
    kNeighbors.add(7);
    kNeighbors.add(9);
    kNeighbors.add(11);

    for(String datasetName : datasests) {
      Instances instances = readInstances(datasetName);
      logger.info(String.format("Dataset %s, num instances %d, num classes %d",
              instances.getRelationName(), instances.numInstances(), instances.numClasses()));
      for(int windowSize : windowSizes) {
        for(float overlapPercent : overlapPercents) {
          int overlap = (int) (windowSize * overlapPercent);
          runBayesClassifications(instances, windowSize, overlap, DEFAULT_SIMILARITY_THRESHOLD);
          for(Integer kNeighbor : kNeighbors) {
            runKNNClassifications(instances, windowSize, overlap, DEFAULT_SIMILARITY_THRESHOLD, kNeighbor, windowSize);
          }
        }
      }
    }

//    ExecutorService e1 = Executors.newFixedThreadPool(6);
//    e1.submit(() -> {
//      runBayesClassifications(instances, 100, 90, DEFAULT_SIMILARITY_THRESHOLD);
//
//    });
//    ExecutorService e2 = Executors.newFixedThreadPool(6);
//    e2.submit(() -> {
//      runBayesClassifications(instances, 500, 250, DEFAULT_SIMILARITY_THRESHOLD);
//      runBayesClassifications(instances, 200, 100, DEFAULT_SIMILARITY_THRESHOLD);
//      runBayesClassifications(instances, 500, 100, DEFAULT_SIMILARITY_THRESHOLD);
//      runBayesClassifications(instances, 500, 0, DEFAULT_SIMILARITY_THRESHOLD);
//    });
  }

  private static void runKNNClassifications(Instances instances, int windowSize, int windowOverlap, double similarityThreshold, int kN, int storedInstances) {
//    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, similarityThreshold);
    Window<Instance> windowsAqr = new SlidingWindow<>(instances.numInstances(), windowSize, windowOverlap);
    AbstractClassifier classifierAQR = new kNN(Optional.of(kN), Optional.of(storedInstances));

    logger.info(String.format("KNN - Window Size %d, windows overlap %d, num iteration %d, K Neighbor %d, KNN Stored Instances %d, AQR distance type %s, AQR similarity threshold %f",
            windowSize, windowOverlap, windowsAqr.getTotalIterationNumber(), kN, storedInstances,
            distanceCalculator.getClass().getSimpleName(), similarityThreshold));

    Window<Instance> windowsStd = new SlidingWindow<>(instances.numInstances(), windowSize, windowOverlap);
    AbstractClassifier classifier = new kNN(Optional.of(kN), Optional.of(storedInstances));

    ArrayList<Double> accuracies = runClassificationStandard(instances, windowsStd, classifier);
    ArrayList<Double> accuraciesAQR = runClassificationAQR(instances, aqr, windowsAqr, classifierAQR);

    String relationName = String.join("_", instances.getRelationName().split(" "));
    String timeStamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
    String configInfo = String.format("ws_%d_wo_%d_k_%d_", windowSize, windowOverlap, kN);

    String filenameAccuraciesAQR = String.format("moa/CSV/AQR_KNN_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportAccuraciesCSV(accuraciesAQR, filenameAccuraciesAQR);
    logger.info(String.format("Exported %s ", filenameAccuraciesAQR));

    String filenameAccuracies = String.format("moa/CSV/STD_KNN_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportAccuraciesCSV(accuracies, filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  private static void runBayesClassifications(Instances instances, int windowSize, int windowOverlap, double similarityThreshold) {
//    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, similarityThreshold);
    Window<Instance> windowsAqr = new SlidingWindow<>(instances.numInstances(), windowSize, windowOverlap);
    AbstractClassifier classifierAQR = new NaiveBayes();
    classifierAQR.resetLearningImpl();

    logger.info(String.format("Bayes - Window Size %d, windows overlap %d, num iteration %d, distance type %s, similarity threshold %f",
            windowSize, windowOverlap, windowsAqr.getTotalIterationNumber(),
            distanceCalculator.getClass().getSimpleName(), similarityThreshold));

    Window<Instance> windowsStd = new SlidingWindow<>(instances.numInstances(), windowSize, windowOverlap);
    AbstractClassifier classifier = new NaiveBayes();
    classifier.resetLearningImpl();

    ArrayList<Double> accuracies = runClassificationStandard(instances, windowsStd, classifier);
    ArrayList<Double> accuraciesAQR = runClassificationAQR(instances, aqr, windowsAqr, classifierAQR);

    String relationName = String.join("_", instances.getRelationName().split(" "));
    String timeStamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
    String configInfo = String.format("ws_%d_wo_%d", windowSize, windowOverlap);

    String filenameAccuraciesAQR = String.format("moa/CSV/AQR_BAYES_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportAccuraciesCSV(accuraciesAQR, filenameAccuraciesAQR);
    logger.info(String.format("Exported %s ", filenameAccuraciesAQR));

    String filenameAccuracies = String.format("moa/CSV/STD_BAYES_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportAccuraciesCSV(accuracies, filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  private static ArrayList<Double> runClassificationAQR(Instances instances, AdaptiveQuickReduct aqr, Window<Instance> windows, AbstractClassifier classifier) {

    int iterationNumber = 1;

    String classificationString = String.format("%s - %s ", AQR_CLASSIFICATION, classifier.getClass().getSimpleName());

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());
    ArrayList<Double> accuracies = new ArrayList<>(windows.getTotalIterationNumber() - 1);

    logger.info(classificationString + "- Starting Computation");

    while(!iWindow.isEmpty()) {
      double currentOverallAccuracy;
      //--------------------------------------TEST----------------------------------------------------------------------
      if(iterationNumber == 1) {
        //test on full window
        try {
          currentOverallAccuracy = getAccuracyValue(classifier, iWindow, instances.numClasses(), classificationString);
        } catch(NullPointerException npe) {
          logger.warn("getAccuracyValue: throws NULL pointer exception");
          currentOverallAccuracy = -1.0;
        }
      }
      else {
        //test on window with previous reduct
        Instances copyInstances = new Instances(instances);
        removeAttributesFromInstances(copyInstances, previousReduct.getReductSet());
        ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> reductWindow = windows.getCurrentWindow(copyInstances.getInstances());

        try {
          currentOverallAccuracy = getAccuracyValue(classifier, reductWindow, instances.numClasses(), classificationString);
        } catch(NullPointerException npe) {
          logger.warn("getAccuracyValue: throws NULL pointer exception");
          currentOverallAccuracy = -1.0;
        }
      }

      accuracies.add(currentOverallAccuracy);
      logger.info(String.format("%s iteration n. %d, Current Accuracy = %f", classificationString, iterationNumber, currentOverallAccuracy));

      //--------------------------------------COMPUTE REDUCT------------------------------------------------------------

      Reduct<Integer> currentReduct = aqr.getReduct(iWindow, previousReduct);
      reducts.add(new Reduct<>(currentReduct));
      previousReduct = new Reduct<>(currentReduct);
      logger.info(String.format("%s iteration n. %d, Obtained Current Reduct %s",
              classificationString, iterationNumber, aqr.getReductFormattedString(currentReduct)));

      //--------------------------------------TRAIN ON WINDOW WITH NEW REDUCT-------------------------------------------

      Instances copyInstances = new Instances(instances);
      removeAttributesFromInstances(copyInstances, currentReduct.getReductSet());
      ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> reductWindow = windows.getCurrentWindow(copyInstances.getInstances());
      reductWindow.forEach(keyValue -> classifier.trainOnInstance(keyValue.getValue()));

      //--------------------------------------LOAD NEXT WINDOW----------------------------------------------------------
      iWindow = windows.getNextWindow(instances.getInstances());
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("%s %d%c", classificationString, percentage, '%'));
    }

    logger.info(classificationString + "- Ending Computation");

    double[][] scores = getAttributeScoresFromReducts(reducts, instances);

    String relationName = String.join("_", instances.getRelationName().split(" "));
    String timestamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
    String filenameScores = String.format("moa/CSV/Scores/Scores_%s_%s_%s.csv", relationName, windows, timestamp);
    exportAttributeScoresCSV(scores, instances, filenameScores);
    logger.info(String.format("Exported %s ", filenameScores));

    return accuracies;
  }

  private static ArrayList<Double> runClassificationStandard(Instances instances, Window<Instance> windows, AbstractClassifier classifier) {

    int iterationNumber = 1;

    String classificationString = String.format("%s - %s ", NO_AQR_CLASSIFICATION, classifier.getClass().getSimpleName());

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    ArrayList<Double> accuracies = new ArrayList<>(windows.getTotalIterationNumber() - 1);

    logger.info(classificationString + "- Starting Computation");

    while(!iWindow.isEmpty()) {

      //--------------------------------------TEST----------------------------------------------------------------------
      double currentOverallAccuracy;
      try {
        currentOverallAccuracy = getAccuracyValue(classifier, iWindow, instances.numClasses(), classificationString);
      } catch(NullPointerException npe) {
        logger.warn("getAccuracyValue: throws NULL pointer exception");
        currentOverallAccuracy = -1.0;
      }

      accuracies.add(currentOverallAccuracy);
      logger.info(String.format("%s iteration n. %d, Current Accuracy = %f", classificationString, iterationNumber, currentOverallAccuracy));

      //--------------------------------------TRAIN ON CURRENT WINDOW --------------------------------------------------

      iWindow.forEach(keyValue -> classifier.trainOnInstance(keyValue.getValue()));

      //--------------------------------------LOAD NEXT WINDOW----------------------------------------------------------
      iWindow = windows.getNextWindow(instances.getInstances());
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("%s %d%c", classificationString, percentage, '%'));
    }

    logger.info(classificationString + "- Ending Computation");

    return accuracies;
  }

}