# Refúgio of Glory - Diretrizes do Claude Code

## 1. Memória e Contexto (Prioridade Máxima)
* **Início da Sessão:** Leia obrigatoriamente o arquivo de memória local (`MEMORIA.md` ou `PROGRESSO.md`). Absorva o estado atual e as regras de negócio antes de agir.
* **Fim da Sessão/Marcos:** Atualize (ou sugira a atualização de) `MEMORIA.md` com o progresso exato e os próximos passos imediatos.
* **Isolamento:** Ignore a ferramenta MCP do Obsidian. Foque exclusivamente no desenvolvimento local e nos arquivos da raiz deste projeto.

## 2. Microsserviços e Regras de RPG
* **Estrutura:** O ecossistema é dividido em `auth-service`, `character-service` e `combat-service`.
* **Consistência:** Antes de alterar códigos de atributos ou lógica de RPG, valide se as modificações alinham-se perfeitamente com as notas locais de design de RPG.

## 3. Padrões de Código e Comunicação
* **Stack:** Respeite a arquitetura padrão de cada serviço (ex: Java/Spring Boot, Node.js).
* **Idioma:** Comunique-se no chat em português do Brasil (pt-BR).
* **Estilo de Resposta:** Seja direto, técnico e sem introduções ou conclusões prolixas ("Direto ao ponto"), sem passar de 6 linhas a não ser que seja pedido uma explicação maior.
* **Ferramentas:** Use estritamente comandos nativos do sistema (`read_file`, `write_file`, `edit_file`) para manipular notas e códigos.

## 4. Regras de Desenvolvimento do Front-end
* **Estrutura:** O ecossistema é dividido em `telas` que estão na pasta `java`.
* **Codigo:** Mantenha o fluxo das telas da maneira que está.
* **Assets:** Caso precise de algum asset, busque na pasta `resources`.

