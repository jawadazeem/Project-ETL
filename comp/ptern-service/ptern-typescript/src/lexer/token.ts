export type Token =
  | { kind: "singleQuotedLiteral"; content: string }
  | { kind: "doubleQuotedLiteral"; content: string }
  | { kind: "characterClass"; name: string }
  | { kind: "integer"; value: number }
  | { kind: "rangeOperator" }
  | { kind: "asterisk" }
  | { kind: "alternativeOperator" }
  | { kind: "equals" }
  | { kind: "leftBrace" }
  | { kind: "rightBrace" }
  | { kind: "leftParen" }
  | { kind: "rightParen" }
  | { kind: "semicolon" }
  | { kind: "as" }
  | { kind: "excluding" }
  | { kind: "true" }
  | { kind: "false" }
  | { kind: "fewest" }
  | { kind: "bang" }
  | { kind: "positionAssertion"; name: string }
  | { kind: "questionMark" }
  | { kind: "identifier"; name: string }
  | { kind: "whitespace"; hasBlankLine: boolean }
  | { kind: "comment"; content: string };

export type LexError =
  | { kind: "unexpectedCharacter"; char: string }
  | { kind: "unterminatedString" }
  | { kind: "inlineComment" };

export function tokenDisplay(token: Token): string {
  switch (token.kind) {
    case "singleQuotedLiteral": return `'${token.content}'`;
    case "doubleQuotedLiteral": return `"${token.content}"`;
    case "characterClass": return `%${token.name}`;
    case "integer": return String(token.value);
    case "rangeOperator": return "..";
    case "asterisk": return "*";
    case "alternativeOperator": return "|";
    case "equals": return "=";
    case "leftBrace": return "{";
    case "rightBrace": return "}";
    case "leftParen": return "(";
    case "rightParen": return ")";
    case "semicolon": return ";";
    case "as": return "as";
    case "excluding": return "excluding";
    case "true": return "true";
    case "false": return "false";
    case "fewest": return "fewest";
    case "bang": return "!";
    case "positionAssertion": return `@${token.name}`;
    case "questionMark": return "?";
    case "identifier": return token.name;
    case "whitespace": return "<whitespace>";
    case "comment": return "<comment>";
  }
}

export function tokensEqual(a: Token, b: Token): boolean {
  if (a.kind !== b.kind) return false;
  switch (a.kind) {
    case "singleQuotedLiteral":
    case "doubleQuotedLiteral":
      return a.content === (b as typeof a).content;
    case "characterClass":
    case "positionAssertion":
    case "identifier":
      return a.name === (b as typeof a).name;
    case "integer":
      return a.value === (b as typeof a).value;
    case "whitespace":
      return a.hasBlankLine === (b as typeof a).hasBlankLine;
    case "comment":
      return a.content === (b as typeof a).content;
    default:
      return true;
  }
}
