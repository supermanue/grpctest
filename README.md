# ABLY TEST

## Test & Execution

Testing:

```bash
./sbt test
```

Executing server:

```bash
./sbt "runMain com.manuel.ably.app.MessageServer"

#or with a different port
./sbt "runMain com.manuel.ably.app.MessageServer portNumber"
```

Executing client

```bash 
./sbt "runMain com.manuel.ably.app.Client"

#or with the desired number of messages
./sbt "runMain com.manuel.ably.app.Client numberOfMessages"
```

## Limitations

Because of having a limited time available to do this home assignment, I have cut some corners and made a few hacks.
They are all annotated in the code with `TODO`. They are trivial things that don't affect the root of the problem to be
solved.

Also, I didn't see clearly what to do with the customer Ids. I would have asked Paul Quinn about it, but I have
implemented this on my weekend and I wanted to wrap it up, so maybe the behavior is not the requested one. I have
followed an approach where a client Id can only be used once, so I'm storing them forever. I don't know if this is
correct, but I don't think it affects the final result too much so I didn't worry about it.

### Testing

I have implemented some basic testing. It does not really cover all the possible scenarios and corner cases but it's
enough for a first approach. If I wanted to create more robust tests I would use something like Generators for the input
types and Property testing for the `tools`. In case you are interested I wrote a couple articles about it in my personal
blog,
[Building useful Scalacheck Generators](https://medium.com/@supermanue/building-useful-scalacheck-generators-71635d1edb9d)
and [Property testing of Isomorphisms: way easier than it sounds!](https://medium.com/@supermanue/property-testing-of-isomorphisms-way-easier-than-it-sounds-a646791b9c5f)
. Sorry for the SPAM :)

## Architecture

I have tried to create a simple app with a small number of files and layers.

### Client

The client is written in a single file. The code is not particularly beautiful but serves its purpose.

### Server

The server is structured as follows:

- `app`: this is the app logic
    - `MessageServer`: it receives the GRPC connections and sends them to `MessageServiceImpl`. It also takes care
      of the wiring. Note that I have used dependency injection with constructor parameters, as this project is simple
      enough no not need anything more complex.
    - `MessageServiceImpl`: it processes the received input for the GRPC call, executes the work using `domain` services
      and returns the result to the client calling.
- `domain`: this is the business logic
    - `model`: files modelling the business objects. This includes the possible errors. I am using
      the `functional errors`
      approach, so in the case of problems I'm not just throwing exceptions but modelling them as business objects. In a
      more complex architecture I would use `Either` but in this case sending them as `Future.failed` is enough.
    - `service`: files modelling the business logic.
    - `port`: IO files. They include a thin layer of logic, so the interfaces return a high-level functionality for
      storage. It could be argued that this logic should go into `service`, but I think that having it clear provides
      abstractions that are easy to reason about
    - `tools`: I created this `toolbox` with functions to be used both by client and server. In a more complex
      architecture this would be a library imported by both.
- `adapter.service`: implementation of the `ports`. Note that here we are only providing concrete instances of the
  caches being used in the ports. I'm using [ScalaCache](https://cb372.github.io/scalacache/) which has implementations
  for all the standard caches (including Redis) and I'm using `Guava` for the implementation here.

### Testing

The test structure maps the `server` one. I just want to show a couple particular ones:

- `MessageServerSpec`: it includes tests for testing concurrent connections
- `MessageStreamServiceTest`: tests the business logic using mocks for the caches.

### Scalability

I haven't tested my system for scalability.

In the server, I feel that the problems can come because of having a big number of messages requested, as in the server
I am generating a list and keeping it in memory. This will surely lead to memory issues with a combination of multiple
requests + large number of messages per request. Also I haven't limited the number of concurrent clients, so it can end
up with an error. This can be set up with some configuration parameters, the used framework (AKKA HTTP) has
out-of-the-box support for that.

In the client, the current implementation can lead to a stack overflow if the server is down for too long, as I am
performing a non-final recursive call. This is definitely a hack and I'm aware of it, but it's nearly 20h on a Sunday
and I cannot invest much more time on this project. I'm sorry for that. At least I'm pointing at the issue and the
solution is fairly simple, I hope that's something  `¯\_(ツ)_/¯`.