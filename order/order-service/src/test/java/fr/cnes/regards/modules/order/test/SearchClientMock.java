/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;

/**
 * Mock of ISearchClient to be used by ServiceConfiguration
 * @author oroussel
 * @author Sébastien Binda
 */
public class SearchClientMock implements IComplexSearchClient {

    public static final UniformResourceName DS1_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS3_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    private static Dataset ds1;

    private static Dataset ds2;

    private static Dataset ds3;

    static {

        Model dsModel = new Model();
        dsModel.setName("datasetModel");
        dsModel.setType(EntityType.DATASET);
        dsModel.setId(1L);

        ds1 = new Dataset(dsModel, "tenant", "DS1", "DS1");
        ds1.setIpId(DS1_IP_ID);

        ds2 = new Dataset(dsModel, "tenant", "DS2", "DS2");
        ds2.setIpId(DS2_IP_ID);

        ds3 = new Dataset(dsModel, "tenant", "DS3", "DS3");
        ds3.setIpId(DS3_IP_ID);
    }

    public static final String QUERY_DS2_DS3 = "tags:(" + DS2_IP_ID.toString() + " OR " + DS3_IP_ID.toString() + ")";

    public static final Map<UniformResourceName, DatasetFeature> DS_MAP = new ImmutableMap.Builder<UniformResourceName, DatasetFeature>()
            .put(DS1_IP_ID, ds1.getFeature()).put(DS2_IP_ID, ds2.getFeature()).put(DS3_IP_ID, ds3.getFeature()).build();

    /**
     * DS1 => 2 documents, 2 RAWDATA files (+ 6 QUICKLOOKS 2 x 3 of each size), 1 Mb each RAW file
     * 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD (1 501 500 b)
     */
    private static DocFilesSummary createSummaryForDs1AllFiles() {
        DocFilesSummary summary = new DocFilesSummary(2, 8, 3_003_000);
        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 8, 3_003_000);
        FilesSummary rawSummary = new FilesSummary(2, 2_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        FilesSummary qlHdSummary = new FilesSummary(2, 1_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        FilesSummary qlMdSummary = new FilesSummary(2, 2_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        FilesSummary qlSdSummary = new FilesSummary(2, 1_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);
        return summary;
    }

    /**
     * 2 docs for DS2, 1 for DS3
     * 1 raw by doc, 3 quicklooks
     * 1Mb, 10 kb, 100 b, 1 b
     */
    private static DocFilesSummary createSummaryForDs2Ds3AllFilesFirstCall() {
        DocFilesSummary summary = new DocFilesSummary(3, 12, 3_030_303);
        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 8, 2_020_202);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(2, 20_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(2, 200));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(2, 2));

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 4, 1_010_101);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_000_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(1, 10_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(1, 100));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(1, 1));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    /**
     * 2 docs for DS2
     * 1 raw by doc
     * 1Mb
     */
    private static DocFilesSummary createSummaryForDs2AllFiles() {
        DocFilesSummary summary = new DocFilesSummary(2, 8, 2_020_202);
        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 8, 2_020_202);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(2, 20_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(2, 200));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(2, 2));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        return summary;
    }

    /**
     * 1 doc for DS3
     * 1 raw by doc
     * 1Mb
     */
    private static DocFilesSummary createSummaryForDs3AllFiles() {
        DocFilesSummary summary = new DocFilesSummary(1, 4, 1_010_101);
        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 4, 1_010_101);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_010_101));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(1, 10_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(1, 100));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(1, 1));

        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    private static DocFilesSummary createSummaryForAllDsAllFiles() {
        DocFilesSummary summary = new DocFilesSummary(5, 20, 5_023_202);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 8, 3_003_00);
        FilesSummary rawSummary = new FilesSummary(2, 2_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        FilesSummary qlHdSummary = new FilesSummary(2, 1_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        FilesSummary qlMdSummary = new FilesSummary(2, 2_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        FilesSummary qlSdSummary = new FilesSummary(2, 1_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);

        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 8, 2_020_202);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(2, 20_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(2, 200));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(2, 2));

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 4, 1_010_101);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_000_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(1, 10_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(1, 100));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(1, 1));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    private static DataType getDataType(String filename) {
        if (filename.endsWith("ql_hd.txt")) {
            return DataType.QUICKLOOK_HD;
        } else if (filename.endsWith("ql_md.txt")) {
            return DataType.QUICKLOOK_MD;
        } else if (filename.endsWith("ql_sd.txt")) {
            return DataType.QUICKLOOK_SD;
        } else {
            return DataType.RAWDATA;
        }
    }

    @Override
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(ComplexSearchRequest complexSearchRequest) {
        List<SearchRequest> requests = complexSearchRequest.getRequests();
        Assert.assertFalse("Cannot handle empty complex search", requests.isEmpty());
        SearchRequest request = requests.stream().findFirst().get();
        String query = request.getSearchParameters().get("q").stream().findFirst().orElse(null);
        String datasetUrn = request.getDatasetUrn();

        if (datasetUrn == null) {
            if (QUERY_DS2_DS3.equals(query)) {
                return new ResponseEntity<>(createSummaryForDs2Ds3AllFilesFirstCall(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(createSummaryForAllDsAllFiles(), HttpStatus.OK);
            }
        } else if (datasetUrn.equals(DS1_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs1AllFiles(), HttpStatus.OK);
        } else if (datasetUrn.equals(DS2_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs2AllFiles(), HttpStatus.OK);
        } else if (datasetUrn.equals(DS3_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs3AllFiles(), HttpStatus.OK);
        } else {
            throw new RuntimeException("Someone completely shit out this test ! Investigate and kick his ass !");
        }
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchDataObjects(
            ComplexSearchRequest complexSearchRequest) {
        if (complexSearchRequest.getPage() == 0) {
            try {
                List<EntityModel<EntityFeature>> list = new ArrayList<>();
                File testDir = new File("src/test/resources/files");
                for (File dir : testDir.listFiles()) {
                    EntityFeature feature = new DataObjectFeature(UniformResourceName.fromString(dir.getName()),
                            dir.getName(), dir.getName());
                    Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
                    for (File file : dir.listFiles()) {
                        DataFile dataFile = new DataFile();
                        dataFile.setOnline(false);
                        dataFile.setUri(new URI("file:///test/" + file.getName()));
                        dataFile.setFilename(file.getName());
                        dataFile.setFilesize(file.length());
                        dataFile.setReference(false);
                        dataFile.setChecksum(file.getName());
                        dataFile.setDigestAlgorithm("MD5");
                        dataFile.setMimeType(file.getName().endsWith("txt") ? MediaType.TEXT_PLAIN
                                : MediaType.APPLICATION_OCTET_STREAM);
                        dataFile.setDataType(getDataType(file.getName()));
                        fileMultimap.put(getDataType(file.getName()), dataFile);
                    }
                    feature.setFiles(fileMultimap);
                    list.add(new EntityModel<>(feature));
                }

                return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(), list,
                        new PagedModel.PageMetadata(list.size(), 0, list.size())));
            } catch (URISyntaxException e) {
                throw new RsRuntimeException(e);
            }
        }
        return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(), Collections.emptyList(),
                new PagedModel.PageMetadata(0, 0, 0)));
    }
}
