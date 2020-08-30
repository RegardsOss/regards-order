package fr.cnes.regards.modules.processing.conf;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProcessingModuleManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingModuleManager.class);

    @Autowired
    private IPluginService pluginService;

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        for (PluginConfiguration p : pluginService.getAllPluginConfigurations()) {
            try {
                pluginService.deletePluginConfiguration(p.getBusinessId());
            } catch (ModuleException e) {
                LOGGER.warn(RESET_FAIL_MESSAGE, e);
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();
        Set<PluginConfiguration> configurations = getPluginConfs(configuration.getConfiguration());

        // First create connections
        for (PluginConfiguration plgConf : configurations) {
            try {
                Optional<PluginConfiguration> existingOne = loadPluginConfiguration(plgConf.getBusinessId());
                if (existingOne.isPresent()) {
                    existingOne.get().setLabel(plgConf.getLabel());
                    existingOne.get().setParameters(plgConf.getParameters());
                    pluginService.updatePluginConfiguration(existingOne.get());
                } else {
                    pluginService.savePluginConfiguration(plgConf);
                }
            } catch (ModuleException e) {
                LOGGER.warn(IMPORT_FAIL_MESSAGE, e);
                importErrors.add(e.getMessage());
            }
        }

        return importErrors;
    }

    private Optional<PluginConfiguration> loadPluginConfiguration(String businessId) {
        PluginConfiguration existingOne = null;
        try {
            existingOne = pluginService.getPluginConfiguration(businessId);
        } catch (EntityNotFoundException e) { // NOSONAR
            // Nothing to do, plugin configuration does not exists.
        }
        return Optional.ofNullable(existingOne);
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // export connections
        for (PluginConfiguration factory : pluginService.getAllPluginConfigurations()) {
            // All connection should be active
            PluginConfiguration exportedConf = pluginService.prepareForExport(factory);
            exportedConf.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConf));
        }
        return ModuleConfiguration.build(info, true, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     * @param items {@link ModuleConfigurationItem}s
     * @return  {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }
}