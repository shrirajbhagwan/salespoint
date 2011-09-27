package org.salespointframework.core.accountancy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.salespointframework.core.database.Database;
import org.salespointframework.core.money.Money;
import org.salespointframework.util.Iterables;
import org.salespointframework.util.Objects;

/**
 * This class represents an accountancy. An accountancy consists of
 * <code>AccountancyEntries</code>.
 * 
 * @author Hannes Weisbach
 * @author Thomas Dedek
 * 
 */
public final class PersistentAccountancy implements
		Accountancy<PersistentAccountancyEntry> {
	/**
	 * <code>EntityManager</code> which is used for this Accountancy. The
	 * <code>Database.INSTANCE</code> has to be initialized first.
	 */
	private EntityManagerFactory emf = Database.INSTANCE
			.getEntityManagerFactory();

	/**
	 * Create a new <code>Accountancy</code>.
	 */
	public PersistentAccountancy() {

	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The new entry is persisted transparently into the underlying database.
	 * Once an <code>AccountancyEntry</code> has been written to the database,
	 * it cannot be modified later on or added a second time. Doing so will
	 * result in a runtime exception to be thrown.
	 */

	@Override
	public final void add(PersistentAccountancyEntry accountancyEntry) {
		Objects.requireNonNull(accountancyEntry, "accountancyEntry");
		EntityManager em = emf.createEntityManager();
		em.persist(accountancyEntry);
		beginCommit(em);
	}

	@Override
	public final PersistentAccountancyEntry get(
			Class<PersistentAccountancyEntry> clazz,
			AccountancyEntryIdentifier accountancyEntryIdentifier) {
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(accountancyEntryIdentifier,
				"accountancyEntryIdentifier");
		EntityManager em = emf.createEntityManager();
		return em.find(clazz, accountancyEntryIdentifier);
	}

	@Override
	public final <T extends PersistentAccountancyEntry> Iterable<T> find(
			Class<T> clazz) {
		Objects.requireNonNull(clazz, "clazz");

		EntityManager em = emf.createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(clazz);

		TypedQuery<T> tq = em.createQuery(q);

		return Iterables.from(tq.getResultList());
	}

	@Override
	public final <T extends PersistentAccountancyEntry> Iterable<T> find(
			Class<T> clazz, DateTime from, DateTime to) {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(clazz, "clazz");

		EntityManager em = emf.createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(clazz);
		Root<T> entry = q.from(clazz);

		Predicate p1 = cb.between(entry.get(PersistentAccountancyEntry_.date),
				from.toDate(), to.toDate());

		q.where(p1);
		TypedQuery<T> tq = em.createQuery(q);

		return Iterables.from(tq.getResultList());
	}

	@Override
	public final <T extends PersistentAccountancyEntry> Map<Interval, Iterable<T>> find(
			Class<T> clazz, DateTime from, DateTime to, Period period) {
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(period, "period");

		DateTime nextStep;
		HashMap<Interval, Iterable<T>> entries = new HashMap<Interval, Iterable<T>>();

		for (; from.isBefore(to.minus(period)); from = from.plus(period)) {
			nextStep = from.plus(period);
			entries.put(new Interval(from, nextStep),
					find(clazz, from, nextStep));
		}
		/*
		 * Remove last interval from loop, to save the test for the last
		 * interval in every iteration. But it's java, it not like you're gonna
		 * notice the speedup, hahaha. BTW: the cake is a lie, hahaha.
		 */
		entries.put(new Interval(from, to), find(clazz, from, to));
		return entries;
	}

	@Override
	public final <T extends PersistentAccountancyEntry> Map<Interval, Money> salesVolume(
			Class<T> clazz, DateTime from, DateTime to, Period period) {
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(period, "period");

		Money total;
		Map<Interval, Money> sales = new HashMap<Interval, Money>();
		Map<Interval, Iterable<T>> entries = find(clazz, from, to, period);

		for (Entry<Interval, Iterable<T>> e : entries.entrySet()) {
			total = Money.ZERO;
			for (T t : e.getValue()) {
				total = total.add_(t.getValue());
			}
			sales.put(e.getKey(), total);
		}

		return sales;
	}

	/**
	 * Adds multiple <code>AccountancyEntry</code>s to this
	 * <code>Accountancy</code> and persists them to underlying database. Once
	 * an <code>AccountancyEntry</code> has been added to the persistence layer,
	 * it cannot be modified or added again. Doing so would result in a runtime
	 * exception.
	 * 
	 * @param accountancyEntries
	 *            an {@link Iterable} of all <code>AccountancyEntry</code>s
	 *            which should be added to the <code>Accountancy</code>
	 */
	public final void addAll(
			Iterable<? extends PersistentAccountancyEntry> accountancyEntries) {
		Objects.requireNonNull(accountancyEntries, "accountancyEntries");
		EntityManager em = emf.createEntityManager();
		for (AccountancyEntry e : accountancyEntries) {
			em.persist(e);
		}
		beginCommit(em);

	}

	// TODO convencience
	// braucht man die wirklich ohne .clas, alle anderen im framework (Catalog,
	// Inventory, Order, Calendar?) sind mit class
	/**
	 * Returns all <code>AccountancyEntry</code>s in between the dates
	 * <code>from</code> and <code>to</code>, including <code>from</code> and
	 * <code>to</code>. So every entry with an time stamp <= <code>to</code> and
	 * >= <code>from</code> is returned. If no entries within the specified time
	 * span exist, an empty <code>Iterable</code> is returned.
	 * 
	 * @param from
	 *            time stamp denoting the start of the requested time period
	 * @param to
	 *            time stamp denoting the end of the requested time period
	 * @return an unmodifiable <code>Iterable</code> containing all entries
	 *         between <code>from</code> and <code>to</code>
	 */
	public final Iterable<PersistentAccountancyEntry> find(DateTime from,
			DateTime to) {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");

		EntityManager em = emf.createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<PersistentAccountancyEntry> q = cb
				.createQuery(PersistentAccountancyEntry.class);
		Root<PersistentAccountancyEntry> r = q
				.from(PersistentAccountancyEntry.class);
		Predicate p = cb.between(r.get(PersistentAccountancyEntry_.date),
				from.toDate(), to.toDate());
		q.where(p);
		TypedQuery<PersistentAccountancyEntry> tq = em.createQuery(q);

		return Iterables.from(tq.getResultList());
	}

	private final void beginCommit(EntityManager entityManager) {
		entityManager.getTransaction().begin();
		entityManager.getTransaction().commit();
	}
}
