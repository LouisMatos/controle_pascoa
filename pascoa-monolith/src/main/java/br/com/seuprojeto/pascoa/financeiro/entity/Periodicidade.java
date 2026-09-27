package br.com.seuprojeto.pascoa.financeiro.entity;

public enum Periodicidade {
    MENSAL,
    TRIMESTRAL,   // a cada 3 meses → valor/3 por mês
    SEMESTRAL,    // a cada 6 meses → valor/6 por mês
    ANUAL         // uma vez por ano → valor/12 por mês
}
