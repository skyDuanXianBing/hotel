package server.demo.dto.home;

public class HomeWorkbenchQueryDTO {
    private String type;
    private String status;
    private int size;
    private String sort;

    public HomeWorkbenchQueryDTO() {
    }

    public HomeWorkbenchQueryDTO(String type, String status, int size, String sort) {
        this.type = type;
        this.status = status;
        this.size = size;
        this.sort = sort;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
