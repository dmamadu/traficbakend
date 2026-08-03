package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.RoleDTO;
import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.Permission;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.exceptions.RoleAlreadyExistsException;
import com.itma.gestionProjet.exceptions.RoleNotFoundException;
import com.itma.gestionProjet.repositories.PermissionRepository;
import com.itma.gestionProjet.repositories.RoleRepository;
import com.itma.gestionProjet.repositories.VerificationTokenRepository;
import com.itma.gestionProjet.requests.RoleRequest;
import com.itma.gestionProjet.services.IRoleService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoleService implements IRoleService {


    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    public RoleService(RoleRepository roleRepository, VerificationTokenRepository tokenRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<User> findRoleByName(String name) {
        return Optional.empty();
    }

    @Override
    public RoleDTO saveRole(RoleRequest p) {

        Optional<Role>  optionalUser = roleRepository.findByName(p.getName());
        if(optionalUser.isPresent())
            throw new RoleAlreadyExistsException("Role déjà existant");
        return convertEntityToDto( roleRepository.save(convertDtoToEntity(p)));
    }

    @Override
    public RoleDTO updateRole(Long id, RoleRequest roleRequest) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        if (!role.getName().equals(roleRequest.getName()) &&
                roleRepository.existsByName(roleRequest.getName())) {
            throw new RuntimeException("Role with this name already exists");
        }
        role.setName(roleRequest.getName());
        Role updatedRole = roleRepository.save(role);
        return convertEntityToDto(updatedRole);
    }

    @Override
    public RoleDTO getRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Le rôle avec l'id " + id + " n'existe pas."));
        return convertEntityToDto(role);
    }

    @Override
    public RoleDTO setRolePermissions(Long roleId, List<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Le rôle avec l'id " + roleId + " n'existe pas."));
        List<Permission> permissions = permissionCodes == null || permissionCodes.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(permissionRepository.findAll().stream()
                        .filter(p -> permissionCodes.contains(p.getCode()))
                        .toList());
        role.setPermissions(permissions);
        return convertEntityToDto(roleRepository.save(role));
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();

    }

    @Override
    public void deleteRole(Role p) {

    }

    @Override
    public void deleteRoleById(Long id) {
        if (roleRepository.existsById(id)) {
            roleRepository.deleteById(id);
        } else {
            throw new RoleNotFoundException("Le rôle avec l'id " + id + " n'existe pas.");
        }
    }

    @Override
    public RoleDTO convertEntityToDto(Role p) {
        // Construction manuelle : ModelMapper (LOOSE) confondrait `permissions` (List<Permission>)
        // et `permissionCodes` (List<String>) et échouerait à les convertir automatiquement.
        RoleDTO dto = new RoleDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPermissionCodes(p.getPermissions() == null
                ? List.of()
                : p.getPermissions().stream().map(Permission::getCode).toList());
        return dto;
    }

    @Override
    public Role convertDtoToEntity(RoleRequest RoleDto) {
        Role role = new Role();
        role = modelMapper.map(RoleDto, Role.class);
        return role;

    }
}
