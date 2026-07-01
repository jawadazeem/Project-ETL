export type SemanticError =
  | { kind: "undefinedReference"; name: string }
  | { kind: "duplicateDefinition"; name: string }
  | { kind: "circularDefinition"; names: string[] }
  | { kind: "duplicateCapture"; name: string }
  | { kind: "captureDefinitionConflict"; name: string }
  | { kind: "invalidRangeEndpoint"; content: string }
  | { kind: "invertedRange"; from: string; to: string }
  | { kind: "invertedRepetitionBounds"; min: number; max: number }
  | { kind: "invalidExclusionOperand" }
  | { kind: "unknownAnnotation"; name: string }
  | { kind: "duplicateAnnotation"; name: string }
  | { kind: "invalidEscapeSequence"; seq: string }
  | { kind: "unknownPositionAssertion"; name: string }
  | { kind: "positionAssertionInRepetition"; name: string }
  | { kind: "substitutionsIgnoreMatchingWithoutSubstitutable" }
  | { kind: "notSubstitutableBody" }
  | { kind: "boundedRepetitionNeedsCapture" }
  | { kind: "emptyLiteral" }
  | { kind: "emptyCharacterSet" }
  | { kind: "ambiguousRepetitionAdjacency"; branchA: string; branchB: string }
  | { kind: "ambiguousRepetitionBody" }
  | { kind: "ambiguousAdjacentRepetition" }
  | { kind: "fewestOnExactRepetition" }
  | { kind: "unusedDefinition"; name: string };
