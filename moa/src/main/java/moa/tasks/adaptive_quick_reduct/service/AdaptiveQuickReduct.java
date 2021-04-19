package moa.tasks.adaptive_quick_reduct.service;

import moa.tasks.adaptive_quick_reduct.model.Reduct;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.DatasetInfos;
import moa.tasks.adaptive_quick_reduct.model.instance_utils.LightInstance;

import java.util.*;
import java.util.stream.Collectors;

public class AdaptiveQuickReduct {

  DatasetInfos datasetInfos;

  public AdaptiveQuickReduct(DatasetInfos datasetInfos) {
    this.datasetInfos = datasetInfos;
  }

  public Reduct<Integer> getReduct(ArrayList<LightInstance> iWindow, Reduct<Integer> previousReduct) {

    ArrayList<HashSet<Integer>> decisionFeaturesD = getInstancesBelongingToClass(iWindow);

    Reduct<Integer> reductWithoutUselessAttributes = getReductWithoutUselessAttributes(previousReduct, iWindow, decisionFeaturesD);

    HashSet<Integer> removedAttributesFromPreviousReduct = getDiffAttributesBetweenReducts(previousReduct, reductWithoutUselessAttributes);

    return getCurrentReduct(reductWithoutUselessAttributes, removedAttributesFromPreviousReduct, decisionFeaturesD, iWindow);
  }

  /**
   * Splits instances of the current window into their corresponding class set
   */
  private ArrayList<HashSet<Integer>> getInstancesBelongingToClass(ArrayList<LightInstance> iWindow) {

    ArrayList<HashSet<Integer>> instToClass = new ArrayList<>(datasetInfos.getNumClasses());

    for(int i = 0; i < datasetInfos.getNumClasses(); i++) {
      instToClass.add(new HashSet<>(Math.floorDiv(datasetInfos.getNumInstances(), 2)));
    }

    iWindow.forEach(instance ->
            instToClass.get(instance.getClassIndex()).add(instance.getInstanceIndex())
    );

    return instToClass;
  }

  /**
   * Performs the Adaptive Quick Reduct step that removes attributes from the previous iteration reduct and that
   * become useless in the current iteration.
   */
  private Reduct<Integer> getReductWithoutUselessAttributes(Reduct<Integer> previousReduct,
                                                            ArrayList<LightInstance> iWindow,
                                                            ArrayList<HashSet<Integer>> decisionFeaturesD) {
    //TODO gestire il caso che il primo attributo ha valore massimo e
    if(previousReduct.isEmpty() || previousReduct.size() == 1) {
      return previousReduct;
    }

    for(int attributeIndex : previousReduct.getReductSet()) {

      HashSet<Integer> truncatedReduct = previousReduct.getReductSet();
      truncatedReduct.remove(attributeIndex);

      double iGamma = getAttributesDegreeOfDependency(truncatedReduct, iWindow, decisionFeaturesD);

      if(iGamma >= previousReduct.getGammaValue()) {
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
                                                 ArrayList<LightInstance> iWindow,
                                                 ArrayList<HashSet<Integer>> decisionFeaturesD) {
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
                                           ArrayList<LightInstance> iWindow) {

    Reduct<Integer> currentReduct = new Reduct<>(previousReduct);

    double currentGamma;

    do {
      HashMap<Integer, HashSet<HashSet<Integer>>> informationGranules = getInformationGranulesAddStep(
              currentReduct.getReductSet(), removedAttributes, iWindow
      );

      Map<Integer, Double> attributesGamma = informationGranules.entrySet().parallelStream().map(entrySet -> new AbstractMap.SimpleEntry<>(
                      entrySet.getKey(), computeGammaValue(entrySet.getValue(), decisionFeaturesD, iWindow.size())
              )
      ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      Map.Entry<Integer, Double> maxEntry = null;

      for(Map.Entry<Integer, Double> entry : attributesGamma.entrySet()) {
        if(maxEntry == null || entry.getValue() > maxEntry.getValue()) {
          maxEntry = entry;
        }
      }
      assert maxEntry != null;
      currentGamma = maxEntry.getValue();
      if(currentGamma > currentReduct.getGammaValue()) {
        currentReduct.addToReductAndUpdateGamma(maxEntry.getKey(), currentGamma);
      }
    }
    while(currentGamma != currentReduct.getGammaValue());

    return currentReduct;
  }

  /**
   * Given a set of information granules referred to a subset of attributes, returns the Degree Dependency value Gamma
   */
  private double computeGammaValue(HashSet<HashSet<Integer>> attributeGranules,
                                   ArrayList<HashSet<Integer>> decisionFeaturesD,
                                   int universeSizeU) {

    int lowerApproximationSize = attributeGranules.parallelStream()
            .filter(granule -> decisionFeaturesD.parallelStream().anyMatch(classFeaturesSet -> classFeaturesSet.containsAll(granule)))
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
                                                                                    ArrayList<LightInstance> iWindow) {
    HashMap<Integer, HashSet<HashSet<Integer>>> informationGranules = new HashMap<>();

    HashSet<Integer> attributesTryToAdd = new HashSet<>();

    datasetInfos.getLabelToIndexAttributeDictionary().forEach((label, index) -> {
      if(!reductElements.contains(index) && !removedAttribute.contains(index)) {
        attributesTryToAdd.add(index);
      }
    });

    attributesTryToAdd.parallelStream().forEach(attributeIndex -> {
      HashSet<LightInstance> instancesSet = new HashSet<>(iWindow);
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
  private HashSet<HashSet<Integer>> computeInformationGranules(HashSet<Integer> attributeSet, ArrayList<LightInstance> iWindow) {

    HashSet<HashSet<Integer>> granules = new HashSet<>();
    HashSet<LightInstance> instancesSet = new HashSet<>(iWindow);

    do {
      LightInstance iInstance = instancesSet.iterator().next();

      Set<LightInstance> sameValueInstances = instancesSet.parallelStream().filter(instance ->
              instance.hasSameAttributesValue(iInstance, attributeSet)
      ).collect(Collectors.toSet());

      Set<Integer> sameValueInstancesIndex = sameValueInstances.parallelStream().map(LightInstance::getInstanceIndex).collect(Collectors.toSet());

      granules.add(new HashSet<>(sameValueInstancesIndex));

      sameValueInstances.forEach(instance -> instancesSet.remove(instance));
    }
    while(!instancesSet.isEmpty());

    return granules;
  }
}
