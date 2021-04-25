package moa.tasks.adaptive_quick_reduct.model;

import java.util.AbstractMap;
import java.util.ArrayList;

public class SlidingWindow<instanceType> extends Window<instanceType> {
  public static final int DEFAULT_WINDOW_SIZE = 500;
  public static final int DEFAULT_OVERLAP = 250;

  private final int windowSize;
  private final int overlap;
  private int iStart;
  private int iEnd;
  private boolean areInstancesFinished = false;
  private int totalIterationNumber;

  public SlidingWindow(int numInstances) {
    this(numInstances, DEFAULT_WINDOW_SIZE, DEFAULT_OVERLAP);
  }

  public SlidingWindow(int numInstances, int windowSize, int overlap) {
    assert windowSize > overlap : "Overlap value is greater than Window Size!";
    this.windowSize = windowSize;
    this.overlap = overlap;
    iStart = 0;
    iEnd = windowSize;

    this.setTotalIterationNumber(numInstances);
  }

  private void setTotalIterationNumber(int numInstances) {
    this.totalIterationNumber = 2 + Math.floorDiv(
            (numInstances - windowSize),
            (this.windowSize - this.overlap)
    );
  }

  @Override
  public int getTotalIterationNumber() {
    return this.totalIterationNumber;
  }

  @Override
  public ArrayList<AbstractMap.SimpleEntry<Integer, instanceType>> getNextWindow(ArrayList<instanceType> allInstances) {
    ArrayList<AbstractMap.SimpleEntry<Integer, instanceType>> instancesToReturn = new ArrayList<>(windowSize);

    if(!areInstancesFinished) {
      int rightLimit;

      if(iEnd < allInstances.size()) {
        rightLimit = iEnd;
      }
      else {
        rightLimit = allInstances.size();
        this.areInstancesFinished = true;
      }

      for(int i = iStart; i < rightLimit; i++) {
        instancesToReturn.add(new AbstractMap.SimpleEntry<>(i, allInstances.get(i)));
      }

      iStart = rightLimit - overlap;
      iEnd += (windowSize - overlap);
    }

    return instancesToReturn;
  }
}
