package org.concessionaria.dao;

import org.concessionaria.exception.PersistenciaException;
import org.concessionaria.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO implements GenericDAO<Cliente> {
    private static final String COLUNAS = "id, nome, cpf, cnh, telefone, email";

    @Override
    public void inserir(Cliente c) {
        String sql = "INSERT INTO cliente (nome, cpf, cnh, telefone, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getCnh());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao inserir cliente.", e);
        }
    }

    @Override
    public void atualizar(Cliente c) {
        String sql = "UPDATE cliente SET nome = ?, cpf = ?, cnh = ?, telefone = ?, email = ? WHERE id = ?";
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getCnh());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEmail());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar cliente.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover cliente.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return buscarUm("SELECT " + COLUNAS + " FROM cliente WHERE id = ?", id);
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        return buscarUm("SELECT " + COLUNAS + " FROM cliente WHERE cpf = ?", cpf);
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement("SELECT " + COLUNAS + " FROM cliente ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs, ""));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar clientes.", e);
        }
        return lista;
    }

    private Optional<Cliente> buscarUm(String sql, Object parametro) {
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, "")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar cliente.", e);
        }
    }

    /** Converte a linha atual do ResultSet em Cliente. {@code p} é o prefixo das colunas (ex.: "c_"). */
    static Cliente mapear(ResultSet rs, String p) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt(p + "id"));
        c.setNome(rs.getString(p + "nome"));
        c.setCpf(rs.getString(p + "cpf"));
        c.setCnh(rs.getString(p + "cnh"));
        c.setTelefone(rs.getString(p + "telefone"));
        c.setEmail(rs.getString(p + "email"));
        return c;
    }
}
