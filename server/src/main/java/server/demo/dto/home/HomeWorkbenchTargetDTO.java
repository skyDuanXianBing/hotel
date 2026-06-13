package server.demo.dto.home;

import java.util.LinkedHashMap;
import java.util.Map;

public class HomeWorkbenchTargetDTO {
    private String type;
    private String path;
    private Map<String, String> query = new LinkedHashMap<>();

    public HomeWorkbenchTargetDTO() {
    }

    public HomeWorkbenchTargetDTO(String type, String path, Map<String, String> query) {
        this.type = type;
        this.path = path;
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }
}
