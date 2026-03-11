package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoGroupListResponse {

    private GroupList list;

    public GroupList getList() {
        return list;
    }

    public void setList(GroupList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupList {
        private List<GroupEntryWrapper> entries;

        public List<GroupEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<GroupEntryWrapper> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupEntryWrapper {
        private GroupEntry entry;

        public GroupEntry getEntry() {
            return entry;
        }

        public void setEntry(GroupEntry entry) {
            this.entry = entry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupEntry {
        private String id;
        private String displayName;
        private Boolean isRoot;

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
