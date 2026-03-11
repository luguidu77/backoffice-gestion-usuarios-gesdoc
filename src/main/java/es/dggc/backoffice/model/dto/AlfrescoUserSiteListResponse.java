package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoUserSiteListResponse {

    private UserSiteList list;

    public UserSiteList getList() {
        return list;
    }

    public void setList(UserSiteList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserSiteList {
        private AlfrescoSitesListResponse.Pagination pagination;
        private List<UserSiteEntryWrapper> entries;

        public AlfrescoSitesListResponse.Pagination getPagination() {
            return pagination;
        }

        public void setPagination(AlfrescoSitesListResponse.Pagination pagination) {
            this.pagination = pagination;
        }

        public List<UserSiteEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<UserSiteEntryWrapper> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserSiteEntryWrapper {
        private UserSiteEntry entry;

        public UserSiteEntry getEntry() {
            return entry;
        }

        public void setEntry(UserSiteEntry entry) {
            this.entry = entry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserSiteEntry {
        private String id;
        private String role;
        private AlfrescoSitesListResponse.SiteEntry site;

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

        public AlfrescoSitesListResponse.SiteEntry getSite() {
            return site;
        }

        public void setSite(AlfrescoSitesListResponse.SiteEntry site) {
            this.site = site;
        }
    }
}
