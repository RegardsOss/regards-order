package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import reactor.core.publisher.Mono;

public interface DomainEntityMapper {

    BatchEntity toEntity(PBatch batch);

    Mono<PBatch> toDomain(BatchEntity batch);


    ExecutionEntity toEntity(PExecution exec);

    Mono<PExecution> toDomain(ExecutionEntity exec);

}
