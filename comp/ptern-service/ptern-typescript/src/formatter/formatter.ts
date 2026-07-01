import { lex } from "../lexer/lexer";
import type { LexError } from "../lexer/token";
import { parse } from "../parser/parser";
import type { Annotation, Definition, Expression, ParseError, ParsedPtern, RangeItem, RepCount, Repetition, Atom } from "../parser/ast";

// ---------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------

export type FormatOptions = {
  lineWidth: number;
  compact: boolean;
  aligned: boolean;
  reordered: boolean;
};

export const defaultFormatOptions: FormatOptions = {
  lineWidth: 80,
  compact: false,
  aligned: true,
  reordered: false,
};

export type FormatError =
  | { kind: "formatLexError"; error: LexError }
  | { kind: "formatParseError"; error: ParseError }
  | { kind: "invalidLineWidth" };

// ---------------------------------------------------------------------------
// Public entry point
// ---------------------------------------------------------------------------

export function format(source: string, options: FormatOptions): string | FormatError {
  if (options.lineWidth < 40) return { kind: "invalidLineWidth" };
  const tokensOrErr = lex(source);
  if (!Array.isArray(tokensOrErr)) return { kind: "formatLexError", error: tokensOrErr };
  const parsedOrErr = parse(tokensOrErr);
  if ("kind" in parsedOrErr) return { kind: "formatParseError", error: parsedOrErr };
  return emitPtern(parsedOrErr, options);
}

// ---------------------------------------------------------------------------
// Piece type for line-breaking
// ---------------------------------------------------------------------------

type Piece =
  | { kind: "text"; content: string }
  | { kind: "seqSpace" }
  | { kind: "alt" };

function pieceLen(p: Piece, compact: boolean): number {
  switch (p.kind) {
    case "text": return p.content.length;
    case "seqSpace": return 1;
    case "alt": return compact ? 1 : 3;
  }
}

function piecesToStr(pieces: Piece[], compact: boolean): string {
  return pieces
    .map(p => {
      switch (p.kind) {
        case "text": return p.content;
        case "seqSpace": return " ";
        case "alt": return compact ? "|" : " | ";
      }
    })
    .join("");
}

// ---------------------------------------------------------------------------
// Top-level emitter
// ---------------------------------------------------------------------------

function emitPtern(parsed: ParsedPtern, opts: FormatOptions): string {
  const compact = opts.compact;
  const aligned = opts.aligned;
  const lineWidth = opts.lineWidth;

  const sortedAnns = [...parsed.annotations].sort((a, b) => a.name.localeCompare(b.name));
  const annCol = aligned && sortedAnns.length > 0
    ? computeAlignCol(sortedAnns.map(a => a.name))
    : 0;
  const annLines = emitAnnotationBlock(sortedAnns, annCol, aligned, compact);

  const orderedDefs = opts.reordered
    ? reorderDefinitions(parsed.definitions)
    : parsed.definitions;
  const defCol = aligned && orderedDefs.length > 0
    ? computeAlignCol(orderedDefs.map(d => d.name))
    : 0;
  const defLines = emitDefinitionBlock(orderedDefs, defCol, aligned, compact, lineWidth);

  const bodyCommentLines = parsed.bodyComments.map(emitCommentLine);
  const bodyPieces = emitExprPieces(parsed.body, compact);
  const bodyLines = breakBodyExpr(bodyPieces, "", lineWidth, compact);

  const hasAnns = sortedAnns.length > 0;
  const hasDefs = orderedDefs.length > 0;
  const annSep = hasAnns && !compact ? [""] : [];
  const defSep = hasDefs && !compact ? [""] : [];

  const pternBlock = parsed.pternComments.length === 0
    ? []
    : [...parsed.pternComments.map(emitCommentLine), ""];

  const allLines = [
    ...pternBlock,
    ...annLines,
    ...annSep,
    ...defLines,
    ...defSep,
    ...bodyCommentLines,
    ...bodyLines,
  ];

  return allLines.join("\n");
}

// ---------------------------------------------------------------------------
// Comment
// ---------------------------------------------------------------------------

function emitCommentLine(content: string): string {
  return "#" + content;
}

// ---------------------------------------------------------------------------
// Annotation block
// ---------------------------------------------------------------------------

function emitAnnotationBlock(
  annotations: Annotation[],
  alignCol: number,
  aligned: boolean,
  compact: boolean,
): string[] {
  const lines: string[] = [];
  annotations.forEach((ann, i) => {
    const isFirst = i === 0;
    if (ann.comments.length > 0 && !isFirst && !compact) lines.push("");
    for (const c of ann.comments) lines.push(emitCommentLine(c));
    lines.push(emitAnnotationLine(ann, alignCol, aligned));
  });
  return lines;
}

function emitAnnotationLine(ann: Annotation, alignCol: number, aligned: boolean): string {
  const valStr = ann.value ? "true" : "false";
  const namePart = "!" + ann.name;
  const spacing = aligned ? " ".repeat(alignCol - namePart.length) : " ";
  return namePart + spacing + "= " + valStr;
}

// ---------------------------------------------------------------------------
// Definition ordering (reordered = true)
// ---------------------------------------------------------------------------

function reorderDefinitions(defs: Definition[]): Definition[] {
  const defNameSet = new Set(defs.map(d => d.name));
  const adj: [string, string[]][] = defs.map(d => [d.name, collectDefRefs(d.body, defNameSet)]);
  return topoLayerSort(defs, adj);
}

function collectDefRefs(expr: Expression, defNames: Set<string>): string[] {
  return expr.alternatives.flatMap(seq =>
    seq.items.flatMap(cap => refsFromRep(cap.inner, defNames)),
  );
}

function refsFromRep(rep: Repetition, defNames: Set<string>): string[] {
  return refsFromExcl(rep.inner, defNames);
}

function refsFromExcl(excl: { base: RangeItem; excluded: RangeItem | null }, defNames: Set<string>): string[] {
  const baseRefs = refsFromRangeItem(excl.base, defNames);
  const exclRefs = excl.excluded !== null ? refsFromRangeItem(excl.excluded, defNames) : [];
  return [...baseRefs, ...exclRefs];
}

function refsFromRangeItem(ri: RangeItem, defNames: Set<string>): string[] {
  if (ri.kind === "charRange") {
    return [...refsFromAtom(ri.from, defNames), ...refsFromAtom(ri.to, defNames)];
  }
  return refsFromAtom(ri.atom, defNames);
}

function refsFromAtom(atom: Atom, defNames: Set<string>): string[] {
  switch (atom.kind) {
    case "interpolation": return defNames.has(atom.name) ? [atom.name] : [];
    case "group": return collectDefRefs(atom.inner, defNames);
    default: return [];
  }
}

function topoLayerSort(defs: Definition[], adj: [string, string[]][]): Definition[] {
  let layers = adj.map<[string, number]>(([name]) => [name, -1]);
  let changed = true;
  const maxIters = adj.length + 1;
  for (let iter = 0; iter < maxIters && changed; iter++) {
    changed = false;
    const newLayers = layers.map<[string, number]>(([name, curLayer]) => {
      const entry = adj.find(([n]) => n === name);
      const deps = entry ? entry[1] : [];
      const newLayer = singleLayer(deps, layers);
      if (newLayer !== curLayer) { changed = true; return [name, newLayer]; }
      return [name, curLayer];
    });
    layers = newLayers;
  }

  const cycleNames = new Set(layers.filter(([, l]) => l < 0).map(([n]) => n));
  const layeredNames = layers
    .filter(([, l]) => l >= 0)
    .sort(([na, la], [nb, lb]) => la !== lb ? la - lb : na.localeCompare(nb))
    .map(([n]) => n);

  const findDef = (name: string) =>
    defs.find(d => d.name === name) ??
    { comments: [], name, body: { alternatives: [] } };

  const layeredDefs = layeredNames.map(findDef);
  const cycleDefs = defs.filter(d => cycleNames.has(d.name));
  return [...layeredDefs, ...cycleDefs];
}

function singleLayer(deps: string[], layers: [string, number][]): number {
  if (deps.length === 0) return 0;
  const resolved: number[] = [];
  for (const dep of deps) {
    const entry = layers.find(([n]) => n === dep);
    if (entry === undefined || entry[1] < 0) return -1;
    resolved.push(entry[1]);
  }
  if (resolved.length !== deps.length) return -1;
  return Math.max(...resolved) + 1;
}

// ---------------------------------------------------------------------------
// Definition block
// ---------------------------------------------------------------------------

function emitDefinitionBlock(
  defs: Definition[],
  alignCol: number,
  aligned: boolean,
  compact: boolean,
  lineWidth: number,
): string[] {
  const lines: string[] = [];
  defs.forEach((def, i) => {
    const isFirst = i === 0;
    if (def.comments.length > 0 && !isFirst && !compact) lines.push("");
    for (const c of def.comments) lines.push(emitCommentLine(c));
    for (const l of emitDefinition(def, alignCol, aligned, compact, lineWidth)) {
      lines.push(l);
    }
  });
  return lines;
}

function emitDefinition(
  def: Definition,
  alignCol: number,
  aligned: boolean,
  compact: boolean,
  lineWidth: number,
): string[] {
  const namePart = def.name;
  const spacing = aligned ? " ".repeat(alignCol - namePart.length) : " ";
  const nameEq = namePart + spacing;
  const fullPrefix = nameEq + "= ";
  const bodyPieces = emitExprPieces(def.body, compact);
  return breakDefinition(fullPrefix, nameEq, bodyPieces, lineWidth, compact);
}

function breakDefinition(
  fullPrefix: string,
  nameEq: string,
  bodyPieces: Piece[],
  lineWidth: number,
  compact: boolean,
): string[] {
  const bodyStr = piecesToStr(bodyPieces, compact);
  const bodyWithSemi = bodyStr + " ;";
  const fullLine = fullPrefix + bodyWithSemi;

  if (fullLine.length <= lineWidth) return [fullLine];

  // D1: body (including " ;") fits in lineWidth - 4
  if (bodyWithSemi.length <= lineWidth - 4) {
    const line1 = nameEq + "=";
    const bodyLines = breakBodyExpr(bodyPieces, "    ", lineWidth, compact);
    const reversed = [...bodyLines].reverse();
    if (reversed.length === 0) return [line1];
    const [last, ...revRest] = reversed;
    return [line1, ...[...revRest].reverse(), last! + " ;"];
  }

  // D2/D3 on the full line
  const col = fullPrefix.length;
  const cont = " ".repeat(col);
  return breakLine(fullPrefix, cont, col, bodyPieces, " ;", lineWidth, compact);
}

// ---------------------------------------------------------------------------
// Body expression line breaking
// ---------------------------------------------------------------------------

function breakBodyExpr(
  pieces: Piece[],
  indent: string,
  lineWidth: number,
  compact: boolean,
): string[] {
  const col = indent.length;
  return breakLine(indent, indent, col, pieces, "", lineWidth, compact);
}

function breakLine(
  prefix: string,
  contPrefix: string,
  col: number,
  pieces: Piece[],
  suffix: string,
  lineWidth: number,
  compact: boolean,
): string[] {
  const flat = piecesToStr(pieces, compact);
  const fullLine = prefix + flat + suffix;
  if (fullLine.length <= lineWidth) return [fullLine];

  const limit = lineWidth - col;

  const seqIdx = findRightmostSeqBreak(pieces, limit, compact);
  if (seqIdx !== null) {
    const before = pieces.slice(0, seqIdx);
    const after = pieces.slice(seqIdx + 1);
    const line1 = prefix + piecesToStr(before, compact);
    const contCol = contPrefix.length;
    return [line1, ...breakLine(contPrefix, contPrefix, contCol, after, suffix, lineWidth, compact)];
  }

  const altIdx = findRightmostAltBreak(pieces, limit, compact);
  if (altIdx !== null) {
    const before = pieces.slice(0, altIdx);
    const after = pieces.slice(altIdx + 1);
    const line1 = prefix + piecesToStr(before, compact);
    const altBar = compact ? "|" : "| ";
    const altPrefix = " ".repeat(col) + altBar;
    const altCol = altPrefix.length;
    return [line1, ...breakLine(altPrefix, altPrefix, altCol, after, suffix, lineWidth, compact)];
  }

  return [fullLine];
}

function findRightmostSeqBreak(pieces: Piece[], limit: number, compact: boolean): number | null {
  let pos = 0;
  let best: number | null = null;
  for (let i = 0; i < pieces.length; i++) {
    const p = pieces[i]!;
    if (p.kind === "seqSpace" && pos <= limit) best = i;
    pos += pieceLen(p, compact);
  }
  return best;
}

function findRightmostAltBreak(pieces: Piece[], limit: number, compact: boolean): number | null {
  let pos = 0;
  let best: number | null = null;
  for (let i = 0; i < pieces.length; i++) {
    const p = pieces[i]!;
    if (p.kind === "alt") {
      const pipePos = compact ? pos : pos + 1;
      if (pipePos <= limit) best = i;
    }
    pos += pieceLen(p, compact);
  }
  return best;
}

// ---------------------------------------------------------------------------
// Expression piece emitters
// ---------------------------------------------------------------------------

function emitExprPieces(expr: Expression, compact: boolean): Piece[] {
  const branches = expr.alternatives;
  if (branches.length === 0) return [];
  if (branches.length === 1) return emitSeqPieces(branches[0]!, compact);
  return intersperse(branches.map(seq => emitSeqPieces(seq, compact)), [{ kind: "alt" } as Piece]).flat();
}

function emitSeqPieces(seq: { items: { inner: Repetition; name: string | null }[] }, compact: boolean): Piece[] {
  const items = seq.items;
  if (items.length === 0) return [];
  if (items.length === 1) return emitCapturePieces(items[0]!, compact);
  return intersperse(items.map(item => emitCapturePieces(item, compact)), [{ kind: "seqSpace" } as Piece]).flat();
}

function emitCapturePieces(cap: { inner: Repetition; name: string | null }, compact: boolean): Piece[] {
  const base = emitRepPieces(cap.inner, compact);
  if (cap.name === null) return base;
  return [...base, { kind: "text", content: " as " + cap.name }];
}

function emitRepPieces(rep: Repetition, compact: boolean): Piece[] {
  const exclStr = emitExclStr(rep.inner, compact);
  if (rep.count === null) return [{ kind: "text", content: exclStr }];
  const sep = compact ? "*" : " * ";
  return [{ kind: "text", content: exclStr + sep + emitRepCountStr(rep.count) }];
}

// ---------------------------------------------------------------------------
// String emitters for atoms and nested expressions
// ---------------------------------------------------------------------------

function emitExclStr(excl: { base: RangeItem; excluded: RangeItem | null }, compact: boolean): string {
  const base = emitRangeItemStr(excl.base, compact);
  if (excl.excluded === null) return base;
  return base + " excluding " + emitRangeItemStr(excl.excluded, compact);
}

function emitRangeItemStr(ri: RangeItem, compact: boolean): string {
  if (ri.kind === "charRange") {
    return emitAtomStr(ri.from, compact) + ".." + emitAtomStr(ri.to, compact);
  }
  return emitAtomStr(ri.atom, compact);
}

function emitAtomStr(atom: Atom, compact: boolean): string {
  switch (atom.kind) {
    case "literal":
      return atom.content.includes("'")
        ? '"' + atom.content + '"'
        : "'" + atom.content + "'";
    case "charClass": return "%" + atom.name;
    case "interpolation": return "{" + atom.name + "}";
    case "positionAssertion": return "@" + atom.name;
    case "group": {
      const inner = emitExprStr(atom.inner, compact);
      return compact ? "(" + inner + ")" : "( " + inner + " )";
    }
  }
}

function emitExprStr(expr: Expression, compact: boolean): string {
  const sep = compact ? "|" : " | ";
  return expr.alternatives.map(seq => emitSeqStr(seq, compact)).join(sep);
}

function emitSeqStr(seq: { items: { inner: Repetition; name: string | null }[] }, compact: boolean): string {
  return seq.items.map(cap => emitCaptureStr(cap, compact)).join(" ");
}

function emitCaptureStr(cap: { inner: Repetition; name: string | null }, compact: boolean): string {
  const base = emitRepStr(cap.inner, compact);
  return cap.name !== null ? base + " as " + cap.name : base;
}

function emitRepStr(rep: Repetition, compact: boolean): string {
  const exclStr = emitExclStr(rep.inner, compact);
  if (rep.count === null) return exclStr;
  const sep = compact ? "*" : " * ";
  return exclStr + sep + emitRepCountStr(rep.count);
}

function emitRepCountStr(rc: RepCount): string {
  const { min, max: upper, lazy } = rc;
  let base: string;
  if (upper.kind === "none") base = String(min);
  else if (upper.kind === "exact") base = min + ".." + upper.value;
  else base = min + "..?";
  return lazy ? base + " fewest" : base;
}

// ---------------------------------------------------------------------------
// Alignment helper
// ---------------------------------------------------------------------------

function computeAlignCol(names: string[]): number {
  const maxLen = names.reduce((acc, n) => Math.max(acc, n.length), 0);
  return maxLen + 2;
}

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

function intersperse<T>(items: T[][], sep: T[]): T[][] {
  if (items.length <= 1) return items;
  const result: T[][] = [];
  for (let i = 0; i < items.length; i++) {
    if (i > 0) result.push(sep);
    result.push(items[i]!);
  }
  return result;
}
