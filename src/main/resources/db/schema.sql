-- Rota Segura - esquema relacional (SQLite)
-- Executado automaticamente na inicialização (CREATE ... IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS cliente (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nome      TEXT NOT NULL,
    cpf       TEXT NOT NULL UNIQUE,
    cnh       TEXT NOT NULL,
    telefone  TEXT,
    email     TEXT
);

-- Hierarquia de veículos (Popular, Sedan, SUV) em tabela única, discriminada por "categoria".
CREATE TABLE IF NOT EXISTS veiculo (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    placa        TEXT NOT NULL UNIQUE,
    marca        TEXT NOT NULL,
    modelo       TEXT NOT NULL,
    ano          INTEGER NOT NULL,
    categoria    TEXT NOT NULL CHECK (categoria IN ('POPULAR', 'SEDAN', 'SUV')),
    valor_diaria REAL NOT NULL CHECK (valor_diaria > 0),
    status       TEXT NOT NULL DEFAULT 'DISPONIVEL' CHECK (status IN ('DISPONIVEL', 'LOCADO'))
);

CREATE TABLE IF NOT EXISTS locacao (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id              INTEGER NOT NULL REFERENCES cliente (id),
    veiculo_id              INTEGER NOT NULL REFERENCES veiculo (id),
    data_retirada           TEXT NOT NULL,
    data_prevista_devolucao TEXT NOT NULL,
    data_devolucao          TEXT,
    valor_diaria            REAL NOT NULL,
    valor_total             REAL,
    status                  TEXT NOT NULL CHECK (status IN ('ABERTA', 'ENCERRADA'))
);

CREATE INDEX IF NOT EXISTS ix_locacao_cliente ON locacao (cliente_id);
CREATE INDEX IF NOT EXISTS ix_locacao_veiculo ON locacao (veiculo_id);

-- Garante no próprio banco que um veículo nunca tenha duas locações abertas ao mesmo tempo.
CREATE UNIQUE INDEX IF NOT EXISTS ux_locacao_veiculo_aberta
    ON locacao (veiculo_id) WHERE status = 'ABERTA';
