package fr.cnes.regards.modules.processing.entity;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import lombok.Value;

@Value
public class StepEntity {

    ExecutionStatus status;
    Long epochTs;
    String message;

}