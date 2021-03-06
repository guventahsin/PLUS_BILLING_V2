package org.meveo.api.dto.hierarchy;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.response.SearchResponse;

@XmlRootElement(name = "UserHierarchyLevels")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserHierarchyLevelsDto extends SearchResponse {

    private static final long serialVersionUID = 8948684323709076291L;

    @XmlElementWrapper(name = "userHierarchyLevels")
    @XmlElement(name = "userHierarchyLevel")
    private List<UserHierarchyLevelDto> userHierarchyLevels = new ArrayList<>();

    public List<UserHierarchyLevelDto> getUserHierarchyLevels() {
        return userHierarchyLevels;
    }

    public void setUserHierarchyLevels(List<UserHierarchyLevelDto> userHierarchyLevels) {
        this.userHierarchyLevels = userHierarchyLevels;
    }
}