package cl.duoc.dsy2207.bff.client;

import cl.duoc.dsy2207.bff.exception.FuncionNoDisponibleException;
import cl.duoc.dsy2207.bff.exception.SolicitudInvalidaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class GraphQLClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String nombreFuncion;

    protected GraphQLClient(String baseUrl, String nombreFuncion, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.nombreFuncion = nombreFuncion;
    }

    protected JsonNode ejecutar(String operacion, String consulta, Map<String, Object> variables) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("query", consulta);
        cuerpo.put("variables", variables == null ? Map.of() : variables);
        ResponseEntity<String> respuesta;
        try {
            byte[] datos = objectMapper.writeValueAsBytes(cuerpo);
            respuesta = restClient.post()
                    .uri("/api/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(datos.length)
                    .body(datos)
                    .exchange((peticion, res) -> ResponseEntity.status(res.getStatusCode())
                            .body(new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (ResourceAccessException e) {
            throw new FuncionNoDisponibleException(
                    "No se pudo contactar la capa GraphQL de la función de " + nombreFuncion + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FuncionNoDisponibleException(
                    "Error al invocar la capa GraphQL de la función de " + nombreFuncion + ": " + e.getMessage(), e);
        }

        JsonNode nodo = leer(respuesta.getBody());
        JsonNode errores = nodo.path("errors");
        if (!errores.isMissingNode() && !errores.isNull() && !(errores.isArray() && errores.isEmpty())) {
            String mensaje = errores.isArray() ? errores.get(0).path("message").asText("Error GraphQL") : errores.asText();
            throw new SolicitudInvalidaException(mensaje);
        }
        if (!respuesta.getStatusCode().is2xxSuccessful() || !nodo.has("data")) {
            throw new FuncionNoDisponibleException("La capa GraphQL de la función de " + nombreFuncion
                    + " respondió " + respuesta.getStatusCode() + ": " + respuesta.getBody());
        }
        return nodo.path("data").path(operacion);
    }

    protected <T> T convertir(JsonNode nodo, Class<T> tipo) {
        return objectMapper.convertValue(nodo, tipo);
    }

    private JsonNode leer(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception e) {
            throw new FuncionNoDisponibleException("Respuesta GraphQL inválida de la función de " + nombreFuncion, e);
        }
    }
}
