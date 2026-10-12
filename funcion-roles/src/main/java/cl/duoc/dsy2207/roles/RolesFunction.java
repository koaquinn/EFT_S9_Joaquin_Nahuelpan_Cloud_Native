package cl.duoc.dsy2207.roles;

import cl.duoc.dsy2207.roles.eventos.PublicadorEventos;
import cl.duoc.dsy2207.roles.model.Rol;
import cl.duoc.dsy2207.roles.repository.RolRepository;
import cl.duoc.dsy2207.roles.util.JsonUtil;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Map;
import java.util.Optional;

public class RolesFunction {
    private final RolRepository repositorio = new RolRepository();

    @FunctionName("listarRoles")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "roles")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            return json(request, HttpStatus.OK, repositorio.listar());
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("obtenerRol")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idRol = Long.parseLong(id);
            return repositorio.buscarPorId(idRol)
                    .map(r -> json(request, HttpStatus.OK, r))
                    .orElseGet(() -> json(request, HttpStatus.NOT_FOUND,
                            Map.of("error", "No existe el rol con id " + idRol)));
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("crearRol")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "roles")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            Optional<String> cuerpo = request.getBody();
            if (cuerpo.isEmpty() || cuerpo.get().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El cuerpo de la solicitud es obligatorio"));
            }
            Rol rol = JsonUtil.fromJson(cuerpo.get(), Rol.class);
            if (rol.getNombre() == null || rol.getNombre().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El campo 'nombre' es obligatorio"));
            }
            Rol creado = repositorio.crear(rol);
            PublicadorEventos.rolCreado(creado);
            return json(request, HttpStatus.CREATED, creado);
        } catch (IllegalArgumentException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("actualizarRol")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "req", methods = {HttpMethod.PUT},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idRol = Long.parseLong(id);
            Optional<String> cuerpo = request.getBody();
            if (cuerpo.isEmpty() || cuerpo.get().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El cuerpo de la solicitud es obligatorio"));
            }
            Rol rol = JsonUtil.fromJson(cuerpo.get(), Rol.class);
            if (rol.getNombre() == null || rol.getNombre().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El campo 'nombre' es obligatorio"));
            }
            boolean actualizado = repositorio.actualizar(idRol, rol);
            if (!actualizado) {
                return json(request, HttpStatus.NOT_FOUND, Map.of("error", "No existe el rol con id " + idRol));
            }
            rol.setId(idRol);
            PublicadorEventos.rolActualizado(rol);
            return json(request, HttpStatus.OK, rol);
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
        } catch (IllegalArgumentException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("eliminarRol")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "req", methods = {HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idRol = Long.parseLong(id);
            Optional<Rol> previo = repositorio.buscarPorId(idRol);
            if (previo.isEmpty() || !repositorio.eliminar(idRol)) {
                return json(request, HttpStatus.NOT_FOUND, Map.of("error", "No existe el rol con id " + idRol));
            }
            PublicadorEventos.rolEliminado(previo.get());
            return json(request, HttpStatus.OK, Map.of("mensaje", "Rol eliminado", "id", idRol));
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
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
        context.getLogger().severe("Error en función de roles: " + e.getMessage());
        return json(request, HttpStatus.INTERNAL_SERVER_ERROR,
                Map.of("error", "Error interno", "detalle", String.valueOf(e.getMessage())));
    }
}
