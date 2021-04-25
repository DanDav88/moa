package moa.tasks.adaptive_quick_reduct.model.distance_utils;

import com.yahoo.labs.samoa.instances.Instance;

import java.util.Set;

public class ManhattanDistanceCalculator extends DistanceCalculatorAbstract {
  private static final String CLASS_NAME = "ManhattanDistance";
  private static final String DESCRIPTION = "Manhattan Distance Between Instances";

  @Override
  public double computeDistance(Instance first, Instance second, Set<Integer> attributeIndexes) {
    if(attributeIndexes.isEmpty())
      return NO_DISTANCE_VALUE;

    double dist = 0.0;

    for(int attributeIndex : attributeIndexes) {
      dist += Math.abs((first.value(attributeIndex) - second.value(attributeIndex)));
    }
    return dist;
  }

  public static String getClassName() {
    return CLASS_NAME;
  }

  public static String getDESCRIPTION() {
    return DESCRIPTION;
  }
}
