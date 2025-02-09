O erro ocorre porque o caminho **/Users/pedrosantos/Documents/workspace/saga-orchestration/data/kafka/data** não está compartilhado com o Docker no seu Mac. Isso impede que o Docker monte esse diretório como volume no contêiner **Kafka**.

### Como Resolver

1. **Abrir as configurações do Docker Desktop**
    - Vá em **Docker Desktop** → **Settings** → **Resources** → **File Sharing**.
    - Clique no botão **+** e adicione o caminho:
      ```
      /Users/pedrosantos/Documents/workspace/saga-orchestration/data/kafka/data
      ```
    - Clique em **Apply & Restart**.

2. **Criar o diretório manualmente**  
   Caso ele não exista, crie-o antes de rodar o `docker-compose`:
   ```sh
   mkdir -p /Users/pedrosantos/Documents/workspace/saga-orchestration/data/kafka/data
   ```

3. **Tentar rodar o `docker-compose` novamente**
   ```sh
   docker-compose up
   ```

Isso deve resolver o problema e permitir que o Kafka utilize o volume corretamente. 🚀