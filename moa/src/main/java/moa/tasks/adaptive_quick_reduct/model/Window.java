package moa.tasks.adaptive_quick_reduct.model;

import java.util.AbstractMap;
import java.util.ArrayList;

public abstract class Window<instanceType> {
  public abstract ArrayList<AbstractMap.SimpleEntry<Integer, instanceType>> getNextWindow(ArrayList<instanceType> allInstances);
  public abstract int getTotalIterationNumber();
}
