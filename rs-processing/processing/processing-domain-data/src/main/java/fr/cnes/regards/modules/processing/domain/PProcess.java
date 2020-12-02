/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;

import java.util.UUID;

public interface PProcess {

    UUID getProcessId();

    String getProcessName();

    Map<String, String> getProcessInfo();

    boolean isActive();

    ConstraintChecker<PBatch> getBatchChecker();

    ConstraintChecker<PExecution> getExecutionChecker();

    Seq<ExecutionParameterDescriptor> getParameters();

    IResultSizeForecast getResultSizeForecast();

    IRunningDurationForecast getRunningDurationForecast();

    IWorkloadEngine getEngine();

    IExecutable getExecutable();

    IOutputToInputMapper getMapper();

    @Value
    class ConcretePProcess implements PProcess {
        UUID processId;
        String processName;
        Map<String, String> processInfo;
        boolean active;
        ConstraintChecker<PBatch> batchChecker;
        ConstraintChecker<PExecution> executionChecker;
        Seq<ExecutionParameterDescriptor> parameters;
        IResultSizeForecast resultSizeForecast;
        IRunningDurationForecast runningDurationForecast;
        IWorkloadEngine engine;
        IExecutable executable;
        IOutputToInputMapper mapper;
    }

}
