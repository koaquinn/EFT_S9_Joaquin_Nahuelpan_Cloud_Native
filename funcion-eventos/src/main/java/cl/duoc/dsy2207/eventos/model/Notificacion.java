package cl.duoc.dsy2207.eventos.model;

import java.time.LocalDateTime;

public record Notificacion(
        Long id,
        String eventoId,
        String tipo,
        String destinatario,
        String asunto,
        String mensaje,
        LocalDateTime fecha) {
}
