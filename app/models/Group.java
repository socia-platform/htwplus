package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import models.base.BaseNotifiable;
import models.base.INotifiable;
import models.enums.GroupType;
import models.enums.LinkType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.StopFilterFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import play.Logger;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;

@Indexed
@Entity
@Table(name = "Group_")
@AnalyzerDef(name = "searchtokenanalyzer", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
		@TokenFilterDef(factory = StandardFilterFactory.class),
		@TokenFilterDef(factory = LowerCaseFilterFactory.class),
		@TokenFilterDef(factory = StopFilterFactory.class, params = { @Parameter(name = "ignoreCase", value = "true") }) })
@org.hibernate.search.annotations.Analyzer(definition = "searchtokenanalyzer")
public class Group extends BaseNotifiable implements INotifiable {
    public static final String GROUP_INVITATION = "group_invitation";
    public static final String GROUP_NEW_REQUEST = "group_new_request";
    public static final String GROUP_REQUEST_SUCCESS = "group_request_success";
    public static final String GROUP_REQUEST_DECLINE = "group_request_decline";

    @Required
	@Column(unique = true)
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String title;

	public String description;

	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
	public Set<GroupAccount> groupAccounts;

	@ManyToOne
	public Account owner;

	@Enumerated(EnumType.STRING)
	public GroupType groupType;

	public String token;

	@OneToMany(mappedBy = "group")
	@OrderBy("createdAt DESC")
	public List<Media> media;
	
	public void setTitle(String title) {
		this.title = title.trim();
	}

    /**
     * Possible invitation list
     */
    @Transient
    public Collection<String> inviteList = null;

	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<>();
		if (Group.findByTitle(this.title) != null) {
			errors.add(new ValidationError("title", "error.title"));
			return errors;
		}
		return null;
	}
	
	public static boolean validateToken(String token) {
        return !(token.equals("") || token.length() < 4 || token.length() > 45);
	}

	public void createWithGroupAccount(Account account) {
		this.owner = account;
		this.create();
		GroupAccount groupAccount = new GroupAccount(account, this,
				LinkType.establish);
		groupAccount.create();

	}

	@Override
	public void create() {
		JPA.em().persist(this);
	}

	@Override
	public void update() {
		// this.id = id;
		// createdAt seems to be overwritten (null) - quickfix? (Iven)
		// this.createdAt = findById(id).createdAt;
		JPA.em().merge(this);
	}

	@Override
	public void delete() {
		// delete all Posts
		List<Post> posts = Post.getPostsForGroup(this, 0, 0);
		for (Post post : posts) {
			post.delete();
		}

		// delete media
		for (Media media : this.media) {
			media.delete();
		}
		
		// Delete Notifications
        Notification.deleteReferences(this);
		JPA.em().remove(this);
	}

	public static Group findById(Long id) {
		return JPA.em().find(Group.class, id);
	}

	public static Group findByTitle(String title) {
		@SuppressWarnings("unchecked")
		List<Group> groups = (List<Group>) JPA.em()
				.createQuery("FROM Group g WHERE g.title = ?1")
				.setParameter(1, title).getResultList();

		if (groups.isEmpty()) {
			return null;
		} else {
			return groups.get(0);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Group> all() {
        return JPA.em().createQuery("FROM Group").getResultList();
	}

    /**
     * Returns true, if an account is member of a group.
     *
     * @param group Group instance
     * @param account Account instance
     * @return True, if account is member of group
     */
	public static boolean isMember(Group group, Account account) {
		@SuppressWarnings("unchecked")
		List<GroupAccount> groupAccounts = (List<GroupAccount>) JPA
				.em()
				.createQuery(
						"SELECT g FROM GroupAccount g WHERE g.account.id = ?1 and g.group.id = ?2 AND linkType = ?3")
				.setParameter(1, account.id).setParameter(2, group.id)
				.setParameter(3, LinkType.establish).getResultList();

        return !groupAccounts.isEmpty();
	}

	/**
	 * Search for a group with a given keyword.
	 * 
	 * @param keyword Keyword to search for
	 * @return List of groups wich matches with the keyword
	 */

	public static FullTextQuery searchForGroupByKeyword(String keyword, int limit, int offset) {
		Logger.info("Group model searchForGroupByKeyword: "
				+ keyword.toLowerCase());
		
		BooleanQuery bQuery = new BooleanQuery();
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(JPA.em());

		Analyzer analyzer = fullTextEntityManager.getSearchFactory().getAnalyzer("searchtokenanalyzer");
		QueryParser parser = new QueryParser(Version.LUCENE_35, "title", analyzer);
		String[] tokenized=null;
		if (!keyword.isEmpty()) {
			try {
				Query query = parser.parse(keyword);
				String cleanedText = query.toString("title");
				Logger.info("[CLEANING] " + cleanedText);
				tokenized = cleanedText.split("\\s");

			} catch (ParseException e) {
				Logger.error(e.getMessage());
			}
		}

	    
		// Create a querybuilder for the group entity
		QueryBuilder qBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(Group.class).get();
		if(tokenized != null && tokenized.length > 1) {
			for (int i = 0; i < tokenized.length; i++) {
				if (i == (tokenized.length - 1)) {
					org.apache.lucene.search.Query query = qBuilder.keyword()
							.wildcard().onField("title")
							.matching(tokenized[i] + "*").createQuery();
					bQuery.add(query, BooleanClause.Occur.MUST);
				} else {
					Term exactTerm = new Term("title", tokenized[i]);
					bQuery.add(new TermQuery(exactTerm),
							BooleanClause.Occur.MUST);
				}

			}
		} else {
			org.apache.lucene.search.Query luceneQuery = qBuilder.keyword()
					.wildcard().onField("title")
					.matching("*" + keyword.toLowerCase() + "*").createQuery();
			bQuery.add(luceneQuery, BooleanClause.Occur.MUST);
		}
			
		//Create a criteria because we just want to search for groups
		Session session = fullTextEntityManager
				.unwrap(org.hibernate.Session.class);
		Criteria criteria = session.createCriteria(Group.class);
		criteria.add(Restrictions.or(
				Restrictions.eq("groupType", GroupType.open),
				Restrictions.eq("groupType", GroupType.close)));
		criteria.addOrder(Order.asc("title"));
		criteria = limit(criteria,limit,offset);
		
		
		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery(bQuery, Group.class);

		criteria.setReadOnly(true);
		fullTextQuery.setCriteriaQuery(criteria);
		fullTextQuery.setSort(new Sort(new SortField("title", SortField.STRING)));
		
		session.clear();
		return fullTextQuery;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Group> groupSearch(String keyword, int limit, int page) {
		
		// create offset
		int offset = (page * limit) - limit;
				
		FullTextQuery fullTextQuery = searchForGroupByKeyword(keyword, limit, offset);
        return fullTextQuery.getResultList();
	}
	
	public static int countGroupSearch(String keyword) {
		
		FullTextQuery fullTextQuery = searchForGroupByKeyword(keyword, 0, 0);
		
		// SearchException: HSEARCH000105: Cannot safely compute getResultSize() when a Criteria with restriction is used.
        return fullTextQuery.getResultList().size();
	}

	public static FullTextQuery searchForCourseByKeyword(String keyword, int limit, int offset) {
		Logger.info("Group model searchForCourseByKeyword: " +keyword.toLowerCase());

		BooleanQuery bQuery = new BooleanQuery();
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(JPA.em());

		Analyzer analyzer = fullTextEntityManager.getSearchFactory()
				.getAnalyzer("searchtokenanalyzer");
		QueryParser parser = new QueryParser(Version.LUCENE_35, "title",
				analyzer);
		String[] tokenized = null;
		if(!keyword.isEmpty()) {
			try {
				Query query = parser.parse(keyword);
				String cleanedText = query.toString("title");
				Logger.info("[CLEANING] " + cleanedText);
				tokenized = cleanedText.split("\\s");

			} catch (ParseException e) {
				Logger.error(e.getMessage());
			}
		}
		// Create a querybuilder for the group entity
		QueryBuilder qBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(Group.class).get();
		if (tokenized != null && tokenized.length > 1) {
			for (int i = 0; i < tokenized.length; i++) {
				if (i == (tokenized.length - 1)) {
					org.apache.lucene.search.Query query = qBuilder.keyword()
							.wildcard().onField("title")
							.matching(tokenized[i] + "*").createQuery();
					bQuery.add(query, BooleanClause.Occur.MUST);
				} else {
					Term exactTerm = new Term("title", tokenized[i]);
					bQuery.add(new TermQuery(exactTerm),
							BooleanClause.Occur.MUST);
				}

			}
		} else {
			org.apache.lucene.search.Query luceneQuery = qBuilder.keyword()
					.wildcard().onField("title")
					.matching("*" + keyword.toLowerCase() + "*").createQuery();
			bQuery.add(luceneQuery, BooleanClause.Occur.MUST);
		}

		Session session = fullTextEntityManager
				.unwrap(org.hibernate.Session.class);
		
		Criteria courseCriteria = session.createCriteria(Group.class);
		courseCriteria.add(Restrictions.eq("groupType", GroupType.course));
		courseCriteria.addOrder(Order.asc("title"));
		courseCriteria = limit(courseCriteria,limit,offset);
		
		
		// wrap Lucene query in a javax.persistence.Query
		FullTextQuery fullTextQuery = fullTextEntityManager
				.createFullTextQuery(bQuery, Group.class);
		
		fullTextQuery.setCriteriaQuery(courseCriteria);
		session.clear();
		return fullTextQuery;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Group> courseSearch(String keyword, int limit, int page) {
		
		// create offset
		int offset = (page * limit) - limit;
				
		FullTextQuery fullTextQuery = searchForCourseByKeyword(keyword, limit, offset);
        return fullTextQuery.getResultList();
	}
	
	public static int countCourseSearch(String keyword) {
		
		FullTextQuery fullTextQuery = searchForCourseByKeyword(keyword, 0, 0);
		
		// SearchException: HSEARCH000105: Cannot safely compute getResultSize() when a Criteria with restriction is used.
        return fullTextQuery.getResultList().size();
	}
	
	
	protected static Criteria limit(Criteria criteria, int limit, int offset) {
		if (limit > 0) {
			criteria.setMaxResults(limit);
		}
		if (offset >= 0) {
			criteria.setFirstResult(offset);
		}
		return criteria;
	}

    @Override
    public Account getSender() {
        return this.temporarySender;
    }

    @Override
    public List<Account> getRecipients() {
        switch (this.type) {
            case Group.GROUP_NEW_REQUEST:
                // group entry request notification, notify the owner of the group
                return this.getAsAccountList(this.owner);
        }

        // this is an invitation, a request accept or decline notification, notify the temporaryRecipients
        return this.temporaryRecipients;
    }

    @Override
    public String getTargetUrl() {
        if (this.type.equals(Group.GROUP_REQUEST_SUCCESS)) {
            return controllers.routes.GroupController.view(this.id, 1).toString();
        }

        return controllers.routes.GroupController.index().toString();
    }
}