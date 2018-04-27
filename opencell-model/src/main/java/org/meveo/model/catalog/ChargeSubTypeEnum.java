package org.meveo.model.catalog;

public enum ChargeSubTypeEnum {

	CHARGE(1, "ChargeSubTypeEnum.CHARGE"),
	DISCOUNT(2, "ChargeSubTypeEnum.DISCOUNT"),
	OCC(3, "ChargeSubTypeEnum.OCC"),
	INSTALLMENT(4, "ChargeSubTypeEnum.INSTALLMENT");
	
    private Integer id;
    private String label;

    ChargeSubTypeEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return this.label;
    }

    public static ChargeSubTypeEnum getValue(Integer id) {
        if (id != null) {
            for (ChargeSubTypeEnum type : values()) {
                if (id.equals(type.getId())) {
                    return type;
                }
            }
        }
        return null;
    }
}
