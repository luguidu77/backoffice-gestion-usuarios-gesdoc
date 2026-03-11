package es.dggc.backoffice.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de respuesta para el listado y detalle de grupos de Alfresco.
 * Se devuelve al frontend desde GET /api/groups y GET /api/groups/{groupId}/members.
 */
public class GroupAdminResponse {

    // ── Listado de grupos ────────────────────────────────────────────────────

    public static class GroupListResponse {
        private List<GroupItem> groups;
        private int totalItems;
        private boolean hasMore;

        public GroupListResponse() {
            this.groups = new ArrayList<>();
        }

        public GroupListResponse(List<GroupItem> groups, int totalItems, boolean hasMore) {
            this.groups = groups;
            this.totalItems = totalItems;
            this.hasMore = hasMore;
        }

        public List<GroupItem> getGroups() { return groups; }
        public void setGroups(List<GroupItem> groups) { this.groups = groups; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

        public boolean isHasMore() { return hasMore; }
        public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    }

    public static class GroupItem {
        private String id;
        private String displayName;
        private boolean isRoot;
        private int memberCount;

        public GroupItem() {}

        public GroupItem(String id, String displayName, boolean isRoot, int memberCount) {
            this.id = id;
            this.displayName = displayName;
            this.isRoot = isRoot;
            this.memberCount = memberCount;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public boolean isRoot() { return isRoot; }
        public void setRoot(boolean root) { isRoot = root; }

        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    }

    // ── Miembros de un grupo ─────────────────────────────────────────────────

    public static class GroupMembersResponse {
        private String groupId;
        private String groupDisplayName;
        private List<GroupMemberItem> members;
        private int totalItems;

        public GroupMembersResponse() {
            this.members = new ArrayList<>();
        }

        public GroupMembersResponse(String groupId, String groupDisplayName,
                List<GroupMemberItem> members, int totalItems) {
            this.groupId = groupId;
            this.groupDisplayName = groupDisplayName;
            this.members = members;
            this.totalItems = totalItems;
        }

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }

        public String getGroupDisplayName() { return groupDisplayName; }
        public void setGroupDisplayName(String groupDisplayName) { this.groupDisplayName = groupDisplayName; }

        public List<GroupMemberItem> getMembers() { return members; }
        public void setMembers(List<GroupMemberItem> members) { this.members = members; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    }

    public static class GroupMemberItem {
        private String id;
        private String displayName;
        /** PERSON o GROUP */
        private String memberType;

        public GroupMemberItem() {}

        public GroupMemberItem(String id, String displayName, String memberType) {
            this.id = id;
            this.displayName = displayName;
            this.memberType = memberType;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getMemberType() { return memberType; }
        public void setMemberType(String memberType) { this.memberType = memberType; }
    }
}
