package cl.duoc.dsy2207.bff.controller;

import cl.duoc.dsy2207.bff.client.RolesGraphQLClient;
import cl.duoc.dsy2207.bff.client.UsuariosGraphQLClient;
import cl.duoc.dsy2207.bff.dto.RolDTO;
import cl.duoc.dsy2207.bff.dto.UsuarioDTO;
import cl.duoc.dsy2207.bff.exception.RecursoNoEncontradoException;
import cl.duoc.dsy2207.bff.exception.SolicitudInvalidaException;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/bff/graphql/usuarios")
public class GraphQLUsuarioController {
    private final UsuariosGraphQLClient usuarios;
    private final RolesGraphQLClient roles;

    public GraphQLUsuarioController(UsuariosGraphQLClient usuarios, RolesGraphQLClient roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @GetMapping
    public List<UsuarioDTO> listar() {
        List<UsuarioDTO> lista = usuarios.listar();
        Map<Long, RolDTO> rolesPorId = roles.listar().stream()
                .collect(Collectors.toMap(RolDTO::getId, Function.identity()));
        lista.forEach(u -> u.setRol(u.getRolId() == null ? null : rolesPorId.get(u.getRolId())));
        return lista;
    }

    @GetMapping("/{id}")
    public UsuarioDTO obtener(@PathVariable Long id) {
        UsuarioDTO usuario = usuarios.obtener(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
        if (usuario.getRolId() != null) {
            usuario.setRol(roles.obtener(usuario.getRolId()).orElse(null));
        }
        return usuario;
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@RequestBody UsuarioDTO usuario) {
        validarRol(usuario.getRolId());
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.crear(usuario));
    }

    @PutMapping("/{id}")
    public UsuarioDTO actualizar(@PathVariable Long id, @RequestBody UsuarioDTO usuario) {
        validarRol(usuario.getRolId());
        return usuarios.actualizar(id, usuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> eliminar(@PathVariable Long id) {
        if (!usuarios.eliminar(id)) {
            throw new RecursoNoEncontradoException("No existe el usuario con id " + id);
        }
        return Map.of("mensaje", "Usuario eliminado", "id", id);
    }

    private void validarRol(Long rolId) {
        if (rolId != null && roles.obtener(rolId).isEmpty()) {
            throw new SolicitudInvalidaException("El rol con id " + rolId + " no existe");
        }
    }
}
