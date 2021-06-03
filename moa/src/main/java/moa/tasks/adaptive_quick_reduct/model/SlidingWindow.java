package moa.tasks.adaptive_quick_reduct.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;

public class SlidingWindow<instanceType> extends Window<instanceType> {
  private static final Logger logger = LogManager.getLogger(SlidingWindow.class);
  public static final int DEFAULT_WINDOW_SIZE = 500;
  public static final int DEFAULT_OVERLAP = 250;

  protected int windowSize;
  protected int overlap;
  protected int iStart;
  protected int iEnd;
  protected int currentStart;
  protected int currentEnd;
  protected boolean areInstancesFinished = false;
  protected int totalIterationNumber;

  public SlidingWindow(){};

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

  protected void setTotalIterationNumber(int numInstances) {
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

      currentStart = iStart;
      currentEnd = rightLimit;

      for(int i = iStart; i < rightLimit; i++) {
        instancesToReturn.add(new AbstractMap.SimpleEntry<>(i, allInstances.get(i)));
      }

      iStart = rightLimit - overlap;
      iEnd += (windowSize - overlap);

      logger.debug(String.format("iStart = %d, iEnd = %d, CurrentStart = %d, CurrentEnd = %d",
              iStart, iEnd, currentStart,currentEnd)
      );
    }

    return instancesToReturn;
  }

  @Override
  public ArrayList<AbstractMap.SimpleEntry<Integer, instanceType>> getCurrentWindow(ArrayList<instanceType> allInstances) {
    ArrayList<AbstractMap.SimpleEntry<Integer, instanceType>> instancesToReturn = new ArrayList<>(windowSize);
    for(int i = currentStart; i < currentEnd; i++) {
      instancesToReturn.add(new AbstractMap.SimpleEntry<>(i, allInstances.get(i)));
    }
    return instancesToReturn;
  }

  @Override
  public String toString() {
    return String.format("SlidingWindow_ws_%d_wo_%d",this.windowSize,this.overlap);
  }

  @Override
  public int getWindowSize() {
    return windowSize;
  }
}
