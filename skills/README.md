# Using cdi-flow from Claude Code

[`cdi-flow/SKILL.md`](cdi-flow/SKILL.md) is a **Claude Code skill**: a file that teaches Claude how
to use this addon - which module a container needs, how to select the beans to record, how to label
a use-case, where the diagrams land and what the usual "nothing was written" causes are.

Install it once and Claude can add cdi-flow to a project, configure it and read the recordings back
without being walked through it every time.

## What a skill is

A skill is a Markdown file with a short YAML header. Claude Code discovers the ones it finds on
start-up and reads the header - name and description - of each. The body is loaded only when a
request actually matches that description, so an installed skill costs nothing until it is relevant.

There is nothing to run and nothing to enable: a file in the right directory *is* the installation.

## Installing it

Pick one of the two locations - it is the same file either way.

### For yourself, in every project

```bash
mkdir -p ~/.claude/skills
cp -r skills/cdi-flow ~/.claude/skills/
```

Without a clone of this repository:

```bash
mkdir -p ~/.claude/skills/cdi-flow
curl -o ~/.claude/skills/cdi-flow/SKILL.md \
  https://raw.githubusercontent.com/os890/dynamic-cdi-flow-renderer/main/skills/cdi-flow/SKILL.md
```

### For one project, shared with everyone working on it

```bash
mkdir -p <your-project>/.claude/skills
cp -r skills/cdi-flow <your-project>/.claude/skills/
```

Committed to that project's repository, everyone who clones it gets the skill with the code - which
is the better choice for a team where cdi-flow is part of how the project is built.

### Either way, the result looks like this

```
~/.claude/skills/cdi-flow/SKILL.md          # personal
<your-project>/.claude/skills/cdi-flow/SKILL.md   # per project
```

The directory name and the `name:` in the file's header should match.

## Making Claude pick it up

Skills are read when a session starts, so **start a new Claude Code session** - or `/exit` and start
it again - after copying the file. An already-running session will not see it.

## Checking that it worked

Ask for something the skill covers, in a project with CDI beans:

```
add cdi-flow to this project and record what happens when an order is placed
```

Claude announces the skill it is using, so a line naming `cdi-flow` means it was found. Other
requests that should reach it:

```
draw a sequence diagram of what this service actually calls at runtime
why did no diagram get written?
which beans is cdi-flow recording here, and how do I narrow it down?
```

If nothing happens, the file is in the wrong place or the session predates it. Check that
`SKILL.md` sits one directory below `skills/`, not directly in it, and start a fresh session.

## Updating it

The skill describes this addon's configuration and behaviour, so refresh it when you update the
dependency - the same copy command over the old file:

```bash
cp -r skills/cdi-flow ~/.claude/skills/
```

To remove it, delete the directory. Nothing else records that it was ever there.

## What Claude can do with it

* **add the addon** - pick the right module for Quarkus, for a Jakarta EE server or for a plain
  CDI-Lite container, and set the two or three properties that matter
* **narrow the recording** to the application's own beans, which is the step a full server needs and
  Quarkus does not
* **label a use-case** so a series of requests becomes one diagram, over the `X-Flow-Label` header or
  through `FlowLabel` in a test
* **read the output back** - which file to open first, and what the directory layout means
* **explain a diagram that looks incomplete** - self-invocation, producer-created objects,
  non-interceptable beans and asynchronous events all have documented reasons, and the skill lists
  them so the answer is not a guess

For what the addon itself does, see the [main README](../README.md).
