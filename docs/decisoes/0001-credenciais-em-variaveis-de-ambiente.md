# 0001. Credenciais em variáveis de ambiente

Status: aceita · Incremento 1.1 · 2026-11-03

## Contexto
A senha do banco estava escrita no `application.properties` e commitada. Em um repositório público, isso é uma credencial exposta.

## Decisão
O `application.properties` lê `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Em desenvolvimento, um arquivo `.env` local (ignorado pelo Git, com modelo em `.env.example`) é lido via `spring.config.import=optional:file:../.env[.properties]`. Variáveis de ambiente reais têm prioridade sobre o arquivo. A senha antiga continua no histórico do Git e deve ser tratada como comprometida.

## Alternativas consideradas
- Só variáveis de ambiente, sem `.env`: mais trabalhoso de configurar no dia a dia.
- `spring-boot-docker-compose`: reavaliada no incremento 1.2 e descartada. Acrescenta "mágica" (sobe o banco sozinho) e tem a mesma dependência de caminho do `.env`. Mantive o `spring.config.import`: é simples, está documentado e o erro é claro.
- Reescrever o histórico para apagar a senha: é uma mudança destrutiva e não resolve cópias já feitas. Fica como decisão separada.

## Consequências
- Nenhum segredo no código novo; o repositório traz só valores fictícios.
- Pior: o `.env` é procurado em `../.env`, então a aplicação precisa ser iniciada de dentro de `Gerenciador de fichas RPG`. Sem as variáveis, o erro é `URL must start with 'jdbc'`.
- Pior: a senha antiga ainda está no histórico; a proteção real é trocá-la onde ela foi usada.
