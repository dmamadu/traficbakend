package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.RoleDTO;
import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.requests.RoleRequest;

import java.util.List;
import java.util.Optional;

public interface IRoleService {

    Optional<User> findRoleByName(String name);

    RoleDTO saveRole(RoleRequest p);
    RoleDTO updateRole(Long id,RoleRequest p);
    RoleDTO getRole(Long id);
    List<Role> getAllRoles();
    void deleteRole(Role p);
    void deleteRoleById(Long id);

    RoleDTO convertEntityToDto(Role p);
    Role convertDtoToEntity(RoleRequest UserDto);

    RoleDTO setRolePermissions(Long roleId, List<String> permissionCodes);
}
