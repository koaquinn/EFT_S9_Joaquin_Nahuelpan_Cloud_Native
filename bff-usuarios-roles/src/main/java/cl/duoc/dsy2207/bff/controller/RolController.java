package cl.duoc.dsy2207.bff.controller;

import cl.duoc.dsy2207.bff.client.RolesClient;
import cl.duoc.dsy2207.bff.dto.RolDTO;
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

@RestController
@RequestMapping("/api/bff/roles")
public class RolController {
    private final RolesClient rolesClient;

    public RolController(RolesClient rolesClient) {
        this.rolesClient = rolesClient;
    }

    @GetMapping
    public List<RolDTO> listar() {
        return rolesClient.listar();
    }

    @GetMapping("/{id}")
    public RolDTO obtener(@PathVariable Long id) {
        return rolesClient.obtener(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> crear(@RequestBody String cuerpo) {
        return rolesClient.crear(cuerpo);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String cuerpo) {
        return rolesClient.actualizar(id, cuerpo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        return rolesClient.eliminar(id);
    }
}
