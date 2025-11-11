package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 排序配置DTO
 */
public class SortConfigDTO {

    @NotBlank(message = "排序类型不能为空")
    private String sortType;

    @NotEmpty(message = "实体ID列表不能为空")
    private List<Long> entityIds;

    // Constructors
    public SortConfigDTO() {}

    public SortConfigDTO(String sortType, List<Long> entityIds) {
        this.sortType = sortType;
        this.entityIds = entityIds;
    }

    // Getters and Setters
    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public List<Long> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(List<Long> entityIds) {
        this.entityIds = entityIds;
    }
}
