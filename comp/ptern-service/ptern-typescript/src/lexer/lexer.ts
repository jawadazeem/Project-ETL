import { type LexError, type Token } from "./token";

// Pop the first Unicode code point from s, returning [char, rest] or null.
function popChar(s: string): [string, string] | null {
  if (s.length === 0) return null;
  const cp = s.codePointAt(0)!;
  const char = String.fromCodePoint(cp);
  return [char, s.slice(char.length)];
}

function isDigit(c: string): boolean {
  const n = c.charCodeAt(0);
  return n >= 48 && n <= 57;
}

function isUpper(c: string): boolean {
  const n = c.charCodeAt(0);
  return n >= 65 && n <= 90;
}

function isLower(c: string): boolean {
  const n = c.charCodeAt(0);
  return n >= 97 && n <= 122;
}

function isAlpha(c: string): boolean {
  return isUpper(c) || isLower(c);
}

function isAlnum(c: string): boolean {
  return isAlpha(c) || isDigit(c);
}

function isHexDigit(c: string): boolean {
  const n = c.charCodeAt(0);
  return (n >= 48 && n <= 57) || (n >= 97 && n <= 102) || (n >= 65 && n <= 70);
}

function takeHexDigits(s: string, count: number): [string, string] | null {
  let digits = "";
  let rest = s;
  for (let i = 0; i < count; i++) {
    const r = popChar(rest);
    if (r === null || !isHexDigit(r[0])) return null;
    digits += r[0];
    rest = r[1];
  }
  return [digits, rest];
}

// Lex a whitespace run. Called after the first whitespace char has been consumed.
function lexWhitespace(
  input: string,
  hadNewline: boolean,
  hasBlankLine: boolean,
  acc: Token[],
): Token[] | LexError {
  const r = popChar(input);
  if (r === null) {
    acc.push({ kind: "whitespace", hasBlankLine });
    return acc;
  }
  const [c, rest] = r;
  if (c === " " || c === "\t") return lexWhitespace(rest, hadNewline, hasBlankLine, acc);
  if (c === "\n" || c === "\r") return lexWhitespace(rest, true, hasBlankLine || hadNewline, acc);
  acc.push({ kind: "whitespace", hasBlankLine });
  return doLex(input, hadNewline, acc);
}

// Lex a comment. Called after the opening '#' has been consumed.
function lexComment(input: string, content: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) {
    acc.push({ kind: "comment", content });
    return acc;
  }
  const [c, rest] = r;
  if (c === "\n" || c === "\r") {
    acc.push({ kind: "comment", content });
    return doLex(input, false, acc);
  }
  return lexComment(rest, content + c, acc);
}

// Lex a single-quoted string. Called after the opening ' has been consumed.
function lexSingleQuoted(input: string, content: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return { kind: "unterminatedString" };
  const [c, rest] = r;
  if (c === "\n" || c === "\r") return { kind: "unterminatedString" };
  if (c === "'") {
    acc.push({ kind: "singleQuotedLiteral", content });
    return doLex(rest, false, acc);
  }
  if (c === "\\") return lexEscape(rest, content, acc, lexSingleQuoted);
  return lexSingleQuoted(rest, content + c, acc);
}

// Lex a double-quoted string. Called after the opening " has been consumed.
function lexDoubleQuoted(input: string, content: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return { kind: "unterminatedString" };
  const [c, rest] = r;
  if (c === "\n" || c === "\r") return { kind: "unterminatedString" };
  if (c === '"') {
    acc.push({ kind: "doubleQuotedLiteral", content });
    return doLex(rest, false, acc);
  }
  if (c === "\\") return lexEscape(rest, content, acc, lexDoubleQuoted);
  return lexDoubleQuoted(rest, content + c, acc);
}

type StringLexer = (input: string, content: string, acc: Token[]) => Token[] | LexError;

// Called after \ has been consumed inside a string literal.
function lexEscape(
  input: string,
  content: string,
  acc: Token[],
  cont: StringLexer,
): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return { kind: "unterminatedString" };
  const [c, rest] = r;
  if (c === "u") {
    const digits = takeHexDigits(rest, 4);
    if (digits === null) return { kind: "unterminatedString" };
    return cont(digits[1], content + "\\u" + digits[0], acc);
  }
  return cont(rest, content + "\\" + c, acc);
}

// Called after % has been consumed.
function lexCharacterClass(input: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return { kind: "unexpectedCharacter", char: "%" };
  const [c, rest] = r;
  if (!isUpper(c)) return { kind: "unexpectedCharacter", char: c };
  return lexCharacterClassRest(rest, c, acc);
}

function lexCharacterClassRest(input: string, name: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) {
    acc.push({ kind: "characterClass", name });
    return doLex(input, false, acc);
  }
  const [c, rest] = r;
  if (isAlpha(c)) return lexCharacterClassRest(rest, name + c, acc);
  acc.push({ kind: "characterClass", name });
  return doLex(input, false, acc);
}

// Called after @ has been consumed.
function lexPositionAssertion(input: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return { kind: "unexpectedCharacter", char: "@" };
  const [c, rest] = r;
  if (!isAlpha(c)) return { kind: "unexpectedCharacter", char: "@" };
  return lexPositionAssertionRest(rest, c, acc);
}

function lexPositionAssertionRest(input: string, name: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) {
    acc.push({ kind: "positionAssertion", name });
    return doLex(input, false, acc);
  }
  const [c, rest] = r;
  if (isAlnum(c) || c === "-") return lexPositionAssertionRest(rest, name + c, acc);
  acc.push({ kind: "positionAssertion", name });
  return doLex(input, false, acc);
}

function lexInteger(input: string, digits: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return finishInteger(digits, input, acc);
  const [c, rest] = r;
  if (isDigit(c)) return lexInteger(rest, digits + c, acc);
  return finishInteger(digits, input, acc);
}

function finishInteger(digits: string, rest: string, acc: Token[]): Token[] | LexError {
  const n = parseInt(digits, 10);
  if (isNaN(n)) return { kind: "unexpectedCharacter", char: digits };
  acc.push({ kind: "integer", value: n });
  return doLex(rest, false, acc);
}

function lexIdentifier(input: string, name: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return finishIdentifier(name, input, acc);
  const [c, rest] = r;
  if (isAlnum(c) || c === "-") return lexIdentifier(rest, name + c, acc);
  return finishIdentifier(name, input, acc);
}

function finishIdentifier(name: string, rest: string, acc: Token[]): Token[] | LexError {
  let tok: Token;
  switch (name) {
    case "as": tok = { kind: "as" }; break;
    case "excluding": tok = { kind: "excluding" }; break;
    case "fewest": tok = { kind: "fewest" }; break;
    case "true": tok = { kind: "true" }; break;
    case "false": tok = { kind: "false" }; break;
    default: tok = { kind: "identifier", name };
  }
  acc.push(tok);
  return doLex(rest, false, acc);
}

// Called after the first . has been consumed.
function lexRangeOperator(input: string, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r !== null && r[0] === ".") {
    acc.push({ kind: "rangeOperator" });
    return doLex(r[1], false, acc);
  }
  return { kind: "unexpectedCharacter", char: "." };
}

// Main dispatch. atLineStart: no non-whitespace seen since last newline (or start).
// Tokens are pushed to acc in order.
function doLex(input: string, atLineStart: boolean, acc: Token[]): Token[] | LexError {
  const r = popChar(input);
  if (r === null) return acc;
  const [c, rest] = r;
  switch (c) {
    case " ": case "\t": return lexWhitespace(rest, atLineStart, false, acc);
    case "\n": case "\r": return lexWhitespace(rest, true, false, acc);
    case "#":
      if (!atLineStart) return { kind: "inlineComment" };
      return lexComment(rest, "", acc);
    case "'": return lexSingleQuoted(rest, "", acc);
    case '"': return lexDoubleQuoted(rest, "", acc);
    case "%": return lexCharacterClass(rest, acc);
    case ".": return lexRangeOperator(rest, acc);
    case "!": acc.push({ kind: "bang" }); return doLex(rest, false, acc);
    case "@": return lexPositionAssertion(rest, acc);
    case "?": acc.push({ kind: "questionMark" }); return doLex(rest, false, acc);
    case "*": acc.push({ kind: "asterisk" }); return doLex(rest, false, acc);
    case "|": acc.push({ kind: "alternativeOperator" }); return doLex(rest, false, acc);
    case "=": acc.push({ kind: "equals" }); return doLex(rest, false, acc);
    case "{": acc.push({ kind: "leftBrace" }); return doLex(rest, false, acc);
    case "}": acc.push({ kind: "rightBrace" }); return doLex(rest, false, acc);
    case "(": acc.push({ kind: "leftParen" }); return doLex(rest, false, acc);
    case ")": acc.push({ kind: "rightParen" }); return doLex(rest, false, acc);
    case ";": acc.push({ kind: "semicolon" }); return doLex(rest, false, acc);
    default:
      if (isDigit(c)) return lexInteger(rest, c, acc);
      if (isAlpha(c)) return lexIdentifier(rest, c, acc);
      return { kind: "unexpectedCharacter", char: c };
  }
}

export function lex(input: string): Token[] | LexError {
  return doLex(input, true, []);
}
