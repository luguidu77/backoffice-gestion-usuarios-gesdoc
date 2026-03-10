package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO que mapea la respuesta de Alfresco al listar miembros de un sitio.
 * 
 * Endpoint: GET
 * /api/-default-/public/alfresco/versions/1/sites/{siteId}/members
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoSiteMembersListResponse {

    private SiteMemberList list;

    public SiteMemberList getList() {
        return list;
    }

    public void setList(SiteMemberList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteMemberList {
        private Pagination pagination;
        private List<SiteMemberEntryWrapper> entries;

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public List<SiteMemberEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<SiteMemberEntryWrapper> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private Integer count;
        private Boolean hasMoreItems;
        private Integer totalItems;
        private Integer skipCount;
        private Integer maxItems;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Boolean getHasMoreItems() {
            return hasMoreItems;
        }

        public void setHasMoreItems(Boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        public Integer getTotalItems() {
            return totalItems;
        }

        public void setTotalItems(Integer totalItems) {
            this.totalItems = totalItems;
        }

        public Integer getSkipCount() {
            return skipCount;
        }

        public void setSkipCount(Integer skipCount) {
            this.skipCount = skipCount;
        }

        public Integer getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(Integer maxItems) {
            this.maxItems = maxItems;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteMemberEntryWrapper {
        private SiteMemberEntry entry;

        public SiteMemberEntry getEntry() {
            return entry;
        }

        public void setEntry(SiteMemberEntry entry) {
            this.entry = entry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteMemberEntry {
        private String id;
        private String role;
        private AlfrescoPersonResponse.PersonEntry person;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public AlfrescoPersonResponse.PersonEntry getPerson() {
            return person;
        }

        public void setPerson(AlfrescoPersonResponse.PersonEntry person) {
            this.person = person;
        }
    }
}
