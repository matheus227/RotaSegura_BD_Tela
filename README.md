# Rota Segura — Locadora de Veículos (TP2: GUI + Persistência)

Aplicação desktop (JavaFX) para o atendimento de balcão da locadora **Rota Segura**, com banco de dados
relacional **local** (SQLite, um único arquivo) acessado pelo padrão **DAO**.

---

## 1. Requisitos de ambiente

| Item | Versão usada no desenvolvimento | Observação |
|---|---|---|
| **JDK** | 26 | Definido em `maven.compiler.source/target` no `pom.xml`. |
| **Maven** | 3.9+ | Baixa JavaFX e o driver SQLite automaticamente. |
| **JavaFX** | 27.0.1 | Vem pelo Maven (`javafx.version` no `pom.xml`); não precisa instalar o SDK. |
| **Banco de dados** | SQLite (driver `sqlite-jdbc`) | **Nada a instalar**: o banco é um arquivo local criado sozinho. |
| **Internet** | Só na 1ª compilação | Para o Maven baixar as dependências. Depois funciona offline. |

Para usar outra versão do JDK/JavaFX, altere `maven.compiler.source`, `maven.compiler.target` e
`javafx.version` no `pom.xml` (o código usa apenas recursos de linguagem do Java 21 ou anteriores).

## 2. Como compilar

```bash
mvn clean compile
```

## 3. Como executar

**Pelo terminal (recomendado):**

```bash
mvn javafx:run
```

**Pelo IntelliJ IDEA:** abra o `pom.xml` como projeto Maven, aguarde o download das dependências e execute
a classe `org.concessionaria.Launcher` (ou `org.concessionaria.App`).

> Execute sempre a partir da **pasta raiz do projeto**: o banco é criado em `./data/rota_segura.db`,
> relativo ao diretório de execução.

## 4. Banco de dados local

- Arquivo: `data/rota_segura.db` (criado automaticamente na primeira execução).
- Outro local: `mvn javafx:run -Drotasegura.db=C:/caminho/meu.db` (ou `-Drotasegura.db=...` nos VM options do IntelliJ).
- **Estrutura** (`src/main/resources/db/schema.sql`): tabelas `cliente`, `veiculo` (hierarquia
  Popular/Sedan/SUV em tabela única, discriminada pela coluna `categoria`) e `locacao` (histórico),
  com chaves estrangeiras, `CHECK`s e um índice único parcial que impede dois contratos abertos para o mesmo veículo.
- **Carga inicial** (`src/main/resources/db/dados_iniciais.sql`): 3 clientes e 6 veículos de exemplo
  (2 de cada categoria), inseridos **apenas quando o arquivo do banco é criado pela primeira vez**.
- Os dois scripts rodam automaticamente na inicialização (também podem ser usados à mão em qualquer cliente SQLite).
- **Para reiniciar do zero**: feche o sistema e apague `data/rota_segura.db`.
sqlite3 data\rota_segura.db
.tables
SELECT * FROM veiculo;
.quit

## 5. Funcionalidades (mapa do enunciado)

| Requisito do TP2 | Onde está |
|---|---|
| Cadastro, edição, consulta e listagem (tabelas) de **Clientes** | Aba *Clientes* — formulário, busca por nome/CPF, grid, excluir. |
| ... de **Veículos** (Popular, Sedan, SUV) | Aba *Veículos* — categoria define a regra de cobrança; filtro por texto e categoria. |
| ... de **Locações** | Aba *Locações* — **abrir**, **editar** (cliente/retirada/prazo), **cancelar** (somente contratos em aberto), listar e filtrar. |
| Validação na interface sem encerramento abrupto | Campos restringem a digitação (`Mascaras`); dados inválidos geram caixas de diálogo (`Alertas`); erros inesperados também viram diálogo (tratador global em `App`). |
| Fechamento de contrato com **polimorfismo visual** | Botão *Encerrar / fechar contrato*: a prévia se recalcula ao mudar a data de devolução, usando a regra da categoria do veículo; confirma, encerra e permite **salvar o relatório (.txt)**. |
| Persistência relacional via **DAO** | Pacote `dao` (`ClienteDAO`, `VeiculoDAO`, `LocacaoDAO`, contrato `GenericDAO`). |
| **Integridade/transacionalidade** do status do veículo | Abrir, encerrar e cancelar gravam a locação **e** o status do veículo na mesma transação (`LocacaoDAO`), com atualização condicional (`WHERE status = ...`) contra concorrência. |
| **Desacoplamento em camadas** | Veja abaixo. |

## 6. Arquitetura em camadas

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Visão (GUI) | `gui` + `resources/view/*.fxml` | Telas, eventos e diálogos. **Sem SQL e sem regra/cálculo de negócio** — só chama os serviços. |
| Regras de negócio | `service` | Validações, regras de locação, relatório. A GUI obtém estimativas e resumos aqui. |
| Model | `model` | Entidades e regras polimórficas de cobrança (`Veiculo` → `Popular` / `Sedan` / `SUV`). |
| Persistência | `dao` + `resources/db/*.sql` | JDBC/SQL (padrão DAO). Todo SQL fica aqui. |
| Transversais | `exception`, `util` | `NegocioException`, `ValidacaoException`, `PersistenciaException`; formatação e validadores. |

Fluxo de exceções: `SQLException` → `PersistenciaException` (DAO) → regras violadas viram `NegocioException`/`ValidacaoException`
(service) → a GUI as traduz em diálogos (`Alertas.tratar`).

## 7. Regras de cobrança (editáveis em `model/`)

- **Popular**: diária × dias; 10% de desconto a partir de 7 dias; multa de 20% da diária por dia de atraso.
- **Sedan**: diária × dias + 10% de taxa de conforto; multa de 30% da diária por dia de atraso.
- **SUV**: (diária + R$ 40,00 de seguro) × dias; multa de 50% da diária por dia de atraso.

Dias utilizados = dias entre retirada e devolução (mínimo 1). Atraso = dias após a data prevista.

## 8. Regras de integridade adicionais

- A **diária é congelada** na locação: alterar o preço do veículo depois não muda contratos existentes.
- Veículo **locado** não pode ser editado nem excluído; veículo ou cliente **com histórico** não podem ser excluídos.
- A **categoria** de um veículo com histórico não pode ser alterada (mudaria a regra de cobrança de contratos já fechados).
- Contratos **encerrados** não podem ser editados nem removidos (histórico financeiro).
- Cancelar uma locação em aberto remove o contrato e devolve o veículo para *Disponível* de forma atômica.

## 9. Problemas comuns

- *"Erro de banco de dados" ao iniciar*: verifique permissão de escrita na pasta `data/` (ou no caminho de `-Drotasegura.db`).
- *`Error: JavaFX runtime components are missing`* ao rodar pelo IDE: execute `Launcher` em vez de `App`, ou use `mvn javafx:run`.
- *Versão de JDK incompatível*: ajuste os valores do `pom.xml` conforme a seção 1.
