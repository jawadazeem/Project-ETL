import {
  type Annotation,
  type Atom,
  type Capture,
  type Definition,
  type Exclusion,
  type Expression,
  type ParseError,
  type ParsedPtern,
  type RangeItem,
  type RepCount,
  type RepUpper,
  type Repetition,
  type Sequence,
} from "./ast";
import { Stream } from "./stream";
import { type Token, tokenDisplay } from "../lexer/token";

// Internal error thrown during parsing, caught by parse().
class InternalParseError {
  constructor(readonly error: ParseError) {}
}

function fail(error: ParseError): never {
  throw new InternalParseError(error);
}

export function parse(tokens: Token[]): ParsedPtern | ParseError {
  try {
    const s = Stream.from(tokens);
    const [ptern, s2] = parsePtern(s);
    if (!s2.isEmpty()) {
      const tok = s2.peek();
      return {
        kind: "unexpectedToken",
        expected: "end of input",
        got: tok !== null ? tokenDisplay(tok) : "<unknown>",
      };
    }
    return ptern;
  } catch (e) {
    if (e instanceof InternalParseError) return e.error;
    throw e;
  }
}

// ---------------------------------------------------------------------------
// Top-level
// ---------------------------------------------------------------------------

function parsePtern(s: Stream): [ParsedPtern, Stream] {
  s = s.skipWhitespace();
  let [firstBlock, s1] = collectCommentBlock(s);
  s = s1;

  let pternComments: string[];
  let carried: string[];
  [pternComments, carried, s] = resolveLeadingComments(firstBlock, s);

  let annotations: Annotation[];
  [annotations, carried, s] = parseAnnotations(s, carried);

  let definitions: Definition[];
  [definitions, carried, s] = parseDefinitions(s, carried);

  const bodyComments = carried;
  const [body, s2] = parseExpression(s);
  s = s2;

  s = s.skipWhitespace();
  const raw = s.peekRaw();
  if (raw?.kind === "comment") fail({ kind: "trailingComment" });
  if (raw !== null) {
    fail({ kind: "unexpectedToken", expected: "end of input", got: tokenDisplay(raw) });
  }

  return [{ pternComments, annotations, definitions, bodyComments, body }, s];
}

function resolveLeadingComments(
  firstBlock: string[],
  s: Stream,
): [string[], string[], Stream] {
  const raw = s.peekRaw();
  if (raw?.kind === "whitespace" && raw.hasBlankLine) {
    const [, s1] = s.advance();
    const s2 = s1.skipNonBlankWhitespace();
    const [itemBlock, s3] = collectCommentBlock(s2);
    const raw2 = s3.peekRaw();
    if (raw2?.kind === "whitespace" && raw2.hasBlankLine) {
      if (itemBlock.length > 0) fail({ kind: "orphanedComment" });
      return [firstBlock, itemBlock, s3];
    }
    return [firstBlock, itemBlock, s3];
  }
  return [[], firstBlock, s];
}

// ---------------------------------------------------------------------------
// Annotations
// ---------------------------------------------------------------------------

function parseAnnotations(
  s: Stream,
  carried: string[],
): [Annotation[], string[], Stream] {
  const acc: Annotation[] = [];
  while (true) {
    s = s.skipWhitespace();
    if (s.peekRaw()?.kind !== "bang") break;
    const [ann, s1] = parseAnnotation(s, carried);
    s = s1;
    const [nextBlock, s2] = collectItemComments(s);
    s = s2;
    const raw = s.peekRaw();
    if (raw?.kind === "whitespace" && raw.hasBlankLine) {
      if (nextBlock.length > 0) fail({ kind: "orphanedComment" });
      carried = nextBlock;
    } else {
      carried = nextBlock;
    }
    acc.push(ann);
  }
  return [acc, carried, s];
}

function parseAnnotation(s: Stream, comments: string[]): [Annotation, Stream] {
  const [, s1] = s.advance(); // consume !
  const s2 = s1.skipWhitespace();
  const [name, s3] = expectIdentifier(s2);
  const s4 = s3.skipWhitespace();
  const [, s5] = expectToken(s4, { kind: "equals" });
  const s6 = s5.skipWhitespace();
  const next = s6.peek();
  if (next?.kind === "true") {
    const [, s7] = s6.advance();
    return [{ comments, name, value: true }, s7];
  }
  if (next?.kind === "false") {
    const [, s7] = s6.advance();
    return [{ comments, name, value: false }, s7];
  }
  if (next !== null) fail({ kind: "unexpectedToken", expected: "true or false", got: tokenDisplay(next) });
  fail({ kind: "unexpectedEndOfInput" });
}

// ---------------------------------------------------------------------------
// Definitions
// ---------------------------------------------------------------------------

function parseDefinitions(
  s: Stream,
  carried: string[],
): [Definition[], string[], Stream] {
  const acc: Definition[] = [];
  while (true) {
    s = s.skipWhitespace();
    const [newComments, s1] = collectCommentBlock(s);
    s = s1;
    const raw = s.peekRaw();
    if (raw?.kind === "whitespace" && raw.hasBlankLine) {
      if (newComments.length > 0) fail({ kind: "orphanedComment" });
      break;
    }
    const allCarried = [...carried, ...newComments];
    if (!looksLikeDefinition(s)) {
      carried = allCarried;
      break;
    }
    const [def, s2] = parseDefinition(s, allCarried);
    s = s2;
    const [nextBlock, s3] = collectItemComments(s);
    s = s3;
    const raw2 = s.peekRaw();
    if (raw2?.kind === "whitespace" && raw2.hasBlankLine) {
      if (nextBlock.length > 0) fail({ kind: "orphanedComment" });
      carried = nextBlock;
    } else {
      carried = nextBlock;
    }
    acc.push(def);
  }
  return [acc, carried, s];
}

function looksLikeDefinition(s: Stream): boolean {
  const remaining = s.remaining();
  if (remaining[0]?.kind !== "identifier") return false;
  const rest = remaining.slice(1).filter(t => t.kind !== "whitespace");
  return rest[0]?.kind === "equals";
}

function parseDefinition(s: Stream, comments: string[]): [Definition, Stream] {
  const [name, s1] = expectIdentifier(s);
  const s2 = s1.skipWhitespace();
  const [, s3] = expectToken(s2, { kind: "equals" });
  const s4 = s3.skipWhitespace();
  const [body, s5] = parseExpression(s4);
  const s6 = s5.skipWhitespace();
  const [, s7] = expectToken(s6, { kind: "semicolon" });
  return [{ comments, name, body }, s7];
}

// ---------------------------------------------------------------------------
// Comment collection helpers
// ---------------------------------------------------------------------------

function collectCommentBlock(s: Stream): [string[], Stream] {
  const acc: string[] = [];
  while (true) {
    const raw = s.peekRaw();
    if (raw?.kind !== "comment") break;
    const [, s1] = s.advance();
    s = s1.skipNonBlankWhitespace();
    acc.push(raw.content);
  }
  return [acc, s];
}

function collectItemComments(s: Stream): [string[], Stream] {
  const s1 = s.skipNonBlankWhitespace();
  return collectCommentBlock(s1);
}

// ---------------------------------------------------------------------------
// Expression: alternation
// ---------------------------------------------------------------------------

function parseExpression(s: Stream): [Expression, Stream] {
  const [firstSeq, s1] = parseSequence(s);
  const alternatives: Sequence[] = [firstSeq];
  s = s1;
  while (true) {
    const s2 = s.skipWhitespace();
    if (s2.peekRaw()?.kind !== "alternativeOperator") break;
    const [, s3] = s2.advance();
    const s4 = s3.skipWhitespace();
    const [seq, s5] = parseSequence(s4);
    alternatives.push(seq);
    s = s5;
  }
  return [{ alternatives }, s];
}

// ---------------------------------------------------------------------------
// Sequence
// ---------------------------------------------------------------------------

function parseSequence(s: Stream): [Sequence, Stream] {
  const [first, s1] = parseCapture(s);
  const items: Capture[] = [first];
  s = s1;
  while (s.nextIsWhitespace() && nextStartsCapture(s)) {
    s = s.eatAllWhitespace();
    if (!startsCapture(s.peek())) break;
    const [cap, s2] = parseCapture(s);
    items.push(cap);
    s = s2;
  }
  return [{ items }, s];
}

function nextStartsCapture(s: Stream): boolean {
  return startsCapture(s.skipWhitespace().peekRaw());
}

function startsCapture(tok: Token | null): boolean {
  if (tok === null) return false;
  return (
    tok.kind === "singleQuotedLiteral" ||
    tok.kind === "doubleQuotedLiteral" ||
    tok.kind === "characterClass" ||
    tok.kind === "positionAssertion" ||
    tok.kind === "leftBrace" ||
    tok.kind === "leftParen"
  );
}

// ---------------------------------------------------------------------------
// Capture: `repetition (as identifier)?`
// ---------------------------------------------------------------------------

function parseCapture(s: Stream): [Capture, Stream] {
  const [rep, s1] = parseRepetition(s);
  const s2 = s1.skipWhitespace();
  if (s2.peekRaw()?.kind === "as") {
    const [, s3] = s2.advance();
    const s4 = s3.skipWhitespace();
    const [name, s5] = expectIdentifier(s4);
    return [{ inner: rep, name }, s5];
  }
  return [{ inner: rep, name: null }, s1];
}

// ---------------------------------------------------------------------------
// Repetition: `exclusion (* rep-count fewest?)?`
// ---------------------------------------------------------------------------

function parseRepetition(s: Stream): [Repetition, Stream] {
  const [excl, s1] = parseExclusion(s);
  const s2 = s1.skipWhitespace();
  if (s2.peekRaw()?.kind !== "asterisk") return [{ inner: excl, count: null }, s1];
  const [, s3] = s2.advance();
  const s4 = s3.skipWhitespace();
  const [count, s5] = parseRepCount(s4);
  const s6 = s5.skipWhitespace();
  if (s6.peekRaw()?.kind === "fewest") {
    const [, s7] = s6.advance();
    return [{ inner: excl, count: { min: count.min, max: count.max, lazy: true } }, s7];
  }
  return [{ inner: excl, count }, s5];
}

function parseRepCount(s: Stream): [RepCount, Stream] {
  const next = s.peek();
  if (next?.kind !== "integer") {
    if (next !== null) fail({ kind: "unexpectedToken", expected: "repetition count (integer)", got: tokenDisplay(next) });
    fail({ kind: "unexpectedEndOfInput" });
  }
  const [, s1] = s.advance();
  const min = next.value;
  if (s1.peekRaw()?.kind === "rangeOperator") {
    const [, s2] = s1.advance();
    const [upper, s3] = parseRepUpper(s2);
    return [{ min, max: upper, lazy: false }, s3];
  }
  return [{ min, max: { kind: "none" }, lazy: false }, s1];
}

function parseRepUpper(s: Stream): [RepUpper, Stream] {
  const next = s.peek();
  if (next?.kind === "questionMark") {
    const [, s1] = s.advance();
    return [{ kind: "unbounded" }, s1];
  }
  if (next?.kind === "integer") {
    const [, s1] = s.advance();
    return [{ kind: "exact", value: next.value }, s1];
  }
  if (next !== null) fail({ kind: "unexpectedToken", expected: "upper bound (integer or ?)", got: tokenDisplay(next) });
  fail({ kind: "unexpectedEndOfInput" });
}

// ---------------------------------------------------------------------------
// Exclusion: `range-item (excluding range-item)?`
// ---------------------------------------------------------------------------

function parseExclusion(s: Stream): [Exclusion, Stream] {
  const [base, s1] = parseRangeItem(s);
  const s2 = s1.skipWhitespace();
  if (s2.peekRaw()?.kind === "excluding") {
    const [, s3] = s2.advance();
    const s4 = s3.skipWhitespace();
    const [excl, s5] = parseRangeItem(s4);
    return [{ base, excluded: excl }, s5];
  }
  return [{ base, excluded: null }, s1];
}

// ---------------------------------------------------------------------------
// Range item: `atom (.. atom)?`
// ---------------------------------------------------------------------------

function parseRangeItem(s: Stream): [RangeItem, Stream] {
  const [from, s1] = parseAtom(s);
  if (s1.peekRaw()?.kind === "rangeOperator") {
    const [, s2] = s1.advance();
    const [to, s3] = parseAtom(s2);
    return [{ kind: "charRange", from, to }, s3];
  }
  return [{ kind: "singleAtom", atom: from }, s1];
}

// ---------------------------------------------------------------------------
// Atom
// ---------------------------------------------------------------------------

function parseAtom(s: Stream): [Atom, Stream] {
  const next = s.peek();
  if (next === null) fail({ kind: "unexpectedEndOfInput" });
  switch (next.kind) {
    case "singleQuotedLiteral":
    case "doubleQuotedLiteral": {
      const [, s1] = s.advance();
      return [{ kind: "literal", content: next.content }, s1];
    }
    case "characterClass": {
      const [, s1] = s.advance();
      return [{ kind: "charClass", name: next.name }, s1];
    }
    case "positionAssertion": {
      const [, s1] = s.advance();
      return [{ kind: "positionAssertion", name: next.name }, s1];
    }
    case "leftBrace": return parseInterpolation(s);
    case "leftParen": return parseGroup(s);
    default:
      fail({ kind: "unexpectedToken", expected: "literal, character class, position assertion, { or (", got: tokenDisplay(next) });
  }
}

function parseInterpolation(s: Stream): [Atom, Stream] {
  const [, s1] = s.advance(); // consume {
  const s2 = s1.skipWhitespace();
  const [name, s3] = expectIdentifier(s2);
  const s4 = s3.skipWhitespace();
  const [, s5] = expectToken(s4, { kind: "rightBrace" });
  return [{ kind: "interpolation", name }, s5];
}

function parseGroup(s: Stream): [Atom, Stream] {
  const [, s1] = s.advance(); // consume (
  const s2 = s1.skipWhitespace();
  const [expr, s3] = parseExpression(s2);
  const s4 = s3.skipWhitespace();
  const [, s5] = expectToken(s4, { kind: "rightParen" });
  return [{ kind: "group", inner: expr }, s5];
}

// ---------------------------------------------------------------------------
// Utility helpers
// ---------------------------------------------------------------------------

function expectIdentifier(s: Stream): [string, Stream] {
  const next = s.peek();
  if (next?.kind === "identifier") {
    const [, s1] = s.advance();
    return [next.name, s1];
  }
  if (next !== null) fail({ kind: "unexpectedToken", expected: "identifier", got: tokenDisplay(next) });
  fail({ kind: "unexpectedEndOfInput" });
}

function expectToken(s: Stream, expected: Token): [Token, Stream] {
  const next = s.peek();
  if (next !== null && next.kind === expected.kind) {
    const [, s1] = s.advance();
    return [next, s1];
  }
  const expectedDisplay = tokenDisplay(expected);
  if (next !== null) fail({ kind: "unexpectedToken", expected: expectedDisplay, got: tokenDisplay(next) });
  fail({ kind: "unexpectedToken", expected: expectedDisplay, got: "end of input" });
}
