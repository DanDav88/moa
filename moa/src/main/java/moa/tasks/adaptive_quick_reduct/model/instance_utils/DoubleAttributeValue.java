package moa.tasks.adaptive_quick_reduct.model.instance_utils;

public class DoubleAttributeValue extends GenericAttributeValue<Double> {
  public DoubleAttributeValue(int attributeIndex, String attributeLabel, Double attributeValue) {
    this.isNumeric = true;
    this.isNominal = false;
    this.attributeValue = attributeValue;
    this.attributeIndex = attributeIndex;
    this.attributeLabel = attributeLabel;
  }

  public boolean equals(GenericAttributeValue<Double> otherAttribute){
    return super.equals(otherAttribute) &&
            this.attributeValue.equals(otherAttribute.attributeValue);
  }

}
