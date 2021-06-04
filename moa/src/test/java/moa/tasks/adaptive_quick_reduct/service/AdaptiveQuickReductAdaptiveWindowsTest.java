package moa.tasks.adaptive_quick_reduct.service;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.lazy.kNN;
import moa.tasks.adaptive_quick_reduct.model.AdaptiveWindow;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.ResultBean;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.EuclideanDistanceCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Optional;

import static moa.tasks.adaptive_quick_reduct.utils.Utils.*;

public class AdaptiveQuickReductAdaptiveWindowsTest {

  public AdaptiveQuickReductAdaptiveWindowsTest() {
  }

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductAdaptiveWindowsTest.class);
  private static final String AQR_CLASSIFICATION = "Aqr Classification run";
  private static final String NO_AQR_CLASSIFICATION = "Standard Classification run";
  private static final double similarityThreshold = 0.1;

  public static void main(String[] args) {

//    testElectricityClassification();
//    testGmscClassification();
    testElectricityDriftDetection();
  }

  private static void testElectricityClassification() {
    String datasetName = "moa/src/test/resources/adaptive_quick_reduct_datasets/electricity_20.arff";

    int initialWindowSize = 500;

    ArrayList<Float> overlapPercents = new ArrayList<>(3);
    overlapPercents.add(0.1f);
    overlapPercents.add(0.5f);
    overlapPercents.add(0.8f);

    ArrayList<Integer> kNeighbors = new ArrayList<>(4);
    kNeighbors.add(3);
//    kNeighbors.add(7);
//    kNeighbors.add(9);
//    kNeighbors.add(11);

    ArrayList<Double> ratioSteps = new ArrayList<>(3);
    ratioSteps.add(0.1);
    ratioSteps.add(0.15);
    ratioSteps.add(0.2);

    Instances instances = readInstances(datasetName);
    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            instances.getRelationName(), instances.numInstances(), instances.numClasses()));

//    for(float overlapPercent : overlapPercents) {
//      for(double ratioStep : ratioSteps) {
//        runBayesClassifications(instances, initialWindowSize, overlapPercent, ratioStep);
//        for(Integer kNeighbor : kNeighbors) {
//          runKNNClassifications(instances, initialWindowSize, overlapPercent, ratioStep, kNeighbor, initialWindowSize);
//        }
//      }
//    }

    for(float overlapPercent : overlapPercents) {
      ratioSteps.stream().forEach(ratioStep -> {
                runBayesClassifications(instances, initialWindowSize, overlapPercent, ratioStep);
                runKNNClassifications(instances, initialWindowSize, overlapPercent, ratioStep, kNeighbors.get(0), initialWindowSize);
              }
      );
    }
  }

  private static void testGmscClassification() {
    String datasetName = "moa/src/test/resources/adaptive_quick_reduct_datasets/cs-training.arff";

    ArrayList<Integer> kNeighbors = new ArrayList<>(4);
//    kNeighbors.add(3);
    kNeighbors.add(7);
//    kNeighbors.add(9);
//    kNeighbors.add(11);

    int initialWindowSize = 500;

    ArrayList<Float> overlapPercents = new ArrayList<>(3);
    overlapPercents.add(0.1f);
    overlapPercents.add(0.5f);
    overlapPercents.add(0.8f);

    ArrayList<Double> ratioSteps = new ArrayList<>(3);
    ratioSteps.add(0.1);
    ratioSteps.add(0.15);
    ratioSteps.add(0.2);

    Instances instances = readInstances(datasetName);
    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            instances.getRelationName(), instances.numInstances(), instances.numClasses()));

//    for(float overlapPercent : overlapPercents) {
//      for(double ratioStep : ratioSteps) {
//        runBayesClassifications(instances, initialWindowSize, overlapPercent, ratioStep);
//        for(Integer kNeighbor : kNeighbors) {
//          runKNNClassifications(instances, initialWindowSize, overlapPercent, ratioStep, kNeighbor, initialWindowSize);
//        }
//      }
//    }

    for(float overlapPercent : overlapPercents) {
      ratioSteps.parallelStream().forEach(ratioStep -> {
                runBayesClassifications(instances, initialWindowSize, overlapPercent, ratioStep);
                runKNNClassifications(instances, initialWindowSize, overlapPercent, ratioStep, kNeighbors.get(0), initialWindowSize);
              }
      );
    }
  }

  private static void testElectricityDriftDetection() {
    String datasetName = "moa/src/test/resources/adaptive_quick_reduct_datasets/electricity_Drifted.arff";

    int initialWindowSize = 500;
    ArrayList<Float> overlapPercents = new ArrayList<>(3);
    overlapPercents.add(0.1f);
    overlapPercents.add(0.5f);
    overlapPercents.add(0.8f);

    ArrayList<Integer> kNeighbors = new ArrayList<>(4);
    kNeighbors.add(3);
    kNeighbors.add(7);

    ArrayList<Double> ratioSteps = new ArrayList<>(3);
    ratioSteps.add(0.1);
    ratioSteps.add(0.15);
    ratioSteps.add(0.2);

    Instances instances = readInstances(datasetName);
    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            instances.getRelationName(), instances.numInstances(), instances.numClasses()));

    for(float overlapPercent : overlapPercents) {
      ratioSteps.forEach(ratioStep -> {
                runBayesClassifications(instances, initialWindowSize, overlapPercent, ratioStep);
                kNeighbors.parallelStream().forEach(kNeighbor ->
                        runKNNClassifications(instances, initialWindowSize, overlapPercent, ratioStep, kNeighbor, initialWindowSize)
                );
              }
      );
    }
  }

  private static void runKNNClassifications(Instances instances, int windowSize, double overlapPercentage, double ratioStep, int kN, int storedInstances) {
//    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, similarityThreshold);
    Window<Instance> windowsAqr = new AdaptiveWindow(instances.numInstances(), windowSize, overlapPercentage, ratioStep);
    AbstractClassifier classifierAQR = new kNN(Optional.of(kN), Optional.of(storedInstances));

    logger.info(String.format("KNN - Window Size %d, windows overlap percentage %f, num iteration %d, K Neighbor %d, KNN Stored Instances %d, AQR distance type %s, AQR similarity threshold %f",
            windowSize, overlapPercentage, windowsAqr.getTotalIterationNumber(), kN, storedInstances,
            distanceCalculator.getClass().getSimpleName(), similarityThreshold));

    Window<Instance> windowsStd = new AdaptiveWindow(instances.numInstances(), windowSize, overlapPercentage, ratioStep);
    AbstractClassifier classifier = new kNN(Optional.of(kN), Optional.of(storedInstances));

    ArrayList<ResultBean> accuracies = runClassificationStandard(instances, windowsStd, classifier);
    ArrayList<ResultBean> accuraciesAQR = runClassificationAQR(instances, aqr, windowsAqr, classifierAQR, false);

    String relationName = String.join("_", instances.getRelationName().split(" "));
    String timeStamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
    String configInfo = String.format("ws_%s_wo_%s_rs_%s_k_%s",
            getPaddedInt(windowSize, 4),
            getPaddedInt((int) (overlapPercentage * 100), 3),
            getPaddedInt((int) (ratioStep * 100), 3),
            getPaddedInt(kN, 2)
    );

    String filenameAccuraciesAQR = String.format("moa/CSV/AQR_KNN_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportResultBeanCSV(accuraciesAQR, filenameAccuraciesAQR);
    logger.info(String.format("Exported %s ", filenameAccuraciesAQR));

    String filenameAccuracies = String.format("moa/CSV/STD_KNN_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportResultBeanCSV(accuracies, filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  private static void runBayesClassifications(Instances instances, int windowSize, double overlapPercentage, double ratioStep) {
//    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, similarityThreshold);
    Window<Instance> windowsAqr = new AdaptiveWindow(instances.numInstances(), windowSize, overlapPercentage, ratioStep);
    AbstractClassifier classifierAQR = new NaiveBayes();
    classifierAQR.resetLearningImpl();

    logger.info(String.format("Bayes - Window Size %d, windows overlap percentage %f, num iteration %d, distance type %s, similarity threshold %f",
            windowSize, overlapPercentage, windowsAqr.getTotalIterationNumber(),
            distanceCalculator.getClass().getSimpleName(), similarityThreshold));

    Window<Instance> windowsStd = new AdaptiveWindow(instances.numInstances(), windowSize, overlapPercentage, ratioStep);
    AbstractClassifier classifier = new NaiveBayes();
    classifier.resetLearningImpl();

    ArrayList<ResultBean> accuracies = runClassificationStandard(instances, windowsStd, classifier);
    ArrayList<ResultBean> accuraciesAQR = runClassificationAQR(instances, aqr, windowsAqr, classifierAQR, true);

    String relationName = String.join("_", instances.getRelationName().split(" "));
    String timeStamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
    String configInfo = String.format("ws_%s_wo_%s_rs_%s",
            getPaddedInt(windowSize, 4),
            getPaddedInt((int) (overlapPercentage * 100), 3),
            getPaddedInt((int) (ratioStep * 100), 3)
    );

    String filenameAccuraciesAQR = String.format("moa/CSV/AQR_BAYES_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportResultBeanCSV(accuraciesAQR, filenameAccuraciesAQR);
    logger.info(String.format("Exported %s ", filenameAccuraciesAQR));

    String filenameAccuracies = String.format("moa/CSV/STD_BAYES_ACC_%s_%s_%s.csv", relationName, configInfo, timeStamp);
    exportResultBeanCSV(accuracies, filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  private static ArrayList<ResultBean> runClassificationAQR(Instances instances,
                                                            AdaptiveQuickReduct aqr,
                                                            Window<Instance> windows,
                                                            AbstractClassifier classifier,
                                                            boolean saveScores) {

    int iterationNumber = 1;

    String classificationString = String.format("%s - %s ", AQR_CLASSIFICATION, classifier.getClass().getSimpleName());

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());
    ArrayList<ResultBean> results = new ArrayList<>(windows.getTotalIterationNumber() - 1);

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

      results.add(new ResultBean(currentOverallAccuracy, windows.getWindowSize()));
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
      if(classifier instanceof kNN) {
//        classifier.resetLearningImpl();
        ((kNN) classifier).setLimitOption(windows.getWindowSize());
      }
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("%s %d%c", classificationString, percentage, '%'));
    }

    logger.info(classificationString + "- Ending Computation");

    if(saveScores) {
      double[][] scores = getAttributeScoresFromReducts(reducts, instances);

      String relationName = String.join("_", instances.getRelationName().split(" "));
      String timestamp = ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss);
      String filenameScores = String.format("moa/CSV/Scores/Scores_%s_%s_%s.csv", relationName, windows, timestamp);
      exportAttributeScoresCSV(scores, instances, filenameScores);
      logger.info(String.format("Exported %s ", filenameScores));
    }

    return results;
  }

  private static ArrayList<ResultBean> runClassificationStandard(Instances instances, Window<Instance> windows, AbstractClassifier classifier) {

    int iterationNumber = 1;

    String classificationString = String.format("%s - %s ", NO_AQR_CLASSIFICATION, classifier.getClass().getSimpleName());

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    ArrayList<ResultBean> resultBeans = new ArrayList<>(windows.getTotalIterationNumber() - 1);

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

      resultBeans.add(new ResultBean(currentOverallAccuracy, windows.getWindowSize()));
      logger.info(String.format("%s iteration n. %d, Current Accuracy = %f", classificationString, iterationNumber, currentOverallAccuracy));

      //--------------------------------------TRAIN ON CURRENT WINDOW --------------------------------------------------

      iWindow.forEach(keyValue -> classifier.trainOnInstance(keyValue.getValue()));

      //--------------------------------------LOAD NEXT WINDOW----------------------------------------------------------
      iWindow = windows.getNextWindow(instances.getInstances());
      if(classifier instanceof kNN) {
//        classifier.resetLearningImpl();
        ((kNN) classifier).setLimitOption(windows.getWindowSize());
      }
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("%s %d%c", classificationString, percentage, '%'));
    }

    logger.info(classificationString + "- Ending Computation");

    return resultBeans;
  }

  private static String getPaddedInt(int number, int numbOfDesiredString) {
    int numbOfDigits = String.valueOf(number).length();
    int diff = numbOfDesiredString - numbOfDigits;
    StringBuilder zeroPrefix = new StringBuilder();

    for(int i = 0; i < diff; i++) {
      zeroPrefix.append("0");
    }

    return String.format("%s%d", zeroPrefix, number);
  }

}