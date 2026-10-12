package cl.duoc.dsy2207.eventos;

import cl.duoc.dsy2207.eventos.model.EventoRecibido;
import cl.duoc.dsy2207.eventos.repository.AuditoriaRepository;
import cl.duoc.dsy2207.eventos.repository.NotificacionRepository;
import cl.duoc.dsy2207.eventos.repository.UsuarioRolRepository;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import java.sql.SQLException;
import java.util.Optional;

public class ConsumidoresEventosFunction {
    private final AuditoriaRepository auditoria = new AuditoriaRepository();
    private final NotificacionRepository notificaciones = new NotificacionRepository();
    private final UsuarioRolRepository usuarioRol = new UsuarioRolRepository();

    @FunctionName("asignarRolPorDefecto")
    public void asignarRolPorDefecto(
            @EventGridTrigger(name = "evento") String evento,
            final ExecutionContext context) throws SQLException {
        EventoRecibido recibido = EventoRecibido.desdeJson(evento);
        if (!recibido.esTipo("UsuarioCreado")) {
            return;
        }
        Long usuarioId = recibido.numero("id");
        if (recibido.texto("rolId") != null) {
            context.getLogger().info("El usuario " + usuarioId + " ya trae rol asignado, se respeta.");
            return;
        }
        String rolPorDefecto = entorno("ROL_POR_DEFECTO", "CONSULTA");
        Optional<Long> rolId = usuarioRol.buscarRolPorNombre(rolPorDefecto);
        if (rolId.isEmpty()) {
            notificaciones.registrar(recibido.id(), "ROL_POR_DEFECTO_NO_DISPONIBLE", entorno("ADMIN_EMAIL", "administracion@duocuc.cl"),
                    "No se pudo asignar el rol por defecto",
                    "El usuario '" + recibido.texto("username") + "' quedo sin rol porque el rol '"
                            + rolPorDefecto + "' no existe.");
            context.getLogger().warning("No existe el rol por defecto " + rolPorDefecto);
            return;
        }
        if (usuarioRol.asignarRolSiNoTiene(usuarioId, rolId.get())) {
            notificaciones.registrar(recibido.id(), "ROL_ASIGNADO", recibido.texto("email"),
                    "Se te asigno un rol",
                    "Hola " + recibido.texto("nombre") + ", se te asigno automaticamente el rol '"
                            + rolPorDefecto + "'.");
            context.getLogger().info("Rol " + rolPorDefecto + " asignado al usuario " + usuarioId);
        } else {
            context.getLogger().info("El usuario " + usuarioId + " ya tenia rol, se ignora el reintento.");
        }
    }

    @FunctionName("quitarRolUsuarios")
    public void quitarRolUsuarios(
            @EventGridTrigger(name = "evento") String evento,
            final ExecutionContext context) throws SQLException {
        EventoRecibido recibido = EventoRecibido.desdeJson(evento);
        if (!recibido.esTipo("RolEliminado")) {
            return;
        }
        String nombreRol = recibido.texto("nombre");
        int cantidad = usuarioRol.quitarRol(recibido.numero("id"));
        String mensaje = "Se elimino el rol '" + nombreRol + "'. " + cantidad + " usuario(s) quedaron sin rol.";
        notificaciones.registrar(recibido.id(), "ROL_QUITADO", entorno("ADMIN_EMAIL", "administracion@duocuc.cl"),
                "Rol eliminado y quitado a sus usuarios", mensaje);
        context.getLogger().info(mensaje);
    }

    @FunctionName("auditarEvento")
    public void auditarEvento(
            @EventGridTrigger(name = "evento") String evento,
            final ExecutionContext context) throws SQLException {
        EventoRecibido recibido = EventoRecibido.desdeJson(evento);
        boolean registrado = auditoria.registrar(recibido);
        context.getLogger().info(registrado
                ? "Auditoria registrada: " + recibido.tipo() + " " + recibido.asunto()
                : "Evento ya auditado, se ignora el reintento: " + recibido.id());
    }

    @FunctionName("notificarUsuario")
    public void notificarUsuario(
            @EventGridTrigger(name = "evento") String evento,
            final ExecutionContext context) throws SQLException {
        EventoRecibido recibido = EventoRecibido.desdeJson(evento);
        String nombre = recibido.texto("nombre");
        String username = recibido.texto("username");
        String email = recibido.texto("email");

        boolean registrada;
        if (recibido.esTipo("UsuarioCreado")) {
            registrada = notificaciones.registrar(recibido.id(), "BIENVENIDA", email,
                    "Bienvenido/a a la plataforma",
                    "Hola " + nombre + ", tu cuenta '" + username + "' fue creada correctamente.");
        } else if (recibido.esTipo("UsuarioActualizado") && "false".equals(recibido.texto("activo"))) {
            registrada = notificaciones.registrar(recibido.id(), "CUENTA_DESACTIVADA", email,
                    "Tu cuenta fue desactivada",
                    "Hola " + nombre + ", tu cuenta '" + username + "' fue desactivada. "
                            + "Si crees que es un error, contacta al administrador.");
        } else if (recibido.esTipo("UsuarioEliminado")) {
            registrada = notificaciones.registrar(recibido.id(), "CUENTA_ELIMINADA", email,
                    "Tu cuenta fue eliminada",
                    "Hola " + nombre + ", tu cuenta '" + username + "' fue eliminada del sistema.");
        } else {
            return;
        }
        context.getLogger().info(registrada
                ? "Notificacion generada para " + email + " (" + recibido.tipo() + ")"
                : "Notificacion ya generada, se ignora el reintento: " + recibido.id());
    }

    private static String entorno(String clave, String valorPorDefecto) {
        String valor = System.getenv(clave);
        return (valor == null || valor.isBlank()) ? valorPorDefecto : valor;
    }
}
