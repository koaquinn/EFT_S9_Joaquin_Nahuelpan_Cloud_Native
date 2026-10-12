package cl.duoc.dsy2207.bff.controller;

import cl.duoc.dsy2207.bff.client.RolesClient;
import cl.duoc.dsy2207.bff.client.UsuariosClient;
import cl.duoc.dsy2207.bff.dto.RolDTO;
import cl.duoc.dsy2207.bff.dto.UsuarioDTO;
import cl.duoc.dsy2207.bff.exception.SolicitudInvalidaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bff/usuarios")
public class UsuarioController {
    private final UsuariosClient usuariosClient;
    private final RolesClient rolesClient;
    private final ObjectMapper objectMapper;

    public UsuarioController(UsuariosClient usuariosClient, RolesClient rolesClient, ObjectMapper objectMapper) {
        this.usuariosClient = usuariosClient;
        this.rolesClient = rolesClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<UsuarioDTO> listar() {
        List<UsuarioDTO> usuarios = usuariosClient.listar();
        Map<Long, RolDTO> rolesPorId = obtenerRolesPorId();
        usuarios.forEach(usuario -> {
            if (usuario.getRolId() != null) {
                usuario.setRol(rolesPorId.get(usuario.getRolId()));
            }
        });
        return usuarios;
    }

    @GetMapping("/{id}")
    public UsuarioDTO obtener(@PathVariable Long id) {
        UsuarioDTO usuario = usuariosClient.obtener(id);
        if (usuario.getRolId() != null) {
            usuario.setRol(rolesClient.obtenerSeguro(usuario.getRolId()));
        }
        return usuario;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> crear(@RequestBody String cuerpo) {
        validarRol(cuerpo);
        return usuariosClient.crear(cuerpo);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String cuerpo) {
        validarRol(cuerpo);
        return usuariosClient.actualizar(id, cuerpo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        return usuariosClient.eliminar(id);
    }

    private void validarRol(String cuerpo) {
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(cuerpo);
        } catch (Exception e) {
            throw new SolicitudInvalidaException("JSON inválido: " + e.getMessage());
        }
        JsonNode rolId = nodo.path("rolId");
        if (!rolId.isMissingNode() && !rolId.isNull() && rolesClient.obtenerSeguro(rolId.asLong()) == null) {
            throw new SolicitudInvalidaException("El rol con id " + rolId.asLong() + " no existe");
        }
    }

    private Map<Long, RolDTO> obtenerRolesPorId() {
        try {
            return rolesClient.listar().stream()
                    .filter(rol -> rol.getId() != null)
                    .collect(Collectors.toMap(RolDTO::getId, Function.identity()));
        } catch (RuntimeException e) {
            return Map.of();
        }
    }
}
