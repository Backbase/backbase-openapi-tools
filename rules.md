# Boat Rule Sets

Boat comes with a number of Rule Sets which can be selected and used. The built in rule sets are documented below.

# InternalRuleSet

Internal rules which exist simply for the purpose of reporting fatal
errors found while attempting to parse the API specification.
The rules cannot be disabled or configured.

# BoatRuleSet

Zally also contains some additional rules enforcing aspects of the OpenAPI spec or other common sense rules that don't form part of the Zalando guidelines. Those addiitonal rules are documented here.

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


As well as the additional rules coming from Zally, there are also additional Boat Quay rules, some of which are Backbase 
specific but could be transferable. These additional rules, are documented below.    
##

## B012: Use Well Understood HTTP Status Codes

Adapted from Zally rule 150. Well-understood codes should be used to ensure that the codes in the spec are being 
correctly implemented. Any well-understood codes used should be used properly.

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

## B011: Use standard HTTP status codes

Adapted from Zally rule 150, this adds extra HTTP codes to the list of standard and well-understood codes. Any HTTP codes 
used in responses must be from the standard list.
Standard HTTP status codes:

    100, 101, 200, 201, 202, 203, 204, 205, 206, 207
    300, 301, 302, 303, 304, 305, 307, 400, 401, 402, 
    403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 
    413, 414, 415, 416, 417, 422, 423, 426, 428, 429, 
    431, 500, 501, 502, 503, 504, 505, 511, default
    
## B010: Pluralize resource names

Resource names should be pluralised in paths.
Path components after prefix and version should be plurals, for example, `/client-api/v1/bars` client and v1 are ignored.

## B009: Check prefix for paths should contain version

URL Paths Must contain a version number to maintain a consistent method of versioning though the apis.
## B008: Check x-icon value in the info block

An x-icon should be provided in the info block of the spec with the assigned value for the API. This should be provided 
so that the api portal can display the api with the correct icon.

## B007: Check prefix for paths

Path prefixes should be from this set of valid prefixes as it should refer to the type of api.

Valid prefixes:
 - client-api
 - service-api
 - integration-api
 
## B006: Check info block description format

Descriptions are important to include for readability and understanding the purpose of a spec.
A description must be present in the info block, it cannot be empty and must be no longer then the configured max length 
which is currently set to 140. 

## B005: Check info block title format

For Readability the title Must not be empty and should be no longer then the configured max title length, 35.

## B004: Check info block tags allowed

Tags in the info block, are required to group apis together, they must be from the list of valid meaningful tags below 
to create a consistent, uniform and well understood set of apis.
The Tags value must not be empty.

Valid info tags:

 - productTags: 
   - Retail 
   - Business
   - Wealth
   - Identity
   - Foundation
   - Basic Support
   - Flow
 - informativeTags: 
   - Mobile,
   - Security
   - Payments
   - Authentication
   - Employee
   - Cash
   - Insights
   
## B003: No reserved words allowed
   
Schema must not contain reserved words as keys, the reserved words from js, spring kotlin and swift, are checked for. 
This is to avoid confusion, when mapping to objects and to avoid injecting code?

## B002: All operationIds must be unique

Duplicate operationIds cause confusion especially when generating code from specifications, they must be unique.

## B001: No license information allowed

OpenAPI must not contain a license  because it's covered by the License Agreement we already negotiate with customers.

## M00012: Open API Version must be set to the correct version

OpenAPI specification version must be 3.0.3 or 3.0.4. Any others are not compatible.

