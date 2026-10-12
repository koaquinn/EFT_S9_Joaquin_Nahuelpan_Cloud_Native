package cl.duoc.dsy2207.bff.client;

import cl.duoc.dsy2207.bff.dto.UsuarioDTO;
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
public class UsuariosGraphQLClient extends GraphQLClient {
    private static final String CAMPOS = "{ id nombre username email activo rolId }";

    public UsuariosGraphQLClient(@Value("${funciones.usuarios-url}") String baseUrl, ObjectMapper objectMapper) {
        super(baseUrl, "usuarios", objectMapper);
    }

    public List<UsuarioDTO> listar() {
        JsonNode datos = ejecutar("usuarios", "query { usuarios " + CAMPOS + " }", null);
        List<UsuarioDTO> usuarios = new ArrayList<>();
        datos.forEach(nodo -> usuarios.add(convertir(nodo, UsuarioDTO.class)));
        return usuarios;
    }

    public Optional<UsuarioDTO> obtener(Long id) {
        JsonNode datos = ejecutar("usuario", "query($id: ID!) { usuario(id: $id) " + CAMPOS + " }",
                Map.of("id", String.valueOf(id)));
        return datos.isNull() || datos.isMissingNode() ? Optional.empty() : Optional.of(convertir(datos, UsuarioDTO.class));
    }

    public UsuarioDTO crear(UsuarioDTO usuario) {
        JsonNode datos = ejecutar("crearUsuario",
                "mutation($nombre: String!, $username: String!, $email: String!, $activo: Boolean, $rolId: Int) "
                        + "{ crearUsuario(nombre: $nombre, username: $username, email: $email, activo: $activo, rolId: $rolId) "
                        + CAMPOS + " }",
                variables(usuario));
        return convertir(datos, UsuarioDTO.class);
    }

    public Optional<UsuarioDTO> actualizar(Long id, UsuarioDTO usuario) {
        Map<String, Object> variables = variables(usuario);
        variables.put("id", String.valueOf(id));
        JsonNode datos = ejecutar("actualizarUsuario",
                "mutation($id: ID!, $nombre: String!, $username: String!, $email: String!, $activo: Boolean, $rolId: Int) "
                        + "{ actualizarUsuario(id: $id, nombre: $nombre, username: $username, email: $email, activo: $activo, rolId: $rolId) "
                        + CAMPOS + " }",
                variables);
        return datos.isNull() || datos.isMissingNode() ? Optional.empty() : Optional.of(convertir(datos, UsuarioDTO.class));
    }

    public boolean eliminar(Long id) {
        JsonNode datos = ejecutar("eliminarUsuario", "mutation($id: ID!) { eliminarUsuario(id: $id) }",
                Map.of("id", String.valueOf(id)));
        return datos.asBoolean(false);
    }

    private Map<String, Object> variables(UsuarioDTO usuario) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", usuario.getNombre());
        variables.put("username", usuario.getUsername());
        variables.put("email", usuario.getEmail());
        variables.put("activo", usuario.getActivo() == null ? Boolean.TRUE : usuario.getActivo());
        variables.put("rolId", usuario.getRolId() == null ? null : usuario.getRolId().intValue());
        return variables;
    }
}
