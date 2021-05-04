package com.example.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
       return builder.build();
    }
}

@RestController
@EnableScheduling
public class SlotController {
	private static URI ABAD_URL;
	private static URI KOP_URL;
	private static URI WHATSAPP_URL;

	@Autowired
	private RestTemplate restTemplate;
	
	@GetMapping("/")
	public String testMethod() {
		return "Tejas test";
	}
	
	@GetMapping("/abad")
	@Scheduled(fixedRate = 60000)
	public String setURIForAurangabad() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		String date = sdf.format(new Date());
		try {
			ABAD_URL = new URI("https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=397&date="+date);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		this.getSlotInfoForURL(ABAD_URL);
		return "Tejas";
	}
	
	public void getSlotInfoForURL(URI uri) {
		ResponseEntity<Map<String,List<CenterPojo>>> responseEntity = restTemplate.exchange(
				uri, 
			    HttpMethod.GET, 
			    null, 
			    new ParameterizedTypeReference<Map<String,List<CenterPojo>>>() {
			    });
		Map<String,List<CenterPojo>> centersRes = responseEntity.getBody();
		ArrayList<CenterPojo> filteredList = this.filter1845(centersRes.get("centers"), 18);
		String message = getMessageFromResponse(filteredList);
		if(filteredList.size() > 0)
			this.sendMessage(message);
	}
	
	private ArrayList<CenterPojo> filter1845(List<CenterPojo> centers, int minAge) {
		ArrayList<CenterPojo> pojo = new ArrayList<CenterPojo>();
		centers.stream().forEach(center -> {
			List<Session> sessions = center.sessions.stream().filter(session -> {
				return session.min_age_limit == minAge && session.available_capacity > 0;
			}).collect(Collectors.toList());
			if(sessions.size() != 0)
			{
				center.sessions = sessions;
				pojo.add(center);
			}
		});
		return pojo;
	}

	private String getMessageFromResponse(ArrayList<CenterPojo> pojo) {
		String message = "";
		for(CenterPojo p : pojo) {
			String sessionMessage = "";
			for(Session session : p.sessions)
				sessionMessage += "Date : " + session.date + "\n" + "Vaccine : " + session.vaccine + "\n";
			message = "Address : " + p.address + "\nSession : " + sessionMessage; 
		}
		return message;
	}
	
	private void sendMessage(String message) {
		message = message.replaceAll(" ", "%20");
		message = message.replaceAll("\n", "%0D%0A");
		try {
			WHATSAPP_URL = new URI("https://api.callmebot.com/whatsapp.php?phone=+918983837675&text="+ message +"&apikey=550940");
			ResponseEntity<String> responseEntity = restTemplate.exchange(
				    WHATSAPP_URL, 
				    HttpMethod.GET, 
				    null, 
				    new ParameterizedTypeReference<String>() {
				    });
			String pojoObjList = responseEntity.getBody();
			System.out.println(pojoObjList);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
//	@GetMapping("/kop")
//	@Scheduled(fixedRate = 60010)
//	public void getSlotInfoForKolhapur() {
//		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
//		String date = sdf.format(new Date());
//		try {
//			KOP_URL = new URI("https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=371&date="+date);
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
//		this.getSlotInfoForURL(KOP_URL);
//	}
	
}
