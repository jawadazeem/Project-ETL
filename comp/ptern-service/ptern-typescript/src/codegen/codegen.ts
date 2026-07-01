import {
  collectCaptureValidators,
  compileClassDefinitions,
  compileDefinitions,
  compileExpressionWithRepInfo,
  determineFlags,
  determineIgnoreMatching,
  type RepetitionInfo,
} from "./regex";
import { buildPlan, type SubstitutionPlan } from "./substitution";
import type { ParsedPtern } from "../parser/ast";

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

export type CompiledPtern = {
  source: string;
  flags: string;
  ignoreMatching: boolean;
  captureValidators: [string, string][];
  isSubstitutable: boolean;
  ignoreSubstitutionMatching: boolean;
  substitutionPlan: SubstitutionPlan | null;
  repetitionInfo: RepetitionInfo[];
};

export function compile(ptern: ParsedPtern): CompiledPtern {
  const flags = determineFlags(ptern);
  const ignoreMatching = determineIgnoreMatching(ptern.annotations);
  const isSubstitutable = ptern.annotations.some(a => a.name === "substitutable" && a.value);
  const ignoreSubstitutionMatching = ptern.annotations.some(
    a => a.name === "substitutions-ignore-matching" && a.value,
  );
  const classDefs = compileClassDefinitions(ptern.definitions);
  const defs = compileDefinitions(ptern.definitions, classDefs);
  const [source, repetitionInfo] = compileExpressionWithRepInfo(ptern.body, defs, classDefs, 0);
  const captureValidators = collectCaptureValidators(ptern.body, defs, classDefs);
  const defBodies = new Map(ptern.definitions.map(def => [def.name, def.body]));
  const substitutionPlan = isSubstitutable ? buildPlan(ptern.body, defBodies) : null;

  return {
    source,
    flags,
    ignoreMatching,
    captureValidators,
    isSubstitutable,
    ignoreSubstitutionMatching,
    substitutionPlan,
    repetitionInfo,
  };
}
