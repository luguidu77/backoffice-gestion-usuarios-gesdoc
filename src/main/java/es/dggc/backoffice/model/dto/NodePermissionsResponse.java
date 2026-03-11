package es.dggc.backoffice.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de respuesta para los permisos de un nodo (departamento/carpeta).
 * Se devuelve al frontend desde GET /api/nodes/{nodeId}/permissions.
 */
public class NodePermissionsResponse {

    private String nodeId;
    private String nodeName;
    private boolean isInheritanceEnabled;
    private List<PermissionEntry> locallySet;
    private List<PermissionEntry> inherited;

    public NodePermissionsResponse() {
        this.locallySet = new ArrayList<>();
        this.inherited = new ArrayList<>();
    }

    public NodePermissionsResponse(String nodeId, String nodeName, boolean isInheritanceEnabled,
            List<PermissionEntry> locallySet, List<PermissionEntry> inherited) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.isInheritanceEnabled = isInheritanceEnabled;
        this.locallySet = locallySet != null ? locallySet : new ArrayList<>();
        this.inherited = inherited != null ? inherited : new ArrayList<>();
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public boolean isInheritanceEnabled() { return isInheritanceEnabled; }
    public void setInheritanceEnabled(boolean inheritanceEnabled) { isInheritanceEnabled = inheritanceEnabled; }

    public List<PermissionEntry> getLocallySet() { return locallySet; }
    public void setLocallySet(List<PermissionEntry> locallySet) { this.locallySet = locallySet; }

    public List<PermissionEntry> getInherited() { return inherited; }
    public void setInherited(List<PermissionEntry> inherited) { this.inherited = inherited; }

    /**
     * DTO de un permiso individual (local o heredado).
     */
    public static class PermissionEntry {
        /** ID de la autoridad: GROUP_xxx o un userId */
        private String authorityId;
        /** Nombre legible (si está disponible) */
        private String authorityDisplayName;
        /** Tipo: GROUP o USER */
        private String authorityType;
        /** Rol: Coordinator, Collaborator, Contributor, Consumer, Editor */
        private String name;
        /** ALLOWED o DENIED */
        private String accessStatus;

        public PermissionEntry() {}

        public PermissionEntry(String authorityId, String authorityDisplayName, String authorityType,
                String name, String accessStatus) {
            this.authorityId = authorityId;
            this.authorityDisplayName = authorityDisplayName;
            this.authorityType = authorityType;
            this.name = name;
            this.accessStatus = accessStatus;
        }

        public String getAuthorityId() { return authorityId; }
        public void setAuthorityId(String authorityId) { this.authorityId = authorityId; }

        public String getAuthorityDisplayName() { return authorityDisplayName; }
        public void setAuthorityDisplayName(String authorityDisplayName) { this.authorityDisplayName = authorityDisplayName; }

        public String getAuthorityType() { return authorityType; }
        public void setAuthorityType(String authorityType) { this.authorityType = authorityType; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAccessStatus() { return accessStatus; }
        public void setAccessStatus(String accessStatus) { this.accessStatus = accessStatus; }
    }
}
