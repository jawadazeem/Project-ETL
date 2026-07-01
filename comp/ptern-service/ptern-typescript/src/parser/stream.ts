import { type Token, tokenDisplay, tokensEqual } from "../lexer/token";

export class Stream {
  private constructor(
    private readonly tokens: readonly Token[],
    private readonly offset: number,
  ) {}

  static from(tokens: Token[]): Stream {
    return new Stream(tokens, 0);
  }

  remaining(): Token[] {
    return this.tokens.slice(this.offset) as Token[];
  }

  peekRaw(): Token | null {
    return this.tokens[this.offset] ?? null;
  }

  // Peek at the next non-whitespace token (does not skip comments).
  peek(): Token | null {
    let i = this.offset;
    while (i < this.tokens.length) {
      const t = this.tokens[i]!;
      if (t.kind !== "whitespace") return t;
      i++;
    }
    return null;
  }

  advance(): [Token | null, Stream] {
    const t = this.tokens[this.offset] ?? null;
    return [t, new Stream(this.tokens, this.offset + (t !== null ? 1 : 0))];
  }

  skipWhitespace(): Stream {
    let i = this.offset;
    while (i < this.tokens.length && this.tokens[i]!.kind === "whitespace") i++;
    return new Stream(this.tokens, i);
  }

  // Drop a single Whitespace(hasBlankLine=false) token if present; stop at blank-line whitespace.
  skipNonBlankWhitespace(): Stream {
    const t = this.tokens[this.offset];
    if (t?.kind === "whitespace" && !t.hasBlankLine) {
      return new Stream(this.tokens, this.offset + 1);
    }
    return this;
  }

  nextIsWhitespace(): boolean {
    return this.tokens[this.offset]?.kind === "whitespace";
  }

  eat(expected: Token): [boolean, Stream] {
    const s = this.skipWhitespace();
    const t = s.tokens[s.offset];
    if (t !== undefined && tokensEqual(t, expected)) {
      return [true, new Stream(s.tokens, s.offset + 1)];
    }
    return [false, this];
  }

  eatWhitespace(): Stream {
    const t = this.tokens[this.offset];
    if (t?.kind === "whitespace") return new Stream(this.tokens, this.offset + 1);
    return this;
  }

  eatAllWhitespace(): Stream {
    return this.skipWhitespace();
  }

  isEmpty(): boolean {
    return this.peek() === null;
  }

  tokenDisplay(token: Token): string {
    return tokenDisplay(token);
  }
}
