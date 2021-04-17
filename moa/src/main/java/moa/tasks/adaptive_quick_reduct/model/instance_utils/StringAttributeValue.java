package moa.tasks.adaptive_quick_reduct.model.instance_utils;

public class StringAttributeValue extends GenericAttributeValue<String> {
  public StringAttributeValue(int attributeIndex, String attributeLabel, String attributeValue) {
    this.isNumeric = false;
    this.isNominal = true;
    this.attributeValue = attributeValue;
    this.attributeIndex = attributeIndex;
    this.attributeLabel = attributeLabel;
  }

  public boolean equals(GenericAttributeValue<String> otherAttribute){
    return super.equals(otherAttribute) &&
            this.attributeValue.equals(otherAttribute.attributeValue);
  }

}