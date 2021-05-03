package moa.tasks.adaptive_quick_reduct.service;

import com.yahoo.labs.samoa.instances.ArffLoader;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import moa.tasks.AdaptiveQuickReductConfig;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.EuclideanDistanceCalculator;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.ManhattanDistanceCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;

import static moa.tasks.adaptive_quick_reduct.utils.Utils.*;
import static org.junit.Assert.*;

public class AdaptiveQuickReductTest {
  public AdaptiveQuickReductTest() {
  }

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductConfig.class);
  private static final int DEFAULT_WINDOWS_SIZE = 100;
  private static final int DEFAULT_WINDOWS_OVERLAP = 90;
  private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1;

  private static final String datasetFileName = "moa/src/test/resources/adaptive_quick_reduct_datasets/electricity.arff";
//  private static final String datasetFileName = "moa/src/test/resources/adaptive_quick_reduct_datasets/testDataset.arff";

  public static void main(String[] args) {

    Instances instances = readInstances(datasetFileName);
    logger.info("Adaptive Quick Reduct Config");
    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            instances.getRelationName(), instances.numInstances(), instances.numClasses()));

    Window<Instance> windows = new SlidingWindow<>(instances.numInstances(), DEFAULT_WINDOWS_SIZE, DEFAULT_WINDOWS_OVERLAP);
//    DistanceCalculatorAbstract distanceCalculator = new EuclideanDistanceCalculator();
    DistanceCalculatorAbstract distanceCalculator = new ManhattanDistanceCalculator();
    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(instances, distanceCalculator, DEFAULT_SIMILARITY_THRESHOLD);

    logger.info(String.format("Window Size %d, windows overlap %d, num iteration %d, distance type %s, similarity threshold %f",
            DEFAULT_WINDOWS_SIZE, DEFAULT_WINDOWS_OVERLAP, windows.getTotalIterationNumber(),
            distanceCalculator.getClass().getSimpleName(), DEFAULT_SIMILARITY_THRESHOLD));

    instances.setClassIndex(instances.numAttributes()-1);
    Instances copyInst = new Instances(instances);

    System.out.println(copyInst);

    copyInst.deleteAttributeAt(1);


    System.out.println(copyInst);

//    runClassification(instances, aqr, windows);
  }

  private static void runClassification(Instances instances, AdaptiveQuickReduct aqr, Window<Instance> windows) {

    int iterationNumber = 1;

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(instances.getInstances());

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());

    logger.info("Adaptive Quick Reduct Config - Starting Computation");
    while(!iWindow.isEmpty()) {

      Reduct<Integer> currentReduct = aqr.getReduct(iWindow, previousReduct);

      reducts.add(new Reduct<>(currentReduct));
      previousReduct = new Reduct<>(currentReduct);

      logger.info(String.format("Iteration n. %d, Obtained Current Reduct %s",
              iterationNumber, aqr.getReductFormattedString(currentReduct)));

      iWindow = windows.getNextWindow(instances.getInstances());
    }
    logger.info("Adaptive Quick Reduct Config - Ending Computation");

    double[][] scores = getAttributeScoresFromReducts(reducts, instances);

    String filename = String.format("moa/CSV/%s_%s.csv",
            String.join("_", instances.getRelationName().split(" ")),
            ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss));
    exportCSV(scores, instances, filename);
    logger.info(String.format("Exported %s ", filename));

  }

}