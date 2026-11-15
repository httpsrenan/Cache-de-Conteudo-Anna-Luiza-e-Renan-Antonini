# Cache de Conteúdo com ConcurrentHashMap (Spring Boot)

**Autores:** Anna Luiza e Renan Antonini — 2CCOB, 6º semestre

API REST em Spring Boot que serve conteúdos usando um **cache próprio, construído à mão** com `ConcurrentHashMap`, sem `@Cacheable`, Caffeine, Redis ou qualquer biblioteca de cache. A "fonte lenta" é simulada com um atraso artificial de 2 segundos, para que a diferença entre **HIT** e **MISS** fique visível.

## Endpoints

| Método | Rota                  | Comportamento                                                                  |
|--------|-----------------------|--------------------------------------------------------------------------------|
| GET    | `/conteudos/{id}`     | Busca no cache; em MISS busca na fonte e armazena. Header `X-Cache: HIT` ou `MISS` |
| POST   | `/conteudos`          | Cadastra um conteúdo (`201 Created`)                                           |
| PUT    | `/conteudos/{id}`     | Atualiza o conteúdo e invalida a entrada no cache                              |
| DELETE | `/conteudos/{id}`     | Exclui o conteúdo e invalida a entrada no cache                                |
| GET    | `/cache/estatisticas` | Tamanho atual, limite, hits, misses, evictions e hit ratio                     |
| DELETE | `/cache`              | Limpa todo o cache (as estatísticas acumuladas são mantidas)                   |

Corpo do POST/PUT:

```json
{ "titulo": "Conteúdo 1", "corpo": "Texto do conteúdo 1", "autor": "Anna" }
```

Exemplo de resposta de `GET /cache/estatisticas`:

```json
{ "tamanhoAtual": 5, "limite": 5, "hits": 4, "misses": 13, "evictions": 2, "hitRatio": 0.2352 }
```

## Como o cache funciona

### Estrutura

O componente `ConteudoCache` (`@Component`) guarda os dados em um `Map<Long, EntradaCache>` implementado por `ConcurrentHashMap`. Cada `EntradaCache` contém:

- o `Conteudo`;
- `armazenadoEm`: o momento em que entrou no cache (usado pelo TTL);
- `ultimoAcesso`: atualizado a cada HIT (usado pelo LRU).

### Fluxo de uma busca (`GET /conteudos/{id}`)

1. A controller chama o service, que chama `cache.obterOuCarregar(id, fonte)`.
2. Se existe entrada **e** ela não expirou: **HIT**. O `ultimoAcesso` é atualizado e o conteúdo é devolvido em poucos milissegundos.
3. Se não existe, ou expirou: **MISS**. O conteúdo é buscado na fonte (com o atraso de 2 s), armazenado no cache e devolvido.
4. A controller devolve o header `X-Cache` conforme o resultado.

### Expiração (TTL)

Se uma entrada está no cache há mais tempo que `app.cache.ttl-segundos`, ela é removida e a busca é tratada como MISS: o conteúdo é buscado de novo na fonte e o cache é atualizado. A expiração **não** conta como eviction.

### Remoção por limite (LRU)

Quando o cache está cheio (`app.cache.max-itens`) e um item novo precisa entrar, é removida a entrada com o **`ultimoAcesso` mais antigo** (menos usada recentemente). Cada remoção por limite incrementa o contador de `evictions`.

### Invalidação

`PUT` e `DELETE` removem do cache a entrada do conteúdo alterado, para que a próxima leitura nunca devolva um dado desatualizado.

### Estatísticas

Contadores `AtomicLong` de hits, misses e evictions. O hit ratio é `hits / (hits + misses)`.

### Concorrência

- O mapa é um `ConcurrentHashMap`, então leituras simultâneas não precisam de lock.
- A inserção é `synchronized`, para que "checar tamanho, remover o LRU e inserir" seja uma operação atômica e o limite nunca seja ultrapassado.
- A busca lenta na fonte acontece fora do lock. Se duas requisições pedirem o mesmo id ao mesmo tempo, ambas podem ir à fonte, o que é aceitável nesta versão "light".

### Logs

Cada busca gera um log (SLF4J) com o resultado, o id e o tempo da operação:

```
INFO  ConteudoCache : Cache MISS - id=3 - 2015 ms
INFO  ConteudoCache : Cache HIT  - id=3 - 0 ms
INFO  ConteudoCache : Cache EVICT - id=2 (limite de 5 itens atingido)
INFO  ConteudoCache : Cache EXPIRED - id=1 (TTL de 60 s)
INFO  ConteudoCache : Cache INVALIDATE - id=3
```

## Organização do código

```
src/main/java/com/exemplo/conteudos
├── controller   ConteudoController, CacheController
├── service      ConteudoService (usa o cache; recebe e retorna entidade)
├── cache        ConteudoCache, EntradaCache, ResultadoBusca, EstatisticasCache
├── mapper       ConteudoMapper (conversão entidade <-> DTO)
├── dto          ConteudoRequest, ConteudoResponse
├── model        Conteudo (entidade JPA)
├── repository   ConteudoRepository
└── exception    ConteudoNaoEncontradoException, ApiExceptionHandler
```

## Demonstração

Os prints da demonstração (Insomnia e log da aplicação) estão em [prints da demonstração.pdf](prints%20da%20demonstra%C3%A7%C3%A3o.pdf), cobrindo:

1. Cadastro de conteúdos (POST)
2. Primeira busca (MISS, ~2 s) e segunda busca (HIT, ~0 ms)
3. Expiração por TTL
4. Invalidação após PUT
5. Remoção LRU com limite de 5 itens
6. Estatísticas finais

### Roteiro para reproduzir (limite = 5)

1. Cadastre 7 conteúdos com `POST /conteudos` (ids 1 a 7).
2. **MISS x HIT:** `GET /conteudos/1` duas vezes.
3. **LRU:** `GET` nos ids 2, 3, 4 e 5 (cache cheio), `GET` no 1 (HIT, renova o acesso) e `GET` no 6. O log mostra `EVICT - id=2`. Um novo `GET` no 2 é MISS.
4. **TTL:** busque um id, espere mais que o TTL (ou suba com `--app.cache.ttl-segundos=10`) e busque de novo: `EXPIRED` seguido de MISS.
5. **Invalidação:** `GET /conteudos/3` (HIT), `PUT /conteudos/3`, `GET /conteudos/3` (MISS com o conteúdo atualizado).
6. `GET /cache/estatisticas` ao final.
