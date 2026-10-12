package cl.duoc.dsy2207.usuarios;

import cl.duoc.dsy2207.usuarios.eventos.PublicadorEventos;
import cl.duoc.dsy2207.usuarios.model.Usuario;
import cl.duoc.dsy2207.usuarios.repository.UsuarioRepository;
import cl.duoc.dsy2207.usuarios.util.JsonUtil;
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

public class UsuariosFunction {
    private final UsuarioRepository repositorio = new UsuarioRepository();

    @FunctionName("listarUsuarios")
    public HttpResponseMessage listar(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            return json(request, HttpStatus.OK, repositorio.listar());
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("obtenerUsuario")
    public HttpResponseMessage obtener(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idUsuario = Long.parseLong(id);
            return repositorio.buscarPorId(idUsuario)
                    .map(u -> json(request, HttpStatus.OK, u))
                    .orElseGet(() -> json(request, HttpStatus.NOT_FOUND,
                            Map.of("error", "No existe el usuario con id " + idUsuario)));
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("crearUsuario")
    public HttpResponseMessage crear(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            Optional<String> cuerpo = request.getBody();
            if (cuerpo.isEmpty() || cuerpo.get().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El cuerpo de la solicitud es obligatorio"));
            }
            Usuario usuario = JsonUtil.fromJson(cuerpo.get(), Usuario.class);
            String validacion = validar(usuario);
            if (validacion != null) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", validacion));
            }
            Usuario creado = repositorio.crear(usuario);
            PublicadorEventos.usuarioCreado(creado);
            return json(request, HttpStatus.CREATED, creado);
        } catch (IllegalArgumentException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("actualizarUsuario")
    public HttpResponseMessage actualizar(
            @HttpTrigger(name = "req", methods = {HttpMethod.PUT},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idUsuario = Long.parseLong(id);
            Optional<String> cuerpo = request.getBody();
            if (cuerpo.isEmpty() || cuerpo.get().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El cuerpo de la solicitud es obligatorio"));
            }
            Usuario usuario = JsonUtil.fromJson(cuerpo.get(), Usuario.class);
            String validacion = validar(usuario);
            if (validacion != null) {
                return json(request, HttpStatus.BAD_REQUEST, Map.of("error", validacion));
            }
            boolean actualizado = repositorio.actualizar(idUsuario, usuario);
            if (!actualizado) {
                return json(request, HttpStatus.NOT_FOUND, Map.of("error", "No existe el usuario con id " + idUsuario));
            }
            usuario.setId(idUsuario);
            PublicadorEventos.usuarioActualizado(usuario);
            return json(request, HttpStatus.OK, usuario);
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
        } catch (IllegalArgumentException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    @FunctionName("eliminarUsuario")
    public HttpResponseMessage eliminar(
            @HttpTrigger(name = "req", methods = {HttpMethod.DELETE},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "usuarios/{id}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {
        try {
            long idUsuario = Long.parseLong(id);
            Optional<Usuario> previo = repositorio.buscarPorId(idUsuario);
            if (previo.isEmpty() || !repositorio.eliminar(idUsuario)) {
                return json(request, HttpStatus.NOT_FOUND, Map.of("error", "No existe el usuario con id " + idUsuario));
            }
            PublicadorEventos.usuarioEliminado(previo.get());
            return json(request, HttpStatus.OK, Map.of("mensaje", "Usuario eliminado", "id", idUsuario));
        } catch (NumberFormatException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("error", "El id debe ser numérico"));
        } catch (Exception e) {
            return error(request, context, e);
        }
    }

    private String validar(Usuario u) {
        if (u.getNombre() == null || u.getNombre().isBlank()) {
            return "El campo 'nombre' es obligatorio";
        }
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            return "El campo 'username' es obligatorio";
        }
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            return "El campo 'email' es obligatorio";
        }
        return null;
    }

    private HttpResponseMessage json(HttpRequestMessage<Optional<String>> request, HttpStatus status, Object cuerpo) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JsonUtil.toJson(cuerpo))
                .build();
    }

    private HttpResponseMessage error(HttpRequestMessage<Optional<String>> request, ExecutionContext context, Exception e) {
        context.getLogger().severe("Error en función de usuarios: " + e.getMessage());
        return json(request, HttpStatus.INTERNAL_SERVER_ERROR,
                Map.of("error", "Error interno", "detalle", String.valueOf(e.getMessage())));
    }
}
