package fr.cnes.regards.modules.processing.rest;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATE_FROM_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATE_TO_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PAGE_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.SIZE_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.STATUS_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.TENANT_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.USER_EMAIL_PARAM;
import static fr.cnes.regards.modules.processing.rest.utils.PageUtils.DEFAULT_PAGE;
import static fr.cnes.regards.modules.processing.rest.utils.PageUtils.DEFAULT_SIZE;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.utils.TimeUtils;
import reactor.core.publisher.Flux;

@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@RequestMapping(path = MONITORING_EXECUTIONS_PATH)
public class PMonitoringReactiveController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringReactiveController.class);

    private final IPExecutionRepository execRepo;

    @Autowired
    public PMonitoringReactiveController(IPExecutionRepository execRepo) {
        this.execRepo = execRepo;
    }

    @GetMapping
    public Flux<PExecution> executions(@RequestParam(name = TENANT_PARAM) String tenant,
            @RequestParam(name = STATUS_PARAM) List<ExecutionStatus> status,
            @RequestParam(name = USER_EMAIL_PARAM, required = false) String userEmail,
            @RequestParam(name = DATE_FROM_PARAM, defaultValue = "2000-01-01T00:00:00.000Z") String fromStr,
            @RequestParam(name = DATE_TO_PARAM, defaultValue = "2100-01-01T00:00:00.000Z") String toStr,
            @RequestParam(name = PAGE_PARAM, defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(name = SIZE_PARAM, defaultValue = DEFAULT_SIZE) int size) {
        LOGGER.info("status={}", status);
        LOGGER.info("userEmail={}", userEmail);
        LOGGER.info("from={}", fromStr);
        LOGGER.info("to={}", toStr);
        OffsetDateTime from = TimeUtils.parseUtc(fromStr);
        OffsetDateTime to = TimeUtils.parseUtc(toStr);

        PageRequest paged = PageRequest.of(page, size);
        if (userEmail == null) {
            return execRepo.findByTenantAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant, status, from,
                                                                                                  to, paged)
                    .doOnError(t -> {
                        LOGGER.error(t.getMessage(), t);
                    });
        } else {
            return execRepo
                    .findByTenantAndUserEmailAndCurrentStatusInAndLastUpdatedAfterAndLastUpdatedBefore(tenant,
                                                                                                       userEmail,
                                                                                                       status, from, to,
                                                                                                       paged)
                    .doOnError(t -> {
                        LOGGER.error(t.getMessage(), t);
                    });
        }
    }

}
