package org.concessionaria.service;

import org.concessionaria.dao.ClienteDAO;
import org.concessionaria.dao.LocacaoDAO;
import org.concessionaria.exception.NegocioException;
import org.concessionaria.exception.ValidacaoException;
import org.concessionaria.model.Cliente;
import org.concessionaria.util.Validador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteService {
    private final ClienteDAO clienteDAO;
    private final LocacaoDAO locacaoDAO;

    public ClienteService(ClienteDAO clienteDAO, LocacaoDAO locacaoDAO) {
        this.clienteDAO = clienteDAO;
        this.locacaoDAO = locacaoDAO;
    }

    public List<Cliente> listar() {
        return clienteDAO.listarTodos();
    }

    /** Valida, normaliza e grava (insere se id == null, senão atualiza). */
    public void salvar(Cliente c) {
        normalizar(c);
        validar(c);

        Optional<Cliente> existente = clienteDAO.buscarPorCpf(c.getCpf());
        if (existente.isPresent() && !existente.get().getId().equals(c.getId())) {
            throw new ValidacaoException("Já existe um cliente cadastrado com este CPF.");
        }

        if (c.getId() == null) {
            clienteDAO.inserir(c);
        } else {
            clienteDAO.atualizar(c);
        }
    }

    public void remover(Cliente c) {
        if (c == null || c.getId() == null) {
            throw new NegocioException("Selecione um cliente para excluir.");
        }
        if (locacaoDAO.contarPorCliente(c.getId()) > 0) {
            throw new NegocioException("Este cliente possui locações registradas e não pode ser excluído.");
        }
        clienteDAO.remover(c.getId());
    }

    private void normalizar(Cliente c) {
        c.setNome(c.getNome() == null ? "" : c.getNome().trim());
        c.setCpf(Validador.somenteDigitos(c.getCpf()));
        c.setCnh(Validador.somenteDigitos(c.getCnh()));
        c.setTelefone(Validador.somenteDigitos(c.getTelefone()));
        c.setEmail(c.getEmail() == null ? "" : c.getEmail().trim());
    }

    private void validar(Cliente c) {
        List<String> erros = new ArrayList<>();
        if (c.getNome().length() < 3) {
            erros.add("• Informe o nome completo (mínimo 3 caracteres).");
        }
        // Dados fictícios são aceitos: valida só a quantidade de dígitos, não o dígito verificador.
        if (c.getCpf().length() != 11) {
            erros.add("• CPF deve conter 11 dígitos.");
        }
        if (c.getCnh().length() != 11) {
            erros.add("• CNH deve conter 11 dígitos.");
        }
        if (!c.getTelefone().isEmpty() && c.getTelefone().length() != 10 && c.getTelefone().length() != 11) {
            erros.add("• Telefone deve ter DDD + 8 ou 9 dígitos.");
        }
        // E-mail é opcional e não tem validação de formato (dados fictícios).
        if (!erros.isEmpty()) {
            throw new ValidacaoException(String.join("\n", erros));
        }
    }
}