# Boat Rule Sets

Boat provides the following rule sets:

* [InternalRuleSet](#internalruleset)
* [BoatRuleSet](#boatruleset)

For information on how to enable these rules and use them in BOAT, see [Enable linting](#enable-linting).

# InternalRuleSet

Internal rules for reporting fatal errors found while attempting to parse the API specification.
These rules cannot be disabled or configured.

# BoatRuleSet

BoatRuleSet contains the following rules from Zalando's [Zally rule set](https://github.com/zalando/zally/blob/master/server/rules.md#zallyruleset). 

## M008: Host should not contain protocol

Information about protocol should be placed in schema and not as part of the host.

## M009: At most one body parameter

Enforces that "there can only be one body parameter" per operation as required by the [swagger spec](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#parameter-object).

## M010: Check case of various terms

Enforced that various terms match case requirements configured via
CaseChecker section in rules-config.conf.

Supports:

- schema property names
- query parameter names
- path parameter names
- tag names

## M011: Checks that all operations are tagged

Tags are often used to group operations together in generated
documentation and so it's useful to ensure that all operations are
properly tagged. This rule ensures that:

- All operations have at least one tag
- All tags are defined in the top level `tags:` section
  (defining order of groups)
- All defined tags are used
- All defined tags have a description

## S005: Do not leave unused definitions

Unused definitions cause confusion and should be avoided.

## S006: Define bounds for numeric properties

Numeric properties typically have bounds associated with them and these
should be expressed clearly in the API specification. If left
unspecified then the minimum or maximum will be defined by the
property's format (signed `int32` or `int64` for `integer` types,
`float` or `double` for `number` types).

## S007: Define maximum length for string properties

Implementations often have limits imposed on the length of strings and
these should be expressed clearly in the API specification. If left
unspecified then clients or servers may make inappropriate assumptions
and fail unexpectedly when supplied extra long strings.

## H001: Base path can be extracted

If all paths start with the same prefix then it would be cleaner to extract that into the basePath rather than repeating for each path.

##

In addition to the above Zally rules, BoatRuleSet contains the following Boat Quay rules. Some of these are Backbase 
specific, but could be transferable to other projects.

##

## B001: No license information allowed

Specs must not contain a license. They are covered by the license agreement already negotiated with customers.
   
## B002: All operationIds must be unique

Every operationId must be unique. This helps makes specs less confusing to read, and avoids potential problems when 
generating code from the specs.

## B003: No reserved words allowed
   
The schema must not contain a language's reserved words as keys. This rule checks for reserved words from JavaScript, 
Spring, Kotlin, and Swift. For example:

- Spring:
    assert, boolean, class, null, new, public
- JavaScript:
    abstract, delete, export, import, volatile, static
- Kotlin:
    break, interface, throw, super
- Swift:
    \#column, \#line, Character, Data, Int, left
    
## B004: Check info block tags allowed

Tags enable APIs to be meaningfully grouped together. The info block's Tags value must not be empty, and can only 
contain entries from the following case-sensitive list:

 - productTags: 
   - Retail 
   - Business
   - Wealth
   - Identity
   - Foundation
   - Basic Support
   - Flow
 - informativeTags: 
   - Mobile
   - Security
   - Payments
   - Authentication
   - Employee
   - Cash
   - Insights

## B005: Check info block title format

For readability the title must not be empty and should be no longer then the configured max title length, 35.

## B006: Check info block description format

To help communicate the purpose of the spec, a description must be present in the info block. This description can't 
be empty, and must be no longer than the configured maximum length, which defaults to 140.

## B007: Check prefix for paths

Path prefixes should be one of the following values:

 - client-api
 - service-api
 - integration-api

## B008: Check x-icon value in the info block

An x-icon should be provided in the info block of the spec. This enables the API portal to display the API with the 
correct icon.

## B009: Check prefix for paths should contain version

URL Paths must contain a version number. This maintains a consistent method of versioning though the APIs.

## B010: Pluralize resource names

Resource names should be pluralized in paths.
Path components after the prefix and version should be plurals. For example, in the path /client-api/v1/bars, the rule 
ignores the prefix client-api and the version v1, but the resource bars is checked.

## B011: Use standard HTTP status codes

Adapted from Zally rule 150, this adds extra HTTP codes to the list of standard codes. 
This rule highlights when a HTTP code is standard but not well-understood. Standard codes can be used, but it's best practice to use well-understood codes instead.
Standard HTTP status codes:

    100, 101, 200, 201, 202, 203, 204, 205, 206, 207
    300, 301, 302, 303, 304, 305, 307, 400, 401, 402, 
    403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 
    413, 414, 415, 416, 417, 422, 423, 426, 428, 429, 
    431, 500, 501, 502, 503, 504, 505, 511, default

## B012: Use well-understood HTTP status codes

Adapted from Zally rule 150. To help ensure that specs are implemented as intended, only use well-understood HTTP codes. 
Only use these codes with their appropriate methods, as outlined in the following tables:

### Well-understood codes:
#### Success Codes
Code | Method
-----|-------
200 | ALL
201 | POST, PUT
202 | POST, PUT, DELETE, PATCH
204 | PUT, DELETE, PATCH
207 | POST

#### Redirection Codes
Code | Method
-----|-------
301 | ALL
303 | PATCH, POST, PUT, DELETE
304 | GET, HEAD

#### Client Side Error Codes
Code | Method
-----|-------
400 | ALL
401 | ALL
403 | ALL
404 | ALL
405 | ALL
406 | ALL
408 | ALL
409 | POST, PUT, DELETE, PATCH
410 | ALL
412 | PUT, DELETE, PATCH
415 | POST, PUT, DELETE, PATCH
422 | ALL
423 | PUT, DELETE, PATCH
428 | ALL
429 | ALL

#### Server Side Error Codes
Code | Method
-----|-------
 500 | ALL
 501 | ALL
 502 | ALL
 503 | ALL
 504 | ALL

## M00012: Open API Version must be set to the correct version

OpenAPI specification version must be 3.0.3 or 3.0.4. Any others are not compatible.


# Enable linting

To enable linting add the execution goal `lint` to your plugin configuration. This hooks linting into your build process.

For example:
    
   ```xml
            <plugin>
                <groupId>com.backbase.oss</groupId>
                <artifactId>boat-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>lint</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>lint</goal>
                        </goals>
                       <configuration>
                              <inputSpec>${unversioned-filename-spec-dir}/</inputSpec>
                              <output>${project.build.directory}/boat-lint-reports</output>
                              <writeLintReport>true</writeLintReport>
                              <ignoreRules>${ignored-lint-rules}</ignoreRules>
                              <showIgnoredRules>true</showIgnoredRules>
                       </configuration>
                    </execution>
                    <execution>
                        <id>generate-rest-template-embedded</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>doc</goal>
                        </goals>
                        <configuration>
                            <inputSpec>{input-spec}</inputSpec>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
   
   ```

The following linting parameters are available:

   * failOnWarning
        - Default: false
        - If true, fails in a case where a warning is found.
        - Optional
   
   * ignoreRules
        - List of rule IDs. Specifies which will be ignored.
        - Default: false
        - Optional
   
   * inputSpec
        - Specifies input spec directory or file.
        - Required: true
   
   * output 
        - Default: ${project.build.directory}/boat-lint-reports)
        - Specifies output directory for lint reports.
        - Optional
   
   * showIgnoredRules 
        - Default: false
        - If true, shows the list of ignored rules.
        - Optional
   
   * writeLintReport
        - Default: true
        - If true, generates lint report.
        - Optional
 
