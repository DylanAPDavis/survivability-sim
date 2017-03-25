package netlab;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class RequestGenerationTest {

    @Test
    public void generateRandomIntStream(){
        long seed = 11242424L;
        int min = 0;
        int max = 100;
        Random rng = new Random(seed);
        List<Integer> values = new ArrayList<>();
        Integer[] expectedArray = {8, 99, 11, 51, 56, 28, 3, 0, 87, 78, 55, 2, 59, 4, 77};
        List<Integer> expectedValues = Arrays.asList(expectedArray);
        for(int i = 0; i < 15; i++){
            values.add(rng.nextInt((max - min) + 1) + min);
        }
        System.out.println(values);
        assert(values.equals(expectedValues));
    }

}
