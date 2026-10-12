package cl.duoc.dsy2207.eventos.model;

import cl.duoc.dsy2207.eventos.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record EventoRecibido(String id, String tipo, String asunto, LocalDateTime fecha, JsonNode datos) {

    private static final String PREFIJO_TIPO = "Duoc.UsuariosRoles.";

    public static EventoRecibido desdeJson(String json) {
        JsonNode nodo = JsonUtil.leerArbol(json);
        String fechaTexto = nodo.path("eventTime").asText(null);
        LocalDateTime fecha = fechaTexto == null
                ? LocalDateTime.now(ZoneOffset.UTC)
                : OffsetDateTime.parse(fechaTexto).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return new EventoRecibido(
                nodo.path("id").asText(),
                nodo.path("eventType").asText(),
                nodo.path("subject").asText(),
                fecha,
                nodo.path("data"));
    }

    public boolean esTipo(String nombreCorto) {
        return (PREFIJO_TIPO + nombreCorto).equals(tipo);
    }

    public Long numero(String campo) {
        String valor = texto(campo);
        return valor == null ? null : Long.valueOf(valor);
    }

    public String texto(String campo) {
        JsonNode valor = datos.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }
}
