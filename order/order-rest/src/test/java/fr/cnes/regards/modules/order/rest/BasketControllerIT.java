/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.rest;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.BadBasketSelectionRequestException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import io.vavr.collection.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = OrderConfiguration.class)
@TestPropertySource(properties = { "regards.tenant=basket", "spring.jpa.properties.hibernate.default_schema=basket" })
public class BasketControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    public static final UniformResourceName DS1_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS3_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO4_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO5_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    private BasketSelectionRequest createBasketSelectionRequest(String datasetUrn, String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        request.setDatasetUrn(datasetUrn);
        return request;
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());

        basketRepos.deleteAll();

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();

        Project project = new Project();
        project.setHost("regards.org");
        Mockito.when(projectsClient.retrieveProject(ArgumentMatchers.anyString()))
                .thenReturn(ResponseEntity.ok(new EntityModel<>(project)));
        Mockito.when(authResolver.getUser()).thenReturn(getDefaultUserEmail());
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
    }

    @Test
    public void testAddBadSelection() {
        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, new BasketSelectionRequest(),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY), "error");
    }

    @Test
    public void testAddNullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections
                .singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                           customizer().expectStatusNoContent(), "error");
    }

    @Test
    public void testAddEmptyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections
                .singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                           customizer().expectStatusNoContent(), "error");
    }

    @Test
    public void testAddFullOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        request.setEntityIdsToInclude(Collections
                .singleton("URN:AIP:DATA:project2:77d75611-fac4-3047-8d3b-e0468fe1063e:V1"));
        request.setDatasetUrn("URN%3AAIP%3ADATASET%3AOlivier%3A4af7fa7f-110e-42c8-b434-7c863c280548%3AV1");

        RequestBuilderCustomizer customizer = customizer().expectStatusNoContent();

        // Add doc
        ConstrainedFields constrainedFields = new ConstrainedFields(BasketSelectionRequest.class);
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(constrainedFields.withPath("content", "basket object").optional().type(JSON_OBJECT_TYPE));
        customizer.document(PayloadDocumentation.relaxedResponseFields(fields));

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request, customizer, "error");
    }

    @Test
    public void testAddOnlyOpensearchSelection() throws BadBasketSelectionRequestException {
        // Test POST without argument : order should be created with RUNNING status
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("legacy");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", "MACHIN: BIDULE AND PATATIPATAT: POUET");
        request.setSearchParameters(parameters);

        performDefaultPost(BasketController.ORDER_BASKET + BasketController.SELECTION, request,
                           customizer().expectStatusNoContent(), "error");
    }


    @Test
    public void testAddThenRemoveProcessDescription() throws BadBasketSelectionRequestException {
        UUID processBusinessId = UUID.randomUUID();
        java.util.HashMap<String, String> parameters = HashMap.of("key", "value").toJavaMap();

        Basket basket = createBasket();

        RequestBuilderCustomizer customizerAdd = customizer()
                .expectStatusOk()
                .expectValue("$.content.datasetSelections[0].processDatasetDescription.processBusinessId", processBusinessId.toString())
                .expectValue("$.content.datasetSelections[0].processDatasetDescription.parameters.key", "value");
        performDefaultPut(
                BasketController.ORDER_BASKET + BasketController.DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS,
                new ProcessDatasetDescription(processBusinessId, parameters),
                customizerAdd,
                "error",
                basket.getDatasetSelections().first().getId());

        RequestBuilderCustomizer customizerRemove = customizer()
                .expectStatusOk()
                .expectDoesNotExist("$.content.datasetSelections[0].processDatasetDescription");
        performDefaultPut(
                BasketController.ORDER_BASKET + BasketController.DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS,
                "",
                customizerRemove,
                "error",
                basket.getDatasetSelections().first().getId());
    }

    @Test
    public void testGetEmptyBasket() {
        performDefaultGet(BasketController.ORDER_BASKET, customizer().expectStatusNoContent(), "error");
    }

    @Test
    public void testGetBasket() {
        createBasket();
        RequestBuilderCustomizer customizer = customizer()
            .expectStatusOk()
            .expectValue("$.content.quota", 8L)
            .expectValue("$.content.datasetSelections[0].filesCount", 10L)
            .expectValue("$.content.datasetSelections[0].filesSize", 124452L)
            .expectValue("$.content.datasetSelections[0].quota", 8L)
            .expectValue("$.content.datasetSelections[0].itemsSelections[0].filesCount", 10L)
            .expectValue("$.content.datasetSelections[0].itemsSelections[0].filesSize", 124452L)
            .expectValue("$.content.datasetSelections[0].itemsSelections[0].quota", 8L);
        performDefaultGet(BasketController.ORDER_BASKET, customizer, "error");
    }

    private Basket createBasket() {
        OffsetDateTime date = OffsetDateTime.now();

        Basket basket = new Basket(getDefaultUserEmail());
        BasketDatasetSelection dsSel = new BasketDatasetSelection();
        dsSel.setDatasetIpid("URN:AIP:DATASET:Olivier:4af7fa7f-110e-42c8-b434-7c863c280548:V1");
        dsSel.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 2L);
        dsSel.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 2L);
        dsSel.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 8L);
        dsSel.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 124450L);
        dsSel.setFileTypeCount(DataType.RAWDATA.name(), 10L);
        dsSel.setFileTypeSize(DataType.RAWDATA.name(), 124452L);
        dsSel.setDatasetLabel("DATASET1");
        dsSel.setObjectsCount(5);

        BasketDatedItemsSelection itemSel = new BasketDatedItemsSelection();
        itemSel.setDate(date);
        itemSel.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 2L);
        itemSel.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 2L);
        itemSel.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 8L);
        itemSel.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 124450L);
        itemSel.setFileTypeCount(DataType.RAWDATA.name(), 10L);
        itemSel.setFileTypeSize(DataType.RAWDATA.name(), 124452L);
        itemSel.setObjectsCount(5);
        itemSel.setSelectionRequest(createBasketSelectionRequest(null, ""));

        dsSel.addItemsSelection(itemSel);
        basket.addDatasetSelection(dsSel);
        basketRepos.save(basket);

        return basket;
    }

    @Test
    public void testRemoveDatasetSelection() throws UnsupportedEncodingException {
        Basket basket = createBasket();
        RequestBuilderCustomizer customizer = customizer()
            .expectStatusOk()
            .expectIsEmpty("$.content.datasetSelections")
            .expectValue("$.content.quota", 0L);
        performDefaultDelete(
            BasketController.ORDER_BASKET + BasketController.DATASET_DATASET_SELECTION_ID,
            customizer,
            "error",
            basket.getDatasetSelections().first().getId());
    }

    @Test
    public void testRemoveDatedItemSelection() throws UnsupportedEncodingException {
        Basket basket = createBasket();
        OffsetDateTime date = basket.getDatasetSelections().first().getItemsSelections().first().getSelectionRequest()
                .getSelectionDate();

        performDefaultDelete(BasketController.ORDER_BASKET
                + BasketController.DATASET_DATASET_SELECTION_ID_ITEMS_SELECTION_DATE, customizer().expectStatusOk(),
                             "error", basket.getDatasetSelections().first().getId(), OffsetDateTimeAdapter.format(date),
                             Charset.defaultCharset().toString());
    }

    @Test
    public void testEmptyBasket() {
        createBasket();
        performDefaultDelete(BasketController.ORDER_BASKET, customizer().expectStatusNoContent(), "error");
    }
}
