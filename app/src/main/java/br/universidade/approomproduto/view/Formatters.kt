package br.universidade.approomproduto.view

import java.util.Locale

/** Formata um valor como moeda brasileira, ex.: R$ 149,90 */
fun formatarMoeda(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "R\$ %.2f", valor)
