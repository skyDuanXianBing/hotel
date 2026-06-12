package server.demo.dto.home;

public class HomeWorkbenchActionDTO {
    private String code;
    private String label;
    private String type;

    public HomeWorkbenchActionDTO() {
    }

    public HomeWorkbenchActionDTO(String code, String label, String type) {
        this.code = code;
        this.label = label;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
