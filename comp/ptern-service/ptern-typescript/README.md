# @ptern/tern — TypeScript Edition

A readable pattern language that compiles to regular expressions.

```typescript
import { compile } from "@ptern/tern";

const isoDate = compile(
  "%Digit * 4 as year '-' %Digit * 2 as month '-' %Digit * 2 as day",
);

isoDate.matchesAllOf("2026-07-04")                   // true
isoDate.matchFirstIn("Filed on 2026-07-04")
// { index: 9, length: 10, captures: { year: "2026", month: "07", day: "04" } }

isoDate.replaceFirstIn("2026-07-04", { year: "2027" })
// "2027-07-04"
```

## Install

```sh
bun add @ptern/tern   # or: npm install @ptern/tern
```

Requires a JavaScript runtime with ES2024 `v`-mode regex support (Node 20+, Bun 1+, modern browsers).

## Quick Start

```typescript
import { compile, format } from "@ptern/tern";

// Compile once, use many times
const version = compile(
  "num = %Digit * 1..10;\n" +
  "{num} as major '.' {num} as minor '.' {num} as patch",
);

// Boolean tests
version.matchesAllOf("1.2.3")    // true
version.matchesAllOf("1.2")      // false

// Occurrence queries — index, length, captures
const m = version.matchFirstIn("running v1.23.0");
m?.captures   // { major: "1", minor: "23", patch: "0" }

// Replacement — pass only the captures you want to change
version.replaceFirstIn("v1.23.0 released", { patch: "1" })   // "v1.23.1 released"

// Formatting
format("!case-insensitive=true\nword=%Alpha*1..?;\n{word}")
// "!case-insensitive = true\n\nword = %Alpha * 1..? ;\n\n{word}"
```

## Documentation

- **[TypeScript User Guide](../../../../ptern-main/documentation/typescript-user-guide.md)** — full guide with examples for every feature
- **[Language Specification](../../../../ptern-main/documentation/ptern-specification.md)** — formal grammar and operational semantics

## Errors

`compile()` throws `PternCompileError` on lex, parse, or semantic failure. `replace*` methods throw `PternReplacementError` on invalid replacement values. `substitute()` throws `PternSubstitutionError`. `format()` throws `PternFormatError`.

```typescript
import { compile, PternCompileError } from "@ptern/tern";

try {
  const p = compile(source);
} catch (e) {
  if (e instanceof PternCompileError) {
    console.error(e.compileError.kind);   // "lexError" | "parseError" | "semanticErrors"
  }
}
```

## Package

ESM-only. Requires TypeScript 5+ for type declarations.
