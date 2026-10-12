package cl.duoc.dsy2207.bff.client;

import cl.duoc.dsy2207.bff.exception.FuncionNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
public class EventosClient {
    private final RestClient restClient;

    public EventosClient(@Value("${funciones.eventos-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ResponseEntity<String> auditoria(String tipo) {
        String uri = tipo == null || tipo.isBlank() ? "/api/auditoria" : "/api/auditoria?tipo=" + tipo;
        return consultar(uri);
    }

    public ResponseEntity<String> notificaciones() {
        return consultar("/api/notificaciones");
    }

    private ResponseEntity<String> consultar(String uri) {
        try {
            return restClient.get().uri(uri).exchange((peticion, respuesta) -> {
                String cuerpo = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.status(respuesta.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo);
            });
        } catch (ResourceAccessException e) {
            throw new FuncionNoDisponibleException(
                    "No se pudo contactar la función de eventos: " + e.getMessage(), e);
        }
    }
}
