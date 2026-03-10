package es.dggc.backoffice.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de respuesta simplificado para el listado de sitios.
 * Se devuelve al frontend desde GET /api/sites.
 */
public class SiteListResponse {

    private List<SiteDto> sites;
    private int totalSites;
    private boolean hasMore;

    public SiteListResponse() {
        this.sites = new ArrayList<>();
        this.totalSites = 0;
        this.hasMore = false;
    }

    public SiteListResponse(List<SiteDto> sites, int totalSites, boolean hasMore) {
        this.sites = sites;
        this.totalSites = totalSites;
        this.hasMore = hasMore;
    }

    public List<SiteDto> getSites() {
        return sites;
    }

    public void setSites(List<SiteDto> sites) {
        this.sites = sites;
    }

    public int getTotalSites() {
        return totalSites;
    }

    public void setTotalSites(int totalSites) {
        this.totalSites = totalSites;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * DTO simplificado de un Sitio de Alfresco.
     */
    public static class SiteDto {
        private String id;
        private String title;
        private String description;
        private String visibility;

        public SiteDto() {
        }

        public SiteDto(String id, String title, String description, String visibility) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.visibility = visibility;
        }

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
}
