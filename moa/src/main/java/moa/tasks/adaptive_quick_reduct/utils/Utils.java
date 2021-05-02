package moa.tasks.adaptive_quick_reduct.utils;

import com.yahoo.labs.samoa.instances.Instances;
import moa.tasks.adaptive_quick_reduct.model.Reduct;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Utils {
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

  public static void exportCSV(double[][] scores, Instances datasetInfos, String filename) {
    try {
      FileWriter writer = new FileWriter(filename);
      String[] attributes = new String[datasetInfos.numAttributes() - 1];
      for(int i = 0; i < datasetInfos.numAttributes() - 1; i++) {
        attributes[i] = datasetInfos.attribute(i).name();
      }
      String header = String.join(",", attributes);
      writer.write(header + System.lineSeparator());

      for (int row = 0; row < scores.length; row++){
        String[] values = new String[scores[row].length];
        for (int col = 0; col< scores[row].length; col++)
          values[col] = String.valueOf(scores[row][col]);

        writer.write(String.join(",", values) + System.lineSeparator());
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }


}
