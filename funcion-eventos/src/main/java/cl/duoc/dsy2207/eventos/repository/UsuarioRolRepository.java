package cl.duoc.dsy2207.eventos.repository;

import cl.duoc.dsy2207.eventos.db.OracleConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioRolRepository {

    public Optional<Long> buscarRolPorNombre(String nombre) throws SQLException {
        String sql = "SELECT ID FROM ROLES WHERE NOMBRE = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("ID")) : Optional.empty();
            }
        }
    }

    public boolean asignarRolSiNoTiene(long usuarioId, long rolId) throws SQLException {
        String sql = "UPDATE USUARIOS SET ROL_ID = ? WHERE ID = ? AND ROL_ID IS NULL";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, rolId);
            ps.setLong(2, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }

    public int quitarRol(long rolId) throws SQLException {
        String sql = "UPDATE USUARIOS SET ROL_ID = NULL WHERE ROL_ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, rolId);
            return ps.executeUpdate();
        }
    }
}
