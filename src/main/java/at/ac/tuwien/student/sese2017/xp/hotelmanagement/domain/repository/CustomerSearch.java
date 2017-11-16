package at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository;

import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.CustomerEntity;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * Provides full text search for CustomerEntity over its name and billingAddress fields.
 * @author Michael
 *
 */
@Repository
@Transactional
public class CustomerSearch {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Does a full text search over all CustomerEntities for search Strings longer
   * than 2 characters.
   * @param text The text to do the full text search with.
   * @return The list of matching CustomerEntity objects.
   */
  @SuppressWarnings("unchecked")
  public List<CustomerEntity> search(String text) {
    if (StringUtils.isEmpty(text) || text.length() < 3) {
      return Collections.emptyList();
    }
    FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

    QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
        .buildQueryBuilder()
        .forEntity(CustomerEntity.class)
        .overridesForField("name", "customanalyzer_query")
        .overridesForField("billingAddress", "customanalyzer_query")
        .get();

    Query query = queryBuilder
        .keyword()
        .fuzzy()
        .withEditDistanceUpTo(1)
        .onFields("name", "billingAddress")
        .matching(text)
        .createQuery();

    return fullTextEntityManager.createFullTextQuery(query, CustomerEntity.class).getResultList();
  }
}