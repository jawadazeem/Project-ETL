import {
  type Atom,
  type Capture,
  type Exclusion,
  type Expression,
  type RangeItem,
  type Repetition,
  type Sequence,
} from "../parser/ast";

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

export type SubstitutionPlan =
  | { kind: "literal"; text: string }
  | { kind: "positionAssertion" }
  | { kind: "notEvaluable" }
  | { kind: "capture"; name: string; inner: SubstitutionPlan }
  | { kind: "sequence"; items: SubstitutionPlan[] }
  | { kind: "alternation"; branches: SubstitutionPlan[] }
  | { kind: "fixedRep"; inner: SubstitutionPlan; count: number }
  | { kind: "boundedRep"; inner: SubstitutionPlan; min: number; max: number | null };

export function buildPlan(
  body: Expression,
  defBodies: Map<string, Expression>,
): SubstitutionPlan {
  const capReps = collectCaptureRepsInExpr(body);
  return buildPlanExpr(body, defBodies, capReps);
}

// ---------------------------------------------------------------------------
// Body-capture repetition collector (for {name} interpolation in the plan)
// ---------------------------------------------------------------------------

function collectCaptureRepsInExpr(expr: Expression): Map<string, Repetition> {
  return expr.alternatives.reduce(
    (acc, seq) => mergeMaps(acc, collectCaptureRepsInSeq(seq)),
    new Map<string, Repetition>(),
  );
}

function collectCaptureRepsInSeq(seq: Sequence): Map<string, Repetition> {
  return seq.items.reduce(
    (acc, cap) => mergeMaps(acc, collectCaptureRepsInCap(cap)),
    new Map<string, Repetition>(),
  );
}

function collectCaptureRepsInCap(cap: Capture): Map<string, Repetition> {
  const own = cap.name !== null
    ? new Map([[cap.name, cap.inner]])
    : new Map<string, Repetition>();
  return mergeMaps(own, collectCaptureRepsInRep(cap.inner));
}

function collectCaptureRepsInRep(rep: Repetition): Map<string, Repetition> {
  return collectCaptureRepsInExcl(rep.inner);
}

function collectCaptureRepsInExcl(excl: Exclusion): Map<string, Repetition> {
  return collectCaptureRepsInItem(excl.base);
}

function collectCaptureRepsInItem(item: RangeItem): Map<string, Repetition> {
  if (item.kind === "charRange") return new Map();
  return collectCaptureRepsInAtom(item.atom);
}

function collectCaptureRepsInAtom(atom: Atom): Map<string, Repetition> {
  if (atom.kind === "group") return collectCaptureRepsInExpr(atom.inner);
  return new Map();
}

// ---------------------------------------------------------------------------
// Plan builder
// ---------------------------------------------------------------------------

function buildPlanExpr(
  expr: Expression,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  const seqs = expr.alternatives;
  if (seqs.length === 1) return buildPlanSeq(seqs[0]!, defBodies, capReps);
  return { kind: "alternation", branches: seqs.map(s => buildPlanSeq(s, defBodies, capReps)) };
}

function buildPlanSeq(
  seq: Sequence,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  const items = seq.items;
  if (items.length === 1) return buildPlanCap(items[0]!, defBodies, capReps);
  return { kind: "sequence", items: items.map(c => buildPlanCap(c, defBodies, capReps)) };
}

function buildPlanCap(
  cap: Capture,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  const inner = buildPlanRep(cap.inner, defBodies, capReps);
  if (cap.name === null) return inner;
  return { kind: "capture", name: cap.name, inner };
}

function buildPlanRep(
  rep: Repetition,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  const base = buildPlanItem(rep.inner.base, defBodies, capReps);
  const inner: SubstitutionPlan = rep.inner.excluded !== null ? { kind: "notEvaluable" } : base;
  if (rep.count === null) return inner;
  const rc = rep.count;
  if (rc.max.kind === "none") return { kind: "fixedRep", inner, count: rc.min };
  if (rc.max.kind === "exact") return { kind: "boundedRep", inner, min: rc.min, max: rc.max.value };
  return { kind: "boundedRep", inner, min: rc.min, max: null }; // unbounded
}

function buildPlanItem(
  item: RangeItem,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  if (item.kind === "charRange") return { kind: "notEvaluable" };
  return buildPlanAtom(item.atom, defBodies, capReps);
}

function buildPlanAtom(
  atom: Atom,
  defBodies: Map<string, Expression>,
  capReps: Map<string, Repetition>,
): SubstitutionPlan {
  switch (atom.kind) {
    case "literal": return { kind: "literal", text: rawToString(atom.content) };
    case "charClass": return { kind: "notEvaluable" };
    case "positionAssertion": return { kind: "positionAssertion" };
    case "interpolation": {
      const body = defBodies.get(atom.name);
      if (body !== undefined) return buildPlanExpr(body, defBodies, capReps);
      if (capReps.has(atom.name)) {
        return { kind: "capture", name: atom.name, inner: { kind: "notEvaluable" } };
      }
      return { kind: "literal", text: "" };
    }
    case "group": return buildPlanExpr(atom.inner, defBodies, capReps);
  }
}

// ---------------------------------------------------------------------------
// Literal decoding (raw content → plain string for PlanLiteral nodes)
// ---------------------------------------------------------------------------

function rawToString(raw: string): string {
  let result = "";
  let i = 0;
  while (i < raw.length) {
    if (raw[i] === "\\") {
      i++;
      if (i >= raw.length) break;
      const c = raw[i]!;
      switch (c) {
        case "n": result += "\n"; i++; break;
        case "t": result += "\t"; i++; break;
        case "r": result += "\r"; i++; break;
        case "a": result += "\x07"; i++; break;
        case "f": result += "\f"; i++; break;
        case "v": result += "\v"; i++; break;
        case "\\": result += "\\"; i++; break;
        case "'": result += "'"; i++; break;
        case '"': result += '"'; i++; break;
        case "u": {
          const hex = raw.slice(i + 1, i + 5);
          const cp = parseInt(hex, 16);
          result += !isNaN(cp) ? String.fromCodePoint(cp) : "";
          i += 5;
          break;
        }
        default: result += c; i++; break;
      }
    } else {
      const cp = raw.codePointAt(i)!;
      result += String.fromCodePoint(cp);
      i += cp > 0xffff ? 2 : 1;
    }
  }
  return result;
}

// ---------------------------------------------------------------------------
// Map merge helper (second wins on duplicate keys)
// ---------------------------------------------------------------------------

function mergeMaps<K, V>(a: Map<K, V>, b: Map<K, V>): Map<K, V> {
  return new Map([...a, ...b]);
}
