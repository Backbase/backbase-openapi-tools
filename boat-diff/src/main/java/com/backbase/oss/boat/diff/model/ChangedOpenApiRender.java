package com.backbase.oss.boat.diff.model;

import lombok.Getter;
import lombok.Setter;

/*
 * This class is for CI Team
 * ChangedOpenApiRender is summary of ONE result ChangedOpenApi
 * ChangedOpenApiRednerList will include all results for CI team.
 * */
@Getter
@Setter
public class ChangedOpenApiRender {

    String name;
    String httpMehhod;
    String whatChanged;
    String isBreaking;

    public ChangedOpenApiRender(String name, String httpMehhod, String whatChanged, String isBreaking) {
        this.name = name;
        this.httpMehhod = httpMehhod;
        this.whatChanged = whatChanged;
        this.isBreaking = isBreaking;
    }

    public ChangedOpenApiRender(Endpoint endpoint, String whatChanged) {
        this.name = endpoint.getPathUrl();
        this.httpMehhod = endpoint.getMethod().toString();
        this.whatChanged = whatChanged;
        this.isBreaking = "Breaking";
    }

    public ChangedOpenApiRender(ChangedOperation changedOperation) {
        this.name = changedOperation.getPathUrl();
        this.httpMehhod = changedOperation.getHttpMethod().toString();
        whatChanged = getApiResponse(changedOperation);
        if (whatChanged != null) {
            this.isBreaking = "Breaking";
        }
    }

    public String getApiResponse(ChangedOperation changedOperation) {
        if (changedOperation.getApiResponses() != null) {
            if (changedOperation.getApiResponses().getIncreased().size() > 0) {
                return "AddedEndPoint";
            }
            if (changedOperation.getApiResponses().getMissing().size() > 0) {
                return "DeletedEndPoint";
            }
            if (changedOperation.getApiResponses().getChanged().size() > 0) {
                return "ChangedEndPoint";
            }
        }
        return "ChangedEndPoint";
    }
}

