package com.curso.alsports.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.ItemVenda;
import com.curso.alsports.model.Produto;
import com.curso.alsports.model.StatusVenda;
import com.curso.alsports.model.Venda;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.ColaboradorRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.repository.VendaRepository;
import com.curso.alsports.repository.VendaSpecification;

@Service
public class VendaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "dataVenda",
            "status");

    private final VendaRepository repository;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ProdutoRepository produtoRepository;

    public VendaService(
            VendaRepository repository,
            ClienteRepository clienteRepository,
            ColaboradorRepository colaboradorRepository,
            ProdutoRepository produtoRepository) {

        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Venda cadastrar(Long clienteId, Long vendedorId) {
        Cliente cliente = buscarCliente(clienteId);
        Colaborador vendedor = buscarVendedor(vendedorId);

        validarParticipantes(cliente, vendedor);

        return repository.save(
                new Venda(cliente, vendedor, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Venda buscarPorId(Long id) {
        return repository.findOneById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Venda não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Venda> pesquisar(
            StatusVenda status,
            Long clienteId,
            LocalDate dataInicial,
            LocalDate dataFinal,
            Pageable pageable) {

        validarOrdenacao(pageable);
        validarPeriodo(dataInicial, dataFinal);

        return repository.findAll(
                VendaSpecification.comFiltros(
                        status, clienteId, dataInicial, dataFinal),
                pageable);
    }

    @Transactional
    public Venda adicionarItem(
            Long vendaId,
            Long produtoId,
            BigDecimal quantidade) {

        Venda venda = buscarEntidade(vendaId);

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + produtoId));

        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException(
                    "Produto deve estar ativo para ser incluído na venda");
        }

        venda.adicionarItem(produto, quantidade);

        return repository.saveAndFlush(venda);
    }

    @Transactional
    public Venda removerItem(Long vendaId, Long itemId) {
        Venda venda = buscarEntidade(vendaId);
        venda.removerItem(itemId);

        return repository.saveAndFlush(venda);
    }

    @Transactional
    public Venda finalizar(Long id) {
        Venda venda = buscarEntidade(id);

        validarParticipantes(venda.getCliente(), venda.getVendedor());

        for (ItemVenda item : venda.getItens()) {
            Produto produto = item.getProduto();

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new IllegalArgumentException(
                        "Todos os produtos da venda devem estar ativos");
            }

            produto.retirarEstoque(item.getQuantidade());
        }

        venda.finalizar();

        return repository.saveAndFlush(venda);
    }

    @Transactional
    public Venda cancelar(Long id) {
        Venda venda = buscarEntidade(id);
        venda.cancelar();

        return repository.saveAndFlush(venda);
    }

    private Venda buscarEntidade(Long id) {
        return repository.findOneById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Venda não encontrada: " + id));
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado: " + clienteId));
    }

    private Colaborador buscarVendedor(Long vendedorId) {
        return colaboradorRepository.findById(vendedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Colaborador não encontrado: " + vendedorId));
    }

    private void validarParticipantes(Cliente cliente, Colaborador vendedor) {
        if (cliente == null || cliente.getPessoa() == null) {
            throw new IllegalArgumentException("Cliente é obrigatório");
        }

        if (!cliente.getPessoa().isAtiva()) {
            throw new IllegalArgumentException(
                    "Cliente deve estar ativo para participar da venda");
        }

        if (vendedor == null || vendedor.getPessoa() == null) {
            throw new IllegalArgumentException("Vendedor é obrigatório");
        }

        if (!vendedor.getPessoa().isAtiva()) {
            throw new IllegalArgumentException(
                    "Vendedor deve estar ativo para participar da venda");
        }

        if (!vendedor.podeRealizarVenda()) {
            throw new IllegalArgumentException(
                    "Somente colaboradores VENDEDOR ou GERENTE podem realizar vendas");
        }
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial != null && dataFinal != null
                && dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final");
        }
    }

    private void validarOrdenacao(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "A paginação deve ser informada");
        }

        pageable.getSort().forEach(ordem -> {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new IllegalArgumentException(
                        "Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
    }
}
