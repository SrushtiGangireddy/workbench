package org.pmiops.workbench.db.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.AnnotationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortAnnotationDefinitionDaoTest {

    private static long COHORT_ID = 1;

    @Autowired
    CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void save() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        String sql = "select count(*) from cohort_annotation_definition where cohort_annotation_definition_id = ?";
        final Object[] sqlParams = { cohortAnnotationDefinition.getCohortAnnotationDefinitionId() };
        final Integer expectedCount = new Integer("1");

        assertEquals(expectedCount, jdbcTemplate.queryForObject(sql, sqlParams, Integer.class));
    }

    @Test
    public void findByCohortIdAndColumnName() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        CohortAnnotationDefinition expectedAnnotationDefinition =
                cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(
                        cohortAnnotationDefinition.getCohortId(),
                        cohortAnnotationDefinition.getColumnName());

        assertEquals(expectedAnnotationDefinition, cohortAnnotationDefinition);
    }

    @Test
    public void findByCohortId() throws Exception {
        CohortAnnotationDefinition cohortAnnotationDefinition = createCohortAnnotationDefinition();

        cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition);

        List<CohortAnnotationDefinition> expectedDBList =
                cohortAnnotationDefinitionDao.findByCohortId(
                        cohortAnnotationDefinition.getCohortId());

        assertEquals(expectedDBList.get(0), cohortAnnotationDefinition);
    }

    private CohortAnnotationDefinition createCohortAnnotationDefinition() {
        return new CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("annotation name")
                .annotationType(AnnotationType.BOOLEAN);
    }

}