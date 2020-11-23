package fr.cnes.regards.modules.processing.rest;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.service.IBatchService;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;

@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@RequestMapping(path = BATCH_PATH)
public class PBatchController {

    private final IBatchService batchService;

    private final IPUserAuthService authFactory;

    private final IAuthenticationResolver authResolver;

    private final IRuntimeTenantResolver tenantResolver;

    @Autowired
    public PBatchController(IBatchService batchService, IPUserAuthService authFactory,
            IAuthenticationResolver authResolver, IRuntimeTenantResolver tenantResolver) {
        this.batchService = batchService;
        this.authFactory = authFactory;
        this.authResolver = authResolver;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping
    @ResourceAccess(description = "Attempt to create a batch for future executions", role = DefaultRole.REGISTERED_USER)
    public PBatchResponse createBatch(@RequestBody PBatchRequest data) {
        PUserAuth auth = authFactory.authFromUserEmailAndRole(tenantResolver.getTenant(), authResolver.getUser(),
                                                              authResolver.getRole());
        return batchService.checkAndCreateBatch(auth, data)
                .map(b -> new PBatchResponse(b.getId(), b.getCorrelationId())).block();
    }

}
