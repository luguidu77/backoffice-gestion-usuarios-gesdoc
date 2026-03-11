package es.dggc.backoffice.model.dto;

import java.util.List;

public class DepartmentListResponse {

    private List<DepartmentDto> departments;
    private Integer totalItems;
    private Boolean hasMore;

    public DepartmentListResponse() {
        this.departments = java.util.Collections.emptyList();
        this.totalItems = 0;
        this.hasMore = false;
    }

    public DepartmentListResponse(List<DepartmentDto> departments, Integer totalItems, Boolean hasMore) {
        this.departments = departments;
        this.totalItems = totalItems;
        this.hasMore = hasMore;
    }

    public List<DepartmentDto> getDepartments() {
        return departments;
    }

    public void setDepartments(List<DepartmentDto> departments) {
        this.departments = departments;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public static class DepartmentDto {
        private String id;
        private String name;
        private String parentId;
        private String unitId;
        private boolean isInheritingPermissions;

        public DepartmentDto() {
        }

        public DepartmentDto(String id, String name, String parentId, String unitId, boolean isInheritingPermissions) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
            this.unitId = unitId;
            this.isInheritingPermissions = isInheritingPermissions;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getUnitId() {
            return unitId;
        }

        public void setUnitId(String unitId) {
            this.unitId = unitId;
        }

        public boolean isInheritingPermissions() {
            return isInheritingPermissions;
        }

        public void setInheritingPermissions(boolean inheritingPermissions) {
            isInheritingPermissions = inheritingPermissions;
        }
    }
}
