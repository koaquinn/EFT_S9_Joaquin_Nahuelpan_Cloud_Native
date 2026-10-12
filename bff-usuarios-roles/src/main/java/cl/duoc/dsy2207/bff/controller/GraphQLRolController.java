package cl.duoc.dsy2207.bff.controller;

import cl.duoc.dsy2207.bff.client.RolesGraphQLClient;
import cl.duoc.dsy2207.bff.dto.RolDTO;
import cl.duoc.dsy2207.bff.exception.RecursoNoEncontradoException;
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

@RestController
@RequestMapping("/api/bff/graphql/roles")
public class GraphQLRolController {
    private final RolesGraphQLClient roles;

    public GraphQLRolController(RolesGraphQLClient roles) {
        this.roles = roles;
    }

    @GetMapping
    public List<RolDTO> listar() {
        return roles.listar();
    }

    @GetMapping("/{id}")
    public RolDTO obtener(@PathVariable Long id) {
        return roles.obtener(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol con id " + id));
    }

    @PostMapping
    public ResponseEntity<RolDTO> crear(@RequestBody RolDTO rol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roles.crear(rol));
    }

    @PutMapping("/{id}")
    public RolDTO actualizar(@PathVariable Long id, @RequestBody RolDTO rol) {
        return roles.actualizar(id, rol)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol con id " + id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> eliminar(@PathVariable Long id) {
        if (!roles.eliminar(id)) {
            throw new RecursoNoEncontradoException("No existe el rol con id " + id);
        }
        return Map.of("mensaje", "Rol eliminado", "id", id);
    }
}
