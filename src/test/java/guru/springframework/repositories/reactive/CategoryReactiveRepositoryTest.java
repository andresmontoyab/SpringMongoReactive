package guru.springframework.repositories.reactive;

import guru.springframework.domain.Category;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataMongoTest
public class CategoryReactiveRepositoryTest {

    @Autowired
    CategoryReactiveRepository categoryReactiveRepository;

    @Before
    public void setUp() throws Exception {
        categoryReactiveRepository.deleteAll().block();
    }

    @Test
    public void saveCategory(){
        Category category = new Category();
        category.setDescription("daa");

        categoryReactiveRepository.save(category).block();

        Long count = categoryReactiveRepository.count().block();

        assertEquals(Long.valueOf(1l), count);
    }

    @Test
    public void findCategoryByDescription() {
        Category category = new Category();
        category.setDescription("daa");

        categoryReactiveRepository.save(category).block();

       Category categoryFound = categoryReactiveRepository.findByDescription("daa").block();

        assertEquals(category.getDescription(), categoryFound.getDescription());
    }
}
