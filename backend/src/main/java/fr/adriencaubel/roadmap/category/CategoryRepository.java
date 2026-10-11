package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.owner.Owner;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Every query is scoped by Owner; there is deliberately no bare findById. */
public interface CategoryRepository extends Repository<Category, Long> {

	List<Category> findAllByOwnerOrderByIdAsc(Owner owner);

	Optional<Category> findByIdAndOwner(Long id, Owner owner);

	/** Case-insensitive through the column collation. */
	boolean existsByOwnerAndName(Owner owner, String name);

	boolean existsByOwnerAndNameAndIdNot(Owner owner, String name, Long id);

	/** Item counts per Category across all the Owner's Roadmaps; unused Categories are absent. */
	@Query("""
			select new fr.adriencaubel.roadmap.category.CategoryItemCount(i.category.id, count(i))
			from Item i where i.category.owner = :owner group by i.category.id""")
	List<CategoryItemCount> countItems(Owner owner);

	Category save(Category category);

	void delete(Category category);
}
