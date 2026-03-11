package es.dggc.backoffice.model.dto;

import java.util.List;

public class GroupListResponse {

    private List<GroupDto> groups;
    private Integer totalItems;

    public GroupListResponse() {
    }

    public GroupListResponse(List<GroupDto> groups, Integer totalItems) {
        this.groups = groups;
        this.totalItems = totalItems;
    }

    public List<GroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupDto> groups) {
        this.groups = groups;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public static class GroupDto {
        private String id;
        private String displayName;
        private Boolean isRoot;

        public GroupDto() {
        }

        public GroupDto(String id, String displayName, Boolean isRoot) {
            this.id = id;
            this.displayName = displayName;
            this.isRoot = isRoot;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Boolean getIsRoot() {
            return isRoot;
        }

        public void setIsRoot(Boolean isRoot) {
            this.isRoot = isRoot;
        }
    }
}
