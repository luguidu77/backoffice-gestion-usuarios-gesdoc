package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Mapeo de la respuesta de Alfresco al listar miembros de un grupo.
 * GET /groups/{groupId}/members
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoGroupMembersListResponse {

    private MemberList list;

    public MemberList getList() { return list; }
    public void setList(MemberList list) { this.list = list; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemberList {
        private AlfrescoSitesListResponse.Pagination pagination;
        private List<MemberEntryWrapper> entries;

        public AlfrescoSitesListResponse.Pagination getPagination() { return pagination; }
        public void setPagination(AlfrescoSitesListResponse.Pagination pagination) { this.pagination = pagination; }

        public List<MemberEntryWrapper> getEntries() { return entries; }
        public void setEntries(List<MemberEntryWrapper> entries) { this.entries = entries; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemberEntryWrapper {
        private MemberEntry entry;

        public MemberEntry getEntry() { return entry; }
        public void setEntry(MemberEntry entry) { this.entry = entry; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemberEntry {
        /** ID: username o GROUP_xxx */
        private String id;
        private String displayName;
        /** PERSON o GROUP */
        private String memberType;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getMemberType() { return memberType; }
        public void setMemberType(String memberType) { this.memberType = memberType; }
    }
}
