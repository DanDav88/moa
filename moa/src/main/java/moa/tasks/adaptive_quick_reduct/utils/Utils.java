package moa.tasks.adaptive_quick_reduct.utils;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import moa.classifiers.AbstractClassifier;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.ResultBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {
  private static final Logger logger = LogManager.getLogger(Utils.class);

  private static final String format = "yyyyMMdd_HH_mm_ss";
  public static DateTimeFormatter formatter_yyyyMMdd_HH_mm_ss = DateTimeFormatter.ofPattern(format);

  public static double[][] getAttributeScoresFromReducts(ArrayList<Reduct<Integer>> reducts, Instances datasetInfos) {
    int reductsNumber = reducts.size();
    int attributesNumber = datasetInfos.numAttributes() - 1;

    double[][] attributeScores = new double[reductsNumber][attributesNumber];

    for(int i = 0; i < reductsNumber; i++) {
      for(int j = 0; j < attributesNumber; j++) {
        attributeScores[i][j] = reducts.get(i).contains(j) ? 1.0 : 0.0;
      }
    }
    return attributeScores;
  }

  public static void exportAttributeScoresCSV(double[][] scores, Instances datasetInfos, String filename) {
    try {
      FileWriter writer = new FileWriter(filename);
      String[] attributes = new String[datasetInfos.numAttributes() - 1];
      for(int i = 0; i < datasetInfos.numAttributes() - 1; i++) {
        attributes[i] = datasetInfos.attribute(i).name();
      }
      String header = String.join(",", attributes);
      writer.write(header + System.lineSeparator());

      for(double[] scoreRow : scores) {
        String[] values = new String[scoreRow.length];
        for(int col = 0; col < scoreRow.length; col++)
          values[col] = String.valueOf(scoreRow[col]);

        writer.write(String.join(",", values) + System.lineSeparator());
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public static void exportAccuraciesCSV(ArrayList<Double> accuracies, String filename) {
    try {
      FileWriter writer = new FileWriter(filename);

      String header = "Iteration_Number,Accuracy";
      writer.write(header + System.lineSeparator());

      for(int i = 0; i < accuracies.size(); i++) {
        String lineToWrite = String.format("%d,%s", i, accuracies.get(i));
        writer.write(lineToWrite + System.lineSeparator());
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public static void exportResultBeanCSV(ArrayList<ResultBean> results, String filename) {
    try {
      FileWriter writer = new FileWriter(filename);

      String header = "Iteration_Number,Accuracy,Window_size";
      writer.write(header + System.lineSeparator());

      for(int i = 0; i < results.size(); i++) {
        String lineToWrite = String.format("%d,%s,%s", i, results.get(i).getAccuracyValue(), results.get(i).getWindowSize());
        writer.write(lineToWrite + System.lineSeparator());
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public static HashSet<String> getAttributesToRemove(Instances instances, HashSet<Integer> currentReduct) {
    int numAttributes = instances.numAttributes() - 1;
    HashSet<Integer> fullSetAttributes = new HashSet<>(numAttributes);

    for(int i = 0; i < numAttributes; i++) {
      fullSetAttributes.add(i);
    }
    fullSetAttributes.removeAll(currentReduct);

    Set<String> attributesName = fullSetAttributes.stream().map(attributeIndex -> instances.attribute(attributeIndex).name()
    ).collect(Collectors.toSet());

    return (HashSet<String>) attributesName;
  }

  public static void removeAttributesFromInstances(Instances instances, HashSet<Integer> currentReduct) {
    HashSet<String> indexAttributesToRemove = getAttributesToRemove(instances, currentReduct);

    indexAttributesToRemove.forEach(attributeName -> {
      int attributeIndex = instances.getAttributeIndexByName(attributeName);
      instances.deleteAttributeAt(attributeIndex);
    });
  }

  public static Instances readInstances(String fileName) {
    Reader reader;
    Instances instances = null;
    try {
      reader = new java.io.BufferedReader(new FileReader(fileName));
      weka.core.Instances wekaInstances = new weka.core.Instances(reader);
      WekaToSamoaInstanceConverter m_wekaToSamoaInstanceConverter = new WekaToSamoaInstanceConverter();
      wekaInstances.setClassIndex(wekaInstances.numAttributes() - 1);
      instances = m_wekaToSamoaInstanceConverter.samoaInstances(wekaInstances);
    } catch(IOException e) {
      e.printStackTrace();
    }
    return instances;
  }

  public static double getAccuracyValue(AbstractClassifier classifier,
                                        ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow,
                                        int numClasses,
                                        String jobName) throws NullPointerException {
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
