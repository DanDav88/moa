package moa.tasks.adaptive_quick_reduct.model.instance_utils;

import com.yahoo.labs.samoa.instances.Instances;

import java.util.ArrayList;
import java.util.HashMap;

public class DatasetInfos {
  private final int DEFAULT_MISSED_INDEX = -1;

  private final int numClasses;
  private final int numAttributes;
  private final int numInstances;
  private final HashMap<String, Integer> labelToIndexAttributeDictionary;
  private final ArrayList<String> indexToLabelAttributeDictionary;
  private final HashMap<String, Integer> labelToIndexClassDictionary;
  private final ArrayList<String> indexToLabelClassDictionary;
  private final String datasetName;

  public DatasetInfos(String datasetName,
                      int numInstances,
                      int numClasses,
                      int numAttributes,
                      HashMap<String, Integer> labelToIndexAttributeDictionary,
                      ArrayList<String> indexToLabelAttributeDictionary,
                      HashMap<String, Integer> labelToIndexClassDictionary,
                      ArrayList<String> indexToLabelClassDictionary) {
    this.datasetName = datasetName;
    this.numInstances = numInstances;
    this.numClasses = numClasses;
    this.numAttributes = numAttributes;
    this.labelToIndexAttributeDictionary = new HashMap<>(labelToIndexAttributeDictionary);
    this.indexToLabelAttributeDictionary = new ArrayList<>(indexToLabelAttributeDictionary);
    this.labelToIndexClassDictionary = new HashMap<>(labelToIndexClassDictionary);
    this.indexToLabelClassDictionary = new ArrayList<>(indexToLabelClassDictionary);
  }

  public DatasetInfos(Instances instances) {
    this.datasetName = instances.getRelationName();
    this.numInstances = instances.numInstances();
    this.numClasses = instances.numClasses();
    this.numAttributes = instances.numAttributes() - 1;
    this.indexToLabelAttributeDictionary = new ArrayList<>(this.numAttributes);
    this.indexToLabelClassDictionary = new ArrayList<>(this.numClasses);
    this.labelToIndexAttributeDictionary = new HashMap<>();
    this.labelToIndexClassDictionary = new HashMap<>();

    for (int i = 0; i< this.numAttributes; i++){
      this.labelToIndexAttributeDictionary.put(instances.attribute(i).name(),i);
      this.indexToLabelAttributeDictionary.add(instances.attribute(i).name());
    }

    for (int i=0; i<this.numClasses; i++){
      this.labelToIndexClassDictionary.put(instances.classAttribute().value(i), i);
      this.indexToLabelClassDictionary.add(instances.classAttribute().value(i));
    }
  }

  public int getNumInstances() {
    return numInstances;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public int getNumClasses() {
    return numClasses;
  }

  public int getNumAttributes() {
    return numAttributes;
  }

  public HashMap<String, Integer> getLabelToIndexAttributeDictionary() {
    return labelToIndexAttributeDictionary;
  }

  public ArrayList<String> getIndexToLabelAttributeDictionary() {
    return indexToLabelAttributeDictionary;
  }

  public HashMap<String, Integer> getLabelToIndexClassDictionary() {
    return labelToIndexClassDictionary;
  }

  public ArrayList<String> getIndexToLabelClassDictionary() {
    return indexToLabelClassDictionary;
  }

  public int getClassIndexByLabel(String classLabel){
    return this.labelToIndexClassDictionary.getOrDefault(classLabel, DEFAULT_MISSED_INDEX);
  }

  public int getAttributeIndexByLabel(String attributeLabel){
    return this.labelToIndexAttributeDictionary.getOrDefault(attributeLabel, DEFAULT_MISSED_INDEX);
  }

  public String getClassLabelByIndex(int indexClass){
    return this.indexToLabelClassDictionary.get(indexClass);
  }

  public String getAttributeLabelByIndex(int indexAttribute){
    return this.indexToLabelAttributeDictionary.get(indexAttribute);
  }
}
