package fr.adriencaubel.roadmap.item;

/** An Item's Status just became Shipped. Listeners decide on its state once the change is committed. */
public record ItemShipped(Long itemId) {
}
