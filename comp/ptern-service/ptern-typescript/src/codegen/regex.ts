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

// ---------------------------------------------------------------------------
// Flags
// ---------------------------------------------------------------------------

export function determineFlags(ptern: ParsedPtern): string {
  const anns = ptern.annotations;
  const caseInsensitive = anns.some(a => a.name === "case-insensitive" && a.value);
  const multiline =
    anns.some(a => a.name === "multiline" && a.value) ||
    hasLineBoundaryInDefs(ptern.definitions) ||
    hasLineBoundaryInExpr(ptern.body);
  if (caseInsensitive && multiline) return "vim";
  if (caseInsensitive) return "vi";
  if (multiline) return "vm";
  return "v";
}

function hasLineBoundaryInDefs(defs: Definition[]): boolean {
  return defs.some(def => hasLineBoundaryInExpr(def.body));
}

function hasLineBoundaryInExpr(expr: Expression): boolean {
  return expr.alternatives.some(hasLineBoundaryInSeq);
}

function hasLineBoundaryInSeq(seq: Sequence): boolean {
  return seq.items.some(hasLineBoundaryInCap);
}

function hasLineBoundaryInCap(cap: Capture): boolean {
  return hasLineBoundaryInExcl(cap.inner.inner);
}

function hasLineBoundaryInExcl(excl: Exclusion): boolean {
  return hasLineBoundaryInItem(excl.base);
}

function hasLineBoundaryInItem(item: RangeItem): boolean {
  if (item.kind === "singleAtom") {
    if (
      item.atom.kind === "positionAssertion" &&
      (item.atom.name === "line-start" || item.atom.name === "line-end")
    ) return true;
    if (item.atom.kind === "group") return hasLineBoundaryInExpr(item.atom.inner);
  }
  return false;
}

export function determineIgnoreMatching(annotations: ParsedPtern["annotations"]): boolean {
  return annotations.some(a => a.name === "replacements-ignore-matching" && a.value);
}

// ---------------------------------------------------------------------------
// Duplicate capture name detection (for !substitutable suppression)
// ---------------------------------------------------------------------------

export function findDuplicateCaptureNames(expr: Expression): string[] {
  const names = collectAllCaptureNamesExpr(expr);
  const seen: string[] = [];
  const dups: string[] = [];
  for (const name of names) {
    if (seen.includes(name)) {
      if (!dups.includes(name)) dups.push(name);
    } else {
      seen.push(name);
    }
  }
  return dups;
}

function collectAllCaptureNamesExpr(expr: Expression): string[] {
  return expr.alternatives.flatMap(collectAllCaptureNamesSeq);
}

function collectAllCaptureNamesSeq(seq: Sequence): string[] {
  return seq.items.flatMap(collectAllCaptureNamesCap);
}

function collectAllCaptureNamesCap(cap: Capture): string[] {
  const own = cap.name !== null ? [cap.name] : [];
  return [...own, ...collectAllCaptureNamesRep(cap.inner)];
}

function collectAllCaptureNamesRep(rep: Repetition): string[] {
  return collectAllCaptureNamesExcl(rep.inner);
}

function collectAllCaptureNamesExcl(excl: Exclusion): string[] {
  return collectAllCaptureNamesItem(excl.base);
}

function collectAllCaptureNamesItem(item: RangeItem): string[] {
  if (item.kind === "singleAtom") return collectAllCaptureNamesAtom(item.atom);
  return [];
}

function collectAllCaptureNamesAtom(atom: Atom): string[] {
  if (atom.kind === "group") return collectAllCaptureNamesExpr(atom.inner);
  return [];
}

// ---------------------------------------------------------------------------
// Definition compilation (recursive, memoised)
// ---------------------------------------------------------------------------

export function compileDefinitions(
  defs: Definition[],
  classDefs: Map<string, string>,
): Map<string, string> {
  const defBodies = new Map<string, Expression>(defs.map(def => [def.name, def.body]));
  return defs.reduce(
    (compiled, def) => compileDefMemo(def.name, defBodies, compiled, classDefs),
    new Map<string, string>(),
  );
}

function compileDefMemo(
  name: string,
  defBodies: Map<string, Expression>,
  compiled: Map<string, string>,
  classDefs: Map<string, string>,
): Map<string, string> {
  if (compiled.has(name)) return compiled;
  const body = defBodies.get(name)!;
  const deps = interpolationsInExpression(body);
  let compiled2 = deps.reduce((c, dep) => {
    if (!defBodies.has(dep)) return c;
    return compileDefMemo(dep, defBodies, c, classDefs);
  }, compiled);
  const frag = compileExpression(body, compiled2, classDefs, []);
  return new Map([...compiled2, [name, frag]]);
}

// ---------------------------------------------------------------------------
// Class-operand compilation for definitions used in `excluding` contexts
// ---------------------------------------------------------------------------

export function compileClassDefinitions(defs: Definition[]): Map<string, string> {
  const defBodies = new Map<string, Expression>(defs.map(def => [def.name, def.body]));
  return defs.reduce(
    (classCompiled, def) => compileClassDefMemo(def.name, defBodies, classCompiled),
    new Map<string, string>(),
  );
}

function compileClassDefMemo(
  name: string,
  defBodies: Map<string, Expression>,
  classCompiled: Map<string, string>,
): Map<string, string> {
  if (classCompiled.has(name)) return classCompiled;
  const body = defBodies.get(name);
  if (body === undefined) return classCompiled;
  const deps = interpolationsInExpression(body);
  let classCompiled2 = deps.reduce((c, dep) => {
    if (!defBodies.has(dep)) return c;
    return compileClassDefMemo(dep, defBodies, c);
  }, classCompiled);
  const classBody = exprAsClassBody(body, classCompiled2);
  if (classBody === "") return classCompiled2;
  return new Map([...classCompiled2, [name, "[" + classBody + "]"]]);
}

function exprAsClassBody(expr: Expression, classDefs: Map<string, string>): string {
  const parts = expr.alternatives.map(seq => seqAsClassBodyExt(seq, classDefs));
  if (parts.some(p => p === "")) return "";
  return parts.join("");
}

function seqAsClassBodyExt(seq: Sequence, classDefs: Map<string, string>): string {
  const items = seq.items;
  if (items.length !== 1) return "";
  const cap = items[0]!;
  if (cap.name !== null || cap.inner.count !== null) return "";
  if (cap.inner.inner.excluded !== null) return "";
  return rangeItemAsClassBodyExt(cap.inner.inner.base, classDefs);
}

function rangeItemAsClassBodyExt(item: RangeItem, classDefs: Map<string, string>): string {
  if (item.kind === "singleAtom") {
    const atom = item.atom;
    if (atom.kind === "literal") return rawToClassChar(atom.content);
    if (atom.kind === "charClass") return charClassStandalone(atom.name);
    if (atom.kind === "group") {
      return atom.inner.alternatives.map(seq => seqAsClassBodyExt(seq, classDefs)).join("");
    }
    if (atom.kind === "interpolation") {
      return classDefs.get(atom.name) ?? "";
    }
    return "";
  }
  // charRange
  if (item.from.kind === "literal" && item.to.kind === "literal") {
    return "[" + rawToClassChar(item.from.content) + "-" + rawToClassChar(item.to.content) + "]";
  }
  return "";
}

// ---------------------------------------------------------------------------
// Expression → regex string
// ---------------------------------------------------------------------------

export function compileExpression(
  expr: Expression,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  const seqs = expr.alternatives;
  const mergeable = seqs.length >= 2 && seqs.every(isClassItem);
  if (mergeable) {
    return "[" + seqs.map(sequenceAsClassBody).join("") + "]";
  }
  return seqs.map(seq => compileSequence(seq, defs, classDefs, suppressed)).join("|");
}

function compileSequence(
  seq: Sequence,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  return seq.items.map(cap => compileCapture(cap, defs, classDefs, suppressed)).join("");
}

function compileCapture(
  cap: Capture,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  const body = compileRepetition(cap.inner, defs, classDefs, suppressed);
  if (cap.name === null) return body;
  return suppressed.includes(cap.name)
    ? "(?:" + body + ")"
    : "(?<" + cap.name + ">" + body + ")";
}

function compileRepetition(
  rep: Repetition,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  const body = compileExclusion(rep.inner, defs, classDefs, suppressed);
  if (rep.count === null) return body;
  return wrapIfNeeded(body) + compileQuantifier(rep.count);
}

export function compileExclusion(
  excl: Exclusion,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  if (excl.excluded === null) {
    return compileRangeItem(excl.base, defs, classDefs, suppressed);
  }
  const baseClass = rangeItemAsClassOperand(excl.base, defs, classDefs);
  const exclClass = rangeItemAsClassOperand(excl.excluded, defs, classDefs);
  return "[" + baseClass + "--" + exclClass + "]";
}

function compileRangeItem(
  item: RangeItem,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  if (item.kind === "singleAtom") return compileAtom(item.atom, defs, classDefs, suppressed);
  if (item.from.kind === "literal" && item.to.kind === "literal") {
    return "[" + rawToClassChar(item.from.content) + "-" + rawToClassChar(item.to.content) + "]";
  }
  return "(?!)";
}

function compileAtom(
  atom: Atom,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  suppressed: string[],
): string {
  switch (atom.kind) {
    case "literal": return rawToRegex(atom.content);
    case "charClass": return charClassStandalone(atom.name);
    case "interpolation": {
      const pattern = defs.get(atom.name);
      return pattern !== undefined ? "(?:" + pattern + ")" : "\\k<" + atom.name + ">";
    }
    case "group":
      return "(?:" + compileExpression(atom.inner, defs, classDefs, suppressed) + ")";
    case "positionAssertion":
      return compilePositionAssertion(atom.name);
  }
}

// ---------------------------------------------------------------------------
// Range items (class operand helpers)
// ---------------------------------------------------------------------------

function rangeItemAsClassOperand(
  item: RangeItem,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): string {
  if (item.kind === "singleAtom") {
    const atom = item.atom;
    if (atom.kind === "charClass") return charClassStandalone(atom.name);
    if (atom.kind === "literal") return "[" + rawToClassChar(atom.content) + "]";
    if (atom.kind === "group") {
      return "[" + atom.inner.alternatives.map(sequenceAsClassBody).join("") + "]";
    }
    if (atom.kind === "interpolation") {
      return classDefs.get(atom.name) ?? "[(?!)]";
    }
    return "[" + compileAtom(atom, defs, classDefs, []) + "]";
  }
  // charRange
  if (item.from.kind === "literal" && item.to.kind === "literal") {
    return "[" + rawToClassChar(item.from.content) + "-" + rawToClassChar(item.to.content) + "]";
  }
  return "[(?!)]";
}

// ---------------------------------------------------------------------------
// Character-class merging for alternations
// ---------------------------------------------------------------------------

function isClassItem(seq: Sequence): boolean {
  const items = seq.items;
  if (items.length !== 1) return false;
  const cap = items[0]!;
  if (cap.name !== null) return false;
  if (cap.inner.count !== null) return false;
  return isClassRangeItem(cap.inner.inner.base);
}

function isClassRangeItem(item: RangeItem): boolean {
  if (item.kind === "singleAtom") {
    const atom = item.atom;
    if (atom.kind === "literal") return decodedLength(atom.content) === 1;
    if (atom.kind === "charClass") return true;
    return false;
  }
  // charRange
  return item.from.kind === "literal" && item.to.kind === "literal";
}

function sequenceAsClassBody(seq: Sequence): string {
  const cap = seq.items[0]!;
  const excl = cap.inner.inner;
  if (excl.excluded === null) return rangeItemAsClassBody(excl.base);
  return (
    "[" +
    rangeItemAsClassOperand(excl.base, new Map(), new Map()) +
    "--" +
    rangeItemAsClassOperand(excl.excluded, new Map(), new Map()) +
    "]"
  );
}

function rangeItemAsClassBody(item: RangeItem): string {
  if (item.kind === "singleAtom") {
    const atom = item.atom;
    if (atom.kind === "literal") return rawToClassChar(atom.content);
    if (atom.kind === "charClass") return charClassStandalone(atom.name);
    return "";
  }
  if (item.from.kind === "literal" && item.to.kind === "literal") {
    return "[" + rawToClassChar(item.from.content) + "-" + rawToClassChar(item.to.content) + "]";
  }
  return "";
}

function decodedLength(raw: string): number {
  let count = 0;
  let i = 0;
  while (i < raw.length) {
    if (raw[i] === "\\") {
      i++;
      if (i >= raw.length) { count++; break; }
      if (raw[i] === "u") i += 5;
      else i++;
      count++;
    } else {
      const cp = raw.codePointAt(i)!;
      i += cp > 0xffff ? 2 : 1;
      count++;
    }
  }
  return count;
}

// ---------------------------------------------------------------------------
// Atoms
// ---------------------------------------------------------------------------

function compilePositionAssertion(name: string): string {
  switch (name) {
    case "word-start":
    case "word-end":
      return "\\b";
    case "line-start": return "^";
    case "line-end": return "$";
    default: return "(?!)";
  }
}

// ---------------------------------------------------------------------------
// Quantifiers
// ---------------------------------------------------------------------------

function compileQuantifier(rc: RepCount): string {
  const lazySuffix = rc.lazy ? "?" : "";
  const base = compileQuantifierBase(rc);
  return base + lazySuffix;
}

function compileQuantifierBase(rc: RepCount): string {
  const { min, max } = rc;
  if (min === 0 && max.kind === "exact" && max.value === 1) return "?";
  if (min === 0 && max.kind === "unbounded") return "*";
  if (min === 1 && max.kind === "unbounded") return "+";
  if (max.kind === "unbounded") return "{" + min + ",}";
  if (max.kind === "none") return "{" + min + "}";
  return "{" + min + "," + max.value + "}";
}

// ---------------------------------------------------------------------------
// Character classes
// ---------------------------------------------------------------------------

export function charClassStandalone(name: string): string {
  switch (name) {
    case "Any": return "[\\s\\S]";
    case "Digit": return "[0-9]";
    case "Alpha": return "[A-Za-z]";
    case "Alnum": return "[A-Za-z0-9]";
    case "Lower": return "[a-z]";
    case "Upper": return "[A-Z]";
    case "Word": return "[A-Za-z0-9_]";
    case "Space": return "[ \\t\\n\\r\\f\\v]";
    case "Blank": return "[ \\t]";
    case "Xdigit": return "[0-9A-Fa-f]";
    case "Ascii": return "[\\x00-\\x7F]";
    case "Cntrl": return "[\\x00-\\x1F\\x7F]";
    case "Graph": return "[\\x21-\\x7E]";
    case "Print": return "[\\x20-\\x7E]";
    case "Punct": return "[\\x21-\\x2F\\x3A-\\x40\\x5B-\\x60\\x7B-\\x7E]";
    case "L":
    case "Letter": return "\\p{L}";
    case "Ll":
    case "LowercaseLetter": return "\\p{Ll}";
    case "Lu":
    case "UppercaseLetter": return "\\p{Lu}";
    case "Lm":
    case "ModifierLetter": return "\\p{Lm}";
    case "Lo":
    case "OtherLetter": return "\\p{Lo}";
    case "Lt":
    case "TitlecaseLetter": return "\\p{Lt}";
    case "M":
    case "Mark": return "\\p{M}";
    case "Mc":
    case "SpacingMark": return "\\p{Mc}";
    case "Me":
    case "EnclosingMark": return "\\p{Me}";
    case "Mn":
    case "NonspacingMark": return "\\p{Mn}";
    case "N":
    case "Number": return "\\p{N}";
    case "Nd":
    case "DecimalNumber": return "\\p{Nd}";
    case "Nl":
    case "LetterNumber": return "\\p{Nl}";
    case "No":
    case "OtherNumber": return "\\p{No}";
    case "P":
    case "Punctuation": return "\\p{P}";
    case "Pc":
    case "ConnectorPunctuation": return "\\p{Pc}";
    case "Pd":
    case "DashPunctuation": return "\\p{Pd}";
    case "Pe":
    case "ClosePunctuation": return "\\p{Pe}";
    case "Pf":
    case "FinalPunctuation": return "\\p{Pf}";
    case "Pi":
    case "InitialPunctuation": return "\\p{Pi}";
    case "Po":
    case "OtherPunctuation": return "\\p{Po}";
    case "Ps":
    case "OpenPunctuation": return "\\p{Ps}";
    case "S":
    case "Symbol": return "\\p{S}";
    case "Sc":
    case "CurrencySymbol": return "\\p{Sc}";
    case "Sk":
    case "ModifierSymbol": return "\\p{Sk}";
    case "Sm":
    case "MathSymbol": return "\\p{Sm}";
    case "So":
    case "OtherSymbol": return "\\p{So}";
    case "Z":
    case "Separator": return "\\p{Z}";
    case "Zl":
    case "LineSeparator": return "\\p{Zl}";
    case "Zp":
    case "ParagraphSeparator": return "\\p{Zp}";
    case "Zs":
    case "SpaceSeparator": return "\\p{Zs}";
    case "C":
    case "Other": return "\\p{C}";
    case "Cc":
    case "Control": return "\\p{Cc}";
    case "Cf":
    case "Format": return "\\p{Cf}";
    case "Cn":
    case "Unassigned": return "\\p{Cn}";
    case "Co":
    case "PrivateUse": return "\\p{Co}";
    case "Cs":
    case "Surrogate": return "\\p{Cs}";
    default: return "(?!)";
  }
}

// ---------------------------------------------------------------------------
// Raw literal content → regex string
// ---------------------------------------------------------------------------

function rawToRegex(content: string): string {
  return processRaw(content, false);
}

function rawToClassChar(content: string): string {
  return processRaw(content, true);
}

function processRaw(s: string, inCc: boolean): string {
  let result = "";
  let i = 0;
  while (i < s.length) {
    if (s[i] === "\\") {
      i++;
      if (i >= s.length) break;
      const c = s[i]!;
      let fragment: string;
      switch (c) {
        case "n": fragment = "\\n"; i++; break;
        case "t": fragment = "\\t"; i++; break;
        case "r": fragment = "\\r"; i++; break;
        case "a": fragment = "\\x07"; i++; break;
        case "f": fragment = "\\f"; i++; break;
        case "v": fragment = "\\v"; i++; break;
        case "\\": fragment = "\\\\"; i++; break;
        case "'": fragment = "'"; i++; break;
        case '"': fragment = '"'; i++; break;
        case "u": {
          const hex = s.slice(i + 1, i + 5);
          fragment = "\\u" + hex;
          i += 5;
          break;
        }
        default: fragment = c; i++; break;
      }
      result += fragment;
    } else {
      const cp = s.codePointAt(i)!;
      const char = String.fromCodePoint(cp);
      result += inCc ? classEscape(char) : regexEscape(char);
      i += cp > 0xffff ? 2 : 1;
    }
  }
  return result;
}

function regexEscape(c: string): string {
  return "\\.^$*+?()[]{|}".includes(c) ? "\\" + c : c;
}

function classEscape(c: string): string {
  return "\\]^-()[{}|/&".includes(c) ? "\\" + c : c;
}

// ---------------------------------------------------------------------------
// Wrapping helpers
// ---------------------------------------------------------------------------

function wrapIfNeeded(s: string): string {
  return isRegexAtom(s) ? s : "(?:" + s + ")";
}

function isRegexAtom(s: string): boolean {
  const len = s.length;
  if (len === 0 || len === 1) return true;
  return s.startsWith("[") || s.startsWith("(?") || s.startsWith("\\");
}

// ---------------------------------------------------------------------------
// Interpolation name collector (for definition dependency ordering)
// ---------------------------------------------------------------------------

function interpolationsInExpression(expr: Expression): string[] {
  return expr.alternatives.flatMap(interpolationsInSequence);
}

function interpolationsInSequence(seq: Sequence): string[] {
  return seq.items.flatMap(interpolationsInCapture);
}

function interpolationsInCapture(cap: Capture): string[] {
  return interpolationsInRepetition(cap.inner);
}

function interpolationsInRepetition(rep: Repetition): string[] {
  return interpolationsInExclusion(rep.inner);
}

function interpolationsInExclusion(excl: Exclusion): string[] {
  const base = interpolationsInRangeItem(excl.base);
  const rest = excl.excluded !== null ? interpolationsInRangeItem(excl.excluded) : [];
  return [...base, ...rest];
}

function interpolationsInRangeItem(item: RangeItem): string[] {
  if (item.kind === "singleAtom") return interpolationsInAtom(item.atom);
  return [];
}

function interpolationsInAtom(atom: Atom): string[] {
  switch (atom.kind) {
    case "literal":
    case "charClass":
    case "positionAssertion":
      return [];
    case "interpolation":
      return [atom.name];
    case "group":
      return interpolationsInExpression(atom.inner);
  }
}

// ---------------------------------------------------------------------------
// Capture validator collection
// ---------------------------------------------------------------------------

export function collectCaptureValidators(
  expr: Expression,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  return expr.alternatives.flatMap(seq =>
    collectValidatorsInSequence(seq, defs, classDefs),
  );
}

function collectValidatorsInSequence(
  seq: Sequence,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  return seq.items.flatMap(cap => collectValidatorsInCapture(cap, defs, classDefs));
}

function collectValidatorsInCapture(
  cap: Capture,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  const body = compileRepetition(cap.inner, defs, classDefs, []);
  const own: [string, string][] = cap.name !== null ? [[cap.name, body]] : [];
  const nested = collectValidatorsInRepetition(cap.inner, defs, classDefs);
  return [...own, ...nested];
}

function collectValidatorsInRepetition(
  rep: Repetition,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  return collectValidatorsInExclusion(rep.inner, defs, classDefs);
}

function collectValidatorsInExclusion(
  excl: Exclusion,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  return collectValidatorsInRangeItem(excl.base, defs, classDefs);
}

function collectValidatorsInRangeItem(
  item: RangeItem,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  if (item.kind === "singleAtom") return collectValidatorsInAtom(item.atom, defs, classDefs);
  return [];
}

function collectValidatorsInAtom(
  atom: Atom,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
): [string, string][] {
  if (atom.kind === "group") return collectCaptureValidators(atom.inner, defs, classDefs);
  return [];
}

// ---------------------------------------------------------------------------
// RepetitionInfo and compile-with-rep-info variants
// ---------------------------------------------------------------------------

export type RepetitionInfo = {
  groupName: string;
  subSource: string;
  captures: string[];
};

export function compileExpressionWithRepInfo(
  expr: Expression,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
): [string, RepetitionInfo[], number] {
  const [src, infos, newCtr] = compileExpressionRi(expr, defs, classDefs, counter, []);
  return [src, infos, newCtr];
}

function compileExpressionRi(
  expr: Expression,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  const seqs = expr.alternatives;
  const mergeable = seqs.length >= 2 && seqs.every(isClassItem);
  if (mergeable) {
    return ["[" + seqs.map(sequenceAsClassBody).join("") + "]", [], counter, seen];
  }
  const parts: string[] = [];
  const allInfos: RepetitionInfo[] = [];
  let ctr = counter;
  let curSeen = seen;
  for (const seq of seqs) {
    const [s, newInfos, newCtr, newSeen] = compileSequenceRi(seq, defs, classDefs, ctr, curSeen);
    parts.push(s);
    allInfos.push(...newInfos);
    ctr = newCtr;
    curSeen = newSeen;
  }
  return [parts.join("|"), allInfos, ctr, curSeen];
}

function compileSequenceRi(
  seq: Sequence,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  const parts: string[] = [];
  const allInfos: RepetitionInfo[] = [];
  let ctr = counter;
  let curSeen = seen;
  for (const cap of seq.items) {
    const [s, newInfos, newCtr, newSeen] = compileCaptureRi(cap, defs, classDefs, ctr, curSeen);
    parts.push(s);
    allInfos.push(...newInfos);
    ctr = newCtr;
    curSeen = newSeen;
  }
  return [parts.join(""), allInfos, ctr, curSeen];
}

function compileCaptureRi(
  cap: Capture,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  const [body, infos, newCtr, newSeen] = compileRepetitionRi(cap.inner, defs, classDefs, counter, seen);
  if (cap.name === null) return [body, infos, newCtr, newSeen];
  if (newSeen.includes(cap.name)) {
    return ["(?:" + body + ")", infos, newCtr, newSeen];
  }
  return ["(?<" + cap.name + ">" + body + ")", infos, newCtr, [cap.name, ...newSeen]];
}

function compileRepetitionRi(
  rep: Repetition,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  if (rep.count === null) {
    return compileExclusionRi(rep.inner, defs, classDefs, counter, seen);
  }
  const rc = rep.count;
  const innerCaps = collectAllCaptureNamesExcl(rep.inner);
  if (innerCaps.length === 0) {
    const [body, infos, newCtr, newSeen] = compileExclusionRi(rep.inner, defs, classDefs, counter, seen);
    return [wrapIfNeeded(body) + compileQuantifier(rc), infos, newCtr, newSeen];
  }
  // Named captures in body — wrap the whole repetition in __rep_N.
  const repName = "__rep_" + counter;
  const [mainBody, innerInfos, newCtr, newSeen] = compileExclusionRi(
    rep.inner, defs, classDefs, counter + 1, seen,
  );
  const subSource = compileExclusion(rep.inner, defs, classDefs, []);
  const main =
    "(?<" + repName + ">" + wrapIfNeeded(mainBody) + compileQuantifier(rc) + ")";
  const info: RepetitionInfo = { groupName: repName, subSource, captures: innerCaps };
  return [main, [info, ...innerInfos], newCtr, newSeen];
}

function compileExclusionRi(
  excl: Exclusion,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  if (excl.excluded === null) {
    return compileRangeItemRi(excl.base, defs, classDefs, counter, seen);
  }
  const baseClass = rangeItemAsClassOperand(excl.base, defs, classDefs);
  const exclClass = rangeItemAsClassOperand(excl.excluded, defs, classDefs);
  return ["[" + baseClass + "--" + exclClass + "]", [], counter, seen];
}

function compileRangeItemRi(
  item: RangeItem,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  if (item.kind === "singleAtom") {
    return compileAtomRi(item.atom, defs, classDefs, counter, seen);
  }
  if (item.from.kind === "literal" && item.to.kind === "literal") {
    return [
      "[" + rawToClassChar(item.from.content) + "-" + rawToClassChar(item.to.content) + "]",
      [], counter, seen,
    ];
  }
  return ["(?!)", [], counter, seen];
}

function compileAtomRi(
  atom: Atom,
  defs: Map<string, string>,
  classDefs: Map<string, string>,
  counter: number,
  seen: string[],
): [string, RepetitionInfo[], number, string[]] {
  switch (atom.kind) {
    case "literal": return [rawToRegex(atom.content), [], counter, seen];
    case "charClass": return [charClassStandalone(atom.name), [], counter, seen];
    case "interpolation": {
      const pattern = defs.get(atom.name);
      const src = pattern !== undefined ? "(?:" + pattern + ")" : "\\k<" + atom.name + ">";
      return [src, [], counter, seen];
    }
    case "group": {
      const [inner, infos, newCtr, newSeen] = compileExpressionRi(
        atom.inner, defs, classDefs, counter, seen,
      );
      return ["(?:" + inner + ")", infos, newCtr, newSeen];
    }
    case "positionAssertion":
      return [compilePositionAssertion(atom.name), [], counter, seen];
  }
}
