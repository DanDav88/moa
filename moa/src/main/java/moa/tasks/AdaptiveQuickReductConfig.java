package moa.tasks;

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.ObjectRepository;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.EuclideanDistanceCalculator;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.ManhattanDistanceCalculator;
import moa.tasks.adaptive_quick_reduct.service.AdaptiveQuickReduct;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;

import static moa.tasks.adaptive_quick_reduct.utils.Utils.*;

public class AdaptiveQuickReductConfig extends FeatureImportanceAbstract {

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductConfig.class);

  @Override
  protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

    logger.info("Adaptive Quick Reduct Config");

    logger.info(String.format("Dataset %s, num instances %d, num classes %d",
            m_instances.getRelationName(), m_instances.numInstances(), m_instances.numClasses()));

    Window<Instance> windows = new SlidingWindow<>(m_instances.numInstances(), windowSizeOption.getValue(), overlapLengthOption.getValue());
    double similarityThreshold = this.similarityThreshold.getValue();

    DistanceCalculatorAbstract distanceCalculator;

    if(this.distanceOption.getChosenLabel().equals(ManhattanDistanceCalculator.getClassName()))
      distanceCalculator = new ManhattanDistanceCalculator();
    else
      distanceCalculator = new EuclideanDistanceCalculator();

    Boolean saveCSV = saveCsvOption.isSet();

    logger.info(String.format("Window Size %d, windows overlap %d, num iteration %d, distance type %s, similarity threshold %f, export CSV = %s",
            windowSizeOption.getValue(), overlapLengthOption.getValue(), windows.getTotalIterationNumber(),
            distanceCalculator.getClass().getCanonicalName(), similarityThreshold, saveCSV));

    setWindowSize(windowSizeOption.getValue());
    double nanSubstitute = this.nanSubstitute.getValue();
    setNaNSubstitute(nanSubstitute);

    progressBar.setValue(0);
    progressBar.setMaximum(windows.getTotalIterationNumber());

    int iterationNumber = 1;

    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow = windows.getNextWindow(m_instances.getInstances());

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());

    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(m_instances, distanceCalculator, similarityThreshold);

    logger.info("Adaptive Quick Reduct Config - Starting Computation");
    while(!iWindow.isEmpty()) {

      Reduct<Integer> currentReduct = aqr.getReduct(iWindow, previousReduct);

      reducts.add(new Reduct<>(currentReduct));
      previousReduct = new Reduct<>(currentReduct);

      logger.info(String.format("Iteration n. %d, Obtained Current Reduct %s",
              iterationNumber, aqr.getReductFormattedString(currentReduct)));

      iWindow = windows.getNextWindow(m_instances.getInstances());
      progressBar.setValue(iterationNumber++);
    }
    logger.info("Adaptive Quick Reduct Config - Ending Computation");

    double[][] scores = getAttributeScoresFromReducts(reducts, m_instances);

    if(saveCSV) {
      String filename = String.format("moa/CSV/%s_%s.csv",
              String.join("_",m_instances.getRelationName().split(" ")),
              ZonedDateTime.now().format(formatter_yyyyMMdd_HH_mm_ss));
      exportCSV(scores, m_instances, filename);
      logger.info(String.format("Exported %s ",filename));
    }

    return scores;
  }

  /**
   * Provides GUI to user so that they can configure parameters for feature importance algorithm.
   */
  public IntOption windowSizeOption = new IntOption("windowSize", 'w', "The size of the windows used.", SlidingWindow.DEFAULT_WINDOW_SIZE);
  public IntOption overlapLengthOption = new IntOption("overlap", 'v', "The length of windows overlap.", SlidingWindow.DEFAULT_OVERLAP);
  public FloatOption nanSubstitute = new FloatOption(
          "NaNSubstitute", 'u',
          "When scores of feature importance are NaN, NaN will be replaced by NaNSubstitute shown in line graph.", 0,
          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  public FloatOption similarityThreshold = new FloatOption(
          "similarityThreshold", 't',
          "Similarity threshold between instances", 0.0,
          Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  public MultiChoiceOption distanceOption = new MultiChoiceOption("DistanceType", 'd', "Define the distance type between instances",
          new String[]{EuclideanDistanceCalculator.getClassName(), ManhattanDistanceCalculator.getClassName()},
          new String[]{EuclideanDistanceCalculator.getDESCRIPTION(), ManhattanDistanceCalculator.getDESCRIPTION()},
          0);
  public FlagOption saveCsvOption = new FlagOption("ExportCSV", 's', "Define if export csv file with scores");

  @Override
  public String getPurposeString() {
    return "This is the implementation of Adaptive Quick Reduct Algorithm";
  }

  @Override
  public Class<?> getTaskResultType() {
    return null;
  }
}
