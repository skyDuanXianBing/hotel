package server.demo.dto;

import java.util.List;

public class OperationLogDTO {
    private Long id;
    private String action;
    private String operator;
    private String timestamp;
    private String type; // order | billing
    private String content;
    private List<OperationLogDetailDTO> details;

    public OperationLogDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<OperationLogDetailDTO> getDetails() {
        return details;
    }

    public void setDetails(List<OperationLogDetailDTO> details) {
        this.details = details;
    }
}

