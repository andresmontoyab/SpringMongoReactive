package guru.springframework.repositories.reactive;

import guru.springframework.domain.UnitOfMeasure;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataMongoTest
public class UnitOfMeasureReactiveRepositoryTest {

    @Autowired
    UnitOfMeasureReactiveRepository unitOfMeasureReactiveRepository;

    @Before
    public void setUp() throws Exception {
        unitOfMeasureReactiveRepository.deleteAll().block();
    }

    @Test
    public void saveUOM() throws  Exception {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setDescription("EACH");

        unitOfMeasureReactiveRepository.save(unitOfMeasure).block();

        Long count = unitOfMeasureReactiveRepository.count().block();

        assertEquals(Long.valueOf(1l), count);
    }


    @Test
    public void findByDescriptionUOM() throws  Exception {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setDescription("EACH");

        unitOfMeasureReactiveRepository.save(unitOfMeasure).block();

        UnitOfMeasure unitOfMeasureFound = unitOfMeasureReactiveRepository.findByDescription("EACH").block();
        assertEquals(unitOfMeasure.getDescription(), unitOfMeasureFound.getDescription());
    }
}
