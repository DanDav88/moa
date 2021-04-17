package moa.tasks.adaptive_quick_reduct.model;

import java.util.HashSet;

public class Reduct<reductType> {

  private final HashSet<reductType> reductSet;

  private double gammaValue;

  public Reduct() {
    this.reductSet = new HashSet<reductType>();
  }

  public Reduct(Reduct<reductType> previous){
    this.reductSet = new HashSet<reductType>(previous.getReductSet());
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

  public void addToReductAndUpdateGamma(reductType element, double newGamma){
    this.reductSet.add(element);
    this.setGammaValue(newGamma);
  }

  public void removeFromReductAndUpdateGamma(reductType element, double newGamma){
    this.reductSet.remove(element);
    this.setGammaValue(newGamma);
  }

  public boolean contains(reductType element){
    return this.reductSet.contains(element);
  }

  public boolean isEmpty(){
    return this.reductSet.isEmpty();
  }

}
