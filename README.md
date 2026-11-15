# API de Conteúdos com cache próprio (ConcurrentHashMap)

## Endpoints
| Método | Rota                  | Observação                                   |
|--------|-----------------------|----------------------------------------------|
| GET    | /conteudos/{id}       | header `X-Cache: HIT` ou `MISS`              |
| POST   | /conteudos            | cadastra                                     |
| PUT    | /conteudos/{id}       | atualiza e invalida a entrada                |
| DELETE | /conteudos/{id}       | exclui e invalida a entrada                  |
| GET    | /cache/estatisticas   | tamanho, limite, hits, misses, evictions, hitRatio |
| DELETE | /cache                | limpa o cache (estatísticas são mantidas)    |

Corpo do POST/PUT:
    { "titulo": "Conteúdo 1", "corpo": "Texto...", "autor": "Anna" }

## Roteiro de demonstração (limite = 5)
1. Cadastre 7 conteúdos (POST x7) -> ids 1..7.
2. **MISS x HIT**: GET /conteudos/1 (~2000 ms, MISS) e de novo (poucos ms, HIT).
3. **LRU**: GET 2, 3, 4, 5 (cache cheio: 1..5). GET 1 (HIT, renova o acesso de 1).
   GET 6 -> remove o menos usado recentemente (id=2) -> log `Cache EVICT - id=2`.
   GET 2 -> MISS (foi removido).
4. **TTL**: para não esperar 60 s, suba com `--app.cache.ttl-segundos=10`
   (ou espere 60 s). GET /conteudos/N, aguarde, GET de novo -> `Cache EXPIRED` + MISS.
5. **Invalidação**: GET /conteudos/3 (HIT) -> PUT /conteudos/3 -> log `Cache INVALIDATE - id=3`
   -> GET /conteudos/3 -> MISS com o conteúdo atualizado.
6. GET /cache/estatisticas ao final.
