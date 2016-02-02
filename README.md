# jtier-ctx

A `Ctx` is a request context, but we wanted a shorter word than `RequestContext` and `Context` by itself is too overloaded. A `Ctx` is primarily used to propagate context from an incoming request to outgoing requests, or various libraries such as logging or metrics, in an RPC exchange. For example, an HTTP request comes in with an `X-Request-Id` header, which should be propagated to all clients, added to a mapped diagnostic context for logs associated with RPC exchange.

A `Ctx` (henceforth referred to as a context) has objects, usually data, associated with it via a typed `Key<T>`. This is the mechanism for propagating context within a process (say from incoming request to outgoing request). It will have some stndard mechanism to convert it to a map or such to put it into headers. Individual context objects are immutable, with associating a key creating a new context rather than mutating an existing one. This is designed to encourage thread safety and being explicit about passing them around.

A context can be associated with a thread. This process is called infection because it is _not_ simply associating the current context with the thread, but associating all future mutations of contexts that happen on that thread with the thread (until the infection is cured). The infection process is designed for tunneling context through context-unaware libraries or code, it is a fallback, not a preferred means.

Contexts have limited lifecycle support. Basically, they are `ALIVE`, `CANCELLED`, or `FINISHED`. A context starts `ALIVE` and can transition to either `CANCELLED` or `FINISHED`. You can register listeners with a context and those listeners will be notified when the context transitions state.

There is deadline functionality associated with a context -- basically you can set a deadline, on a scheduled executor, which will cause the state to transition to `CANCELLED` if the deadline occurs before the context transitions to `FINISHED`. You can query the approximate time remaining until the deadline from the context (approximate because it is measured in nanoseconds, and some will elapse querying the time and reading it).

Finally, a context is hierarchical. You can create a child context of a context and the children will share associated data (from the time of their creation) and lifecycle transitions (at any point) with the parent. A child can undergo lifecycle transitions independently of the parent, but a parent transitioning will also transition all children. This feature is to allow seperate contexts to be created to track child RPCs, or other independently cancellable activies, but be able to cancel or finish them from the root. 

# Why Conflate?

`Ctx` conflates three things right now, contextual data (request id, etc), request lifecycle handling, and timeout management. It would seem that this might be two too many things.

They key thing is that lifecycle state (and triggers on that state change) and timeout management (and triggers on that state change( *are* contextual data. It would be very possible to refactor these two things into plugin type pieces of active data, and possibly this should happen, except these are two things needed on *every* request exchange, so putting them on directly is simply an affordance to make things easier for users.

# Infection, huh?

There are two primary paths for propagating context, the first is purely explicit. In this model anything which needs context data must have it explicitely passed. The second is implicit via thread locals -- in this anything which needs context data pulls it from global thread locals.

Explicit propagation is generally preferred -- be explicit in what you need! Explicit propagation also allows for child contexts, and finer grained control over context within a thread (such as in event driven situations). However, explicit propagation can make many libraries and frameworks, which don't have baked in support for explicit context passing, awkward to use. In practice, we need some mechanism for implicit context passing, even if we prefer explicit propagation.

To resolve this dichotomy we have the infection concept. With infection, a thread is *infected* with context, once infected the last modified context becomes the context for the thread, and that context can be retrieved from a global thread local. Infection basically says "the last context set, or changed, on this thread is the context for the next thing, unless it is given one explicitely passed. 

Infection *is* a hack to allow implicit propagation that is right for 90% of the cases. If this behavior is wrong, explicit state propagation should be used. If this is impossible, a different global thread local can be created and the specific context instance needed can be set on it, though if you find yourself doing this there is something *very* strange with the approach.

# Using `Ctx`

