package cl.duoc.dsy2207.bff.client;

import cl.duoc.dsy2207.bff.dto.RolDTO;
import cl.duoc.dsy2207.bff.exception.FuncionNoDisponibleException;
import cl.duoc.dsy2207.bff.exception.RecursoNoEncontradoException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RolesClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RolesClient(@Value("${funciones.roles-url}") String baseUrl, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<RolDTO> listar() {
        ResponseEntity<String> respuesta = ejecutar(HttpMethod.GET, "/api/roles", null);
        if (respuesta.getStatusCode().is2xxSuccessful()) {
            return leerLista(respuesta.getBody());
        }
        throw traducirError(respuesta);
    }

    public RolDTO obtener(Long id) {
        ResponseEntity<String> respuesta = ejecutar(HttpMethod.GET, "/api/roles/" + id, null);
        if (respuesta.getStatusCode().is2xxSuccessful()) {
            return leer(respuesta.getBody());
        }
        throw traducirError(respuesta);
    }

    public RolDTO obtenerSeguro(Long id) {
        try {
            ResponseEntity<String> respuesta = ejecutar(HttpMethod.GET, "/api/roles/" + id, null);
            if (respuesta.getStatusCode().is2xxSuccessful()) {
                return leer(respuesta.getBody());
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public ResponseEntity<String> crear(String cuerpoJson) {
        return ejecutar(HttpMethod.POST, "/api/roles", cuerpoJson);
    }

    public ResponseEntity<String> actualizar(Long id, String cuerpoJson) {
        return ejecutar(HttpMethod.PUT, "/api/roles/" + id, cuerpoJson);
    }

    public ResponseEntity<String> eliminar(Long id) {
        return ejecutar(HttpMethod.DELETE, "/api/roles/" + id, null);
    }

    private ResponseEntity<String> ejecutar(HttpMethod metodo, String uri, String cuerpoJson) {
        try {
            RestClient.RequestBodySpec spec = restClient.method(metodo).uri(uri);
            if (cuerpoJson != null) {
                spec = spec.contentType(MediaType.APPLICATION_JSON).body(cuerpoJson);
            }
            return spec.exchange((peticion, respuesta) -> {
                byte[] datos = respuesta.getBody().readAllBytes();
                String cuerpo = new String(datos, StandardCharsets.UTF_8);
                return ResponseEntity.status(respuesta.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo);
            });
        } catch (ResourceAccessException e) {
            throw new FuncionNoDisponibleException(
                    "No se pudo contactar la función de roles: " + e.getMessage(), e);
        }
    }

    private RuntimeException traducirError(ResponseEntity<String> respuesta) {
        if (respuesta.getStatusCode().value() == 404) {
            return new RecursoNoEncontradoException(respuesta.getBody());
        }
        return new FuncionNoDisponibleException(
                "La función de roles respondió " + respuesta.getStatusCode() + ": " + respuesta.getBody());
    }

    private RolDTO leer(String json) {
        try {
            return objectMapper.readValue(json, RolDTO.class);
        } catch (Exception e) {
            throw new FuncionNoDisponibleException("Respuesta inválida de la función de roles", e);
        }
    }

    private List<RolDTO> leerLista(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<RolDTO>>() {
            });
        } catch (Exception e) {
            throw new FuncionNoDisponibleException("Respuesta inválida de la función de roles", e);
        }
    }
}
