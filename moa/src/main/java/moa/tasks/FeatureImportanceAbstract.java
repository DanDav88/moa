package moa.tasks;

import com.yahoo.labs.samoa.instances.Instances;
import moa.capabilities.CapabilitiesHandler;

import javax.swing.*;

public abstract class FeatureImportanceAbstract extends ClassificationMainTask implements CapabilitiesHandler {
  private static final long serialVersionUID = 1L;
  /**
   * This holds the current set of instances
   */
  protected Instances m_instances;

  /**
   * Scores produced by feature importance algorithm.
   */
  protected double[][] scores;

  /**
   * When scores of feature importance are NaNs, NaNs will be replaced by NaNSubstitute
   * shown in feature importance line graph.
   */
  protected double m_NaNSubstitute = 0.0;//default value is 0.0

  /**
   * The default windowSize parameter for feature importance algorithm.
   */
  protected int m_windowSize = 500;

  /**
   * The default doNotNormalizeFeatureScore parameter for feature importance algorithm.
   */
  protected boolean m_doNotNormalizeFeatureScore = false;

  /**
   * Use progress bar to show the progress of computing scores of feature importance.
   */
  protected JProgressBar progressBar = new JProgressBar();

  public double getNaNSubstitute() {
    return m_NaNSubstitute;
  }

  public JProgressBar getProgressBar() {
    return progressBar;
  }

  public boolean doNotNormalizeFeatureScore() {
    return m_doNotNormalizeFeatureScore;
  }

  public void setDoNotNormalizeFeatureScore(boolean doNotNormalizeFeatureScore) {
    this.m_doNotNormalizeFeatureScore = doNotNormalizeFeatureScore;
  }

  public int getWindowSize() {
    return m_windowSize;
  }

  public void setWindowSize(int windowSize) {
    this.m_windowSize = windowSize;
  }

  public void setNaNSubstitute(double NaNSubstitute) {
    this.m_NaNSubstitute = NaNSubstitute;
  }

  public void setInstances(Instances instances) {
    this.m_instances = instances;
  }
}
