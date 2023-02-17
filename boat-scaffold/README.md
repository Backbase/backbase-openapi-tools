# Boat OpenAPI generator

The Boat OpenAPI generator is based on the official Open API Generator, version 6.2.1, and it provides several fixes and additional features.
The `boat` plugin has multiple goals:

## Spring Code Generator

| Option | Default | Description |
|-|-|-|
| `addBindingResult` | `false` | Adds BindingResult to Api method definitions' request bodies if UseBeanValidation true, for this to be effective you must configure UseBeanValidation, this is not done automatically |
| `addServletRequest` | `false` | Adds ServletRequest objects to API method definitions |
| `useClassLevelBeanValidation` | `false` | Adds @Validated annotation to API interfaces |
| `useLombokAnnotations` | `false` | Use Lombok annotations to generate properties accessors and `hashCode`/`equals`/`toString` methods |
| `useSetForUniqueItems` | `false` | Use `java.util.Set` for arrays that has the attribute `uniqueItems` to `true` |
| `openApiNullable` | `true` | Whether to use the `jackson-databind-nullable` library |
| `useWithModifiers` | `false` | Generates bean `with` modifiers for fluent style |
| `useProtectedFields` | `false` | Whether to use protected visibility for model fields |

## Java Code Generator

| Option | Default | Description |
|-|-|-|
| `createApiComponent` | `true` | Whether to generate the client as a Spring component (`resttemplate` only) |
| `restTemplateBeanName` | `none` | The qualifier of the `RestTemplate` used by the `ApiClient` (`resttemplate` only) |
| `useClassLevelBeanValidation` | `false` | Adds @Validated annotation to API interfaces |
| `useJacksonConversion` | `false` | Use Jackson to convert query parameters (`resttemplate` only) |
| `useSetForUniqueItems` | `false` | Use `java.util.Set` for arrays that has the attribute `uniqueItems` to `true` |
| `useProtectedFields` | `false` | "Whether to use protected visibility for model fields |
