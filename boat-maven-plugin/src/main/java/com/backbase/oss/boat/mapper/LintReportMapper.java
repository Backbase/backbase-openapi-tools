package com.backbase.oss.boat.mapper;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.fasterxml.jackson.core.JsonPointer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LintReportMapper {

    @Mapping(target = "title", source = "name")
    @Mapping(target = "filePath", source = "spec.name")
//    @Mapping(target = "availableRules", source = "")
    BoatLintReport bayReportToBoatReport(com.backbase.oss.boat.bay.client.model.BoatLintReport bayReport);

}
