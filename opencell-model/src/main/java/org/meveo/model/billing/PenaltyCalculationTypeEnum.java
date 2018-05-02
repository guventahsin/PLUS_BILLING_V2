package org.meveo.model.billing;

public enum PenaltyCalculationTypeEnum {

	INFO(1, "PenaltyCalculationType.INFO"),
	EXECUTE(2, "PenaltyCalculationType.EXECUTE");
	
    private Integer id;
    private String label;

    PenaltyCalculationTypeEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return this.label;
    }

    public static PenaltyCalculationTypeEnum getValue(Integer id) {
        if (id != null) {
            for (PenaltyCalculationTypeEnum type : values()) {
                if (id.equals(type.getId())) {
                    return type;
                }
            }
        }
        return null;
    }
	
}
