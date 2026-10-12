package cl.duoc.dsy2207.eventos.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;

public record RegistroAuditoria(
        Long id,
        String eventoId,
        String tipoEvento,
        String asunto,
        @JsonRawValue String datos,
        LocalDateTime fechaEvento,
        LocalDateTime fechaRegistro) {
}
