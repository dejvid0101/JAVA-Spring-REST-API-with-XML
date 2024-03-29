package me.iis.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import me.iis.server.classes.Current_Weather;
import me.iis.server.classes.MyXMLValidator;
import me.iis.server.classes.Weather_Class;
import me.iis.server.classes.Weather_Collection;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.*;
import java.util.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.springframework.util.ResourceUtils.getFile;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SpringBootApplication
@RestController
@RequestMapping(produces = "application/xml")
public class ServerApplication {
	MyXMLValidator MyValidator=new MyXMLValidator();
	ObjectMapper objectMapper=new ObjectMapper();
	private final RestTemplate restTemplate;
	private final String url="https://api.open-meteo.com/v1/forecast?latitude=45.8&longitude=15.9&current_weather=true";
	private final String XMLfile="src/main/java/me/iis/server/files/weather_file.xml";
	private final String RNGfile="src/main/java/me/iis/server/files/RNG_validation_file.rng";
	private final String XSDfilepath="src/main/java/me/iis/server/files/XSD_validation_file.xsd";
	private final String XSDCollectionFile="src/main/java/me/iis/server/files/XSD_validation_matching_cities.xsd";

	@Autowired
	private HttpServletRequest request;
	public ServerApplication(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

	//takes username and key from which it generates jwt
	//returns jwt
	@PostMapping("/api/generateToken")
	public static String generateToken(@RequestParam(name = "user") String username, @RequestParam(name = "key") String key) {
		long currentTimeMillis = System.currentTimeMillis();
		long expirationTimeMillis = currentTimeMillis + (10 * 60 * 1000); // Token expiration time in ms

		//returns jwt key
		return Jwts.builder()
				//set username
				.setSubject(username)
				.setIssuedAt(new Date(currentTimeMillis))
				.setExpiration(new Date(expirationTimeMillis))
				//set key (must be 265B)
				.signWith(SignatureAlgorithm.HS256, key)
				.compact();
	}

	@PostMapping("/api/getCitiesRestricted")
	@ResponseBody
	//checks if the "Authorization" header jwt key matches signin key set to claims
	//takes temp as parameter, saves weather data for 10 cities to xml file, validates using jaxb against xsd
	//uses xPath to query cities with temp higher than received parameter
	//returns matching cities if validation passed, else returns empty object
	public ResponseEntity<Weather_Collection> getCitiesbyParams(@RequestParam(name = "temp") String temp, @RequestHeader("Authorization") String authorizationHeader) throws JAXBException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {

		// Extract the token from the Authorization header
		String token = authorizationHeader.replace("Bearer ", "");

		// if token is not recognized, return empty weather object as response
		try {
			// checks if the Authorization header jwt key matches signin key(throws xception)
			Claims claims = Jwts.parser()
					.setSigningKey("12345678900000000000000000000000000000000000000000000000000")
					.parseClaimsJws(token)
					.getBody();
		} catch(JwtException e){
			//if auth failed, return Unauthorized
			return new ResponseEntity<>(new Weather_Collection(new ArrayList<Current_Weather>()), new HttpHeaders(), HttpStatus.UNAUTHORIZED);
		}

		//current weather data api links for each city
		String ZagrebUrl="https://api.open-meteo.com/v1/forecast?latitude=45.8&longitude=15.9&current_weather=true";
		String HannoverUrl="https://api.open-meteo.com/v1/forecast?latitude=52.3&longitude=9.8&current_weather=true";
		String SheffieldUrl="https://api.open-meteo.com/v1/forecast?latitude=53.4&longitude=-1.4&current_weather=true";
		String LinzUrl="https://api.open-meteo.com/v1/forecast?latitude=48.3&longitude=14.3&current_weather=true";
		String PlovdivUrl="https://api.open-meteo.com/v1/forecast?latitude=42.1&longitude=24.8&current_weather=true";
		String MalagaUrl="https://api.open-meteo.com/v1/forecast?latitude=36.7&longitude=-4.5&current_weather=true";
		String AalborgUrl="https://api.open-meteo.com/v1/forecast?latitude=57.0&longitude=9.9&current_weather=true";
		String InnsbruckUrl="https://api.open-meteo.com/v1/forecast?latitude=47.2&longitude=11.3&current_weather=true";
		String MilanoUrl="https://api.open-meteo.com/v1/forecast?latitude=45.4&longitude=9.2&current_weather=true";
		String CraiovaUrl="https://api.open-meteo.com/v1/forecast?latitude=44.3&longitude=23.8&current_weather=true";

		List<String> fetchLinks = Arrays.asList(ZagrebUrl, HannoverUrl, SheffieldUrl, LinzUrl, PlovdivUrl, MalagaUrl, AalborgUrl,
				InnsbruckUrl,MilanoUrl,CraiovaUrl);

		List<Current_Weather> citiesWeather=new ArrayList<>();

		//from each city api link, retrieve data, save to object & add to array
		for (String city: fetchLinks
			 ) {
			citiesWeather.add(getWeatherforCity(city));
		}

		//initialize wrapper object that server as XML root element
		Weather_Collection XMLRootElement=new Weather_Collection(citiesWeather);

		//jaxb marshalling
		ObjectToXML(XMLRootElement);

		try {
			//validate the file with XML object collection against XSD_validation_matching_cities.xsd using JAXB validator on MyXMLValidator
			MyValidator.JAXBvalidateXSD(XSDCollectionFile, XMLfile);
		} catch (Exception e){
			e.printStackTrace();
			//if validator returns error, return empty XML object and set status to CONFLICT
			return new ResponseEntity<>(new Weather_Collection(new ArrayList<Current_Weather>()), new HttpHeaders(), HttpStatus.CONFLICT);
		}

		//minTempQuery returns Weather_Collection with cities that match temperature query
		return new ResponseEntity<>(minTempQuery(temp), new HttpHeaders(), HttpStatus.CREATED);
	}

	@PostMapping("/api/getXML")
	//saves Zagreb weather data to xml file, validates against XSD without JAXB and against RNG, if passed returns object, else returns empty object
	public ResponseEntity<Current_Weather> getCurrentZagreb() throws JAXBException, FileNotFoundException, SAXException {

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader(headerName);
			System.out.println(headerName + ": " + headerValue);
		}

		//fetch and save to object
		Current_Weather currentWeather=JSONtoObject();

		//saves fetched Current_Weather to XML file
		ObjectToXML(currentWeather);


		Validator XSDvalidator= MyValidator.InitXSDValidator(XSDfilepath);

		try {
			//xsd validation
			XSDvalidator.validate(new StreamSource(getFile("src/main/java/me/iis/server/files/weather_file.xml")));

			//rng validation
			MyValidator.ValidateRNG(XMLfile,RNGfile);

			//if no error is thrown during xml file validation
			//create entity for xml server response together with RequestMapping annotation
			ResponseEntity<Current_Weather> res=new ResponseEntity<>(currentWeather, new HttpHeaders(), HttpStatus.CREATED);
			return res;
		} catch (SAXException e) {
			//debug
			System.out.println(e.getMessage());

			//if any exception is thrown during validation, xml is not valid and empty xml object is sent as response
			return new ResponseEntity<>(new Current_Weather(), new HttpHeaders(), HttpStatus.CONFLICT);
		} catch (Exception e) {
			//if any exception is thrown during validation, xml is not valid and empty xml object is sent as response
			System.out.println(e.getMessage());
			return new ResponseEntity<>(new Current_Weather(), new HttpHeaders(), HttpStatus.CONFLICT);
		}

	}

	//receives any object and parses to XML file
	private void ObjectToXML(Object ObjecttoMarshal) throws JAXBException {
		//turns current weather object to XML and saves to file for validation
		JAXBContext context = JAXBContext.newInstance(ObjecttoMarshal.getClass());
		Marshaller mar= context.createMarshaller();
		mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		mar.marshal(ObjecttoMarshal, new File("src/main/java/me/iis/server/files/weather_file.xml"));
	}

	private Current_Weather JSONtoObject() {
//fetches current weather JSON, maps to Weather_Class object and returns Weather_Class.Current_Weather object
		Weather_Class res= null;
		try {
			res = objectMapper.readValue(this.restTemplate.getForObject(url, String.class), Weather_Class.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		return res.current_weather;
	}

	private Current_Weather getWeatherforCity (String city) {
		Weather_Class res= null;
		try {
			res = objectMapper.readValue(this.restTemplate.getForObject(city, String.class), Weather_Class.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		return res.current_weather;
	}
//queries xml file using xPath and returns Weather_Collection object with cities that match minTemp requirement
	private Weather_Collection minTempQuery(String minTemp) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		FileInputStream fileIS = new FileInputStream(XMLfile);
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document xmlDocument = builder.parse(fileIS);
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "/weatherCollection/weather[temperature>" + minTemp + "]";
		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

		List<Current_Weather> matchingWeatherObjects=new ArrayList<Current_Weather>();

		//for each match returned by xpath processor node list
		for (int i = 0; i < nodeList.getLength(); i++) {

			//split .getParentNode.getTextContent() string which returns raw element values all in one string (first, set delimiter for easier split)
			String[] cityValues=nodeList.item(i).getTextContent()
					.replaceAll("        ", "|")
					.split("\\|");

			//create and fill Current_Weather object
			Current_Weather matchingCity=new Current_Weather();

			for (int j = 0; j < cityValues.length; j++) {
				switch (j) {
					case 1:
						matchingCity.temperature = Double.valueOf(cityValues[1].trim());
						break;
					case 2:
						matchingCity.windspeed = Double.valueOf(cityValues[2].trim());
						break;
					case 3:
						matchingCity.winddirection = Double.valueOf(cityValues[3].trim());
						break;
					case 4:
						matchingCity.weathercode = Integer.valueOf(cityValues[4].trim());
						break;
					case 5:
						matchingCity.time = cityValues[5];
						break;
				}

			}

			//add object to array
			matchingWeatherObjects.add(matchingCity);
		}

		//append list to Weather_Collection as XML wrapper
		return new Weather_Collection(matchingWeatherObjects);

	}

	@RabbitListener(queues = "rpc_queue")
	public void processMessage(@Payload String message) {
		System.out.println("Received message: " + message);
	}

}
