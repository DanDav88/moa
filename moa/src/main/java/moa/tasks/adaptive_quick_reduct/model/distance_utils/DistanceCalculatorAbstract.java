package moa.tasks.adaptive_quick_reduct.model.distance_utils;

import com.yahoo.labs.samoa.instances.Instance;

import java.util.Set;

public abstract class DistanceCalculatorAbstract {
  public static double NO_DISTANCE_VALUE = -1.0;
  public abstract double computeDistance(Instance first, Instance second, Set<Integer> attributeIndexes);
}
