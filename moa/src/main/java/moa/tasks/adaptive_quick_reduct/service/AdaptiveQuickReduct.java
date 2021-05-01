package moa.tasks.adaptive_quick_reduct.service;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.distance_utils.DistanceCalculatorAbstract;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class AdaptiveQuickReduct {
  private static final Logger logger = LogManager.getLogger(AdaptiveQuickReduct.class);
  private final Instances datasetInfos;
  private final int numClasses;
  private final int numAttributes;
  private final int classIndex;
  private final double similarityThreshold;
  private int iterationNumber;
  private final DistanceCalculatorAbstract distanceCalculator;

  private void updateIterationNumber() {
    this.iterationNumber++;
  }

  public AdaptiveQuickReduct(Instances datasetInfos, DistanceCalculatorAbstract distanceCalculator, double similarityThreshold) {
    this.datasetInfos = datasetInfos;
    this.numClasses = datasetInfos.numClasses();
    this.numAttributes = datasetInfos.numAttributes() - 1;
    this.classIndex = datasetInfos.classIndex();
    this.similarityThreshold = similarityThreshold;
    this.distanceCalculator = distanceCalculator;
    this.iterationNumber = 0;
  }

  public Reduct<Integer> getReduct(ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow, Reduct<Integer> previousReduct) {

    this.updateIterationNumber();

    ArrayList<HashSet<Integer>> decisionFeaturesD = getInstancesBelongingToClass(iWindow);

    logger.debug(String.format("Iteration n. %d, Computed Decision Features D", this.iterationNumber));

    double reComputedPreviousReductGamma = getAttributesDegreeOfDependency(previousReduct.getReductSet(), iWindow, decisionFeaturesD);

    logger.debug(String.format("Iteration n. %d, Previous reduct gamma value updated to new window from %f to %f",
            this.iterationNumber, previousReduct.getGammaValue(), reComputedPreviousReductGamma));

    previousReduct.setGammaValue(reComputedPreviousReductGamma);

    Reduct<Integer> previousReductCopy = new Reduct<>(previousReduct);

    Reduct<Integer> reductWithoutUselessAttributes = getReductWithoutUselessAttributes(previousReduct, iWindow, decisionFeaturesD);

    logger.debug(String.format("Iteration n. %d, Obtained Reduct without useless attributes %s",
            iterationNumber, this.getReductFormattedString(reductWithoutUselessAttributes)));

    HashSet<Integer> removedAttributesFromPreviousReduct = getDiffAttributesBetweenReducts(previousReductCopy, reductWithoutUselessAttributes);

    logger.debug(String.format("Iteration n. %d, Attributes removed from Reduct [%s]",
            iterationNumber, getIndexAttributeString(removedAttributesFromPreviousReduct)));

    return getCurrentReduct(reductWithoutUselessAttributes, removedAttributesFromPreviousReduct, decisionFeaturesD, iWindow);
  }

  /**
   * Splits instances of the current window into their corresponding class set
   */
  private ArrayList<HashSet<Integer>> getInstancesBelongingToClass(ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow) {

    ArrayList<HashSet<Integer>> instToClass = new ArrayList<>(this.numClasses);

    for(int i = 0; i < this.numClasses; i++) {
      instToClass.add(new HashSet<>(Math.floorDiv(iWindow.size(), 2)));
    }

    for(AbstractMap.SimpleEntry<Integer, Instance> currentInstance : iWindow) {
      int classIndex = (int) currentInstance.getValue().value(this.classIndex);
      instToClass.get(classIndex).add(currentInstance.getKey());
    }

    return instToClass;
  }

  /**
   * Performs the Adaptive Quick Reduct step that removes attributes from the previous iteration reduct and that
   * become useless in the current iteration.
   */
  private Reduct<Integer> getReductWithoutUselessAttributes(Reduct<Integer> previousReduct,
                                                            ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow,
                                                            ArrayList<HashSet<Integer>> decisionFeaturesD) {
    if(previousReduct.isEmpty() || previousReduct.size() == 1) {
      return previousReduct;
    }

    HashSet<Integer> previousReductAttributes = new HashSet<>(previousReduct.getReductSet());

    for(int attributeIndex : previousReductAttributes) {

      HashSet<Integer> truncatedReduct = new HashSet<>(previousReduct.getReductSet());
      truncatedReduct.remove(attributeIndex);

      double iGamma = getAttributesDegreeOfDependency(truncatedReduct, iWindow, decisionFeaturesD);

      if(iGamma >= previousReduct.getGammaValue()) {
        logger.debug(String.format("Iteration n. %d, Removed attribute %s. New Gamma value = %f",
                this.iterationNumber, this.datasetInfos.attribute(attributeIndex).name(), iGamma));
        previousReduct.removeFromReductAndUpdateGamma(attributeIndex, iGamma);
        return getReductWithoutUselessAttributes(previousReduct, iWindow, decisionFeaturesD);
      }
    }
    return previousReduct;
  }

  /**
   * Given an attributes set and the current window, returns the Degree Dependency value Gamma
   */
  private double getAttributesDegreeOfDependency(HashSet<Integer> attributes,
                                                 ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow,
                                                 ArrayList<HashSet<Integer>> decisionFeaturesD) {
    if(attributes.isEmpty())
      return 0.0;
    HashSet<HashSet<Integer>> informationGranules = computeInformationGranules(attributes, iWindow);
    return computeGammaValue(informationGranules, decisionFeaturesD, iWindow.size());
  }

  /**
   * Return the attributes that have been removed from the previous reduct
   */
  private HashSet<Integer> getDiffAttributesBetweenReducts(Reduct<Integer> previousReduct, Reduct<Integer> updatedPreviousReduct) {
    HashSet<Integer> diff = new HashSet<>();
    previousReduct.getReductSet().forEach(attributeIndex -> {
      if(!updatedPreviousReduct.contains(attributeIndex))
        diff.add(attributeIndex);
    });
    return diff;
  }

  /**
   * Performs the Adaptive Quick Reduct step that try to add new attributes to the reduct
   */
  private Reduct<Integer> getCurrentReduct(Reduct<Integer> previousReduct,
                                           HashSet<Integer> removedAttributes,
                                           ArrayList<HashSet<Integer>> decisionFeaturesD,
                                           ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow) {

    Reduct<Integer> currentReduct = new Reduct<>(previousReduct);

    double currentGamma = currentReduct.getGammaValue();
    double previousGamma = -1.0;

    while(currentGamma == currentReduct.getGammaValue() && previousGamma != currentGamma && !currentReduct.hasMaxValue()) {

      if(currentGamma > 0.0)
        previousGamma = currentGamma;

      HashMap<Integer, HashSet<HashSet<Integer>>> informationGranules = getInformationGranulesAddStep(
              currentReduct.getReductSet(), removedAttributes, iWindow
      );

      Map<Integer, Double> attributesGamma = informationGranules.entrySet().parallelStream().map(entrySet -> new AbstractMap.SimpleEntry<>(
                      entrySet.getKey(), computeGammaValue(entrySet.getValue(), decisionFeaturesD, iWindow.size())
              )
      ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      Map.Entry<Integer, Double> maxEntry = getMaxEntry(attributesGamma);

      currentGamma = maxEntry.getValue();
      logger.debug(String.format("Iteration n. %d, previous Gamma found = %f -  max Gamma found = %f - currentGamma%f",
              this.iterationNumber,previousGamma, currentGamma, currentReduct.getGammaValue()));
      if(currentGamma > currentReduct.getGammaValue() || (currentGamma == 0.0 && currentReduct.getGammaValue() == 0.0)) {
        logger.debug(String.format("Iteration n. %d, Added attribute %s. New Gamma value = %f",
                this.iterationNumber, this.datasetInfos.attribute(maxEntry.getKey()).name(), currentGamma));
        currentReduct.addToReductAndUpdateGamma(maxEntry.getKey(), currentGamma);
      }
    }

    return currentReduct;
  }

  private Map.Entry<Integer, Double> getMaxEntry(Map<Integer, Double> attributesGamma) {
    Map.Entry<Integer, Double> maxEntry = new AbstractMap.SimpleEntry<>(-1, -1.0);

    for(Map.Entry<Integer, Double> entry : attributesGamma.entrySet()) {
      if(entry.getValue() > maxEntry.getValue()) {
        maxEntry = new AbstractMap.SimpleEntry<>(entry);
      }
    }

    if(maxEntry.getValue() == 0.0) {
      Integer[] keySet = attributesGamma.keySet().toArray(new Integer[0]);
      int randomIndex = (int) (Math.random() * keySet.length);
      assert randomIndex < keySet.length : "Random index is greater than key set size";
      int randomKey = keySet[randomIndex];
      return new AbstractMap.SimpleEntry<>(randomKey, attributesGamma.get(randomKey));
    }

    return maxEntry;
  }


  /**
   * Given a set of information granules referred to a subset of attributes, returns the Degree Dependency value Gamma
   */
  private double computeGammaValue(HashSet<HashSet<Integer>> attributeGranules,
                                   ArrayList<HashSet<Integer>> decisionFeaturesD,
                                   int universeSizeU) {

    int lowerApproximationSize = attributeGranules.stream()
            .filter(granule -> decisionFeaturesD.stream().anyMatch(classFeaturesSet -> classFeaturesSet.containsAll(granule)))
            .map(filteredGranule -> filteredGranule.size())
            .reduce(0, (subtotal, element) -> subtotal + element);

    return (double) lowerApproximationSize / (double) universeSizeU;
  }

  /**
   * Index of attribute not belonging to the reduct -> Sets of sets of instances with same value i.e.:
   * Attributes into Reduct = {0, 2, 4}, Attributes not belonging to Reduct = {1, 3}.
   * (1) -> [..., [x0, x2, x10], [x100, x20, x99], ...] (all inner set of instances have same value for attributes {0,2,4,1}).
   * (3) -> [..., [x1], [x3, x9], [x70, x71, x72, x75, x76], ...] (all inner set of instances have same value for attributes {0,2,4,3}).
   */
  private HashMap<Integer, HashSet<HashSet<Integer>>> getInformationGranulesAddStep(HashSet<Integer> reductElements,
                                                                                    HashSet<Integer> removedAttribute,
                                                                                    ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow) {
    HashMap<Integer, HashSet<HashSet<Integer>>> informationGranules = new HashMap<>();

    HashSet<Integer> attributesTryToAdd = new HashSet<>();

    for(int i = 0; i < this.numAttributes; i++) {
      if(!reductElements.contains(i) && !removedAttribute.contains(i))
        attributesTryToAdd.add(i);
    }

    attributesTryToAdd.parallelStream().forEach(attributeIndex -> {
      HashSet<AbstractMap.SimpleEntry<Integer, Instance>> instancesSet = new HashSet<>(iWindow);
      assert instancesSet.size() == iWindow.size() : "getInformationGranules: Instance Set and window have not the same size!";
      HashSet<Integer> attributeSets = new HashSet<>(reductElements);
      attributeSets.add(attributeIndex);

      HashSet<HashSet<Integer>> attributeSetGranules = computeInformationGranules(attributeSets, iWindow);

      informationGranules.put(attributeIndex, new HashSet<>(attributeSetGranules));
    });

    return informationGranules;
  }

  /**
   * Given a subset of attributes, computes the information granules
   */
  private HashSet<HashSet<Integer>> computeInformationGranules(HashSet<Integer> attributeSet,
                                                               ArrayList<AbstractMap.SimpleEntry<Integer, Instance>> iWindow) {

    HashSet<HashSet<Integer>> granules = new HashSet<>();
    HashSet<AbstractMap.SimpleEntry<Integer, Instance>> instancesSet = new HashSet<>(iWindow);

    HashSet<Integer> numericAttributeSet = new HashSet<>();
    HashSet<Integer> nominalAttributeSet = new HashSet<>();

    for(int attributeIndex : attributeSet) {
      if(datasetInfos.attribute(attributeIndex).isNominal())
        nominalAttributeSet.add(attributeIndex);
      else if(datasetInfos.attribute(attributeIndex).isNumeric())
        numericAttributeSet.add(attributeIndex);
      else
        logger.warn(String.format("Attribute %s is neither Nominal nor Numeric!", datasetInfos.attribute(attributeIndex).name()));
    }

    do {
      AbstractMap.SimpleEntry<Integer, Instance> currentInstance = instancesSet.iterator().next();

      Set<AbstractMap.SimpleEntry<Integer, Instance>> sameValueInstances = instancesSet.stream().filter(iInstance ->
              this.haveSameNominalAttributeValue(currentInstance.getValue(), iInstance.getValue(), nominalAttributeSet) &&
                      this.distanceCalculator.computeDistance(currentInstance.getValue(), iInstance.getValue(), numericAttributeSet) <= this.similarityThreshold
      ).collect(Collectors.toSet());

      Set<Integer> sameValueInstancesIndex = sameValueInstances.stream().sequential().map(instance -> instance.getKey()).collect(Collectors.toSet());

      granules.add(new HashSet<>(sameValueInstancesIndex));

      sameValueInstances.forEach(instance -> instancesSet.remove(instance));
    }
    while(!instancesSet.isEmpty());

    return granules;
  }

  public boolean haveSameNominalAttributeValue(Instance currentInstance, Instance iInstance, HashSet<Integer> attributeSet) {
    return attributeSet.stream().allMatch(attributeIndex ->
            currentInstance.value(attributeIndex) == iInstance.value(attributeIndex)
    );
  }

  public String getReductFormattedString(Reduct<Integer> reduct) {
    String reductElements = getIndexAttributeString(reduct.getReductSet());

    return String.format("Reduct{reductSet=[%s], gammaValue=%f}", reductElements, reduct.getGammaValue());
  }

  public String getIndexAttributeString(HashSet<Integer> attributeIndexSet) {
    return attributeIndexSet.stream()
            .map(attributeIndex -> String.format("%s", this.datasetInfos.attribute(attributeIndex).name()))
            .reduce("", (prev, succ) -> String.format("%s %s ", prev, succ));
  }
}
