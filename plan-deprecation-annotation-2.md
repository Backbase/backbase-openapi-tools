# Plan: `@Deprecated` annotation generation for deprecated API elements

## Goal

When generating Java code (and, where the target language supports it, other
languages), BOAT should mark generated code as deprecated based on two
independent signals in the OpenAPI spec:

1. **Standard OAS `deprecated: true`** on an operation, parameter, schema, or
   property — endpoint/element-level deprecation.
2. **Custom `x-deprecated: true` on `info`** — deprecates the *entire* spec
   (every generated operation/model). May be paired with a custom
   `x-sunset-date` (ISO date) on `info`, giving the expected removal date.
   When present, the sunset date must be included in the deprecation
   message wherever the target annotation/comment mechanism supports a
   free-text message.

## Current state (from codebase survey)

- Standard `deprecated: true` already flows through unmodified:
  openapi-generator's `DefaultCodegen` auto-populates
  `CodegenOperation.isDeprecated`, `CodegenProperty.deprecated`,
  `CodegenModel.isDeprecated` from the OAS `deprecated` flag, and BOAT's
  existing mustache templates already emit a bare `@Deprecated` annotation +
  `@deprecated` Javadoc tag for these:
  - `boat-scaffold/src/main/templates/boat-java/api.mustache`,
    `pojo.mustache`
  - `boat-scaffold/src/main/templates/boat-spring/api.mustache`,
    `apiController.mustache`, `apiDelegate.mustache`, `pojo.mustache`
  - `boat-scaffold/src/main/templates/boat-swift5/api.mustache` (also shows
    precedent for reading a custom vendor extension,
    `x-bb-api-deprecation-description`, straight from a template)
  - `boat-scaffold/src/main/templates/boat-docs/*.mustache` (badge only, no
    message)

  **Gap:** no message/sunset-date text is ever attached — the tag is always
  bare `@deprecated` with nothing after it.

- `x-deprecated` / `x-sunset-date` today are read in exactly one place:
  `boat-quay/boat-quay-rules/src/main/kotlin/com/backbase/oss/boat/quay/ruleset/InfoBlockSunsetDateChecker.kt`
  (lint rule B015). It reads them off `context.api.info.extensions` as raw
  string-literal map keys (`"x-deprecated"`, `"x-sunset-date"`) — there is
  no shared constants class for these keys, and boat-quay is not on
  boat-scaffold's classpath, so the codegen side needs its own copy of this
  logic (same key names, same tri-state boolean parsing).

  **Gap:** nothing in boat-scaffold reads `info`-level extensions at all, and
  nothing propagates a whole-spec deprecation flag down into every generated
  operation/model.

- `boat-engine`'s `Deprecator` transformer and `boat-maven-plugin`'s
  `RemoveDeprecatedMojo` are unrelated — they *strip out* deprecated content
  rather than annotate it. Not touched by this feature.

## Design

### 1. Read the info-level extensions once per spec

Add a small shared helper (new class, since no existing constants class
covers this) in `boat-scaffold`, e.g.
`com.backbase.oss.codegen.utils.DeprecationExtensions`, exposing:

```java
public static final String X_DEPRECATED = "x-deprecated";
public static final String X_SUNSET_DATE = "x-sunset-date";

static boolean isSpecDeprecated(Info info);          // mirrors B015's isTrue() tri-state parsing
static Optional<LocalDate> getSunsetDate(Info info);  // mirrors B015's ISO_LOCAL_DATE parsing
static String buildDeprecationMessage(Optional<LocalDate> sunsetDate); // "This API is deprecated" [+ " and will be removed on {date}."]
```

Reusing the exact same key names and parsing rules as B015 keeps lint and
codegen consistent for anyone comparing behavior.

### 2. Propagate whole-spec deprecation into codegen

In each affected generator's `preprocessOpenAPI(OpenAPI openAPI)` override
(new override where one doesn't exist yet — none currently touch
`info.extensions`):

- Compute `specDeprecated` + `deprecationMessage` once.
- Put them into `additionalProperties` (`boatApiDeprecated`,
  `boatApiDeprecationMessage`) so every template can see them globally,
  following the existing convention used for other spec-wide flags exposed
  via `additionalProperties` (e.g. in `BoatDocsGenerator`'s constructor).

Generators to update:
- `BoatJavaCodeGen` (`boat-scaffold/src/main/java/com/backbase/oss/codegen/java/BoatJavaCodeGen.java`)
- `BoatSpringCodeGen` (same package) — `BoatWebhooksCodeGen` inherits it for free.
- `BoatDocsGenerator` (docs, so the badge can show the message).

### 3. Force element-level `isDeprecated`/`deprecated` when the whole spec is deprecated

Even when an individual operation/property doesn't set `deprecated: true`,
if `boatApiDeprecated` is true it must still be rendered as deprecated.
Override, per generator:

- `fromOperation(...)`: after calling `super.fromOperation(...)`, if
  `additionalProperties.get("boatApiDeprecated") == true`, force
  `op.isDeprecated = true`. Mirrors the existing pattern in
  `BoatSpringCodeGen.fromOperation` (already post-processes the result of
  `super.fromOperation`).
- `postProcessModelProperty(model, property)`: same idea, force
  `property.deprecated = true`. Mirrors the existing
  `BoatSpringCodeGen.postProcessModelProperty` pattern (already mutates
  `property.vendorExtensions` after calling `super`).
- `postProcessModels(...)` / `postProcessAllModels(...)`: force
  `model.isDeprecated = true` for every model when spec-deprecated.

### 4. Carry the message alongside the flag

Rather than only a boolean, also stash `boatApiDeprecationMessage` on the
operation/model/property itself (via `vendorExtensions`, the same mechanism
already used for `x-bb-api-deprecation-description` in the swift5 template)
so templates can render it without re-deriving it:

- In the same `fromOperation`/`postProcessModelProperty`/model-processing
  hooks above, when forcing `isDeprecated`/`deprecated` to true, also set
  `vendorExtensions.put("x-boat-deprecation-message", deprecationMessage)`
  if not already present (an operation-level `deprecated: true` with no
  spec-level info still gets bare deprecation, no message — matches spec:
  the message only exists when `x-sunset-date`/`x-deprecated` are present).

### 5. Template changes

Update the Javadoc `@deprecated` lines (annotation itself stays bare
`@Deprecated` — the Java `@Deprecated` annotation has no free-text message
slot pre/post Java 9; only the Javadoc tag can carry text) to render the
message when present, falling back to today's bare tag otherwise:

- `boat-scaffold/src/main/templates/boat-java/api.mustache`
- `boat-scaffold/src/main/templates/boat-java/pojo.mustache`
- `boat-scaffold/src/main/templates/boat-spring/api.mustache`
- `boat-scaffold/src/main/templates/boat-spring/apiController.mustache`
- `boat-scaffold/src/main/templates/boat-spring/apiDelegate.mustache`
- `boat-scaffold/src/main/templates/boat-spring/pojo.mustache` (also feeds the
  message into the existing `@Schema(deprecated = true, ...)`/`@Operation`
  swagger annotation as a `description` addendum, since springdoc has no
  message field either)

Pattern (illustrative, exact mustache syntax to match existing style):

```mustache
{{#isDeprecated}}
 * @deprecated{{#vendorExtensions.x-boat-deprecation-message}} {{.}}{{/vendorExtensions.x-boat-deprecation-message}}
{{/isDeprecated}}
...
{{#isDeprecated}}
    @Deprecated
{{/isDeprecated}}
```

Docs template (`boat-scaffold/src/main/templates/boat-docs/index.mustache`,
`param.mustache`) gets the message rendered next to the existing
"Deprecated" badge.

### 6. Other languages

Explicitly out of initial scope beyond Java/Spring, but the mechanism
generalizes cleanly since `vendorExtensions` + `additionalProperties` are
generator-agnostic:
- **Swift** already has a working precedent
  (`x-bb-api-deprecation-description` → `@available(*, deprecated, message:
  "...")`). Could be pointed at the same
  `x-boat-deprecation-message`/`boatApiDeprecated` values in a follow-up,
  since Swift's annotation *does* support a message.
- **TypeScript/Angular**: JSDoc `@deprecated` supports free text the same
  way Javadoc does — same template pattern applies to
  `boat-angular` templates in a follow-up.
- Not implementing these now; noting the extension point so it's not a
  redesign later.

## Testing

- Unit tests for `DeprecationExtensions` (boolean tri-state parsing, missing
  info, missing extensions, invalid date).
- Codegen tests (extend existing fixtures under
  `boat-scaffold/src/test/resources` and the corresponding test classes,
  e.g. `BoatCommonJavaCodeGenTests`, boat-spring codegen tests):
  - Spec with `info.x-deprecated: true` + `x-sunset-date` → every generated
    method/model carries `@Deprecated` and a Javadoc `@deprecated` line with
    the sunset date.
  - Spec with only an operation-level `deprecated: true` (no info-level
    flags) → unchanged existing behavior (bare `@Deprecated`, no message) —
    regression guard for the existing feature.
  - Spec with `info.x-deprecated: true` but no `x-sunset-date` → message
    without a date ("This API is deprecated.").
  - Spec with neither → no deprecation anywhere (regression guard).
- Docs generator test: sunset-date message appears in generated HTML next
  to the deprecated badge.

## Rollout

- No new CLI flags/config needed — behavior is driven entirely by spec
  content (`deprecated`, `info.x-deprecated`, `info.x-sunset-date`),
  consistent with how B015 already treats these as always-on, spec-driven
  signals.
- Version bump + CHANGELOG entry per existing release conventions.
- Update BOAT docs (README/wiki or boat-docs pages describing supported
  `x-*` extensions) to document `x-deprecated`/`x-sunset-date` behavior for
  codegen, not just for the B015 lint rule.
