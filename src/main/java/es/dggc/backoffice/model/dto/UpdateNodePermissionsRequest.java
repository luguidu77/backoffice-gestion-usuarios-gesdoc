package es.dggc.backoffice.model.dto;

import java.util.List;

/**
 * Body de la petición para actualizar los permisos de un nodo.
 * Cuerpo de PUT /api/nodes/{nodeId}/permissions
 *
 * Alfresco espera:
 * {
 *   "permissions": {
 *     "isInheritanceEnabled": true/false,
 *     "locallySet": [
 *       { "authorityId": "GROUP_xxx", "name": "Contributor", "accessStatus": "ALLOWED" }
 *     ]
 *   }
 * }
 */
public class UpdateNodePermissionsRequest {

    private Boolean isInheritanceEnabled;
    private List<PermissionItem> locallySet;

    public UpdateNodePermissionsRequest() {}

    public Boolean getIsInheritanceEnabled() { return isInheritanceEnabled; }
    public void setIsInheritanceEnabled(Boolean isInheritanceEnabled) { this.isInheritanceEnabled = isInheritanceEnabled; }

    public List<PermissionItem> getLocallySet() { return locallySet; }
    public void setLocallySet(List<PermissionItem> locallySet) { this.locallySet = locallySet; }

    public static class PermissionItem {
        private String authorityId;
        private String name;
        private String accessStatus;

        public PermissionItem() {}

        public PermissionItem(String authorityId, String name, String accessStatus) {
            this.authorityId = authorityId;
            this.name = name;
            this.accessStatus = accessStatus;
        }

        public String getAuthorityId() { return authorityId; }
        public void setAuthorityId(String authorityId) { this.authorityId = authorityId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAccessStatus() { return accessStatus; }
        public void setAccessStatus(String accessStatus) { this.accessStatus = accessStatus; }
    }
}
