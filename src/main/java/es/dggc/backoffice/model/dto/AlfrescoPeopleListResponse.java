package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO que mapea la respuesta de Alfresco al listar usuarios.
 * 
 * Endpoint: GET /api/-default-/public/alfresco/versions/1/people
 * 
 * Ejemplo de respuesta JSON de Alfresco:
 * {
 *   "list": {
 *     "pagination": {
 *       "count": 2,
 *       "hasMoreItems": false,
 *       "totalItems": 2,
 *       "skipCount": 0,
 *       "maxItems": 100
 *     },
 *     "entries": [
 *       {
 *         "entry": {
 *           "id": "admin",
 *           "firstName": "Administrator",
 *           "lastName": "",
 *           "email": "admin@alfresco.com",
 *           "enabled": true
 *         }
 *       }
 *     ]
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoPeopleListResponse {

    private PeopleList list;

    public PeopleList getList() {
        return list;
    }

    public void setList(PeopleList list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeopleList {
        private Pagination pagination;
        private List<PersonEntryWrapper> entries;

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public List<PersonEntryWrapper> getEntries() {
            return entries;
        }

        public void setEntries(List<PersonEntryWrapper> entries) {
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
    public static class PersonEntryWrapper {
        private AlfrescoPersonResponse.PersonEntry entry;

        public AlfrescoPersonResponse.PersonEntry getEntry() {
            return entry;
        }

        public void setEntry(AlfrescoPersonResponse.PersonEntry entry) {
            this.entry = entry;
        }
    }
}
