package com.curso.alsports.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.ItemVenda;
import com.curso.alsports.model.Venda;

@Component
public class VendaMapper {

    public VendaResponse toResponse(Venda venda) {
        List<ItemVendaResponse> itens = venda.getItens().stream()
                .map(this::toItemResponse)
                .toList();

        VendaResponse response = new VendaResponse();

        response.setId(venda.getId());
        response.setClienteId(venda.getCliente().getId());
        response.setClienteNome(venda.getCliente().getPessoa().getNome());
        response.setVendedorId(venda.getVendedor().getId());
        response.setVendedorNome(venda.getVendedor().getPessoa().getNome());
        response.setDataVenda(venda.getDataVenda());
        response.setStatus(venda.getStatus());
        response.setItens(itens);
        response.setTotal(venda.calcularTotal());

        return response;
    }

    public VendaResumoResponse toResumo(Venda venda) {
        VendaResumoResponse response = new VendaResumoResponse();

        response.setId(venda.getId());
        response.setClienteId(venda.getCliente().getId());
        response.setClienteNome(venda.getCliente().getPessoa().getNome());
        response.setVendedorId(venda.getVendedor().getId());
        response.setVendedorNome(venda.getVendedor().getPessoa().getNome());
        response.setDataVenda(venda.getDataVenda());
        response.setStatus(venda.getStatus());

        return response;
    }

    private ItemVendaResponse toItemResponse(ItemVenda item) {
        ItemVendaResponse response = new ItemVendaResponse();

        response.setId(item.getId());
        response.setProdutoId(item.getProduto().getId());
        response.setProdutoDescricao(item.getProduto().getNome());
        response.setQuantidade(item.getQuantidade());
        response.setValorUnitario(item.getValorUnitario());
        response.setSubtotal(item.calcularSubtotal());

        return response;
    }
}
