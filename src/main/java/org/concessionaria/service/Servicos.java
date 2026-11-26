package org.concessionaria.service;

import org.concessionaria.dao.ClienteDAO;
import org.concessionaria.dao.LocacaoDAO;
import org.concessionaria.dao.VeiculoDAO;

/**
 * Ponto único de montagem das dependências (DAO -> Service).
 * As telas (pacote gui) só conhecem os serviços, nunca os DAOs nem SQL.
 */
public final class Servicos {
    private static final ClienteDAO CLIENTE_DAO = new ClienteDAO();
    private static final VeiculoDAO VEICULO_DAO = new VeiculoDAO();
    private static final LocacaoDAO LOCACAO_DAO = new LocacaoDAO();

    private static final ClienteService CLIENTES = new ClienteService(CLIENTE_DAO, LOCACAO_DAO);
    private static final VeiculoService VEICULOS = new VeiculoService(VEICULO_DAO, LOCACAO_DAO);
    private static final LocacaoService LOCACOES = new LocacaoService(LOCACAO_DAO, VEICULO_DAO);
    private static final RelatorioService RELATORIOS = new RelatorioService();

    private Servicos() {}

    public static ClienteService clientes() { return CLIENTES; }
    public static VeiculoService veiculos() { return VEICULOS; }
    public static LocacaoService locacoes() { return LOCACOES; }
    public static RelatorioService relatorios() { return RELATORIOS; }
}
