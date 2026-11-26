package org.concessionaria.dao;

import org.concessionaria.exception.PersistenciaException;
import org.concessionaria.model.CategoriaVeiculo;
import org.concessionaria.model.StatusVeiculo;
import org.concessionaria.model.Veiculo;
import org.concessionaria.model.VeiculoFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VeiculoDAO implements GenericDAO<Veiculo> {
    private static final String COLUNAS = "id, placa, marca, modelo, ano, categoria, valor_diaria, status";

    @Override
    public void inserir(Veiculo v) {
        String sql = "INSERT INTO veiculo (placa, marca, modelo, ano, categoria, valor_diaria, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getPlaca());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setInt(4, v.getAno());
            ps.setString(5, v.getCategoria().name());
            ps.setDouble(6, v.getValorDiaria().doubleValue());
            ps.setString(7, v.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    v.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao inserir veículo.", e);
        }
    }

    /** Atualiza os dados cadastrais. O status NÃO é alterado aqui: só pelas operações de locação. */
    @Override
    public void atualizar(Veiculo v) {
        String sql = "UPDATE veiculo SET placa = ?, marca = ?, modelo = ?, ano = ?, categoria = ?, "
                + "valor_diaria = ? WHERE id = ?";
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getPlaca());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setInt(4, v.getAno());
            ps.setString(5, v.getCategoria().name());
            ps.setDouble(6, v.getValorDiaria().doubleValue());
            ps.setInt(7, v.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar veículo.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover veículo.", e);
        }
    }

    @Override
    public Optional<Veiculo> buscarPorId(int id) {
        return buscarUm("SELECT " + COLUNAS + " FROM veiculo WHERE id = ?", id);
    }

    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return buscarUm("SELECT " + COLUNAS + " FROM veiculo WHERE placa = ?", placa);
    }

    @Override
    public List<Veiculo> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM veiculo ORDER BY marca, modelo");
    }

    public List<Veiculo> listarDisponiveis() {
        return listar("SELECT " + COLUNAS + " FROM veiculo WHERE status = 'DISPONIVEL' ORDER BY marca, modelo");
    }

    /**
     * Troca o status SOMENTE se o status atual for o esperado (atualização condicional).
     * Participa da transação da conexão recebida.
     *
     * @return true se exatamente uma linha foi alterada
     */
    boolean atualizarStatusSe(Connection con, int id, StatusVeiculo novo, StatusVeiculo esperado) throws SQLException {
        String sql = "UPDATE veiculo SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novo.name());
            ps.setInt(2, id);
            ps.setString(3, esperado.name());
            return ps.executeUpdate() == 1;
        }
    }

    private List<Veiculo> listar(String sql) {
        List<Veiculo> lista = new ArrayList<>();
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs, ""));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar veículos.", e);
        }
        return lista;
    }

    private Optional<Veiculo> buscarUm(String sql, Object parametro) {
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, "")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar veículo.", e);
        }
    }

    static Veiculo mapear(ResultSet rs, String p) throws SQLException {
        CategoriaVeiculo categoria = CategoriaVeiculo.valueOf(rs.getString(p + "categoria"));
        Veiculo v = VeiculoFactory.criar(categoria);
        v.setId(rs.getInt(p + "id"));
        v.setPlaca(rs.getString(p + "placa"));
        v.setMarca(rs.getString(p + "marca"));
        v.setModelo(rs.getString(p + "modelo"));
        v.setAno(rs.getInt(p + "ano"));
        v.setValorDiaria(BigDecimal.valueOf(rs.getDouble(p + "valor_diaria")).setScale(2, RoundingMode.HALF_UP));
        v.setStatus(StatusVeiculo.valueOf(rs.getString(p + "status")));
        return v;
    }
}
