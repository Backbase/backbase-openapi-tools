# Boat OpenAPI generator

The Boat OpenAPI generator is based on the official Open API Generator, version 6.2.1, and it provides several fixes and additional features.
The `boat` plugin has multiple goals:

## Spring Code Generator

| Option | Default | Description |
|-|-|-|
| `addBindingResult` | `false` | Adds BindingResult to Api method definitions' request bodies if UseBeanValidation true, for this to be effective you must configure UseBeanValidation, this is not done automatically |
| `addServletRequest` | `false` | Adds ServletRequest objects to API method definitions |
| `useSetForUniqueItems` | `false` | Use `java.util.Set` for arrays that has the attribute `uniqueItems` to `true` |
| `openApiNullable` | `true` | Whether to use the `jackson-databind-nullable` library |

## Java Code Generator

| Option | Default | Description |
|-|-|-|
| `restTemplateBeanName` | `none` | The qualifier of the `RestTemplate` used by the `ApiClient` (`resttemplate` only) |
| `useJacksonConversion` | `false` | Use Jackson to convert query parameters (`resttemplate` only) |
| `useSetForUniqueItems` | `false` | Use `java.util.Set` for arrays that has the attribute `uniqueItems` to `true` |
