package com.redis.example;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.RediSearchCommands;
import redis.clients.jedis.search.RediSearchUtil;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.Schema.TextField;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.querybuilder.Node;
import redis.clients.jedis.search.querybuilder.QueryBuilders;
import redis.clients.jedis.search.querybuilder.Values;

// The RedisService demonstrates a Spring Service class calling low-level Jedis library
@Service
public class RedisService {
	private String INDEX_CUSTOMER = "idx_cust";
	
	@Value("${jedis.redis.host}")
	private String host;

	@Value("${jedis.redis.port}")
	private int port;

	@Value("${jedis.redis.username}")
	private String username;

	@Value("${jedis.redis.password}")
	private String password;
	
	private JedisPooled jedis = null;

	@PostConstruct
	public void postConstructRoutine() {
		jedis = new JedisPooled(host, port, username, password);
		
		initializeIndex();
	}
	
	public long initializeIndex() {
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		// create index if it doesn't exist
		if(!rediSearch.ftList().contains(INDEX_CUSTOMER)) {
			// Disable stopwords so we can search for people with the name "An"
			// FT.CREATE idx_cust PREFIX 1 "cust:" STOPWORDS 0 SCHEMA "First Name" AS firstname TEXT NOSTEM "Last Name" AS lastname TEXT NOSTEM "CIF" AS cif TAG
			// "Credit card 1" as "cc1" TAG "Credit card 2" as "cc2" TAG
			FTCreateParams params = FTCreateParams.createParams();
			params.addPrefix("cust:");
			
			TextField firstName = new TextField("First name", 2.0, false, true); // no stemming
			TextField lastName = new TextField("Last name", 2.0, false, true); // no stemming
			
			Schema sc = new Schema()
	        .addField(firstName).as("firstname")
	        .addField(lastName).as("lastname")
	        .addTagField("CIF").as("cif")
	        .addTagField("Credit card 1").as("cc1")
	        .addTagField("Credit card 2").as("cc2");

			IndexDefinition def = new IndexDefinition()
			        .setPrefixes(new String[]{"cust:"});
			
			IndexOptions opt = IndexOptions.defaultOptions();
			opt.setNoStopwords();
			        
			rediSearch.ftCreate(INDEX_CUSTOMER, opt.setDefinition(def), sc);
		}
		
		return 0;
	}
	
	public long addCustomer(String key, Map<String, String> hash) {
		long result = jedis.hset(key, hash);
		
		return result;
	}
	
	public SearchResult searchByCif(String cif) {
		// FT.SEARCH idx_cust "@cif:{QYUFWYCW6RJ}"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		Query q = new Query("@cif:{" + RediSearchUtil.escape(cif) + "}").limit(0, 10);
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);

		return result;
	}
	
	public SearchResult searchByCifWithQueryBuilders(String cif) {
		// FT.SEARCH idx_cust "@cif:{QYUFWYCW6RJ}"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		Node n = QueryBuilders.intersect().add("cif", Values.tags(RediSearchUtil.escape(cif)));
		Query q = new Query(n.toString());
		
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);

		return result;
	}
	
	public SearchResult searchFullText(String text) {
		// FT.SEARCH idx_cust "Edi*"
		
		// Notes on VERBATIM option
		//		> FT.EXPLAINCLI "idx_cust" "@lastname:smiths"
		//		1) @lastname:UNION {
		//		2)  @lastname:smiths
		//		3)  @lastname:+smith(expanded)
		//		4)  @lastname:smith(expanded)
		//		5) }
		//		6) 
		//		> FT.EXPLAINCLI "idx_cust" "@lastname:smiths" VERBATIM
		//		1) @lastname:smiths
		//		2)
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		Query q = new Query(text).setVerbatim().limit(0, 10); // not escaping to support special characters, verbatim to prevent stemming
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);
		
		return result;
	}

	public SearchResult searchLastNameOnly(String text) {
		// FT.SEARCH idx_cust "@lastname:Pau*"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		Query q = new Query("@lastname:" + text).setVerbatim().limit(0, 10); // not escaping to support special characters, verbatim to prevent stemming
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);
		
		return result;
	}

	public SearchResult searchLastNameOnlyWithQueryBuilders(String text) {
		// FT.SEARCH idx_cust "@lastname:Pau*"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;

		Node n = QueryBuilders.intersect().add("lastname", text);
		Query q = new Query(n.toString()).setVerbatim().limit(0, 10); // not escaping to support special characters, verbatim to prevent stemming
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);
		
		return result;
	}

	public SearchResult searchCardNumber(String number) {
		// FT.SEARCH idx_cust "(@cc1:{6759\\-6040\\-5042\\-5701\\-836})|(@cc2:{6759\\-6040\\-5042\\-5701\\-836})"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		String formattedNumber = RediSearchUtil.escape(number);
		Query q = new Query("(@cc1:{" + formattedNumber + "})|(@cc2:{" + formattedNumber + "})").limit(0, 10);
		SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);
		
		return result;
	}

	public SearchResult searchCardNumberWithQueryBuilders(String number) {
		// FT.SEARCH idx_cust "(@cc1:{6759\\-6040\\-5042\\-5701\\-836})|(@cc2:{6759\\-6040\\-5042\\-5701\\-836})"
		RediSearchCommands rediSearch = (RediSearchCommands) jedis;
		
		String formattedNumber = RediSearchUtil.escape(number);
		Node n = QueryBuilders.union().add("cc1", Values.tags(formattedNumber)).add("cc2", Values.tags(formattedNumber));
		Query q = new Query(n.toString(Node.Parenthesize.ALWAYS)).limit(0, 10);
        
        	SearchResult result = rediSearch.ftSearch(INDEX_CUSTOMER, q);
		
		return result;
	}
}
