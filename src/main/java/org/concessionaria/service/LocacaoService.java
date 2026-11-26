package org.concessionaria.service;

import org.concessionaria.dao.LocacaoDAO;
import org.concessionaria.dao.VeiculoDAO;
import org.concessionaria.exception.NegocioException;
import org.concessionaria.exception.ValidacaoException;
import org.concessionaria.model.Cliente;
import org.concessionaria.model.Locacao;
import org.concessionaria.model.ResumoFechamento;
import org.concessionaria.model.StatusLocacao;
import org.concessionaria.model.StatusVeiculo;
import org.concessionaria.model.Veiculo;
import org.concessionaria.util.Formatos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Regras de negócio das locações. A camada de visão só chama este serviço:
 * nenhum cálculo de valores é feito nos controllers.
 */
public class LocacaoService {
    public static final int MAXIMO_DIAS = 90;
    private static final int MAXIMO_DIAS_RETROATIVO = 60;

    private final LocacaoDAO locacaoDAO;
    private final VeiculoDAO veiculoDAO;

    public LocacaoService(LocacaoDAO locacaoDAO, VeiculoDAO veiculoDAO) {
        this.locacaoDAO = locacaoDAO;
        this.veiculoDAO = veiculoDAO;
    }

    public List<Locacao> listar() {
        return locacaoDAO.listarTodas();
    }

    /** Abre uma locação; veículo e locação são gravados de forma atômica. */
    public Locacao abrir(Cliente cliente, Veiculo veiculo, LocalDate retirada, int dias) {
        if (cliente == null) {
            throw new ValidacaoException("Selecione o cliente.");
        }
        if (veiculo == null) {
            throw new ValidacaoException("Selecione o veículo.");
        }
        validarPeriodo(retirada, dias, true);

        // Relê o veículo do banco: a tela pode estar desatualizada.
        Veiculo atual = veiculoDAO.buscarPorId(veiculo.getId())
                .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
        if (atual.getStatus() != StatusVeiculo.DISPONIVEL) {
            throw new NegocioException("O veículo " + atual.getPlaca() + " não está disponível.");
        }

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setVeiculo(atual);
        locacao.setDataRetirada(retirada);
        locacao.setDataPrevistaDevolucao(retirada.plusDays(dias));
        locacao.setValorDiariaContratada(atual.getValorDiaria());
        locacao.setStatus(StatusLocacao.ABERTA);

        if (!locacaoDAO.registrarAbertura(locacao)) {
            throw new NegocioException("O veículo " + atual.getPlaca()
                    + " acabou de ser locado por outro atendimento. Atualize a lista.");
        }
        return locacao;
    }

    /**
     * Altera cliente, data de retirada e prazo de uma locação em aberto.
     * O veículo não muda (para trocá-lo, cancele a locação e abra outra).
     */
    public void editar(Locacao locacao, Cliente cliente, LocalDate retirada, int dias) {
        exigirLocacaoAberta(locacao, "editadas");
        if (cliente == null) {
            throw new ValidacaoException("Selecione o cliente.");
        }
        // O limite retroativo só vale se a data de retirada foi realmente alterada:
        // contratos antigos ainda em aberto continuam editáveis.
        boolean retiradaAlterada = retirada == null || !retirada.equals(locacao.getDataRetirada());
        validarPeriodo(retirada, dias, retiradaAlterada);

        Locacao alterada = new Locacao();
        alterada.setId(locacao.getId());
        alterada.setCliente(cliente);
        alterada.setVeiculo(locacao.getVeiculo());
        alterada.setDataRetirada(retirada);
        alterada.setDataPrevistaDevolucao(retirada.plusDays(dias));
        alterada.setValorDiariaContratada(locacao.getValorDiariaContratada());
        alterada.setStatus(locacao.getStatus());

        if (!locacaoDAO.atualizarAberta(alterada)) {
            throw new NegocioException("Esta locação não está mais em aberto. Atualize a lista.");
        }
        locacao.setCliente(alterada.getCliente());
        locacao.setDataRetirada(alterada.getDataRetirada());
        locacao.setDataPrevistaDevolucao(alterada.getDataPrevistaDevolucao());
    }

    /**
     * Cancela uma locação em aberto: remove o contrato e libera o veículo (atômico).
     * Contratos encerrados não podem ser removidos, pois compõem o histórico financeiro.
     */
    public void cancelar(Locacao locacao) {
        exigirLocacaoAberta(locacao, "canceladas");
        if (!locacaoDAO.registrarCancelamento(locacao)) {
            throw new NegocioException("Esta locação não está mais em aberto. Atualize a lista.");
        }
    }

    /** Valor estimado para um veículo e uma quantidade de dias (usa a regra polimórfica da categoria). */
    public BigDecimal estimarValor(Veiculo veiculo, int dias) {
        if (veiculo == null) {
            throw new ValidacaoException("Selecione o veículo.");
        }
        return veiculo.calcularValorBase(dias);
    }

    /** Valor estimado de uma locação, considerando o prazo previsto e a diária contratada. */
    public BigDecimal estimarValor(Locacao locacao) {
        return locacao.calcularValorEstimado();
    }

    /** Calcula o fechamento sem gravar nada (usado para a prévia na tela). */
    public ResumoFechamento simularFechamento(Locacao locacao, LocalDate devolucao) {
        validarDevolucao(locacao, devolucao);
        return locacao.calcularResumo(devolucao);
    }

    /** Resumo de uma locação já encerrada (para reemitir o relatório). */
    public ResumoFechamento resumoDeLocacaoEncerrada(Locacao locacao) {
        if (locacao == null || locacao.getStatus() != StatusLocacao.ENCERRADA || locacao.getDataDevolucao() == null) {
            throw new NegocioException("Selecione uma locação encerrada.");
        }
        return locacao.calcularResumo(locacao.getDataDevolucao());
    }

    /** Encerra a locação e libera o veículo; ambos gravados de forma atômica. */
    public ResumoFechamento encerrar(Locacao locacao, LocalDate devolucao) {
        validarDevolucao(locacao, devolucao);
        ResumoFechamento resumo = locacao.calcularResumo(devolucao);

        locacao.setDataDevolucao(devolucao);
        locacao.setValorTotal(resumo.total());
        if (!locacaoDAO.registrarEncerramento(locacao)) {
            locacao.setDataDevolucao(null);
            locacao.setValorTotal(null);
            throw new NegocioException("Esta locação já foi encerrada ou está inconsistente. Atualize a lista.");
        }
        locacao.setStatus(StatusLocacao.ENCERRADA);
        return resumo;
    }

    private void exigirLocacaoAberta(Locacao locacao, String acaoPlural) {
        if (locacao == null || locacao.getId() == null) {
            throw new NegocioException("Selecione uma locação.");
        }
        if (locacao.getStatus() != StatusLocacao.ABERTA) {
            throw new NegocioException("Somente locações em aberto podem ser " + acaoPlural
                    + ". Contratos encerrados fazem parte do histórico financeiro.");
        }
    }

    private void validarPeriodo(LocalDate retirada, int dias, boolean limitarRetroativo) {
        if (retirada == null) {
            throw new ValidacaoException("Informe a data de retirada.");
        }
        if (retirada.isAfter(LocalDate.now())) {
            throw new ValidacaoException("A data de retirada não pode ser futura.");
        }
        if (limitarRetroativo && retirada.isBefore(LocalDate.now().minusDays(MAXIMO_DIAS_RETROATIVO))) {
            throw new ValidacaoException("A data de retirada não pode ser anterior a "
                    + MAXIMO_DIAS_RETROATIVO + " dias.");
        }
        if (dias < 1 || dias > MAXIMO_DIAS) {
            throw new ValidacaoException("A quantidade de dias deve estar entre 1 e " + MAXIMO_DIAS + ".");
        }
    }

    private void validarDevolucao(Locacao locacao, LocalDate devolucao) {
        if (locacao == null || locacao.getStatus() != StatusLocacao.ABERTA) {
            throw new NegocioException("Selecione uma locação em aberto.");
        }
        if (devolucao == null) {
            throw new ValidacaoException("Informe a data de devolução.");
        }
        if (devolucao.isBefore(locacao.getDataRetirada())) {
            throw new ValidacaoException("A devolução não pode ser anterior à retirada ("
                    + Formatos.data(locacao.getDataRetirada()) + ").");
        }
        if (devolucao.isAfter(LocalDate.now())) {
            throw new ValidacaoException("A devolução não pode ser uma data futura.");
        }
    }
}
