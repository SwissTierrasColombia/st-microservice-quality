package com.ai.st.microservice.quality.modules.shared.infrastructure.microservices;

import com.ai.st.microservice.common.clients.WorkspaceFeignClient;
import com.ai.st.microservice.common.dto.workspaces.MicroserviceMunicipalityDto;
import com.ai.st.microservice.common.dto.workspaces.MicroserviceWorkspaceOperatorDto;
import com.ai.st.microservice.quality.modules.shared.domain.*;
import com.ai.st.microservice.quality.modules.shared.domain.exceptions.MicroserviceUnreachable;
import com.ai.st.microservice.quality.modules.shared.domain.contracts.WorkspaceMicroservice;
import com.ai.st.microservice.quality.modules.shared.domain.exceptions.MunicipalityInvalid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public final class HTTPWorkspaceMicroservice implements WorkspaceMicroservice {

    private final WorkspaceFeignClient workspaceFeignClient;

    public HTTPWorkspaceMicroservice(WorkspaceFeignClient workspaceFeignClient) {
        this.workspaceFeignClient = workspaceFeignClient;
    }

    @Override
    public boolean verifyOperatorBelongToManager(OperatorCode operatorCode, ManagerCode managerCode,
            MunicipalityCode municipalityCode) {

        List<MicroserviceWorkspaceOperatorDto> workspacesOperators;
        try {
            workspacesOperators = workspaceFeignClient.findWorkspacesByOperator(operatorCode.value());

            int size = (int) workspacesOperators.stream()
                    .filter(microserviceWorkspaceDto -> microserviceWorkspaceDto.getMunicipality().getCode()
                            .equals(municipalityCode.value()))
                    .collect(Collectors.toList()).stream()
                    .filter(microserviceWorkspaceOperator -> microserviceWorkspaceOperator.getManagerCode()
                            .equals(managerCode.value()))
                    .count();

            return (size > 0);
        } catch (Exception e) {
            throw new MicroserviceUnreachable("workspaces");
        }
    }

    @Override
    public DepartmentMunicipality getDepartmentMunicipalityName(MunicipalityCode municipalityCode) {
        MicroserviceMunicipalityDto municipalityDto = workspaceFeignClient
                .findMunicipalityByCode(municipalityCode.value());
        if (municipalityDto == null) {
            throw new MunicipalityInvalid();
        }
        return new DepartmentMunicipality(DepartmentName.fromValue(municipalityDto.getDepartment().getName()),
                MunicipalityName.fromValue(municipalityDto.getName()));
    }

}
