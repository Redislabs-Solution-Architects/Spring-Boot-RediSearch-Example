package com.redis.example;

import java.util.ListIterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.SearchResult;

@Controller
public class SpringSessionController {
	@Autowired
	RedisService redisService;
	
	@Autowired
	SampleDataService sampleDataService;
	
	private String prettyPrintSearchResult(SearchResult result) {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Total result: ");
		sb.append(String.valueOf(result.getTotalResults()));
		sb.append("\n");
		
		ListIterator<Document> it = result.getDocuments().listIterator();
		while(it.hasNext()) {
			Document doc = it.next();

			sb.append("\n");
			sb.append("    Key: ");
			sb.append(doc.getId());
			sb.append(", score: ");
			sb.append(doc.getScore());
			sb.append("\n");
			
			for(Entry<String, Object> e: doc.getProperties()) {
				sb.append("        ");
				sb.append(e.getKey());
				sb.append(": ");
				sb.append(e.getValue());
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}

	@GetMapping("/")
	public String home(Model model, HttpSession session) {
		model.addAttribute("queryResults", session.getAttribute("queryResults"));
		
		model.addAttribute("cif", session.getAttribute("cif"));
		model.addAttribute("fullText", session.getAttribute("fullText"));
		model.addAttribute("name", session.getAttribute("name"));
		model.addAttribute("number", session.getAttribute("number"));
		
		return "index";
	}
	
	@PostMapping("/populateData")
	public String populateData(HttpServletRequest request) {
		try {
			sampleDataService.PopulateSampleData(Long.valueOf(request.getParameter("numentries")));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return "redirect:/";
	}
	
	@RequestMapping("/progress")
	public SseEmitter handle() {
        SseEmitter emitter = new SseEmitter();
        sampleDataService.addSseEmitter(emitter);
        return emitter;
    }

	@PostMapping("/searchByCif")
	public String searchByCif(HttpServletRequest request) {
		request.getSession().setAttribute("cif", request.getParameter("cif"));
		String cif = request.getParameter("cif").trim();
		if(cif.length() == 0) {
			request.getSession().setAttribute("queryResults", "");
		}
		else {
			request.getSession().setAttribute("queryResults", 
					prettyPrintSearchResult(redisService.searchByCifWithQueryBuilders(cif)));
		}
		
		return "redirect:/";
	}

	@PostMapping("/searchFullText")
	public String searchFullText(HttpServletRequest request) {
		request.getSession().setAttribute("fullText", request.getParameter("fullText"));
		String name = request.getParameter("fullText").trim();
		if(name.length() == 0) {
			request.getSession().setAttribute("queryResults", "");
		}
		else {
			request.getSession().setAttribute("queryResults", 
					prettyPrintSearchResult(redisService.searchFullText(name)));
		}
		
		return "redirect:/";
	}

	@PostMapping("/searchLastNameOnly")
	public String searchLastNameOnly(HttpServletRequest request) {
		request.getSession().setAttribute("name", request.getParameter("name"));
		String name = request.getParameter("name").trim();
		if(name.length() == 0) {
			request.getSession().setAttribute("queryResults", "");
		}
		else {
			request.getSession().setAttribute("queryResults", 
					prettyPrintSearchResult(redisService.searchLastNameOnlyWithQueryBuilders(name)));
		}
		
		return "redirect:/";
	}

	@PostMapping("/searchCardNumber")
	public String searchCardNumber(HttpServletRequest request) {
		request.getSession().setAttribute("number", request.getParameter("number"));
		String number = request.getParameter("number").trim();
		if(number.length() == 0) {
			request.getSession().setAttribute("queryResults", "");
		}
		else {
			request.getSession().setAttribute("queryResults", 
					prettyPrintSearchResult(redisService.searchCardNumber(number)));
		}
		
		return "redirect:/";
	}
}