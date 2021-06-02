package moa.tasks.adaptive_quick_reduct.model;

import com.yahoo.labs.samoa.instances.Instance;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AdaptiveWindow extends SlidingWindow<Instance> {
  private static final Logger logger = LogManager.getLogger(AdaptiveWindow.class);
  private final double overlapPercentage;
  private double currentAverageDispersion;
  private double previousAverageDispersion;
  private final ArrayList<Integer> windowResizeSteps;
  private final int numInstances;
  private int currentIteration;

  public AdaptiveWindow(int numInstances) {
    this(numInstances, DEFAULT_WINDOW_SIZE, 0.5);
  }

  public AdaptiveWindow(int numInstances, int startingWindowSize, double overlapPercentage) {
    super();
    this.windowSize = startingWindowSize;
    this.overlapPercentage = overlapPercentage;
    this.overlap = this.computeCurrentOverlap();
    this.iStart = 0;
    this.iEnd = this.windowSize;
    this.currentStart = this.iStart;
    this.currentEnd = this.iEnd;
    this.totalIterationNumber = 0;
    this.currentAverageDispersion = 0.0;
    this.previousAverageDispersion = -1.0;
    this.windowResizeSteps = Arrays.stream(
            new Integer[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}
    ).collect(Collectors.toCollection(ArrayList::new));
    this.numInstances = numInstances;
    this.currentIteration = 0;
    this.setTotalIterationNumber(this.numInstances);
  }

  private int computeCurrentOverlap() {
    return (int) (this.windowSize * this.overlapPercentage);
  }

  @Override
  public ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> getNextWindow(ArrayList<Instance> allInstances) {
    double EQUALITY_RATIO = 1.0;
    double RATIO_STEP = 0.2;
    this.currentAverageDispersion = getAverageFeaturesDispersion(allInstances);
    double dispersionRatio = this.currentAverageDispersion / this.previousAverageDispersion;

    logger.debug(String.format("Iteration %d - Previous Dispersion = %f, Current Dispersion = %f, Current/Previous Ratio = %f",
            this.currentIteration, this.previousAverageDispersion, this.currentAverageDispersion, dispersionRatio)
    );
    //first iteration or the ratio is less than 20%

    if(this.previousAverageDispersion < 0 ||
            (dispersionRatio > EQUALITY_RATIO - RATIO_STEP && dispersionRatio < EQUALITY_RATIO + RATIO_STEP)
    ) {
      logger.debug(String.format("Iteration %d - Not Updated Window Size = %d, Not Update Window overlap = %d",
              this.currentIteration, this.windowSize, this.overlap));
      this.previousAverageDispersion = this.currentAverageDispersion;
      this.setTotalIterationNumber(allInstances.size());
      return super.getNextWindow(allInstances);
    }

    boolean isToIncrementWindowSize = dispersionRatio > EQUALITY_RATIO;
    double shift = (EQUALITY_RATIO - dispersionRatio) / RATIO_STEP;
    int intShift = Math.abs((int) (shift - shift % 1.0));

    int currentIndexWindowSize = this.windowResizeSteps.indexOf(this.windowSize);

    if(isToIncrementWindowSize) {
      int incrementIndex = Math.min((currentIndexWindowSize + intShift), (this.windowResizeSteps.size() - 1));
      this.windowSize = this.windowResizeSteps.get(incrementIndex);
      this.overlap = this.computeCurrentOverlap();
      logger.debug(String.format("Iteration %d - Increased Window Size to = %d, Updated Window overlap = %d",
              this.currentIteration, this.windowSize, this.overlap));
    }
    else {
      int decrementIndex = Math.max((currentIndexWindowSize - intShift), 0);
      this.windowSize = this.windowResizeSteps.get(decrementIndex);
      this.overlap = this.computeCurrentOverlap();
      logger.debug(String.format("Iteration %d - Decreased Window Size to = %d, Updated Window overlap = %d",
              this.currentIteration, this.windowSize, this.overlap));
    }

    this.previousAverageDispersion = this.currentAverageDispersion;
    this.setTotalIterationNumber(allInstances.size());
    return super.getNextWindow(allInstances);
  }

  private double getAverageFeaturesDispersion(ArrayList<Instance> allInstance) {
    double[][] features = this.getCurrentWindow(allInstance).stream()
            .map(entry -> {
              double[] instanceFeatures = entry.getValue().toDoubleArray();
              return ArrayUtils.subarray(instanceFeatures, 0, (instanceFeatures.length - 1));
            })
            .toArray(double[][]::new);

    int columnNumber = features[0].length;

    double[] columnsStd = new double[columnNumber];

    for(int c = 0; c < columnNumber; c++) {
      double[] iColumn = getColumn(features, c);
      double columnAverage = (Arrays.stream(iColumn).average()).getAsDouble();
      double partialStd = Arrays.stream(iColumn)
              .map(value -> Math.pow(value - columnAverage, 2))
              .reduce(0, (a, b) -> a + b);

      columnsStd[c] = Math.sqrt(partialStd / (double) features.length);
    }

    return Arrays.stream(columnsStd).average().getAsDouble();
  }

  private double[] getColumn(double[][] matrix, int columnIndex) {
    double[] column = new double[matrix.length];
    try {
      for(int i = 0; i < matrix.length; i++) {
        column[i] = matrix[i][columnIndex];
      }
    } catch(IndexOutOfBoundsException | NullPointerException exception) {
      exception.printStackTrace();
      return column;
    }
    return column;
  }

  protected void setTotalIterationNumber(int numInstances) {
    int remainingInstances = numInstances - this.iStart;
    this.totalIterationNumber = this.currentIteration + 2 + Math.floorDiv(
            (remainingInstances - windowSize),
            (this.windowSize - this.overlap)
    );
    currentIteration++;
  }

  @Override
  public int getTotalIterationNumber() {
    return this.totalIterationNumber;
  }

}
