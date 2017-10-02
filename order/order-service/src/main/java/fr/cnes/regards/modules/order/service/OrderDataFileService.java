package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderDataFileService implements IOrderDataFileService {

    @Autowired
    private IOrderDataFileRepository repos;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IOrderDataFileService self;

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        return repos.save(dataFile);
    }

    @Override
    public Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles) {
        return repos.save(dataFiles);
    }

    @Override
    public List<OrderDataFile> findAllAvailables(Long orderId) {
        return repos.findAllAvailables(orderId);
    }

    @Override
    public List<OrderDataFile> findAll(Long orderId) {
        return repos.findAllByOrderId(orderId);
    }

    @Override
    public void downloadFile(Long orderId, UniformResourceName aipId, String checksum, HttpServletResponse response)
            throws IOException {
        // Maybe this file is into an AIP that is asked several time for this Order (several Dataset for example)
        /// but we just need first one because its name is unique by AIP ID and checksum
        Optional<OrderDataFile> dataFileOpt = repos.findFirstByChecksumAndIpIdAndOrderId(checksum, aipId, orderId);
        if (!dataFileOpt.isPresent()) {
            throw new NoSuchElementException();
        }
        OrderDataFile dataFile = dataFileOpt.get();
        response.addHeader("Content-disposition", "attachment;filename=" + dataFile.getName());
        response.setContentType(dataFile.getMimeType().toString());

        // Reading / Writing
        OutputStream os = response.getOutputStream();
        try (InputStream is = aipClient.downloadFile(aipId.toString(), checksum)) {
            ByteStreams.copy(is, os);
            os.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        // Update OrderDataFile (set State as DOWNLOADED, even if it is online)
        try {
            dataFile.setState(FileState.DOWNLOADED);
            self.save(dataFile);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Set<Order> updateCurrentOrdersCompletionValues() {
        OffsetDateTime now = OffsetDateTime.now();
        // find not yet finished orders and their sum of data files sizes
        List<Object[]> totalOrderFiles = repos.findSumSizesByOrderId(now);
        if (totalOrderFiles.isEmpty()) {
            return Collections.emptySet();
        }
        // All following methods returns a Collection of Object[] whom first elt is an Order
        Function<Object[], Long> getOrderIdFct = array -> ((Order) array[0]).getId();
        // All following methods returns a Collection of Object[] whom second elt is a Long (a sum or a count)
        Function<Object[], Long> getValueFct = array -> ((Long) array[1]);
        // Set or orders not yet finished
        Set<Order> orders = totalOrderFiles.stream().map(array -> (Order) array[0]).collect(Collectors.toSet());
        // Map { order_id -> total files size }
        Map<Long, Long> totalSizeMap = totalOrderFiles.stream().collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Map { order_id -> treated files size  }
        Map<Long, Long> treatedSizeMap = repos
                .selectSumSizesByOrderIdAndStates(now, FileState.ONLINE, FileState.AVAILABLE, FileState.DOWNLOADED,
                                                  FileState.ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));
        // Map { order_id -> files in error count }
        Map<Long, Long> errorCountMap = repos.selectCountFilesByOrderIdAndStates(now, FileState.ERROR).stream()
                .collect(Collectors.toMap(getOrderIdFct, getValueFct));

        // Update all orders completion values
        for (Order order : orders) {
            long totalSize = totalSizeMap.get(order.getId());
            long treatedSize = treatedSizeMap.containsKey(order.getId()) ? treatedSizeMap.get(order.getId()) : 0l;
            order.setPercentCompleted(Math.floorDiv(100 * (int) treatedSize, (int) totalSize));
            long errorCount = errorCountMap.containsKey(order.getId()) ? errorCountMap.get(order.getId()) : 0l;
            order.setFilesInErrorCount((int) errorCount);
        }
        return orders;
    }
}
