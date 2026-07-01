import {
  type Atom,
  type Capture,
  type Definition,
  type Exclusion,
  type Expression,
  type ParsedPtern,
  type RangeItem,
  type Repetition,
  type Sequence,
} from "../parser/ast";
import { type SemanticError } from "./error";

export function resolve(ptern: ParsedPtern): SemanticError[] {
  // 1. Collect definition names; flag duplicates.
  const [defNames, dupDefErrs] = collectDefNames(ptern.definitions);

  // 2. Detect circular definitions.
  const circErrs = findCircularDefinitions(ptern.definitions, defNames);

  // 3. Check for undefined interpolations inside definition bodies.
  const defRefErrs = ptern.definitions.flatMap(def =>
    checkUndefinedRefs(def.body, defNames, []),
  );

  // 4. Collect capture names in the body.
  const bodyCapNames = capturesInExpression(ptern.body);

  // 5. Flag duplicate capture names in the body.
  const dupCapErrs = findDuplicateNames(bodyCapNames, "duplicateCapture");

  // 6. Collect capture names in all definition bodies (for conflict check).
  const defCapNames = ptern.definitions.flatMap(def => capturesInExpression(def.body));

  // 7. Flag any capture name that collides with a definition name.
  const allCapNames = [...bodyCapNames, ...defCapNames];
  const conflictErrs = findCaptureDefConflicts(allCapNames, defNames);

  // 8. Check for undefined interpolations in the body.
  const bodyRefErrs = checkUndefinedRefs(ptern.body, defNames, bodyCapNames);

  // 9. Flag definitions never reachable from the body expression.
  const unusedErrs = findUnusedDefinitions(ptern.definitions, defNames, ptern.body);

  return [
    ...dupDefErrs,
    ...circErrs,
    ...defRefErrs,
    ...dupCapErrs,
    ...conflictErrs,
    ...bodyRefErrs,
    ...unusedErrs,
  ];
}

// ---------------------------------------------------------------------------
// Definition name collection
// ---------------------------------------------------------------------------

function collectDefNames(defs: Definition[]): [string[], SemanticError[]] {
  const names = defs.map(d => d.name);
  const errors = findDuplicateNames(names, "duplicateDefinition");
  return [dedupList(names), errors];
}

// ---------------------------------------------------------------------------
// Circular definition detection
// ---------------------------------------------------------------------------

function findCircularDefinitions(defs: Definition[], defNames: string[]): SemanticError[] {
  const graph = new Map<string, string[]>();
  for (const def of defs) {
    const deps = interpolationsInExpression(def.body).filter(d => defNames.includes(d));
    graph.set(def.name, deps);
  }

  const allCycles = defNames.flatMap(name => dfsCycles(graph, name, []));
  const sortedCycles = allCycles.map(cycle => [...cycle].sort());
  const uniqueCycles = dedupListOfLists(sortedCycles);
  return uniqueCycles.map(names => ({ kind: "circularDefinition", names }) as SemanticError);
}

function dfsCycles(
  graph: Map<string, string[]>,
  node: string,
  path: string[],
): string[][] {
  if (path.includes(node)) return [takeUntilInclusive(path, node)];
  const newPath = [node, ...path];
  const deps = graph.get(node) ?? [];
  return deps.flatMap(dep => dfsCycles(graph, dep, newPath));
}

function takeUntilInclusive(lst: string[], target: string): string[] {
  const result: string[] = [];
  for (const x of lst) {
    result.push(x);
    if (x === target) break;
  }
  return result;
}

// ---------------------------------------------------------------------------
// Unused definition detection
// ---------------------------------------------------------------------------

function findUnusedDefinitions(
  defs: Definition[],
  defNames: string[],
  body: Expression,
): SemanticError[] {
  const graph = new Map<string, string[]>();
  for (const def of defs) {
    const deps = interpolationsInExpression(def.body).filter(d => defNames.includes(d));
    graph.set(def.name, deps);
  }

  const seeds = interpolationsInExpression(body).filter(n => defNames.includes(n));
  const reachable = expandReachable(graph, seeds, []);

  return defNames
    .filter(name => !reachable.includes(name))
    .map(name => ({ kind: "unusedDefinition", name }) as SemanticError);
}

function expandReachable(
  graph: Map<string, string[]>,
  frontier: string[],
  visited: string[],
): string[] {
  if (frontier.length === 0) return visited;
  const [name, ...rest] = frontier as [string, ...string[]];
  if (visited.includes(name)) return expandReachable(graph, rest, visited);
  const deps = graph.get(name) ?? [];
  return expandReachable(graph, [...deps, ...rest], [name, ...visited]);
}

// ---------------------------------------------------------------------------
// Undefined reference checking
// ---------------------------------------------------------------------------

function checkUndefinedRefs(
  expr: Expression,
  defNames: string[],
  capNames: string[],
): SemanticError[] {
  return interpolationsInExpression(expr)
    .filter(name => !defNames.includes(name) && !capNames.includes(name))
    .map(name => ({ kind: "undefinedReference", name }) as SemanticError);
}

// ---------------------------------------------------------------------------
// Capture / definition name conflict checking
// ---------------------------------------------------------------------------

function findCaptureDefConflicts(capNames: string[], defNames: string[]): SemanticError[] {
  return dedupList(capNames)
    .filter(name => defNames.includes(name))
    .map(name => ({ kind: "captureDefinitionConflict", name }) as SemanticError);
}

// ---------------------------------------------------------------------------
// Collecting interpolation names from the AST
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
// Collecting capture names from the AST
// ---------------------------------------------------------------------------

function capturesInExpression(expr: Expression): string[] {
  return expr.alternatives.flatMap(capturesInSequence);
}

function capturesInSequence(seq: Sequence): string[] {
  return seq.items.flatMap(capturesInCapture);
}

function capturesInCapture(cap: Capture): string[] {
  const own = cap.name !== null ? [cap.name] : [];
  return [...own, ...capturesInRepetition(cap.inner)];
}

function capturesInRepetition(rep: Repetition): string[] {
  return capturesInExclusion(rep.inner);
}

function capturesInExclusion(excl: Exclusion): string[] {
  return capturesInRangeItem(excl.base);
}

function capturesInRangeItem(item: RangeItem): string[] {
  if (item.kind === "singleAtom") return capturesInAtom(item.atom);
  return [];
}

function capturesInAtom(atom: Atom): string[] {
  if (atom.kind === "group") return capturesInExpression(atom.inner);
  return [];
}

// ---------------------------------------------------------------------------
// List utilities
// ---------------------------------------------------------------------------

function findDuplicateNames(
  names: string[],
  kind: "duplicateDefinition" | "duplicateCapture",
): SemanticError[] {
  const seen = new Set<string>();
  const dups = new Set<string>();
  for (const name of names) {
    if (seen.has(name)) dups.add(name);
    else seen.add(name);
  }
  return [...dups].map(name => ({ kind, name }) as SemanticError);
}

function dedupList(lst: string[]): string[] {
  const seen = new Set<string>();
  const result: string[] = [];
  for (const x of lst) {
    if (!seen.has(x)) {
      seen.add(x);
      result.push(x);
    }
  }
  return result;
}

function dedupListOfLists(lsts: string[][]): string[][] {
  const result: string[][] = [];
  for (const lst of lsts) {
    if (!result.some(r => r.length === lst.length && r.every((x, i) => x === lst[i]))) {
      result.push(lst);
    }
  }
  return result;
}
