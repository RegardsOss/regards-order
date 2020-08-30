package fr.cnes.regards.modules.processing.client;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.service.IBatchService;
import fr.cnes.regards.modules.processing.service.IProcessService;
import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=processing_rest_tests" })
@ContextConfiguration(classes = ProcessingRestClientTest.Config.class)
@TestPropertySource(
        properties = {
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///rs_testdb_${user.name}",
                "regards.jpa.multitenant.tenants[0].tenant=${user.name}_project"
        }
)
public class ProcessingRestClientTest extends AbstractRegardsWebIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingRestClientTest.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    private IProcessingRestClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private static final String ONLINE_CONF = "ONLINE_CONF";

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        client = FeignClientBuilder.build(
                new TokenClientProvider<>(IProcessingRestClient.class,
                                          "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                gson);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    @Test
    public void download() {
        List<PProcessDTO> ps = client.listAll().getBody();

        ps.forEach(p -> LOGGER.info("Found process: {}", p));

        assertThat(ps).isEqualTo(Values.processes);
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    interface Values {
        List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
        PBatch batch = randomInstance(PBatch.class);
    }

    @Configuration
    static class Config {

        @Bean IProcessService processService() {
            return new IProcessService() {

                @Override public Flux<PProcessDTO> findByTenant(String tenant) {
                    return Flux.fromIterable(Values.processes);
                }
            };
        }
        @Bean IBatchService batchService() {
            return new IBatchService() {
                @Override public Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data) {
                    return Mono.just(Values.batch);
                }
            };
        }

        @Bean IBatchEntityRepository batchEntityRepository () {
            return Mockito.mock(IBatchEntityRepository.class);
        }

        @Bean IExecutionEntityRepository executionEntityRepository() {
            return Mockito.mock(IExecutionEntityRepository.class);
        }

    }

}