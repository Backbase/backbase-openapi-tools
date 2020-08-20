package com.backbase.oss.boat.diff.model;

import java.util.LinkedList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangedOpenApiRenderList {
    LinkedList<ChangedOpenApiRender> renderList;
    ChangedOpenApiRender changedOpenApiRender;

    public ChangedOpenApiRenderList(ChangedOpenApi changedOpenApi) {
        renderList = new LinkedList<>();
        if (!(changedOpenApi.getNewEndpoints().isEmpty())) {
            for (int i=0;i<changedOpenApi.getNewEndpoints().size();i++) {
                changedOpenApiRender=new ChangedOpenApiRender(changedOpenApi.getNewEndpoints().get(i),"New EndPoints");
                renderList.add(changedOpenApiRender);
            }
        }
        if (!(changedOpenApi.getMissingEndpoints().isEmpty())) {
            for (int i=0;i<changedOpenApi.getMissingEndpoints().size();i++) {
                changedOpenApiRender=new ChangedOpenApiRender(changedOpenApi.getMissingEndpoints().get(i),"Deleted EndPoints");
                renderList.add(changedOpenApiRender);
            }
        }
        if (!(changedOpenApi.getChangedOperations().isEmpty())) {
            for (int i=0;i<changedOpenApi.getChangedOperations().size();i++) {
                changedOpenApiRender=new ChangedOpenApiRender(changedOpenApi.getChangedOperations().get(i));
                renderList.add(changedOpenApiRender);
            }
        }
    }
}
