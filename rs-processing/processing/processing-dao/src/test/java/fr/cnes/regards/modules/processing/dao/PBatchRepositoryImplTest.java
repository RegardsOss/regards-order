package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.micrometer.core.annotation.Timed;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static io.vavr.collection.List.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


public class PBatchRepositoryImplTest extends AbstractRepoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PBatchRepositoryImplTest.class);

    public static final int CONCURRENT_RUNS = 200;

    @Test
    public void batch_save_then_getOne() throws Exception {
        java.util.List<Runnable> asserts = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(CONCURRENT_RUNS);

        Flux.range(0, CONCURRENT_RUNS).flatMap(i -> {
            //////////////////////////
            // TESTING BATCH SAVE/FIND
            //////////////////////////
            // GIVEN
            PBatch newBatch = randomInstance(PBatch.class).asNew();
            LOGGER.info("ATTEMPT {}, PBatch used is: {}", i, newBatch);

            // WHEN
            return saveBatch(i, newBatch).flatMap(persistedBatch -> {
                return findBatch(i, persistedBatch).flatMap(foundBatch -> {

                    // THEN
                    asserts.add(() -> assertThat(foundBatch).isEqualTo(persistedBatch));

                    //////////////////////////
                    // TESTING EXEC SAVE/FIND
                    //////////////////////////
                    // GIVEN
                    PExecution newExec = asNew(randomInstance(PExecution.class))
                            .withBatchId(persistedBatch.getId())
                            .withExpectedDuration(Duration.ofSeconds(10));

                    // WHEN
                    return saveExec(i, newExec).flatMap(persistedExec -> {
                        return findExec(i, persistedExec).flatMap(foundExec -> {

                            // THEN
                            asserts.add(() -> assertThat(foundExec).isEqualTo(persistedExec));

                            //////////////////////////
                            // ADDING A STEP TO EXEC
                            //////////////////////////
                            // GIVEN
                            PStep newStep = randomInstance(PStep.class).clean().withTime(nowUtc().withNano(0));

                            // WHEN
                            return addStep(i, persistedExec, newStep).flatMap(exec -> {
                                // THEN
                                Seq<PStep> steps = exec.getSteps();
                                PStep foundStep = steps.last();
                                asserts.add(() -> {
                                    assertThat(steps).hasSize(1);
                                    assertThat(foundStep).isEqualTo(newStep);
                                });
                                return Unit.mono();
                            });
                        });
                    });
                });
            })
            .doOnTerminate(latch::countDown);
        })
        .doOnError(t -> {
            asserts.add(() -> {
                LOGGER.error(t.getMessage(), t); fail(t.getMessage());
            });
            Stream.range(0, latch.getCount()).forEach(j -> latch.countDown());
        })
        .subscribe();

        latch.await(20L, TimeUnit.SECONDS);

        LOGGER.info("Asserts to run: {}", asserts.size());
        asserts.forEach(Runnable::run);

        LOGGER.info("Done");
    }

    @Timed("save batch")
    private Mono<PBatch> saveBatch(int i, PBatch pBatch) {
        return domainBatchRepo.save(pBatch)
                .doOnNext(b -> LOGGER.info("ATTEMPT {}, Saved batch {}", i, b));
    }

    @Timed("save exec")
    private Mono<PExecution> saveExec(int i, PExecution pExec) {
        return domainExecRepo.create(pExec)
                .doOnNext(e -> LOGGER.info("ATTEMPT {}, Saved exec {}", i, e));
    }

    @Timed("add step to exec")
    private Mono<PExecution> addStep(int i, PExecution persistedExec, PStep step) {
        PExecution executionWithNewStep = persistedExec.addStep(step);
        return domainExecRepo.update(executionWithNewStep)
                .doOnNext(e -> LOGGER.info("ATTEMPT {}, Added step {} into {}", i, step, e))
                .doOnError(t -> LOGGER.error("ATTEMPT {}, Failed to save exec with new step: persisted={}, version={}, exec={}",
                     i, executionWithNewStep.isPersisted(), executionWithNewStep.getVersion(), executionWithNewStep, t));
    }

    @Timed("find batch")
    private Mono<PBatch> findBatch(int i, PBatch pBatch) {
        return domainBatchRepo.findById(pBatch.getId())
                .doOnNext(b -> LOGGER.info("ATTEMPT {}, Found batch {}", i, b));
    }

    @Timed("find exec")
    private Mono<PExecution> findExec(int i, PExecution pExec) {
        return domainExecRepo.findById(pExec.getId())
                .doOnNext(e -> LOGGER.info("ATTEMPT {}, Found exec {}", i, e));
    }

    private static PExecution asNew(PExecution exec) {
        OffsetDateTime lastUpdated = nowUtc();
        return exec
                .withSteps(empty())
                .withId(UUID.randomUUID())
                .withPersisted(false)
                .withVersion(0)
                .withCreated(lastUpdated)
                .withLastUpdated(lastUpdated);
    }

}