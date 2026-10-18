package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.organisation.CurrentOrganisation;
import fr.adriencaubel.roadmap.organisation.Organisation;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

	private final CategoryRepository categories;
	private final CurrentOwner currentOwner;
	private final CurrentOrganisation currentOrganisation;

	/** Each Category with the number of Items using it across all Roadmaps. */
	@Transactional(readOnly = true)
	public List<CategoryUsage> list() {
		Organisation organisation = currentOrganisation.get();
		Map<Long, Long> counts = categories.countItems(organisation).stream()
				.collect(Collectors.toMap(CategoryItemCount::categoryId, CategoryItemCount::itemCount));
		return categories.findAllByOrganisationOrderByIdAsc(organisation).stream()
				.map(category -> new CategoryUsage(category, counts.getOrDefault(category.getId(), 0L)))
				.toList();
	}

	public Category create(String name, String colourKey) {
		Organisation organisation = currentOrganisation.get();
		Category category = new Category(currentOwner.get(), name, CategoryColour.fromKey(colourKey));
		if (categories.existsByOrganisationAndName(organisation, category.getName())) {
			throw duplicate(category.getName());
		}
		return categories.save(category);
	}

	/** Null fields are left unchanged. */
	public Category update(Long id, String name, String colourKey) {
		Organisation organisation = currentOrganisation.get();
		Category category = get(id);
		if (name != null) {
			// Check before renaming: the query would otherwise flush the new name and hit the unique key first.
			String normalized = Category.normalizeName(name);
			if (categories.existsByOrganisationAndNameAndIdNot(organisation, normalized, id)) {
				throw duplicate(normalized);
			}
			category.rename(normalized);
		}
		if (colourKey != null) {
			category.recolour(CategoryColour.fromKey(colourKey));
		}
		return category;
	}

	/** Items that used the Category keep existing; the FK sets their category to null. */
	public void delete(Long id) {
		categories.delete(get(id));
	}

	@Transactional(readOnly = true)
	public Category get(Long id) {
		return categories.findByIdAndOrganisation(id, currentOrganisation.get())
				.orElseThrow(() -> new NotFoundException("Category not found"));
	}

	private static ConflictException duplicate(String name) {
		return new ConflictException("A Category named \"" + name + "\" already exists");
	}
}
