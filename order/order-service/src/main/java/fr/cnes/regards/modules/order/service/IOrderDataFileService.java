package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * @author oroussel
 */
public interface IOrderDataFileService {

    OrderDataFile save(OrderDataFile dataFile);

    Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles);

    List<OrderDataFile> findAllAvailables(Long orderId);

    void downloadFile(Long orderId, UniformResourceName aipId, String checksum, HttpServletResponse response)
            throws IOException;
}