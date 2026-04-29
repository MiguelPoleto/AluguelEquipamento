# 🔧 Sistema de Aluguel de Equipamentos

Sistema desktop desenvolvido em Java com JavaFX para gerenciamento de aluguel de equipamentos, incluindo cadastro de clientes, controle de reservas, retiradas, devoluções e manutenções.

---

## 🛠️ Tecnologias utilizadas

- **Java 17+**
- **JavaFX** — interface gráfica
- **Maven** — gerenciamento de dependências
- **MySQL** — banco de dados relacional
- **JDBC** — conexão com o banco via DAO

---

## 📁 Estrutura do projeto

```
aluguel-equipamento/
├── src/main/java/br/com/aluguelequipamento/
│   ├── App.java                  # Ponto de entrada da aplicação
│   ├── controller/               # Controladores das telas (MVC)
│   ├── model/
│   │   ├── dao/                  # Acesso ao banco de dados
│   │   └── domain/               # Entidades do sistema
└── src/main/resources/
    └── br/com/aluguelequipamento/
        └── view/                 # Arquivos FXML (telas)
```

---

## 🗂️ Entidades

| Entidade       | Descrição                                  |
|----------------|--------------------------------------------|
| `Cliente`      | Dados dos clientes cadastrados             |
| `Equipamento`  | Equipamentos disponíveis para aluguel      |
| `Reserva`      | Reservas realizadas pelos clientes         |
| `Retirada`     | Registro de retirada de equipamentos       |
| `Devolucao`    | Registro de devolução de equipamentos      |
| `Manutencao`   | Histórico de manutenções dos equipamentos  |

---

## ⚙️ Como executar

### Pré-requisitos

- Java 17 ou superior instalado
- Maven instalado
- MySQL rodando localmente

### Configurar o banco de dados

1. Acesse o MySQL e execute o script localizado em `scriptSQL/`
2. Ajuste as credenciais de conexão em `ConexaoDAO.java`

### Rodar o projeto

```bash
# Clonar o repositório
git clone https://github.com/seu-usuario/aluguel-equipamento.git

# Entrar na pasta
cd aluguel-equipamento

# Compilar e executar
mvn clean javafx:run
```

---

## 📌 Padrão de arquitetura

O projeto segue o padrão **MVC (Model-View-Controller)** com camada **DAO**:

- **Model** → entidades (`domain/`) e acesso a dados (`dao/`)
- **View** → telas em FXML (`resources/view/`)
- **Controller** → lógica de interface (`controller/`)

---

## 📄 Licença

Este projeto é de uso acadêmico/educacional.