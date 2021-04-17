package moa.tasks.adaptive_quick_reduct.model;

import java.util.ArrayList;

public abstract class Window<instanceType> {
  protected ArrayList<instanceType> allInstances;

  public abstract ArrayList<instanceType> getNextWindow();

  protected Window(ArrayList<instanceType> instances){
    this.allInstances = instances;
  }

  public abstract int getTotalIterationNumber();

}
