package fr.adriencaubel.roadmap.owner;

/**
 * The auth seam: feature code asks this for the Owner of the current request
 * and scopes every query by it.
 * <p>
 * Until Google sign-in exists, {@link DevCurrentOwner} is the only
 * implementation. The real one (from the security context, anonymous → 401)
 * replaces it; feature code does not change.
 */
public interface CurrentOwner {

	Owner get();
}
