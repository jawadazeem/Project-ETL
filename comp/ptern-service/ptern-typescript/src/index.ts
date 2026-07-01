import { lex } from "./lexer/lexer";
import type { LexError } from "./lexer/token";
import { parse } from "./parser/parser";
import type { ParseError } from "./parser/ast";
import { validate } from "./semantic/validator";
import { resolve } from "./semantic/resolver";
import { check } from "./semantic/backtracking";
import type { SemanticError } from "./semantic/error";
import { compile as compilePtern } from "./codegen/codegen";
import { computePternBounds } from "./semantic/bounds";
import type { SubstitutionPlan } from "./codegen/substitution";
import {
  defaultFormatOptions,
  format as formatInternal,
} from "./formatter/formatter";
import type { FormatOptions, FormatError } from "./formatter/formatter";
import {
  makeRegex,
  testRegex,
  execRich,
  execFromRich,
  execAllRich,
  replaceRichWithArrays,
  replaceFromRichWithArrays,
  replaceAllRichWithArrays,
} from "./runtime/replace";

// ---------------------------------------------------------------------------
// Re-exports for convenience
// ---------------------------------------------------------------------------

export type { LexError, ParseError, SemanticError, FormatOptions, FormatError };

// ---------------------------------------------------------------------------
// Public error types
// ---------------------------------------------------------------------------

export type CompileError =
  | { kind: "lexError"; error: LexError }
  | { kind: "parseError"; error: ParseError }
  | { kind: "semanticErrors"; errors: SemanticError[] };

export type MatchOccurrence = {
  index: number;
  length: number;
  captures: Record<string, string>;
};

export type ReplacementValue = string | string[];
export type ReplacementMap = Record<string, ReplacementValue>;

export type ReplacementError =
  | { kind: "invalidReplacementValue"; captureName: string; value: string }
  | { kind: "wrongReplacementType"; captureName: string }
  | { kind: "arrayLengthMismatch"; captureName: string; provided: number; actual: number }
  | { kind: "duplicateRepetitionCapture"; captureName: string };

export type SubstitutionError =
  | { kind: "notSubstitutable" }
  | { kind: "missingCapture"; name: string }
  | { kind: "captureMismatch"; name: string; value: string }
  | { kind: "arrayLengthError"; name: string; length: number; min: number; max: number | null }
  | { kind: "noMatchingBranch" };

// ---------------------------------------------------------------------------
// Error classes (thrown by public API methods)
// ---------------------------------------------------------------------------

export class PternCompileError extends Error {
  readonly compileError: CompileError;
  constructor(ce: CompileError) {
    super("Ptern compile error: " + ce.kind);
    this.name = "PternCompileError";
    this.compileError = ce;
  }
}

export class PternReplacementError extends Error {
  readonly replacementError: ReplacementError;
  constructor(re: ReplacementError) {
    super("Ptern replacement error: " + re.kind);
    this.name = "PternReplacementError";
    this.replacementError = re;
  }
}

export class PternSubstitutionError extends Error {
  readonly substitutionError: SubstitutionError;
  constructor(se: SubstitutionError) {
    super("Ptern substitution error: " + se.kind);
    this.name = "PternSubstitutionError";
    this.substitutionError = se;
  }
}

export class PternFormatError extends Error {
  readonly formatError: FormatError;
  constructor(fe: FormatError) {
    super("Ptern format error: " + fe.kind);
    this.name = "PternFormatError";
    this.formatError = fe;
  }
}

// ---------------------------------------------------------------------------
// Compile
// ---------------------------------------------------------------------------

export function compile(source: string): Ptern {
  const tokensOrErr = lex(source);
  if (!Array.isArray(tokensOrErr)) {
    throw new PternCompileError({ kind: "lexError", error: tokensOrErr });
  }
  const parsedOrErr = parse(tokensOrErr);
  if ("kind" in parsedOrErr) {
    throw new PternCompileError({ kind: "parseError", error: parsedOrErr });
  }

  const allErrors = [
    ...validate(parsedOrErr),
    ...resolve(parsedOrErr),
    ...check(parsedOrErr),
  ];
  const semanticErrors = allErrors.filter(e => e.kind !== "duplicateCapture");
  if (semanticErrors.length > 0) {
    throw new PternCompileError({ kind: "semanticErrors", errors: semanticErrors });
  }

  const compiled = compilePtern(parsedOrErr);
  const bounds = computePternBounds(parsedOrErr);
  const src = compiled.source;
  const baseFlags = compiled.flags;
  const dFlags = baseFlags.includes("d") ? baseFlags : baseFlags + "d";
  const gFlags = dFlags.includes("g") ? dFlags : dFlags + "g";

  const captureValidators = buildCaptureValidators(compiled.captureValidators, dFlags);
  const repInfoList: [string, string, string[]][] = compiled.repetitionInfo.map(ri => [
    ri.groupName,
    ri.subSource,
    ri.captures,
  ]);

  return new Ptern(
    makeRegex("^(?:" + src + ")$", dFlags),
    makeRegex("^(?:" + src + ")", dFlags),
    makeRegex("(?:" + src + ")$", dFlags),
    makeRegex(src, dFlags),
    makeRegex(src, gFlags),
    bounds.min,
    bounds.max,
    compiled.ignoreMatching,
    captureValidators,
    compiled.isSubstitutable,
    compiled.ignoreSubstitutionMatching,
    compiled.substitutionPlan,
    src,
    dFlags,
    repInfoList,
  );
}

function buildCaptureValidators(fragments: [string, string][], flags: string): Map<string, RegExp> {
  const result = new Map<string, RegExp>();
  for (const [name, fragment] of fragments) {
    if (!result.has(name)) {
      result.set(name, makeRegex("^(?:" + fragment + ")$", flags));
    }
  }
  return result;
}

// ---------------------------------------------------------------------------
// Format
// ---------------------------------------------------------------------------

export function format(source: string, options?: Partial<FormatOptions>): string {
  const opts: FormatOptions = { ...defaultFormatOptions, ...options };
  const result = formatInternal(source, opts);
  if (typeof result !== "string") {
    throw new PternFormatError(result);
  }
  return result;
}

// ---------------------------------------------------------------------------
// Ptern class
// ---------------------------------------------------------------------------

export class Ptern {
  readonly #fullRe: RegExp;
  readonly #startsRe: RegExp;
  readonly #endsRe: RegExp;
  readonly #containsRe: RegExp;
  readonly #containsGRe: RegExp;
  readonly #minLen: number;
  readonly #maxLen: number | null;
  readonly #ignoreMatching: boolean;
  readonly #captureValidators: Map<string, RegExp>;
  readonly #isSubstitutable: boolean;
  readonly #ignoreSubstitutionMatching: boolean;
  readonly #substitutionPlan: SubstitutionPlan | null;
  readonly #flags: string;
  readonly #repInfoList: [string, string, string[]][];

  constructor(
    fullRe: RegExp,
    startsRe: RegExp,
    endsRe: RegExp,
    containsRe: RegExp,
    containsGRe: RegExp,
    minLen: number,
    maxLen: number | null,
    ignoreMatching: boolean,
    captureValidators: Map<string, RegExp>,
    isSubstitutable: boolean,
    ignoreSubstitutionMatching: boolean,
    substitutionPlan: SubstitutionPlan | null,
    _source: string,
    flags: string,
    repInfoList: [string, string, string[]][],
  ) {
    this.#fullRe = fullRe;
    this.#startsRe = startsRe;
    this.#endsRe = endsRe;
    this.#containsRe = containsRe;
    this.#containsGRe = containsGRe;
    this.#minLen = minLen;
    this.#maxLen = maxLen;
    this.#ignoreMatching = ignoreMatching;
    this.#captureValidators = captureValidators;
    this.#isSubstitutable = isSubstitutable;
    this.#ignoreSubstitutionMatching = ignoreSubstitutionMatching;
    this.#substitutionPlan = substitutionPlan;
    this.#flags = flags;
    this.#repInfoList = repInfoList;
  }

  // ---------------------------------------------------------------------------
  // Matching
  // ---------------------------------------------------------------------------

  matchesAllOf(input: string): boolean {
    return testRegex(this.#fullRe, input);
  }

  matchesStartOf(input: string): boolean {
    return testRegex(this.#startsRe, input);
  }

  matchesEndOf(input: string): boolean {
    return testRegex(this.#endsRe, input);
  }

  matchesIn(input: string): boolean {
    return testRegex(this.#containsRe, input);
  }

  matchAllOf(input: string): MatchOccurrence | null {
    const m = execRich(this.#fullRe, input);
    return m !== null ? toOccurrence(m) : null;
  }

  matchStartOf(input: string): MatchOccurrence | null {
    const m = execRich(this.#startsRe, input);
    return m !== null ? toOccurrence(m) : null;
  }

  matchEndOf(input: string): MatchOccurrence | null {
    const m = execRich(this.#endsRe, input);
    return m !== null ? toOccurrence(m) : null;
  }

  matchFirstIn(input: string): MatchOccurrence | null {
    const m = execRich(this.#containsRe, input);
    return m !== null ? toOccurrence(m) : null;
  }

  matchNextIn(input: string, startIndex: number): MatchOccurrence | null {
    const m = execFromRich(this.#containsGRe, input, startIndex);
    return m !== null ? toOccurrence(m) : null;
  }

  matchAllIn(input: string): MatchOccurrence[] {
    return execAllRich(this.#containsGRe, input).map(toOccurrence);
  }

  // ---------------------------------------------------------------------------
  // Replacing
  // ---------------------------------------------------------------------------

  replaceAllOf(input: string, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceRichWithArrays(
      this.#fullRe, input, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  replaceStartOf(input: string, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceRichWithArrays(
      this.#startsRe, input, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  replaceEndOf(input: string, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceRichWithArrays(
      this.#endsRe, input, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  replaceFirstIn(input: string, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceRichWithArrays(
      this.#containsRe, input, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  replaceNextIn(input: string, startIndex: number, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceFromRichWithArrays(
      this.#containsGRe, input, startIndex, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  replaceAllIn(input: string, replacements: ReplacementMap): string {
    const err = this.#validateReplacements(replacements);
    if (err !== null) throw new PternReplacementError(err);
    const [scalars, arrays] = splitReplacements(replacements);
    const outcome = replaceAllRichWithArrays(
      this.#containsGRe, input, scalars, arrays, this.#repInfoList, this.#flags,
    );
    if (!outcome.ok) throw new PternReplacementError(ffiFfiError(outcome.mismatches));
    return outcome.value;
  }

  // ---------------------------------------------------------------------------
  // Substitution
  // ---------------------------------------------------------------------------

  substitute(captures: ReplacementMap): string {
    if (!this.#isSubstitutable || this.#substitutionPlan === null) {
      throw new PternSubstitutionError({ kind: "notSubstitutable" });
    }
    const result = evaluatePlan(
      this.#substitutionPlan,
      captures,
      this.#captureValidators,
      this.#ignoreSubstitutionMatching,
      new Map(),
    );
    if (!result.ok) throw new PternSubstitutionError(result.error);
    return result.value;
  }

  // ---------------------------------------------------------------------------
  // Metadata
  // ---------------------------------------------------------------------------

  minLength(): number {
    return this.#minLen;
  }

  maxLength(): number | null {
    return this.#maxLen;
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  #validateReplacements(replacements: ReplacementMap): ReplacementError | null {
    for (const [name, value] of Object.entries(replacements)) {
      if (typeof value === "string") {
        // scalar
        if (!this.#ignoreMatching) {
          const re = this.#captureValidators.get(name);
          if (re !== undefined && !testRegex(re, value)) {
            return { kind: "invalidReplacementValue", captureName: name, value };
          }
        }
      } else {
        // array
        const nReps = repGroupCount(this.#repInfoList, name);
        if (nReps === 0) return { kind: "wrongReplacementType", captureName: name };
        if (nReps > 1) return { kind: "duplicateRepetitionCapture", captureName: name };
        if (!this.#ignoreMatching) {
          const re = this.#captureValidators.get(name);
          if (re !== undefined) {
            for (const v of value) {
              if (!testRegex(re, v)) {
                return { kind: "invalidReplacementValue", captureName: name, value: v };
              }
            }
          }
        }
      }
    }
    return null;
  }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

function toOccurrence(m: { index: number; length: number; captures: [string, string][] }): MatchOccurrence {
  return {
    index: m.index,
    length: m.length,
    captures: Object.fromEntries(m.captures),
  };
}

function repGroupCount(repInfoList: [string, string, string[]][], captureName: string): number {
  return repInfoList.filter(([, , caps]) => caps.includes(captureName)).length;
}

function splitReplacements(
  replacements: ReplacementMap,
): [[string, string][], [string, string[]][]] {
  const scalars: [string, string][] = [];
  const arrays: [string, string[]][] = [];
  for (const [name, val] of Object.entries(replacements)) {
    if (typeof val === "string") scalars.push([name, val]);
    else arrays.push([name, val]);
  }
  return [scalars, arrays];
}

function ffiFfiError(mismatches: [string, number, number][]): ReplacementError {
  const [name, provided, actual] = mismatches[0]!;
  return { kind: "arrayLengthMismatch", captureName: name, provided, actual };
}

// ---------------------------------------------------------------------------
// Substitution plan evaluation
// ---------------------------------------------------------------------------

type EvalResult =
  | { ok: true; value: string; cursors: Map<string, number> }
  | { ok: false; error: SubstitutionError };

function evaluatePlan(
  plan: SubstitutionPlan,
  captures: ReplacementMap,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
): EvalResult {
  switch (plan.kind) {
    case "literal":
      return { ok: true, value: plan.text, cursors };

    case "positionAssertion":
      return { ok: true, value: "", cursors };

    case "notEvaluable":
      return { ok: false, error: { kind: "noMatchingBranch" } };

    case "capture": {
      const val = captures[plan.name];
      if (val === undefined) {
        const inner = evaluatePlan(plan.inner, captures, validators, ignoreMatching, cursors);
        if (!inner.ok && inner.error.kind === "noMatchingBranch") {
          return { ok: false, error: { kind: "missingCapture", name: plan.name } };
        }
        return inner;
      }
      if (typeof val === "string") {
        const err = validateSubCapture(plan.name, val, validators, ignoreMatching);
        if (err !== null) return { ok: false, error: err };
        return { ok: true, value: val, cursors };
      }
      return evalArrayCapture(plan.name, val, validators, ignoreMatching, cursors);
    }

    case "sequence":
      return evaluateSequence(plan.items, captures, validators, ignoreMatching, cursors, "");

    case "alternation":
      return tryBranches(plan.branches, captures, validators, ignoreMatching, cursors);

    case "fixedRep":
      return repeatPlan(plan.inner, plan.count, captures, validators, ignoreMatching, cursors, "");

    case "boundedRep":
      return evalBoundedRep(plan.inner, plan.min, plan.max, captures, validators, ignoreMatching, cursors);
  }
}

function validateSubCapture(
  name: string,
  value: string,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
): SubstitutionError | null {
  if (ignoreMatching) return null;
  const re = validators.get(name);
  if (re === undefined) return null;
  if (!testRegex(re, value)) return { kind: "captureMismatch", name, value };
  return null;
}

function evalArrayCapture(
  name: string,
  vs: string[],
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
): EvalResult {
  const cursor = cursors.get(name) ?? 0;
  const remaining = vs.slice(cursor);
  if (remaining.length === 0) return { ok: false, error: { kind: "missingCapture", name } };
  const elem = remaining[0]!;
  const err = validateSubCapture(name, elem, validators, ignoreMatching);
  if (err !== null) return { ok: false, error: err };
  const newCursors = new Map(cursors);
  newCursors.set(name, cursor + 1);
  return { ok: true, value: elem, cursors: newCursors };
}

function evaluateSequence(
  items: SubstitutionPlan[],
  captures: ReplacementMap,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
  acc: string,
): EvalResult {
  if (items.length === 0) return { ok: true, value: acc, cursors };
  const [item, ...rest] = items;
  const r = evaluatePlan(item!, captures, validators, ignoreMatching, cursors);
  if (!r.ok) return r;
  return evaluateSequence(rest, captures, validators, ignoreMatching, r.cursors, acc + r.value);
}

function tryBranches(
  branches: SubstitutionPlan[],
  captures: ReplacementMap,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
): EvalResult {
  if (branches.length === 0) return { ok: false, error: { kind: "noMatchingBranch" } };
  const [branch, ...rest] = branches;
  const r = evaluatePlan(branch!, captures, validators, ignoreMatching, cursors);
  if (!r.ok && (r.error.kind === "missingCapture" || r.error.kind === "noMatchingBranch")) {
    return tryBranches(rest, captures, validators, ignoreMatching, cursors);
  }
  return r;
}

function repeatPlan(
  plan: SubstitutionPlan,
  remaining: number,
  captures: ReplacementMap,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
  acc: string,
): EvalResult {
  if (remaining === 0) return { ok: true, value: acc, cursors };
  const r = evaluatePlan(plan, captures, validators, ignoreMatching, cursors);
  if (!r.ok) return r;
  return repeatPlan(plan, remaining - 1, captures, validators, ignoreMatching, r.cursors, acc + r.value);
}

function evalBoundedRep(
  inner: SubstitutionPlan,
  min: number,
  maxOpt: number | null,
  captures: ReplacementMap,
  validators: Map<string, RegExp>,
  ignoreMatching: boolean,
  cursors: Map<string, number>,
): EvalResult {
  const names = collectDirectCaptureNames(inner);
  const countResult = determineIterCount(names, captures, cursors, min, maxOpt);
  if (!countResult.ok) return { ok: false, error: countResult.error };
  if (countResult.value === null) {
    if (min === 0) return { ok: true, value: "", cursors };
    return { ok: false, error: { kind: "noMatchingBranch" } };
  }
  return repeatPlan(inner, countResult.value, captures, validators, ignoreMatching, cursors, "");
}

function collectDirectCaptureNames(plan: SubstitutionPlan): string[] {
  switch (plan.kind) {
    case "capture": return [plan.name];
    case "sequence": return plan.items.flatMap(collectDirectCaptureNames);
    case "alternation":
      return deduplicateNames(plan.branches.flatMap(collectDirectCaptureNames));
    case "fixedRep": return collectDirectCaptureNames(plan.inner);
    case "boundedRep": return collectDirectCaptureNames(plan.inner);
    default: return [];
  }
}

function deduplicateNames(names: string[]): string[] {
  return [...new Set(names)];
}

type IterCountResult =
  | { ok: true; value: number | null }
  | { ok: false; error: SubstitutionError };

function determineIterCount(
  names: string[],
  captures: ReplacementMap,
  cursors: Map<string, number>,
  min: number,
  maxOpt: number | null,
): IterCountResult {
  let found: number | null = null;
  let firstName = "";
  for (const name of names) {
    const val = captures[name];
    if (val === undefined || typeof val === "string") continue;
    const cursor = cursors.get(name) ?? 0;
    const remaining = val.length - cursor;
    if (found === null) {
      found = remaining;
      firstName = name;
    } else if (found !== remaining) {
      const totalLen = val.length;
      return { ok: false, error: { kind: "arrayLengthError", name, length: totalLen, min, max: maxOpt } };
    }
  }
  if (found === null) return { ok: true, value: null };
  return checkIterBounds(found, firstName, captures, min, maxOpt);
}

function checkIterBounds(
  n: number,
  name: string,
  captures: ReplacementMap,
  min: number,
  maxOpt: number | null,
): IterCountResult {
  const aboveMax = maxOpt !== null && n > maxOpt;
  if (n >= min && !aboveMax) return { ok: true, value: n };
  const val = captures[name];
  if (Array.isArray(val)) {
    return {
      ok: false,
      error: { kind: "arrayLengthError", name, length: val.length, min, max: maxOpt },
    };
  }
  return { ok: false, error: { kind: "noMatchingBranch" } };
}
