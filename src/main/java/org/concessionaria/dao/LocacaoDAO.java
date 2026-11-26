package org.concessionaria.dao;

import org.concessionaria.exception.PersistenciaException;
import org.concessionaria.model.Locacao;
import org.concessionaria.model.StatusLocacao;
import org.concessionaria.model.StatusVeiculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de locações. As operações que mudam o status do veículo (abertura e encerramento)
 * rodam em UMA transação: ou grava a locação E o novo status, ou não grava nada.
 */
public class LocacaoDAO {
    private static final String SELECT_COMPLETO = """
            SELECT l.id, l.data_retirada, l.data_prevista_devolucao, l.data_devolucao,
                   l.valor_diaria, l.valor_total, l.status,
                   c.id AS c_id, c.nome AS c_nome, c.cpf AS c_cpf, c.cnh AS c_cnh,
                   c.telefone AS c_telefone, c.email AS c_email,
                   v.id AS v_id, v.placa AS v_placa, v.marca AS v_marca, v.modelo AS v_modelo,
                   v.ano AS v_ano, v.categoria AS v_categoria, v.valor_diaria AS v_valor_diaria,
                   v.status AS v_status
              FROM locacao l
              JOIN cliente c ON c.id = l.cliente_id
              JOIN veiculo v ON v.id = l.veiculo_id
            """;

    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    /**
     * Abre a locação: marca o veículo como LOCADO (somente se ainda estiver DISPONIVEL) e insere a locação.
     *
     * @return false se o veículo já não estava disponível (nada é gravado)
     */
    public boolean registrarAbertura(Locacao l) {
        String sql = "INSERT INTO locacao (cliente_id, veiculo_id, data_retirada, data_prevista_devolucao, "
                + "valor_diaria, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoFactory.obter()) {
            con.setAutoCommit(false);
            try {
                boolean reservado = veiculoDAO.atualizarStatusSe(
                        con, l.getVeiculo().getId(), StatusVeiculo.LOCADO, StatusVeiculo.DISPONIVEL);
                if (!reservado) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, l.getCliente().getId());
                    ps.setInt(2, l.getVeiculo().getId());
                    ps.setString(3, l.getDataRetirada().toString());
                    ps.setString(4, l.getDataPrevistaDevolucao().toString());
                    ps.setDouble(5, l.getValorDiariaContratada().doubleValue());
                    ps.setString(6, StatusLocacao.ABERTA.name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            l.setId(keys.getInt(1));
                        }
                    }
                }
                con.commit();
                l.getVeiculo().setStatus(StatusVeiculo.LOCADO);
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao abrir a locação.", e);
        }
    }

    /**
     * Encerra a locação (somente se ainda ABERTA) e devolve o veículo para DISPONIVEL, na mesma transação.
     *
     * @return false se a locação já não estava aberta (nada é gravado)
     */
    public boolean registrarEncerramento(Locacao l) {
        String sql = "UPDATE locacao SET data_devolucao = ?, valor_total = ?, status = ? "
                + "WHERE id = ? AND status = 'ABERTA'";
        try (Connection con = ConexaoFactory.obter()) {
            con.setAutoCommit(false);
            try {
                int linhas;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, l.getDataDevolucao().toString());
                    ps.setDouble(2, l.getValorTotal().doubleValue());
                    ps.setString(3, StatusLocacao.ENCERRADA.name());
                    ps.setInt(4, l.getId());
                    linhas = ps.executeUpdate();
                }
                if (linhas != 1) {
                    con.rollback();
                    return false;
                }
                boolean liberado = veiculoDAO.atualizarStatusSe(
                        con, l.getVeiculo().getId(), StatusVeiculo.DISPONIVEL, StatusVeiculo.LOCADO);
                if (!liberado) {
                    con.rollback();
                    return false;
                }
                con.commit();
                l.getVeiculo().setStatus(StatusVeiculo.DISPONIVEL);
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao encerrar a locação.", e);
        }
    }

    /**
     * Altera cliente e período de uma locação ainda ABERTA (o veículo e a diária contratada não mudam).
     *
     * @return false se a locação já não estava aberta (nada é gravado)
     */
    public boolean atualizarAberta(Locacao l) {
        String sql = "UPDATE locacao SET cliente_id = ?, data_retirada = ?, data_prevista_devolucao = ? "
                + "WHERE id = ? AND status = 'ABERTA'";
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, l.getCliente().getId());
            ps.setString(2, l.getDataRetirada().toString());
            ps.setString(3, l.getDataPrevistaDevolucao().toString());
            ps.setInt(4, l.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar a locação.", e);
        }
    }

    /**
     * Cancela (remove) uma locação ABERTA e devolve o veículo para DISPONIVEL, na mesma transação.
     * Locações encerradas nunca são removidas: fazem parte do histórico financeiro.
     *
     * @return false se a locação já não estava aberta (nada é gravado)
     */
    public boolean registrarCancelamento(Locacao l) {
        String sql = "DELETE FROM locacao WHERE id = ? AND status = 'ABERTA'";
        try (Connection con = ConexaoFactory.obter()) {
            con.setAutoCommit(false);
            try {
                int linhas;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, l.getId());
                    linhas = ps.executeUpdate();
                }
                if (linhas != 1) {
                    con.rollback();
                    return false;
                }
                boolean liberado = veiculoDAO.atualizarStatusSe(
                        con, l.getVeiculo().getId(), StatusVeiculo.DISPONIVEL, StatusVeiculo.LOCADO);
                if (!liberado) {
                    con.rollback();
                    return false;
                }
                con.commit();
                l.getVeiculo().setStatus(StatusVeiculo.DISPONIVEL);
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao cancelar a locação.", e);
        }
    }

    public Optional<Locacao> buscarPorId(int id) {
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + " WHERE l.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar locação.", e);
        }
    }

    public List<Locacao> listarTodas() {
        List<Locacao> lista = new ArrayList<>();
        try (Connection con = ConexaoFactory.obter();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + " ORDER BY l.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar locações.", e);
        }
        return lista;
    }

    public int contarPorCliente(int clienteId) {
        return contar("SELECT COUNT(*) FROM locacao WHERE cliente_id = ?", clienteId);
    }

    public int contarPorVeiculo(int veiculoId) {
        return contar("SELECT COUNT(*) FROM locacao WHERE veiculo_id = ?", veiculoId);
    }

    private int contar(String sql, int id) {
        try (Connection con = ConexaoFactory.obter(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao consultar locações.", e);
        }
    }

    private Locacao mapear(ResultSet rs) throws SQLException {
        Locacao l = new Locacao();
        l.setId(rs.getInt("id"));
        l.setCliente(ClienteDAO.mapear(rs, "c_"));
        l.setVeiculo(VeiculoDAO.mapear(rs, "v_"));
        l.setDataRetirada(LocalDate.parse(rs.getString("data_retirada")));
        l.setDataPrevistaDevolucao(LocalDate.parse(rs.getString("data_prevista_devolucao")));
        String devolucao = rs.getString("data_devolucao");
        l.setDataDevolucao(devolucao == null ? null : LocalDate.parse(devolucao));
        l.setValorDiariaContratada(BigDecimal.valueOf(rs.getDouble("valor_diaria")).setScale(2, RoundingMode.HALF_UP));
        double total = rs.getDouble("valor_total");
        l.setValorTotal(rs.wasNull() ? null : BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP));
        l.setStatus(StatusLocacao.valueOf(rs.getString("status")));
        return l;
    }
}
