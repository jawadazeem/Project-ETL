import type { Atom, Definition, Exclusion, Expression, RangeItem, Repetition } from "../parser/ast";
import { decodedLength } from "./validator";

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

export type Bounds = { min: number; max: number | null };

export function computePternBounds(parsed: {
  definitions: Definition[];
  body: Expression;
}): Bounds {
  const defBounds = computeDefBoundsAll(parsed.definitions);
  return computeExpressionBounds(parsed.body, defBounds);
}

// ---------------------------------------------------------------------------
// Definition bounds (memoised)
// ---------------------------------------------------------------------------

function computeDefBoundsAll(defs: Definition[]): Map<string, Bounds> {
  const defExprs = new Map<string, Expression>(defs.map(d => [d.name, d.body]));
  const acc = new Map<string, Bounds>();
  for (const def of defs) {
    computeDefBoundsMemo(def.name, defExprs, acc);
  }
  return acc;
}

function computeDefBoundsMemo(
  name: string,
  defExprs: Map<string, Expression>,
  acc: Map<string, Bounds>,
): void {
  if (acc.has(name)) return;
  const body = defExprs.get(name);
  if (body === undefined) return;
  const bounds = computeExpressionBounds(body, acc);
  acc.set(name, bounds);
}

// ---------------------------------------------------------------------------
// Expression bounds
// ---------------------------------------------------------------------------

function computeExpressionBounds(expr: Expression, defs: Map<string, Bounds>): Bounds {
  const seqs = expr.alternatives;
  if (seqs.length === 0) return { min: 0, max: 0 };
  const first = computeSequenceBounds(seqs[0]!, defs);
  return seqs.slice(1).reduce((acc, seq) => {
    const b = computeSequenceBounds(seq, defs);
    return { min: Math.min(acc.min, b.min), max: maxOpt(acc.max, b.max) };
  }, first);
}

function computeSequenceBounds(seq: { items: { inner: Repetition }[] }, defs: Map<string, Bounds>): Bounds {
  return seq.items.reduce<Bounds>(
    (acc, cap) => {
      const b = computeRepetitionBounds(cap.inner, defs);
      return { min: acc.min + b.min, max: addOpt(acc.max, b.max) };
    },
    { min: 0, max: 0 },
  );
}

function computeRepetitionBounds(rep: Repetition, defs: Map<string, Bounds>): Bounds {
  const inner = computeExclusionBounds(rep.inner, defs);
  if (rep.count === null) return inner;
  const { min, max: repMax } = rep.count;
  if (repMax.kind === "exact") return { min: inner.min * min, max: mulOpt(inner.max, repMax.value) };
  if (repMax.kind === "none") return { min: inner.min * min, max: mulOpt(inner.max, min) };
  // unbounded
  return { min: inner.min * min, max: null };
}

function computeExclusionBounds(excl: Exclusion, defs: Map<string, Bounds>): Bounds {
  return computeRangeItemBounds(excl.base, defs);
}

function computeRangeItemBounds(item: RangeItem, defs: Map<string, Bounds>): Bounds {
  if (item.kind === "charRange") return { min: 1, max: 1 };
  return computeAtomBounds(item.atom, defs);
}

function computeAtomBounds(atom: Atom, defs: Map<string, Bounds>): Bounds {
  switch (atom.kind) {
    case "literal": {
      const len = decodedLength(atom.content);
      return { min: len, max: len };
    }
    case "charClass":
      return { min: 1, max: 1 };
    case "interpolation":
      return defs.get(atom.name) ?? { min: 0, max: 0 };
    case "group":
      return computeExpressionBounds(atom.inner, defs);
    case "positionAssertion":
      return { min: 0, max: 0 };
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function addOpt(a: number | null, b: number | null): number | null {
  if (a === null || b === null) return null;
  return a + b;
}

function maxOpt(a: number | null, b: number | null): number | null {
  if (a === null || b === null) return null;
  return Math.max(a, b);
}

function mulOpt(a: number | null, n: number): number | null {
  if (a === null) return null;
  return a * n;
}
