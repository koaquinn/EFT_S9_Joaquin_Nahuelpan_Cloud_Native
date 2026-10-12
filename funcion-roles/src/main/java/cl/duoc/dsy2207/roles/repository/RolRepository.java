package cl.duoc.dsy2207.roles.repository;

import cl.duoc.dsy2207.roles.db.OracleConnectionFactory;
import cl.duoc.dsy2207.roles.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RolRepository {
    public List<Rol> listar() throws SQLException {
        String sql = "SELECT ID, NOMBRE, DESCRIPCION FROM ROLES ORDER BY ID";
        List<Rol> roles = new ArrayList<>();
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapear(rs));
            }
        }
        return roles;
    }

    public Optional<Rol> buscarPorId(long id) throws SQLException {
        String sql = "SELECT ID, NOMBRE, DESCRIPCION FROM ROLES WHERE ID = ?";
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

    public Rol crear(Rol rol) throws SQLException {
        String sql = "INSERT INTO ROLES (NOMBRE, DESCRIPCION) VALUES (?, ?)";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID"})) {
            ps.setString(1, rol.getNombre());
            ps.setString(2, rol.getDescripcion());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    rol.setId(claves.getLong(1));
                }
            }
        }
        return rol;
    }

    public boolean actualizar(long id, Rol rol) throws SQLException {
        String sql = "UPDATE ROLES SET NOMBRE = ?, DESCRIPCION = ? WHERE ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, rol.getNombre());
            ps.setString(2, rol.getDescripcion());
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(long id) throws SQLException {
        String sql = "DELETE FROM ROLES WHERE ID = ?";
        try (Connection cn = OracleConnectionFactory.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol r = new Rol();
        r.setId(rs.getLong("ID"));
        r.setNombre(rs.getString("NOMBRE"));
        r.setDescripcion(rs.getString("DESCRIPCION"));
        return r;
    }
}
