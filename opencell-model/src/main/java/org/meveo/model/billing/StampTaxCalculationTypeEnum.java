package org.meveo.model.billing;

public enum StampTaxCalculationTypeEnum {

	INFO(1, "StampTaxCalculationType.INFO"),
	EXECUTE(2, "StampTaxCalculationType.EXECUTE");
	
    private Integer id;
    private String label;

    StampTaxCalculationTypeEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return this.label;
    }

    public static StampTaxCalculationTypeEnum getValue(Integer id) {
        if (id != null) {
            for (StampTaxCalculationTypeEnum type : values()) {
                if (id.equals(type.getId())) {
                    return type;
                }
            }
        }
        return null;
    }
	
}
