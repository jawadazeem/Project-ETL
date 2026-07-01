// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type MatchResult = {
  index: number;
  length: number;
  captures: [string, string][];
};

type RepInfo = [string, string, string[]]; // [groupName, subSource, captureNames]

export type ReplaceOutcome =
  | { ok: true; value: string }
  | { ok: false; mismatches: [string, number, number][] };

// ---------------------------------------------------------------------------
// Regex construction and test
// ---------------------------------------------------------------------------

export function makeRegex(source: string, flags: string): RegExp {
  return new RegExp(source, flags);
}

export function testRegex(regex: RegExp, input: string): boolean {
  regex.lastIndex = 0;
  return regex.test(input);
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

function capturesList(m: RegExpExecArray): [string, string][] {
  const groups = m.groups ?? {};
  return Object.entries(groups)
    .filter(([k, v]) => !k.startsWith("__rep_") && typeof v === "string")
    .map(([k, v]) => [k, v as string]);
}

// Build the replacement text for one match using per-capture indices (requires 'd' flag).
function applyCaptureReplacements(
  m: RegExpExecArray,
  replacementsArr: [string, string][],
): string {
  const matchStart = m.index;
  const groupIndices = (m as { indices?: { groups?: Record<string, [number, number]> } }).indices?.groups ?? {};
  let matchText = m[0]!;
  const patches = replacementsArr
    .filter(([name]) => groupIndices[name] !== undefined)
    .map(([name, newVal]) => ({
      relStart: groupIndices[name]![0] - matchStart,
      relEnd: groupIndices[name]![1] - matchStart,
      newVal,
    }))
    .sort((a, b) => b.relStart - a.relStart);
  for (const { relStart, relEnd, newVal } of patches) {
    matchText = matchText.slice(0, relStart) + newVal + matchText.slice(relEnd);
  }
  return matchText;
}

// Build patches for one regex match, handling both scalar and array replacements.
function applyCaptureReplacementsWithArrays(
  m: RegExpExecArray,
  input: string,
  scalarsArr: [string, string][],
  arraysArr: [string, string[]][],
  repInfoArr: RepInfo[],
  flags: string,
): { result: string; mismatches: [string, number, number][] } {
  const matchStart = m.index;
  const mWithIndices = m as RegExpExecArray & { indices?: { groups?: Record<string, [number, number]> } };
  const groupIndices = mWithIndices.indices?.groups ?? {};
  const patches: { absStart: number; absEnd: number; newVal: string }[] = [];
  const mismatches: [string, number, number][] = [];

  // Build rep-info maps.
  const repInfoByGroup = new Map<string, { sub: string; caps: string[] }>();
  const repGroupForCapture = new Map<string, string>();
  for (const [gn, sub, caps] of repInfoArr) {
    repInfoByGroup.set(gn, { sub, caps });
    for (const cap of caps) repGroupForCapture.set(cap, gn);
  }

  // Sub-regex flags: strip 'g' and 'd', then add both back.
  const subFlags = flags.replace(/[gd]/g, "") + "dg";

  function collectIterSpans(
    name: string,
    repGroupName: string,
  ): { outerSpan: [number, number] | undefined; iterSpans: [number, number][] } {
    const repSpan = groupIndices[repGroupName];
    if (repSpan === undefined) return { outerSpan: undefined, iterSpans: [] };

    const outerSpan = groupIndices[name];
    const hasOuter = outerSpan !== undefined && outerSpan[0] < repSpan[0];

    const { sub } = repInfoByGroup.get(repGroupName)!;
    const subRe = new RegExp(sub, subFlags);
    subRe.lastIndex = repSpan[0];
    const iterSpans: [number, number][] = [];
    let mi: RegExpExecArray | null;
    while ((mi = subRe.exec(input)) !== null && mi.index < repSpan[1]) {
      const subWithIndices = mi as RegExpExecArray & { indices?: { groups?: Record<string, [number, number]> } };
      const iterSpan = subWithIndices.indices?.groups?.[name];
      if (iterSpan !== undefined) iterSpans.push(iterSpan);
      if (mi[0].length === 0) subRe.lastIndex++;
    }

    return { outerSpan: hasOuter ? outerSpan : undefined, iterSpans };
  }

  // Scalar replacements.
  for (const [name, newVal] of scalarsArr) {
    const repGroupName = repGroupForCapture.get(name);
    if (repGroupName) {
      const { outerSpan, iterSpans } = collectIterSpans(name, repGroupName);
      if (outerSpan !== undefined) {
        patches.push({ absStart: outerSpan[0], absEnd: outerSpan[1], newVal });
      }
      for (const span of iterSpans) {
        patches.push({ absStart: span[0], absEnd: span[1], newVal });
      }
    } else {
      const span = groupIndices[name];
      if (span !== undefined) {
        patches.push({ absStart: span[0], absEnd: span[1], newVal });
      }
    }
  }

  // Array replacements.
  for (const [name, vals] of arraysArr) {
    const repGroupName = repGroupForCapture.get(name);
    if (!repGroupName) continue;

    const repSpan = groupIndices[repGroupName];
    if (repSpan === undefined) continue;

    const { outerSpan, iterSpans } = collectIterSpans(name, repGroupName);
    const arrayOffset = outerSpan !== undefined ? 1 : 0;
    if (outerSpan !== undefined) {
      patches.push({ absStart: outerSpan[0], absEnd: outerSpan[1], newVal: vals[0] ?? "" });
    }

    const k = iterSpans.length;
    const provided = vals.length - arrayOffset;
    if (provided !== k) {
      mismatches.push([name, vals.length, k + arrayOffset]);
      continue;
    }

    for (let i = 0; i < k; i++) {
      patches.push({ absStart: iterSpans[i]![0], absEnd: iterSpans[i]![1], newVal: vals[arrayOffset + i] ?? "" });
    }
  }

  // Apply patches right-to-left.
  patches.sort((a, b) => b.absStart - a.absStart);
  let matchText = m[0]!;
  for (const { absStart, absEnd, newVal } of patches) {
    const rel0 = absStart - matchStart;
    const rel1 = absEnd - matchStart;
    matchText = matchText.slice(0, rel0) + newVal + matchText.slice(rel1);
  }
  return { result: matchText, mismatches };
}

// ---------------------------------------------------------------------------
// Exec
// ---------------------------------------------------------------------------

export function execRich(regex: RegExp, input: string): MatchResult | null {
  regex.lastIndex = 0;
  const m = regex.exec(input);
  if (m === null) return null;
  return { index: m.index, length: m[0].length, captures: capturesList(m) };
}

export function execFromRich(regex: RegExp, input: string, startIndex: number): MatchResult | null {
  regex.lastIndex = startIndex;
  const m = regex.exec(input);
  if (m === null) return null;
  return { index: m.index, length: m[0].length, captures: capturesList(m) };
}

export function execAllRich(regex: RegExp, input: string): MatchResult[] {
  regex.lastIndex = 0;
  const results: MatchResult[] = [];
  let m: RegExpExecArray | null;
  while ((m = regex.exec(input)) !== null) {
    results.push({ index: m.index, length: m[0].length, captures: capturesList(m) });
    if (m[0].length === 0) regex.lastIndex++;
  }
  return results;
}

// ---------------------------------------------------------------------------
// Replace (requires 'd' flag on all regexes; 'g' flag additionally for from/all)
// ---------------------------------------------------------------------------

export function replaceRichWithArrays(
  regex: RegExp,
  input: string,
  scalars: [string, string][],
  arrays: [string, string[]][],
  repInfo: RepInfo[],
  flags: string,
): ReplaceOutcome {
  regex.lastIndex = 0;
  const m = regex.exec(input);
  if (m === null) return { ok: true, value: input };
  const { result: newText, mismatches } = applyCaptureReplacementsWithArrays(
    m, input, scalars, arrays, repInfo, flags,
  );
  if (mismatches.length > 0) return { ok: false, mismatches };
  return { ok: true, value: input.slice(0, m.index) + newText + input.slice(m.index + m[0].length) };
}

export function replaceFromRichWithArrays(
  regex: RegExp,
  input: string,
  startIndex: number,
  scalars: [string, string][],
  arrays: [string, string[]][],
  repInfo: RepInfo[],
  flags: string,
): ReplaceOutcome {
  regex.lastIndex = startIndex;
  const m = regex.exec(input);
  if (m === null) return { ok: true, value: input };
  const { result: newText, mismatches } = applyCaptureReplacementsWithArrays(
    m, input, scalars, arrays, repInfo, flags,
  );
  if (mismatches.length > 0) return { ok: false, mismatches };
  return { ok: true, value: input.slice(0, m.index) + newText + input.slice(m.index + m[0].length) };
}

export function replaceAllRichWithArrays(
  regex: RegExp,
  input: string,
  scalars: [string, string][],
  arrays: [string, string[]][],
  repInfo: RepInfo[],
  flags: string,
): ReplaceOutcome {
  regex.lastIndex = 0;
  const parts: string[] = [];
  let lastEnd = 0;
  let m: RegExpExecArray | null;
  while ((m = regex.exec(input)) !== null) {
    parts.push(input.slice(lastEnd, m.index));
    const { result: newText, mismatches } = applyCaptureReplacementsWithArrays(
      m, input, scalars, arrays, repInfo, flags,
    );
    if (mismatches.length > 0) return { ok: false, mismatches };
    parts.push(newText);
    lastEnd = m.index + m[0].length;
    if (m[0].length === 0) regex.lastIndex++;
  }
  parts.push(input.slice(lastEnd));
  return { ok: true, value: parts.join("") };
}
