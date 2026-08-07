# cdi-flow in a Quarkus application, end to end

A CRUD application — Quarkus backend, Angular front-end, one process — whose use-cases are driven
through a real browser while it records a sequence diagram of each of them.

```bash
mvn -f ../../pom.xml clean install -DskipTests   # once - see below

./run.sh                      # build, drive the use-cases, and say where the diagrams are
./run.sh --format plantuml    # the same flows as .puml instead
./run.sh --no-title           # without the use-case as the diagram's title
open target/flow-diagrams/use-cases.md
```

That is the whole thing. The script builds, runs the suite and prints a path; everything about
recording is in the addon.

**The one-off first**: this example is built standalone rather than as part of the reactor, so it
resolves `cdi-flow-quarkus` from your local repository - and the addon is not released anywhere, so
nothing puts it there but a build of this repository. Skip that install and the run fails on an
unresolvable dependency before it ever starts.

Besides a JDK and Maven, `./run.sh` needs **Node.js with pnpm** and the Playwright browsers it
drives.

## What the application does for it

**One dependency**, in [`pom.xml`](pom.xml):

```xml
<dependency>
    <groupId>org.os890.cdi.uml</groupId>
    <artifactId>cdi-flow-quarkus</artifactId>
</dependency>
```

**Two lines of configuration**, in [`application.properties`](src/main/resources/application.properties):

```properties
cdi-flow.enabled=true
cdi-flow.output-directory=target/flow-diagrams
```

`enabled` is needed because this is a *packaged* application — a production build records nothing
unless it is told to. In dev-mode and in tests the extension needs neither line.

**Nothing in the sources.** No annotation, no interceptor, no startup hook, no include-pattern: the
extension attaches the recorder to the beans of this application while Quarkus builds it, and arms
it when the application starts.

## What the test-suite does for it

One fixture, [`e2e/tests/flow.ts`](e2e/tests/flow.ts) — every request of a test carries the name of
that test:

```ts
export const test = base.extend({
  context: async ({ context }, use, testInfo) => {
    await context.setExtraHTTPHeaders({ 'X-Flow-Label': testInfo.title });
    await use(context);
  },
});
```

The addon's request-filter reads that header, and everything recorded while the request is handled is
filed under that use-case. A test may add a `description` annotation, which ends up above its diagram
in the generated document.

One application serves the whole suite: there is no restart between use-cases, and no output
directory to juggle.

## What comes out

```
target/flow-diagrams/
├── use-cases.md                        every use-case, described, with its diagram inline
├── a-customer-is-created-with-tags/
│   ├── use-case.mmd                    the whole use-case: one block per request
│   ├── README.md                       the chains it is made of, and how often each occurred
│   └── CustomerResource_create_….mmd   one file per distinct chain
├── a-customer-without-a-name-is-refused/
├── a-customer-is-edited/
└── a-customer-is-deleted/
```

The beans are arranged to show what the recorder can do:

| In the diagram | Where it comes from |
|---|---|
| a nested chain, four levels deep | `CustomerResource` → `CustomerService` → the beans it calls |
| `loop 3 times` | `TagNormalizer`, called once per tag |
| `-)` with `[event]` | `AuditObserver`, observing the synchronous `CustomerCreated` event |
| `--x throws BusinessRuleException` | a customer without a name, refused by `CustomerValidation` |
| several blocks in one diagram | a use-case which lists, creates and then updates |

## The recorded diagrams

Copied out of `target/flow-diagrams/` as they were written - nothing here is hand-drawn, and only the timings differ from run to run.

### a customer is created with tags

The service validates the customer, normalizes each tag, stores it, asks for a customer-number and fires an event - which is where the audit observer joins the chain. The list before it is the front-end loading the table. 3 requests, one block each.

```mermaid
---
title: "a customer is created with tags"
---
sequenceDiagram
    autonumber
    participant Caller as caller
    participant CustomerResource
    participant CustomerService
    participant CustomerRepository
    participant CustomerValidation
    participant TagNormalizer
    participant CustomerNumbers
    participant AuditObserver
    participant AuditLog
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.80 ms | thread executor-thread-1
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.20 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.38 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.80 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,AuditLog: CustomerResource.create — 3.68 ms | thread executor-thread-2
        Caller->>CustomerResource: create(Customer)
        activate CustomerResource
            CustomerResource->>CustomerService: create(Customer)
            activate CustomerService
                CustomerService->>CustomerValidation: check(Customer)
                activate CustomerValidation
                CustomerValidation-->>CustomerService: void [0.01 ms]
                deactivate CustomerValidation
                loop 3 times
                    CustomerService->>TagNormalizer: normalize(String)
                    activate TagNormalizer
                    TagNormalizer-->>CustomerService: String [0.01 ms]
                    deactivate TagNormalizer
                end
                CustomerService->>CustomerRepository: save(Customer)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: Customer [0.01 ms]
                deactivate CustomerRepository
                CustomerService->>CustomerNumbers: nextFor(Customer)
                activate CustomerNumbers
                CustomerNumbers-->>CustomerService: String [0.37 ms]
                deactivate CustomerNumbers
                CustomerService-)AuditObserver: [event] onCustomerCreated(CustomerCreated)
                activate AuditObserver
                    AuditObserver->>AuditLog: record(String)
                    activate AuditLog
                    AuditLog-->>AuditObserver: void [0.00 ms]
                    deactivate AuditLog
                AuditObserver-->>CustomerService: void [0.14 ms]
                deactivate AuditObserver
            CustomerService-->>CustomerResource: Customer [1.04 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: Response [3.68 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.03 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.03 ms]
        deactivate CustomerResource
    end
```

### a customer without a name is refused

The validation refuses it, and the exception travels back out through every frame it passed before the mapper turns it into a 422. 3 requests, one block each.

```mermaid
---
title: "a customer without a name is refused"
---
sequenceDiagram
    autonumber
    participant Caller as caller
    participant CustomerResource
    participant CustomerService
    participant CustomerRepository
    participant CustomerValidation
    participant BusinessRuleMapper
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.04 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.04 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerValidation: CustomerResource.create — 0.07 ms | thread executor-thread-2
        Caller->>CustomerResource: create(Customer)
        activate CustomerResource
            CustomerResource->>CustomerService: create(Customer)
            activate CustomerService
                CustomerService->>CustomerValidation: check(Customer)
                activate CustomerValidation
                CustomerValidation--xCustomerService: throws BusinessRuleException [0.03 ms]
                deactivate CustomerValidation
            CustomerService--xCustomerResource: throws BusinessRuleException [0.05 ms]
            deactivate CustomerService
        CustomerResource--xCaller: throws BusinessRuleException [0.07 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,BusinessRuleMapper: BusinessRuleMapper.toResponse — 0.04 ms | thread executor-thread-2
        Caller->>BusinessRuleMapper: toResponse(BusinessRuleException)
        activate BusinessRuleMapper
        BusinessRuleMapper-->>Caller: Response [0.04 ms]
        deactivate BusinessRuleMapper
    end
```

### a customer is edited

The list, the customer it creates to have something to edit, the list again, the update, and the list showing the new e-mail. 5 requests, one block each.

```mermaid
---
title: "a customer is edited"
---
sequenceDiagram
    autonumber
    participant Caller as caller
    participant CustomerResource
    participant CustomerService
    participant CustomerRepository
    participant CustomerValidation
    participant CustomerNumbers
    participant AuditObserver
    participant AuditLog
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.03 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.03 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,AuditLog: CustomerResource.create — 0.10 ms | thread executor-thread-2
        Caller->>CustomerResource: create(Customer)
        activate CustomerResource
            CustomerResource->>CustomerService: create(Customer)
            activate CustomerService
                CustomerService->>CustomerValidation: check(Customer)
                activate CustomerValidation
                CustomerValidation-->>CustomerService: void [0.00 ms]
                deactivate CustomerValidation
                CustomerService->>CustomerRepository: save(Customer)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: Customer [0.00 ms]
                deactivate CustomerRepository
                CustomerService->>CustomerNumbers: nextFor(Customer)
                activate CustomerNumbers
                CustomerNumbers-->>CustomerService: String [0.01 ms]
                deactivate CustomerNumbers
                CustomerService-)AuditObserver: [event] onCustomerCreated(CustomerCreated)
                activate AuditObserver
                    AuditObserver->>AuditLog: record(String)
                    activate AuditLog
                    AuditLog-->>AuditObserver: void [0.00 ms]
                    deactivate AuditLog
                AuditObserver-->>CustomerService: void [0.01 ms]
                deactivate AuditObserver
            CustomerService-->>CustomerResource: Customer [0.08 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: Response [0.10 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.05 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.02 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.03 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.05 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerValidation: CustomerResource.update — 0.96 ms | thread executor-thread-2
        Caller->>CustomerResource: update(long, Customer)
        activate CustomerResource
            CustomerResource->>CustomerService: update(long, Customer)
            activate CustomerService
                CustomerService->>CustomerRepository: find(long)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: Optional [0.01 ms]
                deactivate CustomerRepository
                CustomerService->>CustomerValidation: check(Customer)
                activate CustomerValidation
                CustomerValidation-->>CustomerService: void [0.01 ms]
                deactivate CustomerValidation
                CustomerService->>CustomerRepository: save(Customer)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: Customer [0.00 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: Optional [0.79 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: Response [0.96 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.03 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.03 ms]
        deactivate CustomerResource
    end
```

### a customer is deleted

The same shape, ending in a delete - and the list afterwards no longer holds the row. 5 requests, one block each.

```mermaid
---
title: "a customer is deleted"
---
sequenceDiagram
    autonumber
    participant Caller as caller
    participant CustomerResource
    participant CustomerService
    participant CustomerRepository
    participant CustomerValidation
    participant CustomerNumbers
    participant AuditObserver
    participant AuditLog
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.03 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.03 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,AuditLog: CustomerResource.create — 0.10 ms | thread executor-thread-2
        Caller->>CustomerResource: create(Customer)
        activate CustomerResource
            CustomerResource->>CustomerService: create(Customer)
            activate CustomerService
                CustomerService->>CustomerValidation: check(Customer)
                activate CustomerValidation
                CustomerValidation-->>CustomerService: void [0.00 ms]
                deactivate CustomerValidation
                CustomerService->>CustomerRepository: save(Customer)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: Customer [0.00 ms]
                deactivate CustomerRepository
                CustomerService->>CustomerNumbers: nextFor(Customer)
                activate CustomerNumbers
                CustomerNumbers-->>CustomerService: String [0.01 ms]
                deactivate CustomerNumbers
                CustomerService-)AuditObserver: [event] onCustomerCreated(CustomerCreated)
                activate AuditObserver
                    AuditObserver->>AuditLog: record(String)
                    activate AuditLog
                    AuditLog-->>AuditObserver: void [0.00 ms]
                    deactivate AuditLog
                AuditObserver-->>CustomerService: void [0.01 ms]
                deactivate AuditObserver
            CustomerService-->>CustomerResource: Customer [0.07 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: Response [0.10 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.04 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.02 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.03 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.04 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.delete — 0.05 ms | thread executor-thread-2
        Caller->>CustomerResource: delete(long)
        activate CustomerResource
            CustomerResource->>CustomerService: delete(long)
            activate CustomerService
                CustomerService->>CustomerRepository: delete(long)
                activate CustomerRepository
                CustomerRepository-->>CustomerService: boolean [0.01 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: boolean [0.02 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: Response [0.05 ms]
        deactivate CustomerResource
    end
    rect rgb(244, 244, 244)
        Note over Caller,CustomerRepository: CustomerResource.list — 0.13 ms | thread executor-thread-2
        Caller->>CustomerResource: list()
        activate CustomerResource
            CustomerResource->>CustomerService: list()
            activate CustomerService
                CustomerService->>CustomerRepository: findAll()
                activate CustomerRepository
                CustomerRepository-->>CustomerService: List [0.05 ms]
                deactivate CustomerRepository
            CustomerService-->>CustomerResource: List [0.11 ms]
            deactivate CustomerService
        CustomerResource-->>Caller: List [0.13 ms]
        deactivate CustomerResource
    end
```
## Running it differently

```bash
./run.sh --format plantuml       # the same flows in the other notation, as .puml
./run.sh --no-title              # leave the use-case off the diagrams
./run.sh --grep "deleted"        # anything else is handed to Playwright
mvn quarkus:dev                  # dev-mode records too, with no configuration at all
```

`--format` sets nothing but `cdi-flow.output-format` for the run: which calls are recorded, how they
nest and how they fold is decided by the same code either way, and the combined diagram of a use-case
is stitched in the notation asked for - `group` blocks instead of `rect` ones. The generated document
inlines whichever it is.

In dev-mode the diagrams appear as you click through <http://localhost:8091>, and every reload keeps
recording into the same use-case directories.
