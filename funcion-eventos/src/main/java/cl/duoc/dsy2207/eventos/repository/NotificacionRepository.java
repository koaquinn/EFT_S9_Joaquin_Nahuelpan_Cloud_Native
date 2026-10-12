package cl.duoc.dsy2207.eventos.repository;

import cl.duoc.dsy2207.eventos.db.OracleConnectionFactory;
import cl.duoc.dsy2207.eventos.model.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class NotificacionRepository {

    public boolean registrar(String eventoId, String tipo, String destinatario, String asunto, String mensaje)
            throws SQLException {
        String sql = "INSERT INTO NOTIFICACIONES (EVENTO_ID, TIPO, DESTINATARIO, ASUNTO, MENSAJE) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, eventoId);
            ps.setString(2, tipo);
            ps.setString(3, destinatario);
            ps.setString(4, asunto);
            ps.setString(5, mensaje);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException duplicado) {
            return false;
        }
    }

    public List<Notificacion> listar() throws SQLException {
        String sql = "SELECT ID, EVENTO_ID, TIPO, DESTINATARIO, ASUNTO, MENSAJE, FECHA FROM NOTIFICACIONES ORDER BY ID DESC";
        List<Notificacion> notificaciones = new ArrayList<>();
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                notificaciones.add(new Notificacion(
                        rs.getLong("ID"),
                        rs.getString("EVENTO_ID"),
                        rs.getString("TIPO"),
                        rs.getString("DESTINATARIO"),
                        rs.getString("ASUNTO"),
                        rs.getString("MENSAJE"),
                        rs.getTimestamp("FECHA").toLocalDateTime()));
            }
        }
        return notificaciones;
    }
}
