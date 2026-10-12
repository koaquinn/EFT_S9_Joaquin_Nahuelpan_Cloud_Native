package cl.duoc.dsy2207.roles.eventos;

import cl.duoc.dsy2207.roles.model.Rol;
import cl.duoc.dsy2207.roles.util.JsonUtil;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.AddHeadersPolicy;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

import java.util.Map;
import java.util.logging.Logger;

public final class PublicadorEventos {
    private static final Logger LOG = Logger.getLogger(PublicadorEventos.class.getName());
    private static final String PREFIJO_TIPO = "Duoc.UsuariosRoles.";
    private static final String VERSION_DATOS = "1.0";

    private static volatile EventGridPublisherClient<EventGridEvent> cliente;

    private PublicadorEventos() {
    }

    public static void rolCreado(Rol rol) {
        publicar("RolCreado", rol);
    }

    public static void rolActualizado(Rol rol) {
        publicar("RolActualizado", rol);
    }

    public static void rolEliminado(Rol rol) {
        publicar("RolEliminado", rol);
    }

    private static void publicar(String tipo, Rol rol) {
        EventGridPublisherClient<EventGridEvent> publicador = obtenerCliente();
        if (publicador == null) {
            LOG.warning("Event Grid no configurado: se omite el evento " + tipo);
            return;
        }
        try {
            Map<String, Object> datos = JsonUtil.aMapa(rol);
            EventGridEvent evento = new EventGridEvent(
                    "/roles/" + rol.getId(),
                    PREFIJO_TIPO + tipo,
                    BinaryData.fromObject(datos),
                    VERSION_DATOS);
            publicador.sendEvent(evento);
            LOG.info("Evento publicado en Event Grid: " + tipo + " (rol " + rol.getId() + ")");
        } catch (RuntimeException e) {
            LOG.warning("No se pudo publicar el evento " + tipo + ": " + e.getMessage());
        }
    }

    private static EventGridPublisherClient<EventGridEvent> obtenerCliente() {
        if (cliente == null) {
            synchronized (PublicadorEventos.class) {
                if (cliente == null) {
                    String endpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
                    String clave = System.getenv("EVENTGRID_TOPIC_KEY");
                    if (endpoint == null || endpoint.isBlank() || clave == null || clave.isBlank()) {
                        return null;
                    }
                    EventGridPublisherClientBuilder builder = new EventGridPublisherClientBuilder().endpoint(endpoint);
                    if (endpoint.startsWith("https://")) {
                        builder.credential(new AzureKeyCredential(clave));
                    } else {
                        HttpHeaders cabeceras = new HttpHeaders().set(HttpHeaderName.fromString("aeg-sas-key"), clave);
                        builder.pipeline(new HttpPipelineBuilder().policies(new AddHeadersPolicy(cabeceras)).build());
                    }
                    cliente = builder.buildEventGridEventPublisherClient();
                }
            }
        }
        return cliente;
    }
}
