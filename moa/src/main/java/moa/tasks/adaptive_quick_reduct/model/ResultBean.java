package moa.tasks.adaptive_quick_reduct.model;

public class ResultBean {
  private double accuracyValue;
  private int windowSize;

  public ResultBean(double accuracyValue, int windowSize) {
    this.accuracyValue = accuracyValue;
    this.windowSize = windowSize;
  }

  public void setAccuracyValue(double accuracyValue) {
    this.accuracyValue = accuracyValue;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }

  public double getAccuracyValue() {
    return accuracyValue;
  }

  public int getWindowSize() {
    return windowSize;
  }

}
