-- Rota Segura - carga inicial de exemplo (dados fictícios).
-- Executada automaticamente apenas quando o arquivo do banco é criado pela primeira vez.
-- Não use ponto e vírgula dentro de textos: o script é separado por ';'.

INSERT INTO cliente (nome, cpf, cnh, telefone, email) VALUES
    ('Maria da Silva',  '52998224725', '12345678901', '14999990001', 'maria.silva@email.com'),
    ('João Pereira',    '11144477735', '98765432100', '14999990002', 'joao.pereira@email.com'),
    ('Ana Souza',       '39053344705', '45612378900', '14999990003', NULL);

INSERT INTO veiculo (placa, marca, modelo, ano, categoria, valor_diaria, status) VALUES
    ('ABC1D23', 'Fiat',       'Argo',    2024, 'POPULAR', 120.00, 'DISPONIVEL'),
    ('DEF2E34', 'Chevrolet',  'Onix',    2023, 'POPULAR', 115.00, 'DISPONIVEL'),
    ('GHI3F45', 'Toyota',     'Corolla', 2024, 'SEDAN',   220.00, 'DISPONIVEL'),
    ('JKL4G56', 'Honda',      'Civic',   2023, 'SEDAN',   240.00, 'DISPONIVEL'),
    ('MNO5H67', 'Jeep',       'Compass', 2024, 'SUV',     320.00, 'DISPONIVEL'),
    ('PQR6I78', 'Hyundai',    'Creta',   2023, 'SUV',     290.00, 'DISPONIVEL');
