package es.dggc.backoffice.model.dto;

import java.util.List;

/**
 * DTO de respuesta para el listado de usuarios.
 * 
 * Simplifica la respuesta de Alfresco para el frontend.
 */
public class UserListResponse {

    private List<UserDto> users;
    private Integer totalUsers;
    private Boolean hasMore;

    public UserListResponse() {
    }

    public UserListResponse(List<UserDto> users, Integer totalUsers, Boolean hasMore) {
        this.users = users;
        this.totalUsers = totalUsers;
        this.hasMore = hasMore;
    }

    public List<UserDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserDto> users) {
        this.users = users;
    }

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * DTO simplificado de usuario.
     */
    public static class UserDto {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private Boolean enabled;

        public UserDto() {
        }

        public UserDto(String id, String firstName, String lastName, String email, Boolean enabled) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getFullName() {
            String first = firstName != null ? firstName : "";
            String last = lastName != null ? lastName : "";
            return (first + " " + last).trim();
        }
    }
}
