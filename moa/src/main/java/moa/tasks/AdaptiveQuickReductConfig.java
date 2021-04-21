package moa.tasks;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.DatasetInfos;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.LightInstance;
import moa.tasks.adaptive_quick_reduct.service.AdaptiveQuickReduct;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;

public class AdaptiveQuickReductConfig extends FeatureImportanceAbstract {

  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReductConfig.class);

  @Override
  protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
    logger.info("Starting Adaptive Quick Reduct Config");

    DatasetInfos datasetInfos = new DatasetInfos(m_instances);

    ArrayList<LightInstance> myInstances = new ArrayList<>(datasetInfos.getNumInstances());

    for(int i = 0; i < datasetInfos.getNumInstances(); i++) {
      myInstances.add(new LightInstance(m_instances.getInstances().get(i), i));
    }

    Window<LightInstance> windows = new SlidingWindow<>(myInstances, windowSizeOption.getValue(), overlapLengthOption.getValue());

    setWindowSize(windowSizeOption.getValue());
    double nanSubstitute = this.nanSubstitute.getValue();
    setNaNSubstitute(nanSubstitute);

    ArrayList<ArrayList<Double>> attributesScores = new ArrayList<>(windows.getTotalIterationNumber());

    progressBar.setValue(0);
    progressBar.setMaximum(windows.getTotalIterationNumber());

    int iterationNumber = 0;

    ArrayList<LightInstance> iWindow = windows.getNextWindow();

    Reduct<Integer> previousReduct = new Reduct<>();
    ArrayList<Reduct<Integer>> reducts = new ArrayList<>(windows.getTotalIterationNumber());

    AdaptiveQuickReduct aqr = new AdaptiveQuickReduct(datasetInfos);

    while(!iWindow.isEmpty()) {

      Reduct<Integer> currentReduct = aqr.getReduct(iWindow, previousReduct);

      reducts.add(new Reduct<>(currentReduct));
      previousReduct = new Reduct<>(currentReduct);

      logger.info(String.format("Iterazione n. %d, %s", iterationNumber, getReductFormattedString(currentReduct, datasetInfos)));

      iWindow = windows.getNextWindow();
      progressBar.setValue(++iterationNumber);
    }

    return getAttributeScoresFromReducts(reducts, datasetInfos);
  }


  private double[][] getAttributeScoresFromReducts(ArrayList<Reduct<Integer>> reducts, DatasetInfos datasetInfos) {
    int reductsNumber = reducts.size();
    int attributesNumber = datasetInfos.getNumAttributes();

    double[][] attributeScores = new double[reductsNumber][attributesNumber];

    for(int i = 0; i < reductsNumber; i++) {
      for(int j = 0; j < attributesNumber; j++) {
        attributeScores[i][j] = reducts.get(i).contains(j) ? 1.0 : 0.0;
      }
    }
    return attributeScores;
  }

  private String getReductFormattedString(Reduct<Integer> reduct, DatasetInfos datasetInfos) {
    String reductElements = reduct.getReductSet().stream()
            .map(attributeIndex -> String.format("%s", datasetInfos.getAttributeLabelByIndex(attributeIndex)))
            .reduce("", (prev, succ) -> String.format("%s %s ", prev, succ));

    return String.format("Reduct{reductSet=[%s], gammaValue=%f}", reductElements, reduct.getGammaValue());
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

  @Override
  public String getPurposeString() {
    return "This is a hello world task. returns not real feature scores.";
  }

  @Override
  public Class<?> getTaskResultType() {
    return null;
  }
}
