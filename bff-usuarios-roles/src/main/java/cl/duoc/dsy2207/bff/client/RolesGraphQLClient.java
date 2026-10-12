package cl.duoc.dsy2207.bff.client;

import cl.duoc.dsy2207.bff.dto.RolDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RolesGraphQLClient extends GraphQLClient {
    private static final String CAMPOS = "{ id nombre descripcion }";

    public RolesGraphQLClient(@Value("${funciones.roles-url}") String baseUrl, ObjectMapper objectMapper) {
        super(baseUrl, "roles", objectMapper);
    }

    public List<RolDTO> listar() {
        JsonNode datos = ejecutar("roles", "query { roles " + CAMPOS + " }", null);
        List<RolDTO> roles = new ArrayList<>();
        datos.forEach(nodo -> roles.add(convertir(nodo, RolDTO.class)));
        return roles;
    }

    public Optional<RolDTO> obtener(Long id) {
        JsonNode datos = ejecutar("rol", "query($id: ID!) { rol(id: $id) " + CAMPOS + " }",
                Map.of("id", String.valueOf(id)));
        return datos.isNull() || datos.isMissingNode() ? Optional.empty() : Optional.of(convertir(datos, RolDTO.class));
    }

    public RolDTO crear(RolDTO rol) {
        JsonNode datos = ejecutar("crearRol",
                "mutation($nombre: String!, $descripcion: String) { crearRol(nombre: $nombre, descripcion: $descripcion) "
                        + CAMPOS + " }",
                variables(rol));
        return convertir(datos, RolDTO.class);
    }

    public Optional<RolDTO> actualizar(Long id, RolDTO rol) {
        Map<String, Object> variables = variables(rol);
        variables.put("id", String.valueOf(id));
        JsonNode datos = ejecutar("actualizarRol",
                "mutation($id: ID!, $nombre: String!, $descripcion: String) "
                        + "{ actualizarRol(id: $id, nombre: $nombre, descripcion: $descripcion) " + CAMPOS + " }",
                variables);
        return datos.isNull() || datos.isMissingNode() ? Optional.empty() : Optional.of(convertir(datos, RolDTO.class));
    }

    public boolean eliminar(Long id) {
        JsonNode datos = ejecutar("eliminarRol", "mutation($id: ID!) { eliminarRol(id: $id) }",
                Map.of("id", String.valueOf(id)));
        return datos.asBoolean(false);
    }

    private Map<String, Object> variables(RolDTO rol) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", rol.getNombre());
        variables.put("descripcion", rol.getDescripcion());
        return variables;
    }
}
