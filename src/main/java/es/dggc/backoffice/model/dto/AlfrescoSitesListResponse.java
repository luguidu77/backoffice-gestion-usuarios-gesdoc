package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Mapeo de la respuesta JSON de Alfresco al listar sitios.
 *
 * Ejemplo de respuesta de Alfresco:
 * {
 * "list": {
 * "pagination": { "count": 5, "hasMoreItems": false, "totalItems": 5, ... },
 * "entries": [
 * { "entry": { "id": "site1", "title": "Sitio 1", "description": "...",
 * "visibility": "PUBLIC" } },
 * ...
 * ]
 * }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoSitesListResponse {

    private SiteList list;

    public SiteList getList() {
        return list;
    }

    public void setList(SiteList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteList {
        private Pagination pagination;
        private List<SiteEntryWrapper> entries;

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public List<SiteEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<SiteEntryWrapper> entries) {
            this.entries = entries;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteEntryWrapper {
        private SiteEntry entry;

        public SiteEntry getEntry() {
            return entry;
        }

        public void setEntry(SiteEntry entry) {
            this.entry = entry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SiteEntry {
        private String id;
        private String title;
        private String description;
        private String visibility;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
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

        public boolean getHasMoreItems() {
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
}
