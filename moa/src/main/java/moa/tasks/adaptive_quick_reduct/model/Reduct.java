package moa.tasks.adaptive_quick_reduct.model;

import java.util.HashSet;

public class Reduct<reductType> {

  private final HashSet<reductType> reductSet;

  private static final double MAX_REDUCT_VALUE = 1.0;

  private double gammaValue;

  public Reduct() {
    this.reductSet = new HashSet<>();
  }

  public Reduct(Reduct<reductType> previous) {
    this.reductSet = new HashSet<>(previous.getReductSet());
    this.setGammaValue(previous.getGammaValue());
  }

  public HashSet<reductType> getReductSet() {
    return reductSet;
  }

  public double getGammaValue() {
    return gammaValue;
  }

  public void setGammaValue(double gammaValue) {
    this.gammaValue = gammaValue;
  }

  public void addToReductAndUpdateGamma(reductType element, double newGamma) {
    this.reductSet.add(element);
    this.setGammaValue(newGamma);
  }

  public void removeFromReductAndUpdateGamma(reductType element, double newGamma) {
    this.reductSet.remove(element);
    this.setGammaValue(newGamma);
  }

  public boolean contains(reductType element) {
    return this.reductSet.contains(element);
  }

  public int size() {
    return this.reductSet.size();
  }

  public boolean isEmpty() {
    return this.reductSet.isEmpty();
  }

  public boolean isEqual(Reduct<reductType> otherReduct) {
    return this.size() == otherReduct.size() &&
            this.reductSet.containsAll(otherReduct.getReductSet());
  }

  public boolean hasMaxValue(){
    return this.getGammaValue() == MAX_REDUCT_VALUE;
  }

}
