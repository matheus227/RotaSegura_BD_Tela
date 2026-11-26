package org.concessionaria.service;

import org.concessionaria.dao.LocacaoDAO;
import org.concessionaria.dao.VeiculoDAO;
import org.concessionaria.exception.NegocioException;
import org.concessionaria.exception.ValidacaoException;
import org.concessionaria.model.StatusVeiculo;
import org.concessionaria.model.Veiculo;
import org.concessionaria.util.Validador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VeiculoService {
    private final VeiculoDAO veiculoDAO;
    private final LocacaoDAO locacaoDAO;

    public VeiculoService(VeiculoDAO veiculoDAO, LocacaoDAO locacaoDAO) {
        this.veiculoDAO = veiculoDAO;
        this.locacaoDAO = locacaoDAO;
    }

    public List<Veiculo> listar() {
        return veiculoDAO.listarTodos();
    }

    public List<Veiculo> listarDisponiveis() {
        return veiculoDAO.listarDisponiveis();
    }

    public void salvar(Veiculo v) {
        v.setPlaca(Validador.normalizarPlaca(v.getPlaca()));
        v.setMarca(v.getMarca() == null ? "" : v.getMarca().trim());
        v.setModelo(v.getModelo() == null ? "" : v.getModelo().trim());
        validar(v);
        v.setValorDiaria(v.getValorDiaria().setScale(2, RoundingMode.HALF_UP));

        Optional<Veiculo> mesmaPlaca = veiculoDAO.buscarPorPlaca(v.getPlaca());
        if (mesmaPlaca.isPresent() && !mesmaPlaca.get().getId().equals(v.getId())) {
            throw new ValidacaoException("Já existe um veículo cadastrado com esta placa.");
        }

        if (v.getId() == null) {
            v.setStatus(StatusVeiculo.DISPONIVEL);
            veiculoDAO.inserir(v);
        } else {
            Veiculo atual = veiculoDAO.buscarPorId(v.getId())
                    .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
            if (atual.getStatus() == StatusVeiculo.LOCADO) {
                throw new NegocioException("Veículo locado não pode ser alterado. Encerre a locação primeiro.");
            }
            // Trocar a categoria muda a regra de cobrança; com histórico, reescreveria contratos já fechados.
            if (atual.getCategoria() != v.getCategoria() && locacaoDAO.contarPorVeiculo(v.getId()) > 0) {
                throw new NegocioException("A categoria não pode ser alterada em um veículo com histórico de "
                        + "locações, pois mudaria o cálculo dos contratos anteriores.");
            }
            veiculoDAO.atualizar(v);
        }
    }

    public void remover(Veiculo v) {
        if (v == null || v.getId() == null) {
            throw new NegocioException("Selecione um veículo para excluir.");
        }
        if (v.getStatus() == StatusVeiculo.LOCADO) {
            throw new NegocioException("Veículo locado não pode ser excluído.");
        }
        if (locacaoDAO.contarPorVeiculo(v.getId()) > 0) {
            throw new NegocioException("Este veículo possui histórico de locações e não pode ser excluído.");
        }
        veiculoDAO.remover(v.getId());
    }

    private void validar(Veiculo v) {
        List<String> erros = new ArrayList<>();
        if (!Validador.placaValida(v.getPlaca())) {
            erros.add("• Placa inválida (use ABC1234 ou ABC1D23).");
        }
        if (v.getMarca().isEmpty()) {
            erros.add("• Informe a marca.");
        }
        if (v.getModelo().isEmpty()) {
            erros.add("• Informe o modelo.");
        }
        int anoMaximo = Year.now().getValue() + 1;
        if (v.getAno() < 1990 || v.getAno() > anoMaximo) {
            erros.add("• Ano deve estar entre 1990 e " + anoMaximo + ".");
        }
        if (v.getValorDiaria() == null || v.getValorDiaria().compareTo(BigDecimal.ZERO) <= 0) {
            erros.add("• A diária deve ser maior que zero.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(String.join("\n", erros));
        }
    }
}
