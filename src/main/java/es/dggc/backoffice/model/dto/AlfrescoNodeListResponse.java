package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoNodeListResponse {

    private NodeList list;

    public NodeList getList() {
        return list;
    }

    public void setList(NodeList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeList {
        private Pagination pagination;
        private List<NodeEntryWrapper> entries;

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public List<NodeEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<NodeEntryWrapper> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private int count;
        private boolean hasMoreItems;
        private int totalItems;
        private int skipCount;
        private int maxItems;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isHasMoreItems() {
            return hasMoreItems;
        }

        public void setHasMoreItems(boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public void setTotalItems(int totalItems) {
            this.totalItems = totalItems;
        }

        public int getSkipCount() {
            return skipCount;
        }

        public void setSkipCount(int skipCount) {
            this.skipCount = skipCount;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeEntryWrapper {
        private NodeEntry entry;

        public NodeEntry getEntry() {
            return entry;
        }

        public void setEntry(NodeEntry entry) {
            this.entry = entry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeEntry {
        private String id;
        private String name;
        private String nodeType;
        private boolean isFolder;
        private boolean isFile;
        private String parentId;

        // Permisos si se pide include=permissions
        private PermissionsInfo permissions;

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

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public boolean isFolder() {
            return isFolder;
        }

        public void setFolder(boolean folder) {
            isFolder = folder;
        }

        public boolean isFile() {
            return isFile;
        }

        public void setFile(boolean file) {
            isFile = file;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public PermissionsInfo getPermissions() {
            return permissions;
        }

        public void setPermissions(PermissionsInfo permissions) {
            this.permissions = permissions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PermissionsInfo {
        private boolean isInheritanceEnabled;
        private List<PermissionElement> locallySet;
        private List<PermissionElement> inherited;

        public boolean isInheritanceEnabled() {
            return isInheritanceEnabled;
        }

        public void setInheritanceEnabled(boolean inheritanceEnabled) {
            isInheritanceEnabled = inheritanceEnabled;
        }

        public List<PermissionElement> getLocallySet() {
            return locallySet;
        }

        public void setLocallySet(List<PermissionElement> locallySet) {
            this.locallySet = locallySet;
        }

        public List<PermissionElement> getInherited() {
            return inherited;
        }

        public void setInherited(List<PermissionElement> inherited) {
            this.inherited = inherited;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PermissionElement {
        private String authorityId;
        private String name;
        private String accessStatus;

        public String getAuthorityId() {
            return authorityId;
        }

        public void setAuthorityId(String authorityId) {
            this.authorityId = authorityId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAccessStatus() {
            return accessStatus;
        }

        public void setAccessStatus(String accessStatus) {
            this.accessStatus = accessStatus;
        }
    }
}
