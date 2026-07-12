package server.demo.dto.internaltask;

public class InternalTaskAssigneeDTO {
    private final Long userId; private final String displayName; private final String avatar;
    private final String baseRole; private final String employeeType; private final Long cleanerId;
    public InternalTaskAssigneeDTO(Long userId, String displayName, String avatar, String baseRole,
                                   String employeeType, Long cleanerId) {
        this.userId = userId; this.displayName = displayName; this.avatar = avatar; this.baseRole = baseRole;
        this.employeeType = employeeType; this.cleanerId = cleanerId;
    }
    public Long getUserId() { return userId; } public String getDisplayName() { return displayName; }
    public String getAvatar() { return avatar; } public String getBaseRole() { return baseRole; }
    public String getEmployeeType() { return employeeType; } public Long getCleanerId() { return cleanerId; }
}
