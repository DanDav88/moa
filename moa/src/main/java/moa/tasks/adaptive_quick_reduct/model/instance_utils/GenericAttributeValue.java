package moa.tasks.adaptive_quick_reduct.model.instance_utils;

public abstract class GenericAttributeValue<attributeType>{
  protected boolean isNumeric;
  protected boolean isNominal;
  protected attributeType attributeValue;
  protected int attributeIndex;
  protected String attributeLabel;

  public attributeType getAttributeValue() {
    return this.attributeValue;
  }

  public boolean isNumeric() {
    return isNumeric;
  }

  public boolean isNominal() {
    return isNominal;
  }

  public int getAttributeIndex() {
    return attributeIndex;
  }

  public String getAttributeLabel() {
    return attributeLabel;
  }

  public boolean equals(GenericAttributeValue<attributeType> otherAttribute) {
    return ((this.isNominal == otherAttribute.isNominal) ||
            (this.isNumeric == otherAttribute.isNumeric())) &&
            this.getAttributeIndex() == otherAttribute.getAttributeIndex();
  }
}
