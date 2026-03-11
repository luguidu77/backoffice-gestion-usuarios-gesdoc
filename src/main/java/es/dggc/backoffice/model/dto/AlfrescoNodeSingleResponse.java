package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wrapper para la respuesta de un único nodo de Alfresco.
 * GET /nodes/{nodeId}?include=permissions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoNodeSingleResponse {

    private AlfrescoNodeListResponse.NodeEntry entry;

    public AlfrescoNodeListResponse.NodeEntry getEntry() {
        return entry;
    }

    public void setEntry(AlfrescoNodeListResponse.NodeEntry entry) {
        this.entry = entry;
    }
}
