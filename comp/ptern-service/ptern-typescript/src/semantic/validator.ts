import {
  type Atom,
  type Capture,
  type Definition,
  type Exclusion,
  type Expression,
  type ParsedPtern,
  type RangeItem,
  type RepCount,
  type Repetition,
  type Sequence,
} from "../parser/ast";
import { type SemanticError } from "./error";

const KNOWN_ANNOTATIONS = new Set([
  "case-insensitive",
  "multiline",
  "replacements-ignore-matching",
  "substitutable",
  "substitutions-ignore-matching",
  "allow-backtracking",
]);

const KNOWN_POSITION_ASSERTIONS = new Set([
  "word-start",
  "word-end",
  "line-start",
  "line-end",
]);

export function validate(ptern: ParsedPtern): SemanticError[] {
  const isSubstitutable = ptern.annotations.some(a => a.name === "substitutable" && a.value);
  const defBodies = new Map<string, Expression>(ptern.definitions.map(d => [d.name, d.body]));

  const substAnnotErrs = validateSubstitutionAnnotations(ptern.annotations);
  const bodySubstErrs: SemanticError[] = isSubstitutable && !isSubstitutableExpr(ptern.body, defBodies)
    ? [{ kind: "notSubstitutableBody" }]
    : [];

  return [
    ...validateAnnotations(ptern.annotations),
    ...substAnnotErrs,
    ...bodySubstErrs,
    ...validateDefinitions(ptern.definitions),
    ...validateExpression(ptern.body, false, isSubstitutable, defBodies),
  ];
}

// ---------------------------------------------------------------------------
// Annotations
// ---------------------------------------------------------------------------

function validateAnnotations(anns: ParsedPtern["annotations"]): SemanticError[] {
  const nameErrs: SemanticError[] = anns.flatMap(ann =>
    KNOWN_ANNOTATIONS.has(ann.name) ? [] : [{ kind: "unknownAnnotation", name: ann.name } as SemanticError],
  );
  return [...nameErrs, ...findDuplicateNames(anns.map(a => a.name), "duplicateAnnotation")];
}

function validateSubstitutionAnnotations(anns: ParsedPtern["annotations"]): SemanticError[] {
  const isSubstitutable = anns.some(a => a.name === "substitutable" && a.value);
  const ignoreMatchingSet = anns.some(a => a.name === "substitutions-ignore-matching" && a.value);
  return ignoreMatchingSet && !isSubstitutable
    ? [{ kind: "substitutionsIgnoreMatchingWithoutSubstitutable" }]
    : [];
}

// ---------------------------------------------------------------------------
// Definitions
// ---------------------------------------------------------------------------

function validateDefinitions(defs: Definition[]): SemanticError[] {
  return defs.flatMap(def => validateExpression(def.body, false, false, new Map()));
}

// ---------------------------------------------------------------------------
// Expression tree walk
// ---------------------------------------------------------------------------

function validateExpression(
  expr: Expression,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  return expr.alternatives.flatMap(seq => validateSequence(seq, insideRep, isSubst, defBodies));
}

function validateSequence(
  seq: Sequence,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  return seq.items.flatMap(cap => validateCapture(cap, insideRep, isSubst, defBodies));
}

function validateCapture(
  cap: Capture,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  const covered = isSubst && cap.name !== null;
  return validateRepetition(cap.inner, insideRep, isSubst, covered, defBodies);
}

function validateRepetition(
  rep: Repetition,
  insideRep: boolean,
  isSubst: boolean,
  coveredByCapture: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  const countErrs: SemanticError[] = [];
  if (rep.count !== null) {
    countErrs.push(...validateRepCount(rep.count));
    const excl = rep.inner;
    if (excl.excluded === null && excl.base.kind === "singleAtom" && excl.base.atom.kind === "positionAssertion") {
      countErrs.push({ kind: "positionAssertionInRepetition", name: excl.base.atom.name });
    }
    if (isSubst && !coveredByCapture) {
      const max = rep.count.max;
      if (max.kind === "exact" || max.kind === "unbounded") {
        if (!hasNamedCaptureInExclusion(rep.inner)) {
          countErrs.push({ kind: "boundedRepetitionNeedsCapture" });
        }
      }
    }
  }
  const subInside = rep.count !== null ? true : insideRep;
  return [...countErrs, ...validateExclusion(rep.inner, subInside, isSubst, defBodies)];
}

function validateRepCount(rc: RepCount): SemanticError[] {
  const errs: SemanticError[] = [];
  if (rc.max.kind === "none" && rc.lazy) errs.push({ kind: "fewestOnExactRepetition" });
  if (rc.max.kind === "exact" && rc.min > rc.max.value) {
    errs.push({ kind: "invertedRepetitionBounds", min: rc.min, max: rc.max.value });
  }
  return errs;
}

function validateExclusion(
  excl: Exclusion,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  const baseErrs = validateRangeItem(excl.base, insideRep, isSubst, defBodies);
  if (excl.excluded === null) return baseErrs;
  const exclErrs = validateRangeItem(excl.excluded, insideRep, isSubst, defBodies);
  const setErrs: SemanticError[] = isCharSet(excl.base, defBodies) && isCharSet(excl.excluded, defBodies)
    ? (rangeItemsEqual(excl.base, excl.excluded) ? [{ kind: "emptyCharacterSet" }] : [])
    : [{ kind: "invalidExclusionOperand" }];
  return [...baseErrs, ...exclErrs, ...setErrs];
}

function validateRangeItem(
  item: RangeItem,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  if (item.kind === "singleAtom") return validateAtom(item.atom, insideRep, isSubst, defBodies);
  return validateCharRange(item.from, item.to);
}

function validateCharRange(from: Atom, to: Atom): SemanticError[] {
  const checkEndpoint = (atom: Atom): SemanticError[] => {
    if (atom.kind === "literal") {
      const errs: SemanticError[] = decodedLength(atom.content) !== 1
        ? [{ kind: "invalidRangeEndpoint", content: atom.content }]
        : [];
      return [...errs, ...validateLiteralEscapes(atom.content)];
    }
    return [{ kind: "invalidRangeEndpoint", content: "<non-literal>" }];
  };
  const fromErrs = checkEndpoint(from);
  const toErrs = checkEndpoint(to);
  const invErrs: SemanticError[] = [];
  if (from.kind === "literal" && to.kind === "literal" && from.content.length === 1 && to.content.length === 1) {
    if (from.content.codePointAt(0)! > to.content.codePointAt(0)!) {
      invErrs.push({ kind: "invertedRange", from: from.content, to: to.content });
    }
  }
  return [...fromErrs, ...toErrs, ...invErrs];
}

function validateAtom(
  atom: Atom,
  insideRep: boolean,
  isSubst: boolean,
  defBodies: Map<string, Expression>,
): SemanticError[] {
  switch (atom.kind) {
    case "literal":
      if (atom.content === "") return [{ kind: "emptyLiteral" }];
      return validateLiteralEscapes(atom.content);
    case "charClass":
    case "interpolation":
      return [];
    case "group":
      return validateExpression(atom.inner, insideRep, isSubst, defBodies);
    case "positionAssertion":
      return KNOWN_POSITION_ASSERTIONS.has(atom.name)
        ? []
        : [{ kind: "unknownPositionAssertion", name: atom.name }];
  }
}

// ---------------------------------------------------------------------------
// Substitutability checks
// ---------------------------------------------------------------------------

function isSubstitutableExpr(expr: Expression, defBodies: Map<string, Expression>): boolean {
  return expr.alternatives.every(seq => isSubstitutableSeq(seq, defBodies));
}

function isSubstitutableSeq(seq: Sequence, defBodies: Map<string, Expression>): boolean {
  return seq.items.every(cap => isSubstitutableCap(cap, defBodies));
}

function isSubstitutableCap(cap: Capture, defBodies: Map<string, Expression>): boolean {
  return cap.name !== null || isSubstitutableRep(cap.inner, defBodies);
}

function isSubstitutableRep(rep: Repetition, defBodies: Map<string, Expression>): boolean {
  if (rep.count === null) return isSubstitutableExcl(rep.inner, defBodies);
  if (rep.count.max.kind === "none") return isSubstitutableExcl(rep.inner, defBodies);
  return hasNamedCaptureInExclusion(rep.inner);
}

function isSubstitutableExcl(excl: Exclusion, defBodies: Map<string, Expression>): boolean {
  if (excl.excluded !== null) return false;
  return isSubstitutableItem(excl.base, defBodies);
}

function isSubstitutableItem(item: RangeItem, defBodies: Map<string, Expression>): boolean {
  if (item.kind === "charRange") return false;
  return isSubstitutableAtom(item.atom, defBodies);
}

function isSubstitutableAtom(atom: Atom, defBodies: Map<string, Expression>): boolean {
  switch (atom.kind) {
    case "literal":
    case "positionAssertion":
      return true;
    case "charClass":
      return false;
    case "interpolation": {
      const body = defBodies.get(atom.name);
      return body !== undefined && isSubstitutableExpr(body, defBodies);
    }
    case "group":
      return isSubstitutableExpr(atom.inner, defBodies);
  }
}

function hasNamedCaptureInExclusion(excl: Exclusion): boolean {
  return hasNamedCaptureInItem(excl.base);
}

function hasNamedCaptureInItem(item: RangeItem): boolean {
  return item.kind === "singleAtom" && hasNamedCaptureInAtom(item.atom);
}

function hasNamedCaptureInAtom(atom: Atom): boolean {
  return atom.kind === "group" && hasNamedCaptureInExpr(atom.inner);
}

function hasNamedCaptureInExpr(expr: Expression): boolean {
  return expr.alternatives.some(seq => hasNamedCaptureInSeq(seq));
}

function hasNamedCaptureInSeq(seq: Sequence): boolean {
  return seq.items.some(cap => hasNamedCaptureInCap(cap));
}

function hasNamedCaptureInCap(cap: Capture): boolean {
  return cap.name !== null || hasNamedCaptureInRep(cap.inner);
}

function hasNamedCaptureInRep(rep: Repetition): boolean {
  return hasNamedCaptureInExclusion(rep.inner);
}

// ---------------------------------------------------------------------------
// Character set helpers
// ---------------------------------------------------------------------------

function isSimpleCharSet(item: RangeItem): boolean {
  if (item.kind === "charRange") {
    return item.from.kind === "literal" && item.to.kind === "literal";
  }
  const atom = item.atom;
  if (atom.kind === "literal") return decodedLength(atom.content) === 1;
  return atom.kind === "charClass";
}

function isCharSet(item: RangeItem, defBodies: Map<string, Expression>): boolean {
  if (item.kind === "singleAtom") {
    const atom = item.atom;
    if (atom.kind === "group") {
      const alts = atom.inner.alternatives;
      return alts.length > 0 && alts.every(alt => isCharSetGroupAlt(alt));
    }
    if (atom.kind === "interpolation") {
      const body = defBodies.get(atom.name);
      return body !== undefined && isCharSetInterpBody(body, defBodies);
    }
  }
  return isSimpleCharSet(item);
}

function isCharSetGroupAlt(seq: Sequence): boolean {
  const items = seq.items;
  if (items.length !== 1) return false;
  const cap = items[0]!;
  return (
    cap.name === null &&
    cap.inner.count === null &&
    cap.inner.inner.excluded === null &&
    isSimpleCharSet(cap.inner.inner.base)
  );
}

function isCharSetInterpBody(expr: Expression, defBodies: Map<string, Expression>): boolean {
  return expr.alternatives.length > 0 && expr.alternatives.every(alt => isCharSetInterpAlt(alt, defBodies));
}

function isCharSetInterpAlt(seq: Sequence, defBodies: Map<string, Expression>): boolean {
  const items = seq.items;
  if (items.length !== 1) return false;
  const cap = items[0]!;
  return (
    cap.name === null &&
    cap.inner.count === null &&
    cap.inner.inner.excluded === null &&
    isCharSet(cap.inner.inner.base, defBodies)
  );
}

function rangeItemsEqual(a: RangeItem, b: RangeItem): boolean {
  if (a.kind !== b.kind) return false;
  if (a.kind === "charRange" && b.kind === "charRange") {
    return atomsEqual(a.from, b.from) && atomsEqual(a.to, b.to);
  }
  if (a.kind === "singleAtom" && b.kind === "singleAtom") return atomsEqual(a.atom, b.atom);
  return false;
}

function atomsEqual(a: Atom, b: Atom): boolean {
  if (a.kind !== b.kind) return false;
  if (a.kind === "literal" && b.kind === "literal") return a.content === b.content;
  if (a.kind === "charClass" && b.kind === "charClass") return a.name === b.name;
  return false;
}

// ---------------------------------------------------------------------------
// Escape sequence validation
// ---------------------------------------------------------------------------

function validateLiteralEscapes(content: string): SemanticError[] {
  const errs: SemanticError[] = [];
  let s = content;
  while (s.length > 0) {
    if (s[0] === "\\") {
      s = s.slice(1);
      if (s.length === 0) { errs.push({ kind: "invalidEscapeSequence", seq: "\\" }); break; }
      const c = s[0]!;
      if ("ntrAfv'\"\\".includes(c)) {
        s = s.slice(1);
      } else if (c === "u") {
        s = s.slice(5); // skip u + 4 hex digits (already validated by lexer)
      } else {
        errs.push({ kind: "invalidEscapeSequence", seq: "\\" + c });
        s = s.slice(1);
      }
    } else {
      const cp = s.codePointAt(0)!;
      s = s.slice(String.fromCodePoint(cp).length);
    }
  }
  return errs;
}

// ---------------------------------------------------------------------------
// Decoded length (escape sequences count as 1)
// ---------------------------------------------------------------------------

function decodedLength(content: string): number {
  let count = 0;
  let s = content;
  while (s.length > 0) {
    if (s[0] === "\\") {
      s = s.slice(1);
      if (s.length === 0) { count++; break; }
      const c = s[0]!;
      if (c === "u") s = s.slice(5);
      else s = s.slice(1);
      count++;
    } else {
      const cp = s.codePointAt(0)!;
      s = s.slice(String.fromCodePoint(cp).length);
      count++;
    }
  }
  return count;
}

// ---------------------------------------------------------------------------
// Duplicate detection helper
// ---------------------------------------------------------------------------

function findDuplicateNames(
  names: string[],
  kind: "duplicateAnnotation" | "duplicateDefinition" | "duplicateCapture",
): SemanticError[] {
  const seen = new Set<string>();
  const dups = new Set<string>();
  for (const name of names) {
    if (seen.has(name)) dups.add(name);
    else seen.add(name);
  }
  return [...dups].map(name => ({ kind, name }) as SemanticError);
}

// Re-export decodedLength and isCharSet for use by other modules.
export { decodedLength, isCharSet, isSimpleCharSet, hasNamedCaptureInExclusion };
