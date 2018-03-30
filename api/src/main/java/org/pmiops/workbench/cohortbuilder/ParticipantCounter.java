package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides counts of unique subjects
 * defined by the provided {@link SearchRequest}.
 */
@Service
public class ParticipantCounter {

    private DomainLookupService domainLookupService;

    private static final String COUNT_SQL_TEMPLATE =
            "select count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "where\n";

    private static final String ID_SQL_TEMPLATE =
            "select person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "where\n";

    private static final String CHART_INFO_SQL_TEMPLATE =
            "select concept1.concept_code as gender, \n" +
                    "case when concept2.concept_name is null then 'Unknown' else concept2.concept_name end as race, \n" +
                    "case " + getAgeRangeSql(0, 18) + "\n" +
                    getAgeRangeSql(19, 44) + "\n" +
                    getAgeRangeSql(45, 64) + "\n" +
                    "else '> 65'\n" +
                    "end as ageRange,\n" +
                    "count(*) as count\n" +
                    "from `${projectId}.${dataSetId}.person` person\n" +
                    "left join `${projectId}.${dataSetId}.concept` concept1 on (person.gender_concept_id = concept1.concept_id and concept1.vocabulary_id = 'Gender')\n" +
                    "left join `${projectId}.${dataSetId}.concept` concept2 on (person.race_concept_id = concept2.concept_id and concept2.vocabulary_id = 'Race')\n" +
                    "where\n";

    private static final String CHART_INFO_SQL_GROUP_BY = "group by gender, race, ageRange\n" + "order by gender, race, ageRange\n";

    private static final String ID_SQL_ORDER_BY = "order by person_id\n" + "limit";

    private static final String OFFSET_SUFFIX = " offset ";

    private static final String UNION_TEMPLATE = "union all\n";

    private static final String INCLUDE_SQL_TEMPLATE = "${mainTable}.person_id in (${includeSql})\n";

    private static final String PERSON_ID_WHITELIST_PARAM = "person_id_whitelist";
    private static final String PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist";

    private static final String PERSON_ID_WHITELIST_TEMPLATE = "${mainTable}.person_id in unnest(@" +
        PERSON_ID_WHITELIST_PARAM + ")\n";
    private static final String PERSON_ID_BLACKLIST_TEMPLATE = "${mainTable}.person_id not in unnest(@" +
        PERSON_ID_BLACKLIST_PARAM + ")\n";

    private static final String EXCLUDE_SQL_TEMPLATE =
            "not exists\n" +
                    "(select 'x' from\n" +
                    "(${excludeSql})\n" +
                    "x where x.person_id = ${mainTable}.person_id)\n";

    private static final String PERSON_TABLE = "person";

    @Autowired
    public ParticipantCounter(DomainLookupService domainLookupService) {
        this.domainLookupService = domainLookupService;
    }

    /**
     * Provides counts with demographic info for charts
     * defined by the provided {@link SearchRequest}.
     */
    public QueryJobConfiguration buildParticipantCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, COUNT_SQL_TEMPLATE, "");
    }

    /**
     * Provides counts of unique subjects
     * defined by the provided {@link SearchRequest}.
     */
    public QueryJobConfiguration buildChartInfoCounterQuery(ParticipantCriteria participantCriteria) {
        return buildQuery(participantCriteria, CHART_INFO_SQL_TEMPLATE, CHART_INFO_SQL_GROUP_BY);
    }

    public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria,
        long resultSize, long offset) {
        String endSql = ID_SQL_ORDER_BY + " " + resultSize;
        if (offset > 0) {
            endSql += OFFSET_SUFFIX + offset;
        }
        return buildQuery(participantCriteria, ID_SQL_TEMPLATE, endSql);
    }

    public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
        String sqlTemplate, String endSql) {
        return buildQuery(participantCriteria, sqlTemplate, endSql, PERSON_TABLE);
    }

    public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
        String sqlTemplate, String endSql, String mainTable) {
        return buildQuery(participantCriteria, sqlTemplate, endSql, mainTable, new HashMap<>());
    }

    public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
        String sqlTemplate, String endSql, String mainTable,
        Map<String, QueryParameterValue> params) {
        SearchRequest request = participantCriteria.getSearchRequest();
        StringBuilder queryBuilder = new StringBuilder(sqlTemplate.replace("${mainTable}", mainTable));

        if (request == null) {
            queryBuilder.append(PERSON_ID_WHITELIST_TEMPLATE.replace("${mainTable}", mainTable));
            params.put(PERSON_ID_WHITELIST_PARAM, QueryParameterValue.array(
                participantCriteria.getParticipantIdsToInclude().toArray(new Long[0]), Long.class));
        } else {
          domainLookupService.findCodesForEmptyDomains(request.getIncludes());
          domainLookupService.findCodesForEmptyDomains(request.getExcludes());

          if (request.getIncludes().isEmpty() && request.getExcludes().isEmpty()) {
            throw new BadRequestException(
                "Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
          }

          // build query for included search groups
          StringJoiner joiner = buildQuery(request.getIncludes(), mainTable, params, false);

          // if includes is empty then don't add the excludes clause
          if (joiner.toString().isEmpty()) {
            joiner.merge(buildQuery(request.getExcludes(), mainTable, params, false));
          } else {
            joiner.merge(buildQuery(request.getExcludes(), mainTable, params, true));
          }
          Set<Long> participantIdsToExclude = participantCriteria.getParticipantIdsToExclude();
          if (!participantIdsToExclude.isEmpty()) {
              joiner.add(PERSON_ID_BLACKLIST_TEMPLATE.replace("${mainTable}", mainTable));
              params.put(PERSON_ID_BLACKLIST_PARAM, QueryParameterValue.array(
                  participantIdsToExclude.toArray(new Long[0]), Long.class));
          }
          queryBuilder.append(joiner.toString());
        }
        queryBuilder.append(endSql.replace("${mainTable}", mainTable));

        return QueryJobConfiguration
                .newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build();
    }

    private StringJoiner buildQuery(List<SearchGroup> groups, String mainTable,
        Map<String, QueryParameterValue> params, Boolean excludeSQL) {
        StringJoiner joiner = new StringJoiner("and ");
        List<String> queryParts = new ArrayList<>();
        for (SearchGroup includeGroup : groups) {
            for (SearchGroupItem includeItem : includeGroup.getItems()) {
                QueryJobConfiguration queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
                        .buildQueryJobConfig(new QueryParameters()
                                .type(includeItem.getType())
                                .parameters(includeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            if (excludeSQL) {
                joiner.add(EXCLUDE_SQL_TEMPLATE.replace("${mainTable}", mainTable)
                    .replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts)));
            } else {
                joiner.add(INCLUDE_SQL_TEMPLATE.replace("${mainTable}", mainTable)
                    .replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts)));
            }
            queryParts = new ArrayList<>();
        }
        return joiner;
    }

    /**
     * Helper method to build sql snippet.
     * @param lo - lower bound of the age range
     * @param hi - upper bound of the age range
     * @return
     */
    private static String getAgeRangeSql(int lo, int hi) {
        return "when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) >= " + lo +
                " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE(person.year_of_birth, person.month_of_birth, person.day_of_birth), MONTH)/12) as INT64) <= " + hi + " then '" + lo + "-" + hi + "'";
    }
}
