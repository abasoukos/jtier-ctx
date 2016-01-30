# jtier-ding

A Ding is a request context, but we wanted a shorter word than `RequestContext`. Dings are primarily used to propagate context from an incoming request to outgoing requests in an RPC exchange -- for example, an HTTP request comes in with an `X-Request-Id` header, which should be propagated to all clients, added to a mapped diagnostic context for log 
