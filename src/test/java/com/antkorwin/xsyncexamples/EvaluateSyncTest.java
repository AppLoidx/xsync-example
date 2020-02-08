package com.antkorwin.xsyncexamples;

import com.antkorwin.xsync.XSync;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.IntStream;

/**
 * @author Arthur Kupriyanov on 08.02.2020
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class EvaluateSyncTest {
    private static final int ITERATION_CNT = 100000;
    private static final int INCREMENT_ITERATION = 10;

    private final NonAtomicInt firstNonAtomicCounter = new NonAtomicInt(0);
    private final NonAtomicInt secondNonAtomicCounter = new NonAtomicInt(0);

    private final String KEY_1 = "a";
    private final String KEY_2 = "b";

    @Autowired
    @Qualifier("stringXSync")
    private XSync<String> xSync;

    @Test
    public void testWithXSync(){
        final Integer[] val1 = new Integer[1];
        final Integer[] val2 = new Integer[1];

        IntStream.range(0, ITERATION_CNT)
                .boxed()
                .parallel()
                .forEach(i -> {
                    val1[0] = syncByStringMethod(KEY_1);
                    val2[0] = syncByStringMethod(KEY_2);
                });

        Assertions.assertThat(val1[0]).isEqualTo(ITERATION_CNT * INCREMENT_ITERATION);
        Assertions.assertThat(val2[0]).isEqualTo(ITERATION_CNT * INCREMENT_ITERATION);
    }

    @Test
    public void testWithoutXSync(){
        final Integer[] val1 = new Integer[1];
        final Integer[] val2 = new Integer[1];

        IntStream.range(0, ITERATION_CNT)
                .boxed()
                .parallel()
                .forEach(i -> {
                    val1[0] = nonSyncMethod(KEY_1);
                    val2[0] = nonSyncMethod(KEY_2);
                });

        Assertions.assertThat(val1[0]).isNotEqualTo(ITERATION_CNT * INCREMENT_ITERATION );
        Assertions.assertThat(val2[0]).isNotEqualTo(ITERATION_CNT * INCREMENT_ITERATION );
    }

    private Integer syncByStringMethod(String mutexKey){
        return xSync.evaluate(mutexKey,  () -> nonSyncMethod(mutexKey));
    }

    private Integer nonSyncMethod(String mutexKey){
        if (mutexKey.equals(KEY_1)){

            incrementIterations().forEach(i -> firstNonAtomicCounter.increment());
            return firstNonAtomicCounter.getValue();

        } else {

            incrementIterations().forEach(i -> secondNonAtomicCounter.increment());
            return secondNonAtomicCounter.getValue();

        }
    }

    private IntStream incrementIterations(){
        return IntStream.range(0, INCREMENT_ITERATION);
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean("stringXSync")
        public XSync<String> stringXSync() {
            return new XSync<>();
        }
    }
}
