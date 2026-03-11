package es.dggc.backoffice.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de respuesta para el listado de sitios a los que pertenece un usuario,
 * incluyendo el rol que tiene en cada uno.
 *
 * Se devuelve al frontend desde GET /api/users/{userId}/sites.
 */
public class UserSiteMembershipListResponse {

    private List<UserSiteMembershipDto> sites;
    private int totalItems;

    public UserSiteMembershipListResponse() {
        this.sites = new ArrayList<>();
        this.totalItems = 0;
    }

    public UserSiteMembershipListResponse(List<UserSiteMembershipDto> sites, int totalItems) {
        this.sites = sites;
        this.totalItems = totalItems;
    }

    public List<UserSiteMembershipDto> getSites() {
        return sites;
    }

    public void setSites(List<UserSiteMembershipDto> sites) {
        this.sites = sites;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    /**
     * DTO que representa la pertenencia de un usuario a un sitio con su rol.
     */
    public static class UserSiteMembershipDto {
        /** ID del sitio (p.ej. "unidad-tecnologia") */
        private String siteId;
        /** Título legible del sitio */
        private String siteTitle;
        /** Rol del usuario en el sitio: SiteManager, SiteCollaborator, SiteConsumer, SiteContributor */
        private String role;
        /** Visibilidad del sitio: PUBLIC, MODERATED, PRIVATE */
        private String visibility;

        public UserSiteMembershipDto() {
        }

        public UserSiteMembershipDto(String siteId, String siteTitle, String role, String visibility) {
            this.siteId = siteId;
            this.siteTitle = siteTitle;
            this.role = role;
            this.visibility = visibility;
        }

        public String getSiteId() {
            return siteId;
        }

        public void setSiteId(String siteId) {
            this.siteId = siteId;
        }

        public String getSiteTitle() {
            return siteTitle;
        }

        public void setSiteTitle(String siteTitle) {
            this.siteTitle = siteTitle;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }
    }
}
