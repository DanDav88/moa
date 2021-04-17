package moa.tasks.adaptive_quick_reduct.model.instance_utils;

import com.yahoo.labs.samoa.instances.Instance;

import java.util.ArrayList;

public class LightInstance {
  private final int classIndex;
  private final String classLabel;
  private int instanceIndex;
  private final ArrayList<GenericAttributeValue> attributes;

  public LightInstance(ArrayList<GenericAttributeValue> attributes, int classIndex, String classLabel, int instanceIndex) {
    this.attributes = new ArrayList<>(attributes);
    this.classIndex = classIndex;
    this.classLabel = classLabel;
    this.instanceIndex = instanceIndex;
  }

  public LightInstance(Instance instance, int instanceIndex) {
    this.instanceIndex = instanceIndex;
    this.classIndex = (int) instance.value(instance.classIndex());
    this.classLabel = instance.classAttribute().getAttributeValues().get(this.classIndex);
    this.attributes = new ArrayList<>(instance.numAttributes() - 1);

    for(int i = 0; i< instance.numAttributes() - 1; i++){
      if (instance.attribute(i).isNumeric()){
        attributes.add(new DoubleAttributeValue(i, instance.attribute(i).name(),instance.value(i)));
      }
      else if(instance.attribute(i).isNominal()){
         int stringKeyValue = (int)instance.value(i);
         attributes.add(new StringAttributeValue(i, instance.attribute(i).name(), instance.attribute(i).getAttributeValues().get(stringKeyValue)));
      }
    }
  }

  public void setInstanceIndex(int instanceIndex) {
    this.instanceIndex = instanceIndex;
  }

  public int getInstanceIndex() {
    return instanceIndex;
  }

  public int getClassIndex() {
    return classIndex;
  }

  public String getClassLabel() {
    return classLabel;
  }

  public ArrayList<GenericAttributeValue> getAttributes() {
    return this.attributes;
  }
}
