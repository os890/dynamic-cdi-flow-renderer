# cdi-flow in a Quarkus application, end to end

A CRUD application — Quarkus backend, Angular front-end, one process — whose use-cases are driven
through a real browser while it records a sequence diagram of each of them.

```bash
./run.sh                      # build, drive the use-cases, and say where the diagrams are
open target/flow-diagrams/use-cases.md
```

That is the whole thing. The script builds, runs the suite and prints a path; everything about
recording is in the addon.

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
| `loop 2 times` | `TagNormalizer`, called once per tag |
| `-)` with `[event]` | `AuditObserver`, observing the synchronous `CustomerCreated` event |
| `--x throws BusinessRuleException` | a customer without a name, refused by `CustomerValidation` |
| several blocks in one diagram | a use-case which lists, creates and then updates |

## Running it differently

```bash
./run.sh --grep "deleted"        # arguments are handed to Playwright
mvn quarkus:dev                  # dev-mode records too, with no configuration at all
```

In dev-mode the diagrams appear as you click through <http://localhost:8091>, and every reload keeps
recording into the same use-case directories.
