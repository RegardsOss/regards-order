package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.Function2;
import io.vavr.collection.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;

@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public class ProcessPluginConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginConfigController(
            IRuntimeTenantResolver runtimeTenantResolver,
            IProcessPluginConfigService rightsConfigService
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(path = PROCESS_CONFIG_PATH, consumes = MediaType.ALL_VALUE)
    public Flux<ProcessPluginConfigurationRightsDTO> findAll() {
        return withinTenantFlux((auth, tenant) -> {
            return rightsConfigService.findAllRightsPluginConfigs();
        });
    }


    @GetMapping(path = PROCESS_CONFIG_BID_PATH, consumes = MediaType.ALL_VALUE)
    public Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.findByBusinessId(processBusinessId);
        });
    }

    @PostMapping(path = PROCESS_CONFIG_PATH)
    public Mono<ProcessPluginConfigurationRightsDTO> create(
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.create(tenant, rightsDto);
        });
    }

    @PutMapping(path = PROCESS_CONFIG_BID_PATH)
    public Mono<ProcessPluginConfigurationRightsDTO> update(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.update(tenant, processBusinessId, rightsDto);
        });
    }

    @DeleteMapping(path = PROCESS_CONFIG_BID_PATH, consumes = MediaType.ALL_VALUE)
    public Mono<ProcessPluginConfigurationRightsDTO> save(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.delete(processBusinessId);
        });
    }

    @GetMapping(path = PROCESS_METADATA_PATH, consumes = MediaType.ALL_VALUE)
    public Flux<PluginMetaData> listAllDetectedPlugins() {
        return withinTenantFlux((auth, tenant) -> {
            return Flux.fromIterable(PluginUtils.getPlugins().values());
        });
    }

    @GetMapping(path = PROCESS_LINKDATASET_PATH)
    public Flux<ProcessLabelDTO> findProcessesByDataset(
            @PathVariable(DATASET_PARAM) String dataset
    ) {
        return withinTenantFlux((auth, tenant) -> {
            return rightsConfigService.getDatasetLinkedProcesses(dataset);
        });
    }

    @PostMapping(path = PROCESS_BY_DATASETS_PATH)
    public Mono<Map<String, io.vavr.collection.List<ProcessLabelDTO>>> findProcessesByDatasets(
            @RequestBody List<String> datasets
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.findProcessesByDatasets(datasets).map(ProcessesByDatasetsDTO::getMap);
        });
    }

    @PutMapping(path = PROCESS_LINKDATASET_PATH)
    public Mono<Void> attachDatasetToProcesses (
            @RequestBody List<UUID> processBusinessIds,
            @PathVariable(DATASET_PARAM) String dataset
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.putDatasetLinkedProcesses(processBusinessIds, dataset);
        });
    }

    @PutMapping(path = PROCESS_CONFIG_BID_USERROLE_PATH, consumes = MediaType.ALL_VALUE)
    public Mono<Void> attachRoleToProcess (
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(USER_ROLE_PARAM) String userRole
    ) {
        return withinTenantMono((auth, tenant) -> {
            return rightsConfigService.attachRoleToProcess(processBusinessId, userRole);
        });
    }

    private Mono<SecurityContext> getContext() {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Action requires JWT token"))));
    }


    private <T> Mono<T> withinTenantMono(Function2<JWTAuthentication, String, Mono<T>> fn) {
        return getContext()
                .flatMap(ctx -> {
                    JWTAuthentication auth = (JWTAuthentication) ctx.getAuthentication();
                    String tenant = auth.getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return fn.apply(auth, tenant)
                            .doOnTerminate(runtimeTenantResolver::clearTenant);
                })
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    private <T> Flux<T> withinTenantFlux(Function2<JWTAuthentication, String, Flux<T>> fn) {
        return getContext()
                .flatMapMany(ctx -> {
                    JWTAuthentication auth = (JWTAuthentication) ctx.getAuthentication();
                    String tenant = auth.getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return fn.apply(auth, tenant)
                            .doOnTerminate(runtimeTenantResolver::clearTenant);
                })
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

}