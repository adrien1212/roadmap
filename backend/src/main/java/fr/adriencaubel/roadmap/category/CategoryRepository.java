package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.organisation.Organisation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Every query is scoped by Organisation; there is deliberately no bare findById. */
public interface CategoryRepository extends Repository<Category, Long> {

	List<Category> findAllByOrganisationOrderByIdAsc(Organisation organisation);

	Optional<Category> findByIdAndOrganisation(Long id, Organisation organisation);

	/** Case-insensitive through the column collation. */
	boolean existsByOrganisationAndName(Organisation organisation, String name);

	boolean existsByOrganisationAndNameAndIdNot(Organisation organisation, String name, Long id);

	/** Item counts per Category across all the Organisation's Roadmaps; unused Categories are absent. */
	@Query("""
			select new fr.adriencaubel.roadmap.category.CategoryItemCount(i.category.id, count(i))
			from Item i where i.category.organisation = :organisation group by i.category.id""")
	List<CategoryItemCount> countItems(Organisation organisation);

	Category save(Category category);

	void delete(Category category);
}
