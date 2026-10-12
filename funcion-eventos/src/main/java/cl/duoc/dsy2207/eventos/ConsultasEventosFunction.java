package cl.duoc.dsy2207.eventos;

import cl.duoc.dsy2207.eventos.repository.AuditoriaRepository;
import cl.duoc.dsy2207.eventos.repository.NotificacionRepository;
import cl.duoc.dsy2207.eventos.util.JsonUtil;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Map;
import java.util.Optional;

public class ConsultasEventosFunction {
    private final AuditoriaRepository auditoria = new AuditoriaRepository();
    private final NotificacionRepository notificaciones = new NotificacionRepository();

    @FunctionName("listarAuditoria")
    public HttpResponseMessage listarAuditoria(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "auditoria")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            String tipo = request.getQueryParameters().get("tipo");
            return json(request, HttpStatus.OK, auditoria.listar(tipo == null || tipo.isBlank() ? null : tipo));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("listarNotificaciones")
    public HttpResponseMessage listarNotificaciones(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "notificaciones")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            return json(request, HttpStatus.OK, notificaciones.listar());
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    private HttpResponseMessage json(HttpRequestMessage<Optional<String>> request, HttpStatus status, Object cuerpo) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JsonUtil.toJson(cuerpo))
                .build();
    }

    private HttpResponseMessage error(HttpRequestMessage<Optional<String>> request, ExecutionContext context, Exception e) {
        context.getLogger().severe("Error en funcion de eventos: " + e.getMessage());
        return json(request, HttpStatus.INTERNAL_SERVER_ERROR,
                Map.of("error", "Error interno", "detalle", String.valueOf(e.getMessage())));
    }
}
