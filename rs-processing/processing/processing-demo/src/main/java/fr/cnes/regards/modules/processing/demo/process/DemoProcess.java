package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.demo.DemoConstants.PROFILE;
import static fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType.STRING;

public class DemoProcess implements PProcess {

    private final IWorkloadEngine engine;
    private final DemoSimulatedAsyncProcessFactory asyncProcessFactory;


    public DemoProcess(IWorkloadEngine engine, DemoSimulatedAsyncProcessFactory asyncProcessFactory) {
        this.engine = engine;
        this.asyncProcessFactory = asyncProcessFactory;
    }

    @Override public UUID getProcessId() {
        return UUID.fromString("500f6543-4379-461d-80f2-8fe3bd211e9d");
    }

    @Override public String getProcessName() {
        return "DemoProcess";
    }

    @Override public Map<String, String> getProcessInfo() {
        return HashMap.empty();
    }

    @Override public boolean isActive() {
        return true;
    }

    @Override public ConstraintChecker<PBatch> getBatchChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override public ConstraintChecker<PExecution> getExecutionChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override public Seq<ExecutionParameterDescriptor> getParameters() {
        return List.of(
            new ExecutionParameterDescriptor(PROFILE, STRING, "Profile for this process execution", false, false, true)
        );
    }

    @Override public IResultSizeForecast getResultSizeForecast() {
        // For example, doubles the size of the input
        return i -> i * 2;
    }

    @Override public IRunningDurationForecast getRunningDurationForecast() {
        // For example, takes 1 second per megabyte of input
        return IRunningDurationForecast.secondsPerMegabytes(1);
    }

    @Override public IWorkloadEngine getEngine() {
        return engine;
    }

    @Override public IOutputToInputMapper getMapper() {
        return (ctx, o) -> List.empty();
    }

    @Override public IExecutable getExecutable() {
        return new DemoProcessExecutable(asyncProcessFactory);
    }
}