package moa.tasks.adaptive_quick_reduct.service;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.lazy.kNN;
import moa.tasks.AdaptiveQuickReductConfig;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.ManhattanDistanceCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static moa.tasks.adaptive_quick_reduct.utils.Utils.*;

public class AdaptiveQuickReductTest {

  public AdaptiveQuickReductTest() {
  }

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductConfig.class);
  private static final int DEFAULT_WINDOWS_SIZE = 100;
  private static final int DEFAULT_WINDOWS_OVERLAP = 90;
  private static final String AQR_CLASSIFICATION = "Aqr Classification run";
  private static final String NO_AQR_CLASSIFICATION = "Standard Classification run";
  private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1;

  private static final String datasetFileName = "moa/src/test/resources/adaptive_quick_reduct_datasets/electricity.arff";
//  private static final String datasetFileName = "moa/src/test/resources/adaptive_quick_reduct_datasets/testDataset.arff";

  public static void main(String[] args) {

    Instances instances = readInstances(datasetFileName);

    logger.info("Adaptive Quick Reduct Config");
    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            instances.getRelationName(), instances.numInstances(), instances.numClasses()));

    Window<Instance> windowsAqr = new SlidingWindow<>(instances.numInstances(), DEFAULT_WINDOWS_SIZE, DEFAULT_WINDOWS_OVERLAP);
    Window<Instance> windowsStd = new SlidingWindow<>(instances.numInstances(), DEFAULT_WINDOWS_SIZE, DEFAULT_WINDOWS_OVERLAP);
//    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, DEFAULT_SIMILARITY_THRESHOLD);

    logger.info(String.format("Window Size %d, windows overlap %d, num iteration %d, distance type %s, similarity threshold %f",
            DEFAULT_WINDOWS_SIZE, DEFAULT_WINDOWS_OVERLAP, windowsAqr.getTotalIterationNumber(),
            distanceCalculator.getClass().getSimpleName(), DEFAULT_SIMILARITY_THRESHOLD));

//    System.out.println(copyInst);

//    AbstractClassifier classifier = new kNN();
//    runClassificationStandard(instances, windowsStd, classifier);
//    AbstractClassifier classifierAQR = new kNN();
//    runClassificationAQR(instances, aqr, windowsAqr, classifierAQR);

    ExecutorService executor1 = Executors.newSingleThreadExecutor();
    executor1.submit(() -> {
      AbstractClassifier classifier = new kNN();
      runClassificationStandard(instances, windowsStd, classifier);
    });

    ExecutorService executor2 = Executors.newSingleThreadExecutor();
    executor2.submit(() -> {
      AbstractClassifier classifierAQR = new kNN();
      runClassificationAQR(instances, aqr, windowsAqr, classifierAQR);
    });
    try {
      executor1.wait();
      executor2.wait();
    } catch(InterruptedException e) {
      e.printStackTrace();
    }

  }

  private static void runClassificationAQR(Instances instances, AdaptiveQuickReduct aqr, Window<Instance> windows, AbstractClassifier classifier) {

    int iterationNumber = 1;

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());
    ArrayList<Double> accuracies = new ArrayList<>(windows.getTotalIterationNumber() - 1);

    logger.info("Adaptive Quick Reduct - Starting Computation");

    while(!iWindow.isEmpty()) {
      double currentOverallAccuracy;
      //--------------------------------------TEST----------------------------------------------------------------------
      if(iterationNumber == 1) {
        //test on full window
        currentOverallAccuracy = getAccuracyValue(classifier, iWindow, instances.numClasses(), AQR_CLASSIFICATION);
      }
      else {
        //test on window with previous reduct
        Instances copyInstances = new Instances(instances);
        removeAttributesFromInstances(copyInstances, previousReduct.getReductSet());

        ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> reductWindow = windows.getCurrentWindow(copyInstances.getInstances());
        currentOverallAccuracy = getAccuracyValue(classifier, reductWindow, instances.numClasses(), AQR_CLASSIFICATION);
      }

      accuracies.add(currentOverallAccuracy);
      logger.info(String.format("%s iteration n. %d, Current Accuracy = %f", AQR_CLASSIFICATION, iterationNumber, currentOverallAccuracy));

      //--------------------------------------COMPUTE REDUCT------------------------------------------------------------

      Reduct<Integer> currentReduct = aqr.getReduct(iWindow, previousReduct);
      reducts.add(new Reduct<>(currentReduct));
      previousReduct = new Reduct<>(currentReduct);
      logger.info(String.format("%s iteration n. %d, Obtained Current Reduct %s",
              AQR_CLASSIFICATION, iterationNumber, aqr.getReductFormattedString(currentReduct)));

      //--------------------------------------TRAIN ON WINDOW WITH NEW REDUCT-------------------------------------------

      Instances copyInstances = new Instances(instances);
      removeAttributesFromInstances(copyInstances, currentReduct.getReductSet());
      ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> reductWindow = windows.getCurrentWindow(copyInstances.getInstances());
      reductWindow.forEach(keyValue -> classifier.trainOnInstance(keyValue.getValue()));

      //--------------------------------------LOAD NEXT WINDOW----------------------------------------------------------
      iWindow = windows.getNextWindow(instances.getInstances());
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("Adaptive Quick Reduct %d%c", percentage, '%'));
    }

    logger.info("Adaptive Quick Reduct - Ending Computation");

    double[][] scores = getAttributeScoresFromReducts(reducts, instances);

    String filenameScores = String.format("moa/CSV/Scores_%s_%s.csv",
            String.join("_", instances.getRelationName().split(" ")),
            ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss));
    exportAttributeScoresCSV(scores, instances, filenameScores);
    logger.info(String.format("Exported %s ", filenameScores));

    String filenameAccuracies = String.format("moa/CSV/AQR_Accuracies_%s_%s.csv",
            String.join("_", instances.getRelationName().split(" ")),
            ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss));

    exportAccuraciesCSV(accuracies,filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  private static void runClassificationStandard(Instances instances, Window<Instance> windows, AbstractClassifier classifier) {

    int iterationNumber = 1;

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    ArrayList<Double> accuracies = new ArrayList<>(windows.getTotalIterationNumber() - 1);

    logger.info("Classification - Starting Computation");

    while(!iWindow.isEmpty()) {

      //--------------------------------------TEST----------------------------------------------------------------------
      double currentOverallAccuracy = getAccuracyValue(classifier, iWindow, instances.numClasses(), NO_AQR_CLASSIFICATION);

      accuracies.add(currentOverallAccuracy);
      logger.info(String.format("%s iteration n. %d, Current Accuracy = %f", NO_AQR_CLASSIFICATION, iterationNumber, currentOverallAccuracy));

      //--------------------------------------TRAIN ON CURRENT WINDOW --------------------------------------------------

      iWindow.forEach(keyValue -> classifier.trainOnInstance(keyValue.getValue()));

      //--------------------------------------LOAD NEXT WINDOW----------------------------------------------------------
      iWindow = windows.getNextWindow(instances.getInstances());
      iterationNumber++;
      int percentage = Math.round(((float) iterationNumber / (float) windows.getTotalIterationNumber()) * 100.0f);
      logger.info(String.format("Classification %d%c", percentage, '%'));
    }

    logger.info("Classification - Ending Computation");

    String filenameAccuracies = String.format("moa/CSV/Classifier_Accuracies_%s_%s.csv",
            String.join("_", instances.getRelationName().split(" ")),
            ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss));

    exportAccuraciesCSV(accuracies,filenameAccuracies);
    logger.info(String.format("Exported %s ", filenameAccuracies));
  }

  public static double getAccuracyValue(AbstractClassifier classifier,
                                        ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow,
                                        int numClasses,
                                        String jobName) {
    int[] truePositive = new int[numClasses];
    int[] trueNegative = new int[numClasses];
    int[] falsePositive = new int[numClasses];
    int[] falseNegative = new int[numClasses];
    for(int i = 0; i < numClasses; i++) {
      truePositive[i] = 0;
      trueNegative[i] = 0;
      falsePositive[i] = 0;
      falseNegative[i] = 0;
    }

    iWindow.forEach(keyValue -> {
      int predictedIndex = moa.core.Utils.maxIndex(classifier.getVotesForInstance(keyValue.getValue()));
      int correctIndex = (int) keyValue.getValue().classValue();
      if(predictedIndex == correctIndex) {
        truePositive[correctIndex]++;
        for(int i = 0; i < numClasses; i++) {
          if(i != correctIndex)
            trueNegative[i]++;
        }
      }
      else {
        falsePositive[predictedIndex]++;
        falseNegative[correctIndex]++;
        for(int i = 0; i < numClasses; i++) {
          if(i != correctIndex && i != predictedIndex)
            trueNegative[i]++;
        }
      }
    });

    int truePositiveSum = 0;
    int trueNegativeSum = 0;
    int falsePositiveSum = 0;
    int falseNegativeSum = 0;

    for(int i = 0; i < numClasses; i++) {
      truePositiveSum += truePositive[i];
      trueNegativeSum += trueNegative[i];
      falsePositiveSum += falsePositive[i];
      falseNegativeSum += falseNegative[i];

      logger.debug(String.format("%s Class %d, TP = %d, TN = %d, FP = %d, FN = %d, Accuracy %f",
              jobName, i, truePositive[i], trueNegative[i], falsePositive[i], falseNegative[i],
              computeAccuracy(truePositive[i], trueNegative[i], falsePositive[i], falseNegative[i])));
    }
    return computeAccuracy(truePositiveSum, trueNegativeSum, falsePositiveSum, falseNegativeSum);
  }

  public static double computeAccuracy(int TP, int TN, int FP, int FN) {
    return (double) (TP + TN) / (TP + TN + FN + FP);
  }

}