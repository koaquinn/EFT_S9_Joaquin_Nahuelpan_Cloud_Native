package cl.duoc.dsy2207.eventos.repository;

import cl.duoc.dsy2207.eventos.db.OracleConnectionFactory;
import cl.duoc.dsy2207.eventos.model.EventoRecibido;
import cl.duoc.dsy2207.eventos.model.RegistroAuditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    public boolean registrar(EventoRecibido evento) throws SQLException {
        String sql = "INSERT INTO AUDITORIA_EVENTOS (EVENTO_ID, TIPO_EVENTO, ASUNTO, DATOS, FECHA_EVENTO) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, evento.id());
            ps.setString(2, evento.tipo());
            ps.setString(3, evento.asunto());
            ps.setString(4, evento.datos().toString());
            ps.setTimestamp(5, Timestamp.valueOf(evento.fecha()));
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException duplicado) {
            return false;
        }
    }

    public List<RegistroAuditoria> listar(String tipo) throws SQLException {
        String sql = "SELECT ID, EVENTO_ID, TIPO_EVENTO, ASUNTO, DATOS, FECHA_EVENTO, FECHA_REGISTRO "
                + "FROM AUDITORIA_EVENTOS "
                + (tipo == null ? "" : "WHERE TIPO_EVENTO LIKE ? ")
                + "ORDER BY ID DESC";
        List<RegistroAuditoria> registros = new ArrayList<>();
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (tipo != null) {
                ps.setString(1, "%" + tipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroAuditoria(
                            rs.getLong("ID"),
                            rs.getString("EVENTO_ID"),
                            rs.getString("TIPO_EVENTO"),
                            rs.getString("ASUNTO"),
                            rs.getString("DATOS"),
                            rs.getTimestamp("FECHA_EVENTO").toLocalDateTime(),
                            rs.getTimestamp("FECHA_REGISTRO").toLocalDateTime()));
                }
            }
        }
        return registros;
    }
}
