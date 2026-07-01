import {
  type Atom,
  type Capture,
  type Exclusion,
  type Expression,
  type ParsedPtern,
  type RangeItem,
  type RepCount,
  type Repetition,
  type Sequence,
} from "../parser/ast";
import { type SemanticError } from "./error";

// ---------------------------------------------------------------------------
// Public entry point
// ---------------------------------------------------------------------------

export function check(ptern: ParsedPtern): SemanticError[] {
  if (ptern.annotations.some(a => a.name === "allow-backtracking" && a.value)) {
    return [];
  }
  // dict.merge(build_defs, build_capture_exprs): second arg wins on key clash
  const defs = new Map<string, Expression>([
    ...buildDefs(ptern.definitions),
    ...buildCaptureExprs(ptern.body),
  ]);
  return checkExpression(ptern.body, defs);
}

// ---------------------------------------------------------------------------
// CharSet — conservative character-set representation for first/last analysis
// ---------------------------------------------------------------------------

type CharSet =
  | { kind: "empty" }
  | { kind: "any" }
  | { kind: "literal"; char: string }
  | { kind: "named"; name: string }
  | { kind: "union"; sets: CharSet[] }
  | { kind: "excl"; base: CharSet; excl: CharSet };

const EMPTY_SET: CharSet = { kind: "empty" };
const ANY_CHAR: CharSet = { kind: "any" };

function literalChar(char: string): CharSet {
  return { kind: "literal", char };
}

function namedClass(name: string): CharSet {
  return { kind: "named", name };
}

function unionSet(a: CharSet, b: CharSet): CharSet {
  if (a.kind === "empty") return b;
  if (b.kind === "empty") return a;
  if (a.kind === "union" && b.kind === "union") return { kind: "union", sets: [...a.sets, ...b.sets] };
  if (a.kind === "union") return { kind: "union", sets: [...a.sets, b] };
  if (b.kind === "union") return { kind: "union", sets: [a, ...b.sets] };
  return { kind: "union", sets: [a, b] };
}

function intersects(a: CharSet, b: CharSet): boolean {
  if (a.kind === "empty" || b.kind === "empty") return false;
  if (a.kind === "any" || b.kind === "any") return true;
  if (a.kind === "excl") return intersects(a.base, b) && !isSubset(b, a.excl);
  if (b.kind === "excl") return intersects(a, b.base) && !isSubset(a, b.excl);
  if (a.kind === "union") return a.sets.some(x => intersects(x, b));
  if (b.kind === "union") return b.sets.some(y => intersects(a, y));
  if (a.kind === "literal" && b.kind === "literal") return a.char === b.char;
  if (a.kind === "named" && b.kind === "named") return namedClassesIntersect(a.name, b.name);
  if (a.kind === "named" && b.kind === "literal") return charInNamedClass(b.char, a.name);
  if (a.kind === "literal" && b.kind === "named") return charInNamedClass(a.char, b.name);
  return false;
}

function isSubset(other: CharSet, excl: CharSet): boolean {
  if (other.kind === "literal" && excl.kind === "literal") return other.char === excl.char;
  if (other.kind === "literal" && excl.kind === "named") return charInNamedClass(other.char, excl.name);
  if (other.kind === "named" && excl.kind === "named") return other.name === excl.name;
  return false;
}

const DISJOINT_PAIRS: [string, string][] = [
  ["Alpha", "Digit"], ["L", "Digit"], ["Alpha", "N"], ["Upper", "Digit"],
  ["Lower", "Digit"], ["Upper", "Lower"], ["L", "N"], ["Upper", "N"],
  ["Lower", "N"], ["Upper", "Space"], ["Lower", "Space"], ["Alpha", "Space"],
  ["L", "Space"], ["N", "Space"], ["Digit", "Space"], ["Alnum", "Space"],
];

function namedClassesIntersect(a: string, b: string): boolean {
  if (a === b) return true;
  return !DISJOINT_PAIRS.some(([x, y]) => (x === a && y === b) || (x === b && y === a));
}

function charInNamedClass(c: string, className: string): boolean {
  switch (className) {
    case "Any": return true;
    case "Digit": return "0123456789".includes(c);
    case "N":
      return "0123456789".includes(c) ||
        !"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ \t\n\r".includes(c);
    case "Upper": return "ABCDEFGHIJKLMNOPQRSTUVWXYZ".includes(c);
    case "Lower": return "abcdefghijklmnopqrstuvwxyz".includes(c);
    case "Alpha":
    case "L":
      return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".includes(c) ||
        !"0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ \t\n\r".includes(c);
    case "Alnum": return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".includes(c);
    case "Xdigit": return "0123456789ABCDEFabcdef".includes(c);
    case "Space": return c === " " || c === "\t" || c === "\n" || c === "\r";
    default: return true;
  }
}

// ---------------------------------------------------------------------------
// nullable / first_charset / last_charset
// ---------------------------------------------------------------------------

function nullableExpr(expr: Expression, defs: Map<string, Expression>): boolean {
  return expr.alternatives.some(seq => nullableSeq(seq, defs));
}

function nullableSeq(seq: Sequence, defs: Map<string, Expression>): boolean {
  return seq.items.every(cap => nullableCap(cap, defs));
}

function nullableCap(cap: Capture, defs: Map<string, Expression>): boolean {
  return nullableRep(cap.inner, defs);
}

function nullableRep(rep: Repetition, defs: Map<string, Expression>): boolean {
  if (rep.count === null) return nullableExcl(rep.inner, defs);
  if (rep.count.min === 0) return true;
  return false;
}

function nullableExcl(excl: Exclusion, defs: Map<string, Expression>): boolean {
  if (excl.base.kind === "singleAtom") return nullableAtom(excl.base.atom, defs);
  return false;
}

function nullableAtom(atom: Atom, defs: Map<string, Expression>): boolean {
  switch (atom.kind) {
    case "positionAssertion": return true;
    case "group": return nullableExpr(atom.inner, defs);
    case "interpolation": {
      const body = defs.get(atom.name);
      return body !== undefined ? nullableExpr(body, defs) : false;
    }
    case "literal":
    case "charClass":
      return false;
  }
}

function firstCharsetExpr(expr: Expression, defs: Map<string, Expression>): CharSet {
  return expr.alternatives.reduce(
    (acc, seq) => unionSet(acc, firstCharsetSeq(seq, defs)),
    EMPTY_SET,
  );
}

function firstCharsetSeq(seq: Sequence, defs: Map<string, Expression>): CharSet {
  return firstCharsetItems(seq.items, defs);
}

function firstCharsetItems(items: Capture[], defs: Map<string, Expression>): CharSet {
  if (items.length === 0) return EMPTY_SET;
  const [cap, ...rest] = items as [Capture, ...Capture[]];
  const capFirst = firstCharsetCap(cap, defs);
  if (!nullableCap(cap, defs)) return capFirst;
  return unionSet(capFirst, firstCharsetItems(rest, defs));
}

function firstCharsetCap(cap: Capture, defs: Map<string, Expression>): CharSet {
  return firstCharsetRep(cap.inner, defs);
}

function firstCharsetRep(rep: Repetition, defs: Map<string, Expression>): CharSet {
  return firstCharsetExcl(rep.inner, defs);
}

function firstCharsetExcl(excl: Exclusion, defs: Map<string, Expression>): CharSet {
  const baseCs = excl.base.kind === "singleAtom"
    ? firstCharsetAtom(excl.base.atom, defs)
    : ANY_CHAR;
  if (excl.excluded === null) return baseCs;
  const exclCs = excl.excluded.kind === "singleAtom"
    ? firstCharsetAtom(excl.excluded.atom, defs)
    : ANY_CHAR;
  return { kind: "excl", base: baseCs, excl: exclCs };
}

function firstCharsetAtom(atom: Atom, defs: Map<string, Expression>): CharSet {
  switch (atom.kind) {
    case "literal": {
      const first = [...atom.content][0];
      return first !== undefined ? literalChar(first) : EMPTY_SET;
    }
    case "charClass": return namedClass(atom.name);
    case "positionAssertion": return EMPTY_SET;
    case "group": return firstCharsetExpr(atom.inner, defs);
    case "interpolation": {
      const body = defs.get(atom.name);
      return body !== undefined ? firstCharsetExpr(body, defs) : ANY_CHAR;
    }
  }
}

function lastCharsetExpr(expr: Expression, defs: Map<string, Expression>): CharSet {
  return expr.alternatives.reduce(
    (acc, seq) => unionSet(acc, lastCharsetSeq(seq, defs)),
    EMPTY_SET,
  );
}

function lastCharsetSeq(seq: Sequence, defs: Map<string, Expression>): CharSet {
  return lastCharsetItems([...seq.items].reverse(), defs);
}

function lastCharsetItems(revItems: Capture[], defs: Map<string, Expression>): CharSet {
  if (revItems.length === 0) return EMPTY_SET;
  const [cap, ...rest] = revItems as [Capture, ...Capture[]];
  const capLast = lastCharsetCap(cap, defs);
  if (!nullableCap(cap, defs)) return capLast;
  return unionSet(capLast, lastCharsetItems(rest, defs));
}

function lastCharsetCap(cap: Capture, defs: Map<string, Expression>): CharSet {
  return lastCharsetRep(cap.inner, defs);
}

function lastCharsetRep(rep: Repetition, defs: Map<string, Expression>): CharSet {
  return lastCharsetExcl(rep.inner, defs);
}

function lastCharsetExcl(excl: Exclusion, defs: Map<string, Expression>): CharSet {
  const baseCs = excl.base.kind === "singleAtom"
    ? lastCharsetAtom(excl.base.atom, defs)
    : ANY_CHAR;
  if (excl.excluded === null) return baseCs;
  const exclCs = excl.excluded.kind === "singleAtom"
    ? firstCharsetAtom(excl.excluded.atom, defs)
    : ANY_CHAR;
  return { kind: "excl", base: baseCs, excl: exclCs };
}

function lastCharsetAtom(atom: Atom, defs: Map<string, Expression>): CharSet {
  switch (atom.kind) {
    case "literal": {
      const chars = [...atom.content];
      const last = chars[chars.length - 1];
      return last !== undefined ? literalChar(last) : EMPTY_SET;
    }
    case "charClass": return namedClass(atom.name);
    case "positionAssertion": return EMPTY_SET;
    case "group": return lastCharsetExpr(atom.inner, defs);
    case "interpolation": {
      const body = defs.get(atom.name);
      return body !== undefined ? lastCharsetExpr(body, defs) : ANY_CHAR;
    }
  }
}

// ---------------------------------------------------------------------------
// Fixed-length detection
// ---------------------------------------------------------------------------

function fixedLenOfExcl(excl: Exclusion, defs: Map<string, Expression>): number | null {
  if (excl.base.kind === "charRange") return 1;
  return fixedLenOfAtom(excl.base.atom, defs);
}

function fixedLenOfAtom(atom: Atom, defs: Map<string, Expression>): number | null {
  switch (atom.kind) {
    case "literal": return [...atom.content].length;
    case "charClass": return 1;
    case "positionAssertion": return 0;
    case "group": return fixedLenOfExpr(atom.inner, defs);
    case "interpolation": {
      const body = defs.get(atom.name);
      return body !== undefined ? fixedLenOfExpr(body, defs) : null;
    }
  }
}

function fixedLenOfExpr(expr: Expression, defs: Map<string, Expression>): number | null {
  const seqs = expr.alternatives;
  if (seqs.length === 0) return 0;
  const first = fixedLenOfSeq(seqs[0]!, defs);
  if (first === null) return null;
  return seqs.slice(1).every(s => fixedLenOfSeq(s, defs) === first) ? first : null;
}

function fixedLenOfSeq(seq: Sequence, defs: Map<string, Expression>): number | null {
  let total = 0;
  for (const cap of seq.items) {
    const n = fixedLenOfCap(cap, defs);
    if (n === null) return null;
    total += n;
  }
  return total;
}

function fixedLenOfCap(cap: Capture, defs: Map<string, Expression>): number | null {
  return fixedLenOfRep(cap.inner, defs);
}

function fixedLenOfRep(rep: Repetition, defs: Map<string, Expression>): number | null {
  if (rep.count === null) return fixedLenOfExcl(rep.inner, defs);
  const rc = rep.count;
  if (rc.max.kind === "none") {
    const n = fixedLenOfExcl(rep.inner, defs);
    return n !== null ? n * rc.min : null;
  }
  if (rc.max.kind === "exact") {
    if (rc.min === rc.max.value) {
      const n = fixedLenOfExcl(rep.inner, defs);
      return n !== null ? n * rc.min : null;
    }
    return null;
  }
  return null; // unbounded
}

// ---------------------------------------------------------------------------
// Variable-length and count helpers
// ---------------------------------------------------------------------------

function isVariableCount(count: RepCount | null): boolean {
  if (count === null) return false;
  if (count.max.kind === "none") return false;
  if (count.max.kind === "exact") return count.min !== count.max.value;
  return true; // unbounded
}

function isUnboundedCount(count: RepCount | null): boolean {
  return count !== null && count.max.kind === "unbounded";
}

function isVariableLengthExcl(excl: Exclusion, defs: Map<string, Expression>): boolean {
  return fixedLenOfExcl(excl, defs) === null;
}

// ---------------------------------------------------------------------------
// Recursive walk
// ---------------------------------------------------------------------------

function checkExpression(expr: Expression, defs: Map<string, Expression>): SemanticError[] {
  return expr.alternatives.flatMap(seq => checkSequence(seq, defs));
}

function checkSequence(seq: Sequence, defs: Map<string, Expression>): SemanticError[] {
  const adjErrors = checkAdjacentUnbounded(seq.items, defs);
  const innerErrors = seq.items.flatMap(cap => checkCapture(cap, defs));
  return [...adjErrors, ...innerErrors];
}

function checkCapture(cap: Capture, defs: Map<string, Expression>): SemanticError[] {
  return checkRepetition(cap.inner, defs);
}

function checkRepetition(rep: Repetition, defs: Map<string, Expression>): SemanticError[] {
  const check12 = rep.count !== null ? checkRepetitionBody(rep, defs) : [];
  const innerErrors = checkExclusion(rep.inner, defs);
  return [...check12, ...innerErrors];
}

function checkRepetitionBody(rep: Repetition, defs: Map<string, Expression>): SemanticError[] {
  const base = rep.inner.base;
  if (base.kind === "singleAtom" && base.atom.kind === "group") {
    const branches = base.atom.inner.alternatives;
    if (branches.length >= 2 && isVariableCount(rep.count)) {
      return checkPairwiseBranches(branches, defs);
    }
  }
  return checkBodySelfAmbiguity(rep, defs);
}

function checkPairwiseBranches(branches: Sequence[], defs: Map<string, Expression>): SemanticError[] {
  const errs: SemanticError[] = [];
  for (let i = 0; i < branches.length; i++) {
    for (let j = i + 1; j < branches.length; j++) {
      const bi = branches[i]!;
      const bj = branches[j]!;
      if (
        intersects(lastCharsetSeq(bi, defs), firstCharsetSeq(bj, defs)) ||
        intersects(lastCharsetSeq(bj, defs), firstCharsetSeq(bi, defs))
      ) {
        errs.push({
          kind: "ambiguousRepetitionAdjacency",
          branchA: seqLabel(bi),
          branchB: seqLabel(bj),
        });
      }
    }
  }
  return errs;
}

function checkBodySelfAmbiguity(rep: Repetition, defs: Map<string, Expression>): SemanticError[] {
  if (!isVariableLengthExcl(rep.inner, defs)) return [];
  const fc = firstCharsetExcl(rep.inner, defs);
  const lc = lastCharsetExcl(rep.inner, defs);
  return intersects(lc, fc) ? [{ kind: "ambiguousRepetitionBody" }] : [];
}

function checkExclusion(excl: Exclusion, defs: Map<string, Expression>): SemanticError[] {
  if (excl.base.kind === "singleAtom") return checkAtom(excl.base.atom, defs);
  return [];
}

function checkAtom(atom: Atom, defs: Map<string, Expression>): SemanticError[] {
  if (atom.kind === "group") return checkExpression(atom.inner, defs);
  return [];
}

// ---------------------------------------------------------------------------
// Check adjacent unbounded repetitions in a sequence
// ---------------------------------------------------------------------------

function checkAdjacentUnbounded(items: Capture[], defs: Map<string, Expression>): SemanticError[] {
  const errs: SemanticError[] = [];
  for (let i = 0; i < items.length - 1; i++) {
    const capA = items[i]!;
    const capB = items[i + 1]!;
    if (isUnboundedCount(capA.inner.count) && isUnboundedCount(capB.inner.count)) {
      const lc = lastCharsetCap(capA, defs);
      const fc = firstCharsetCap(capB, defs);
      if (intersects(lc, fc)) {
        errs.push({ kind: "ambiguousAdjacentRepetition" });
      }
    }
  }
  return errs;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function buildDefs(definitions: ParsedPtern["definitions"]): Map<string, Expression> {
  const m = new Map<string, Expression>();
  for (const def of definitions) m.set(def.name, def.body);
  return m;
}

function buildCaptureExprs(expr: Expression): Map<string, Expression> {
  return collectCapsFromExpr(expr, new Map());
}

function collectCapsFromExpr(
  expr: Expression,
  acc: Map<string, Expression>,
): Map<string, Expression> {
  for (const seq of expr.alternatives) {
    acc = collectCapsFromSeq(seq, acc);
  }
  return acc;
}

function collectCapsFromSeq(
  seq: Sequence,
  acc: Map<string, Expression>,
): Map<string, Expression> {
  for (const cap of seq.items) {
    acc = collectCapsFromCap(cap, acc);
  }
  return acc;
}

function collectCapsFromCap(
  cap: Capture,
  acc: Map<string, Expression>,
): Map<string, Expression> {
  let acc2 = acc;
  if (cap.name !== null && !acc.has(cap.name)) {
    const capExpr: Expression = {
      alternatives: [{ items: [{ inner: cap.inner, name: null }] }],
    };
    acc2 = new Map(acc);
    acc2.set(cap.name, capExpr);
  }
  return collectCapsFromExcl(cap.inner.inner, acc2);
}

function collectCapsFromExcl(
  excl: Exclusion,
  acc: Map<string, Expression>,
): Map<string, Expression> {
  if (excl.base.kind === "singleAtom" && excl.base.atom.kind === "group") {
    return collectCapsFromExpr(excl.base.atom.inner, acc);
  }
  return acc;
}

// ---------------------------------------------------------------------------
// Label helpers (for error messages)
// ---------------------------------------------------------------------------

function seqLabel(seq: Sequence): string {
  return seq.items.map(captureLabel).join(" ");
}

function captureLabel(cap: Capture): string {
  return repLabel(cap.inner);
}

function repLabel(rep: Repetition): string {
  return exclLabel(rep.inner);
}

function exclLabel(excl: Exclusion): string {
  if (excl.base.kind === "singleAtom") return atomLabel(excl.base.atom);
  return atomLabel(excl.base.from) + ".." + atomLabel(excl.base.to);
}

function atomLabel(atom: Atom): string {
  switch (atom.kind) {
    case "literal": return "'" + atom.content + "'";
    case "charClass": return "%" + atom.name;
    case "positionAssertion": return "@" + atom.name;
    case "interpolation": return "{" + atom.name + "}";
    case "group": return "(...)";
  }
}
