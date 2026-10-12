package cl.duoc.dsy2207.usuarios.repository;

import cl.duoc.dsy2207.usuarios.db.OracleConnectionFactory;
import cl.duoc.dsy2207.usuarios.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    public List<Usuario> listar() throws SQLException {
        String sql = "SELECT ID, NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID FROM USUARIOS ORDER BY ID";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        }
        return usuarios;
    }

    public Optional<Usuario> buscarPorId(long id) throws SQLException {
        String sql = "SELECT ID, NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID FROM USUARIOS WHERE ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Usuario crear(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO USUARIOS (NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID"})) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsername());
            ps.setString(3, usuario.getEmail());
            ps.setInt(4, Boolean.FALSE.equals(usuario.getActivo()) ? 0 : 1);
            if (usuario.getRolId() != null) {
                ps.setLong(5, usuario.getRolId());
            } else {
                ps.setNull(5, java.sql.Types.NUMERIC);
            }
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    usuario.setId(claves.getLong(1));
                }
            }
        }
        return usuario;
    }

    public boolean actualizar(long id, Usuario usuario) throws SQLException {
        String sql = "UPDATE USUARIOS SET NOMBRE = ?, USERNAME = ?, EMAIL = ?, ACTIVO = ?, ROL_ID = ? WHERE ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsername());
            ps.setString(3, usuario.getEmail());
            ps.setInt(4, Boolean.FALSE.equals(usuario.getActivo()) ? 0 : 1);
            if (usuario.getRolId() != null) {
                ps.setLong(5, usuario.getRolId());
            } else {
                ps.setNull(5, java.sql.Types.NUMERIC);
            }
            ps.setLong(6, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(long id) throws SQLException {
        String sql = "DELETE FROM USUARIOS WHERE ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("ID"));
        u.setNombre(rs.getString("NOMBRE"));
        u.setUsername(rs.getString("USERNAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setActivo(rs.getInt("ACTIVO") == 1);
        long rolId = rs.getLong("ROL_ID");
        u.setRolId(rs.wasNull() ? null : rolId);
        return u;
    }
}
