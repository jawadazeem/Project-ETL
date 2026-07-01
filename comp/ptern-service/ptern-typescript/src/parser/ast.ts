export type RepUpper =
  | { kind: "exact"; value: number }
  | { kind: "unbounded" }
  | { kind: "none" };

export type RepCount = {
  min: number;
  max: RepUpper;
  lazy: boolean;
};

export type Atom =
  | { kind: "literal"; content: string }
  | { kind: "charClass"; name: string }
  | { kind: "interpolation"; name: string }
  | { kind: "group"; inner: Expression }
  | { kind: "positionAssertion"; name: string };

export type RangeItem =
  | { kind: "singleAtom"; atom: Atom }
  | { kind: "charRange"; from: Atom; to: Atom };

export type Exclusion = {
  base: RangeItem;
  excluded: RangeItem | null;
};

export type Repetition = {
  inner: Exclusion;
  count: RepCount | null;
};

export type Capture = {
  inner: Repetition;
  name: string | null;
};

export type Sequence = { items: Capture[] };

// An alternation of one or more sequences.
export type Expression = { alternatives: Sequence[] };

export type Annotation = {
  comments: string[];
  name: string;
  value: boolean;
};

export type Definition = {
  comments: string[];
  name: string;
  body: Expression;
};

export type ParsedPtern = {
  pternComments: string[];
  annotations: Annotation[];
  definitions: Definition[];
  bodyComments: string[];
  body: Expression;
};

export type ParseError =
  | { kind: "unexpectedEndOfInput" }
  | { kind: "unexpectedToken"; expected: string; got: string }
  | { kind: "orphanedComment" }
  | { kind: "trailingComment" };
