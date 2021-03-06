package at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.impl;

import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.CustomerEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.custom.CustomSearchRepository;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * Custom function implementations for CustomerRepository.
 *
 * @author Michael
 */
public class CustomerRepositoryImpl implements CustomSearchRepository<CustomerEntity> {
  /**
   * Entity manager.
   */
  private final EntityManager entityManager;

  /**
   * Constructor with dependency injection.
   * @param entityManager current entity manager.
   */
  @Autowired
  public CustomerRepositoryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Provides full text search for CustomerEntity over its name and billingAddress fields.
   *
   * <p>Does a full text search over all CustomerEntities for search Strings longer
   * than 2 characters.</p>
   * @param text The text to do the full text search with.
   * @return The list of matching CustomerEntity objects.
   * @author Michael
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<CustomerEntity> search(String text) {
    if (StringUtils.isEmpty(text) || text.length() < 3) {
      return Collections.emptyList();
    }

    FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

    QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
        .buildQueryBuilder()
        .forEntity(CustomerEntity.class)
        .get();

    Query query = queryBuilder
        .keyword()
        .fuzzy()
        .withEditDistanceUpTo(1)
        .onFields(
            "billingAddress.city",
            "billingAddress.state",
            "billingAddress.zipCode")
        .andField("name").boostedTo(5F)
        .andField("billingAddress.name").boostedTo(4F)
        .andField("billingAddress.streetAddress1").boostedTo(3F)
        .andField("billingAddress.streetAddress2").boostedTo(3F)
        .matching(text)
        .createQuery();
    return fullTextEntityManager.createFullTextQuery(query, CustomerEntity.class).getResultList();
  }
}
