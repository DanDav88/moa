package moa.tasks;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.core.ObjectRepository;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.SlidingWindow;
import moa.tasks.adaptive_quick_reduct.model.Window;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.DatasetInfos;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.LightInstance;

import java.util.ArrayList;

public class AdaptiveQuickReduct extends FeatureImportanceAbstract {

  @Override
  protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {

    DatasetInfos datasetInfos = new DatasetInfos(m_instances);

    ArrayList<LightInstance> myInstances = new ArrayList<>(m_instances.numInstances());

    for (int i = 0; i< m_instances.numInstances(); i++){
      myInstances.add(new LightInstance(m_instances.getInstances().get(i),i));
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

    while(!iWindow.isEmpty()) {

      iWindow = windows.getNextWindow();
      progressBar.setValue(++iterationNumber);
    }

//    HashSet<Attribute> attributes = m_instances.classAttribute();

//    if(m_instances != null) {
//      for(int j = 0; j < numInstances; j += windowSize) {
//        Instance[] instancesCurrentWindow = new Instance[windowSize];
//        int l = 0;
//        for(int i = j; i < (Math.min(j + windowSize, numInstances)); i++) {
//          instancesCurrentWindow[l] = m_instances.get(i);
//          l++;
//          progressBar.setValue(i + j);
//        }
//
//        double[] currentScore = getCustomScores(instancesCurrentWindow);
//
//
//        for(int k = 0; k < columns; k++) {
//          scores[row][k] = currentScore[k];
//        }
//        row++;
//      }
//    }
    return attributesScores;
  }

  private Reduct<Integer> performAdaptiveQuickReductStep(ArrayList<Instance> iWindow,
                                                         Reduct<Integer> previousReduct) {

    Reduct<Integer> iReduct = new Reduct<Integer>(previousReduct);

    if (previousReduct.isEmpty()){


    }
    else{

    }


    return null;
  }


  private double[][] convertArrayListsToArray(ArrayList<ArrayList<Double>> scores) {
    int rows = scores.size();
    int columns = scores.get(0).size();


    double[][] convertedList = new double[rows][columns];
    for(int i = 0; i < columns; i++) {
      for(int j = 0; j < columns; j++) {
        convertedList[i][j] = scores.get(i).get(j);
      }
    }
    return convertedList;
  }

  /**
   * Provides GUI to user so that they can configure parameters for feature importance algorithm.
   */
  public IntOption windowSizeOption = new IntOption("windowSize", 'p', "The size of the windows used.", SlidingWindow.DEFAULT_WINDOW_SIZE);
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
