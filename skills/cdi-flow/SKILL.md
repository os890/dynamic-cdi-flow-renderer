---
name: cdi-flow
description: Record what a CDI application actually does at runtime and get it back as a Mermaid or PlantUML sequence diagram. Use when the user wants to see, draw, document or review the call-flow of Java/Jakarta EE/Quarkus code - "what does this service actually call", "draw a sequence diagram of this use-case", "document this flow", "why is this request slow" - or when they mention cdi-flow, dynamic-cdi-flow-renderer, flow-diagrams or X-Flow-Label. Covers adding the addon to a project, selecting the beans to record, labelling use-cases, and reading the generated output.
---

# cdi-flow

A portable CDI extension that applies a recording interceptor to beans while the container boots,
records every public method-call of the resulting chain, and writes the chain out as a **sequence
diagram** when the outermost call returns.

No annotation on the beans, no code change - the jar on the class-path is the integration.

**Diagrams are recorded, never drawn.** When a user asks "what does this call", read the recorded
diagram rather than inferring a call-graph from the sources: it shows the calls that actually
happened, including the ones going through proxies, events and exception paths.

> **This is a development tool, not for production.** It puts an interceptor on every selected bean
> and writes a file per outermost call. Fine for exercising one scenario; unsuitable for a
> production workload. If it reaches a production class-path, set `cdi-flow.enabled=false` there.

## Choosing the module

`groupId` is `org.os890.cdi.uml` for all of them.

| The user's container | Dependency | Notes |
|---|---|---|
| **Quarkus** | `cdi-flow-quarkus` | one dependency, nothing else. Attaches the binding while Quarkus builds, registers the label-filter, collects observer-methods. Records in dev-mode and in tests with no configuration; a **production build** needs `cdi-flow.enabled=true` spelled out |
| **Weld, OpenWebBeans, a Jakarta EE server** | `dynamic-cdi-flow-renderer` | the portable extension does the attaching. Add `cdi-flow-jaxrs` too if the application serves REST and use-cases should be labelled per request |
| **Another CDI-Lite container** | `cdi-flow-lite` | a build compatible extension attaches the binding at build time |

```xml
<dependency>
    <groupId>org.os890.cdi.uml</groupId>
    <artifactId>dynamic-cdi-flow-renderer</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Only `jakarta.enterprise.cdi-api` and - optionally - `microprofile-config-api` are needed, both
`provided`. The addon references no CDI implementation.

## Setting it up in a project

1. **Add the dependency** for the container above. Scope it to `test` when only tests should record.
2. **Narrow what is recorded.** On a full Jakarta EE server this is the step that matters: the
   server has beans of its own (the REST layer, the CDI implementation, JSF) and recording those
   says nothing about the application. Set an include-pattern on the application packages:
   ```properties
   cdi-flow.include-pattern=com\\.acme\\.order\\..*
   ```
   The Quarkus extension knows which beans belong to the application archive and needs no pattern.
3. **Point the output somewhere findable** - the default is `java.io.tmpdir`:
   ```properties
   cdi-flow.output-directory=target/flow-diagrams
   ```
4. **Run the scenario** - a test, or one entry-point triggered by hand - and read the files.

Configuration is read once while the container boots, through MicroProfile Config when it is on the
class-path and through **system properties / environment variables** otherwise. A server without a
MicroProfile Config implementation therefore takes system properties set before the container starts.
Environment variables use the usual mapping: `cdi-flow.output-directory` → `CDI_FLOW_OUTPUT_DIRECTORY`.

## Recording a use-case rather than single calls

A flow ends when its outermost call returns, and each HTTP request is an outermost call on its own
thread - so one browser-driven use-case produces a *series* of flows. Labelling ties them together.

**Over REST** (needs `cdi-flow-jaxrs`, or Quarkus, which registers the filter itself) - the caller
names the use-case in a header:

```ts
await context.setExtraHTTPHeaders({ 'X-Flow-Label': testInfo.title });
```

**In-process**, from a test or a `main`:

```java
FlowLabel.set("an order is placed");
try {
    orderService.placeOrder("ACME-1", 3);
} finally {
    FlowLabel.clear();
}
```

`FlowLabel.set(name, description)` adds a description; the header for that one is `X-Flow-Description`.

Every flow recorded while the label is set is filed under that use-case, and each diagram is
**titled** with it. An unlabelled flow gets no title.

## Reading the output

```
<output-directory>/
├── use-cases.md                     every use-case, with its diagram inline  ← start here
└── <the use-case>/
    ├── use-case.mmd                 the whole use-case, one block per request
    ├── README.md                    the chains it is made of, and how often each occurred
    └── <EntryPoint>_<method>_….mmd  one file per distinct chain
```

Unlabelled flows are written as single files named
`<EntryPoint>_<method>_<start>_<end>.mmd` (`.puml` for PlantUML). The entry-point is the bean whose
call triggered the first interceptor; parameters are not part of the name, and two flows finishing
in the same millisecond get a `-1`, `-2`, … suffix rather than overwriting each other.

Identical chains - same participants, same calls, different microseconds - are collapsed to one file
and counted, because a use-case that checks the session before every request reads the same list
several times.

**`use-cases.md` is the file to open first**, and to show the user: the diagrams are inline and
render directly on GitHub.

## Configuration

Everything is optional.

| Property | Default | Meaning |
|---|---|---|
| `cdi-flow.enabled` | `true` | master switch - `false` keeps the extension entirely out of the way |
| `cdi-flow.include-pattern` | *(unset)* | regex on the fully qualified bean-class-name |
| `cdi-flow.include-stereotypes` | *(unset)* | comma-separated CDI stereotypes; a bean carrying one is recorded |
| `cdi-flow.exclude-pattern` | *(unset)* | regex which removes a bean again, whatever selected it |
| `cdi-flow.output-directory` | `java.io.tmpdir` | where the files go, created on demand |
| `cdi-flow.output-format` | `mermaid` | `mermaid` (`.mmd`) or `plantuml` (`.puml`) |
| `cdi-flow.hotspot-threshold-ms` | *(unset)* | mark calls slower than this |
| `cdi-flow.fold-loops` | `true` | render repeated identical calls as a `loop N times` block |
| `cdi-flow.collapse-proxy-frames` | `true` | safety-net against duplicated proxy frames |
| `cdi-flow.write-files` | `true` | `false` records into registered sinks only |
| `cdi-flow.title-diagrams` | `true` | `false` leaves the use-case title off every diagram |
| `cdi-flow.group-by-label` | `true` | `false` files a labelled flow as a plain single diagram instead of into a use-case directory |
| `cdi-flow.report` | `true` | `false` writes the same plain single files and no `use-cases.md` |
| `cdi-flow.max-combined-requests` | `25` | above this many requests, `use-cases.md` links a use-case's combined diagram instead of inlining it. The recording itself is not capped |
| `cdi-flow.combined-exclude-pattern` | *(unset)* | keeps a named entry-point out of the combined diagram without dropping it from the recording |
| `cdi-flow.label-header` | `X-Flow-Label` | the header naming the use-case |
| `cdi-flow.description-header` | `X-Flow-Description` | the header describing it |
| `cdi-flow.file-header` | *(unset)* | a header line written into every generated file, e.g. a licence notice |

### Selecting the beans

`include-pattern` and `include-stereotypes` are two **alternative selectors** - a bean is recorded as
soon as **one** applies:

| `include-pattern` | `include-stereotypes` | recorded |
|---|---|---|
| unset | unset | **every bean** - the default |
| set | unset | beans whose name matches |
| unset | set | beans carrying one of the stereotypes |
| set | set | the **union**: name matches **or** a stereotype is present |

`exclude-pattern` is not part of that union - it is a veto and removes a bean again whatever picked
it. Stereotypes resolve transitively, so a bean annotated `@AuditedReadOnlyService`, itself annotated
`@Service`, is selected by `include-stereotypes=com.acme.Service`. An `include-pattern` of `.*`
already selects everything and makes a stereotype next to it pointless.

## Doing something else with a recording

```java
public interface FlowSink {
    void onFlowRecorded(CallFlow flow);
}
```

Register it with `FlowSinks.register(sink)` - which works before the container is booted - or simply
make it a CDI bean. `CallFlow` gives the whole tree (`CallNode`, including `isHotspot()`) plus
`toDiagram()` for the configured notation and `toMermaid()` / `toPlantUml()` for a specific one.
Sinks are never recorded themselves.

Several collected flows render as **one** diagram:

```java
String diagram = CombinedFlowDiagram.of(flows, DiagramFormat.MERMAID, "an order is placed");
```

One block per flow, in the order handed over, sharing the participant lanes; `null` as the title
leaves it off. It neither collapses identical chains nor caps them - which is what an assertion
comparing a recording against an expected diagram needs.

The base package is `org.os890.cdi.uml.dynamic.flow.renderer`; the application-facing types live in
its `api` and `config` sub-packages.

## When no diagram appears

Work through this before suspecting a bug - each cause is a documented behaviour:

| Symptom | Cause |
|---|---|
| nothing at all was written | the outermost call has not returned yet, or `cdi-flow.enabled=false`, or - on a **production** Quarkus build - `cdi-flow.enabled=true` was never set |
| files exist but not where expected | `cdi-flow.output-directory` is unset, so they are in `java.io.tmpdir` |
| a bean is missing from the diagram | an `include-pattern` does not cover it, an `exclude-pattern` vetoes it, or it cannot be intercepted: a final class, a final business-method, no accessible constructor, a record, an enum, a non-static inner class. Those are skipped silently (logged at `FINE`) because adding a binding would turn a working deployment into a `DeploymentException` |
| a call inside one bean is missing | **self-invocation is not recorded** - `this.other()` never leaves the instance, so no interceptor runs. Inherent to interceptor-based recording. What the self-called method calls further on still shows, one level too high |
| a `@Produces`-created object is not there | it is not a managed bean, so the container never intercepts it. The producer-method itself is recorded |
| a private/protected call is missing | only **public** methods are recorded, and `equals`/`hashCode`/`toString` and everything declared by `Object` are filtered out |
| an async observer got its own diagram | a flow belongs to exactly one thread. `fireAsync` is delivered on a worker thread, so it records separately - deliberate |
| a stereotype selects nothing | a configured name that is not on the class-path or not actually a CDI stereotype is reported with a warning at boot |
| the whole server shows up | expected without an `include-pattern` on a full Jakarta EE server - set one on the application packages |

Nothing to check about argument values: **they are never recorded**. Only parameter types are, no
`toString()` is called on application objects, and no application data reaches the files.

## What the diagrams show

Synchronous **CDI events** are delivered on the firing thread, so observers belong to the same chain
and get an event-arrow (`-)` in Mermaid) instead of a call-arrow. **Exceptions** mark every frame
they travelled through (`--x`) and the diagram is still written, so it shows how far the chain got.
**Recursion** is kept level by level. Container proxies and interceptor subclasses never reach the
diagram - the runtime class is mapped back to the class the user wrote.

**Hotspots**: `cdi-flow.hotspot-threshold-ms` marks only the frame worth opening. The outermost call
is never marked (it contains everything), and a slow frame is marked only when nothing below it is
slow as well - so the marker sits on the innermost still-slow call of a branch. A call has to be
*strictly* longer than the threshold, and an entry-point that is slow entirely on its own produces no
marker, because there is no inner frame to point at.

## Rendering to PNG

The `.mmd` and `.puml` files are text. `render-diagrams.sh` in the cdi-flow repository writes a
`.png` next to each one via the `mermaid-cli` and `plantuml` container images, rendering everything
below a directory in a single container run. For Mermaid, GitHub and most Markdown viewers render
the source directly - no rendering step needed to show a user a diagram.
