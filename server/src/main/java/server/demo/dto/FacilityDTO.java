package server.demo.dto;

/**
 * Su 设施节点。
 */
public class FacilityDTO {

    private String group;
    private String name;

    public FacilityDTO() {
    }

    public FacilityDTO(String group, String name) {
        this.group = group;
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
