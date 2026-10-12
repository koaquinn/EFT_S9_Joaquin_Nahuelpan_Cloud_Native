package cl.duoc.dsy2207.bff.controller;

import cl.duoc.dsy2207.bff.client.EventosClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/eventos")
public class EventoController {
    private final EventosClient eventosClient;

    public EventoController(EventosClient eventosClient) {
        this.eventosClient = eventosClient;
    }

    @GetMapping("/auditoria")
    public ResponseEntity<String> auditoria(@RequestParam(required = false) String tipo) {
        return eventosClient.auditoria(tipo);
    }

    @GetMapping("/notificaciones")
    public ResponseEntity<String> notificaciones() {
        return eventosClient.notificaciones();
    }
}
