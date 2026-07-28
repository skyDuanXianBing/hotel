package server.demo.dto;

import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;

import java.util.List;

/**
 * 分组连同成员一次性返回，避免客户端按分组逐个请求成员造成 N+1。
 */
public class RoomGroupWithMembersDTO {

    private Long id;
    private String name;
    private String description;
    private List<RoomGroupMember> members;

    public RoomGroupWithMembersDTO() {
    }

    public RoomGroupWithMembersDTO(RoomGroup group, List<RoomGroupMember> members) {
        this.id = group.getId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.members = members;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RoomGroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<RoomGroupMember> members) {
        this.members = members;
    }
}
