/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.order.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.test.Service2Configuration;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = Service2Configuration.class)
@DirtiesContext
public class OrderService2IT {

    private static final String TENANT = "ORDER";

    // Only one dataset
    private static String DS_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, TENANT,
            new UUID(0l, 0l), 1).toString();

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IOrderRepository orderRepos;

    @Autowired
    private IOrderDataFileRepository dataFileRepos;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Before
    public void init() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();

        dataFileRepos.deleteAll();

        jobInfoRepos.deleteAll();
    }

    @Test
    public void testBucketsJobs() throws IOException, InterruptedException {

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.PROJECT_ADMIN.toString());

        Basket basket = new Basket("tulavu@qui.fr");
        SortedSet<BasketDatasetSelection> dsSelections = new TreeSet<>();
        basket.setDatasetSelections(dsSelections);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS_IP_ID);
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFilesCount(12);
        dsSelection.setFilesSize(3_000_171l);
        dsSelection.setOpenSearchRequest("ALL");
        dsSelections.add(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket);

        UniformResourceName IP_ID1 = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, TENANT,
                new UUID(1, 1), 1);
        System.out.println(IP_ID1);

        Thread.sleep(20_000);
    }

    private OrderDataFile createOrderDataFile(Order order, UniformResourceName aipId, String filename, boolean online)
            throws URISyntaxException {
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/" + filename);
        dataFile1.setName(filename);
        dataFile1.setIpId(aipId);
        if (online) {
            dataFile1.setOnline(true);
        } else {
            dataFile1.setOnline(false);
            dataFile1.setState(FileState.AVAILABLE);
        }
        dataFile1.setChecksum(filename);
        dataFile1.setSize(new File("src/test/resources/files/", filename).length());
        dataFile1.setOrderId(order.getId());
        dataFile1.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        return dataFile1;
    }

}