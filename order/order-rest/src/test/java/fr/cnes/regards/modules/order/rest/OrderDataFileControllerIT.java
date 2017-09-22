package fr.cnes.regards.modules.order.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@TestPropertySource(locations = "classpath:test.properties")
public class OrderDataFileControllerIT extends AbstractRegardsIT {

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IAipClient aipClient;

    private static final String USER = "raphael@mechoui.fr";

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
                                                                                "ORDER", UUID.randomUUID(), 1);

    @Configuration
    static class Conf {

        @Bean
        public ICatalogClient catalogClient() {
            return Mockito.mock(ICatalogClient.class);
        }

        @Bean
        public IAipClient aipClient() {
            IAipClient mock = Mockito.mock(IAipClient.class);
            Mockito.when(mock.downloadFile(Matchers.anyString(), Mockito.eq("FILE1")))
                    .thenReturn(this.getClass().getResourceAsStream("/files/file1.txt"));
            return mock;
        }
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
    }

    @Test
    public void testDownloadFile() throws URISyntaxException, IOException {
        Order order = new Order();
        order.setOwner(USER);
        order.setCreationDate(OffsetDateTime.now());
        order.setExpirationDate(order.getCreationDate().plus(3, ChronoUnit.DAYS));
        order = orderRepository.save(order);

        // One dataset task
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");

        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/file1.txt");
        dataFile1.setName("file1.txt");
        dataFile1.setIpId(DO1_IP_ID);
        dataFile1.setOnline(true);
        String checksum = "FILE1";
        dataFile1.setChecksum(checksum);
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.TEXT_PLAIN);
        files1Task.addFile(dataFile1);
        ds1Task.addReliantTask(files1Task);

        order = orderRepository.save(order);
        ds1Task = order.getDatasetTasks().first();

        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        ResultActions resultActions = performDefaultGet(
                "/orders/{orderId}/dataset/{datasetId}/aips/{aipId}/files/{checksum}", expectations,
                "Should return result", order.getId(), ds1Task.getId(), dataFile1.getIpId().toString(),
                dataFile1.getChecksum());

        assertMediaType(resultActions, MediaType.TEXT_PLAIN);
        File resultFile = File.createTempFile("ORDER", "");
        resultFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            InputStream is = new ByteArrayInputStream(resultActions.andReturn().getResponse().getContentAsByteArray());
            ByteStreams.copy(is, fos);
            is.close();
        }
        Assert.assertTrue(Files.equal(new File("src/test/resources/files/file1.txt"), resultFile));

        tenantResolver.forceTenant(DEFAULT_TENANT); // ?

        Optional<OrderDataFile> dataFileOpt = dataFileRepository
                .findFirstByChecksumAndIpIdAndOrderId(checksum, DO1_IP_ID, order.getId());
        Assert.assertTrue(dataFileOpt.isPresent());
        Assert.assertEquals(FileState.DOWNLOADED,
                            dataFileOpt.get().getState());
    }
}