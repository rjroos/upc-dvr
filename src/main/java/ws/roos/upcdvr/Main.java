package ws.roos.upcdvr;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Main {
	
	private String username = "ditisnietmijnusername";
	private String password = "ditisnietmijnwachtwoord";
	private boolean debug = false;
	
	private String zoeken = "Nieuwsuur";
	
	public static void main(String[] args) throws IOException {
		Main main = new Main();
		main.main();
	}
	
	private void main() throws IOException {
		HttpClient client = new HttpClient();
		
		// GetMethod gm = new GetMethod("https://tvgids.upc.nl/customerApi/api/User/session");
		// execute(client, gm);
		
		PostMethod pm = new PostMethod("https://tvgids.upc.nl/customerApi/api/User/session");
		pm.addParameter("rememberMe", "false");
		pm.addParameter("username", username);
		pm.addParameter("password", password);
		execute(client, pm);
		
		GetMethod gm = new GetMethod("https://tvgids.upc.nl/TV/wa/Search?q=" + zoeken + "&city=");
		String html = execute(client, gm);
		
		Document d = XMLUtils.htmlToDocument(html);
		List<Element> results = XMLUtils.getXPathList(
				d.getDocumentElement(), "//table[@id='search_results']//tbody");
		for (Element result : results) {
			Element eProgramRow = XMLUtils.getXPathNode(
					result, "./tr/td[@class='program-name']");
			List<Element> eEpisodes = XMLUtils.getXPathList(
					result, "./tr/td[@class='program_details']");
			
			out("Programma: " + XMLUtils.getContent(eProgramRow));
			for (Element eEpisode : eEpisodes) {
				// <a href="/TV/?eId=eid_109951313_6t_2011-09-08T12:30Z">14:30 - 14:55 / do / 08 sep</a>
				Element eName = XMLUtils.getChildNode(eEpisode, "a");
				Element eDetail = XMLUtils.getChildNode(eEpisode, "p");
				
				Pattern p = Pattern.compile("\\?eId=eid_(\\d+)_");
				Matcher m = p.matcher(eName.getAttribute("href"));
				if (! m.find()) {
					throw new IllegalStateException(
							"Geen eId voor " + XMLUtils.format(eName, 0));
				}

				debug(" - " + XMLUtils.getContent(eName));
				debug(" - " + XMLUtils.getContent(eDetail));
				
				String opneemResult = neemOp(client, Integer.parseInt(m.group(1)));
				
				out(" - aflevering " + XMLUtils.getContent(eName) + ": " + opneemResult);
				
				sleep(1000);
			}
		}
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private String /* result */ neemOp(HttpClient client, int eventId) throws IOException {
		// POST https://tvgids.upc.nl/customerApi/api/User/bookings
		// shouldOverride=false&eventId=110681864
		PostMethod pm = new PostMethod("https://tvgids.upc.nl/customerApi/api/User/bookings");
		pm.addParameter("shouldOverride", "false");
		pm.addParameter("eventId", "" + eventId);
		String result = execute(client, pm);

		JSONObject json;
		if (result.startsWith("{")) {
			json = JSONObject.fromObject(result);
		} else {
			JSONArray arr = JSONArray.fromObject(result);
			json = arr.getJSONObject(0);
		}
		
		if (json.has("err")) {
			debug("response: " + json);
			return "ERROR: " + json.getString("msg");
		}
		return json.getString("status");
	}

	private String execute(HttpClient client, HttpMethod gm)
			throws IOException, HttpException {
		debug(" request: " + gm.getPath());
		int response = client.executeMethod(gm);
		debug("response: " + response);
		String str = gm.getResponseBodyAsString();
		debug("response: " + str);
		return str;
	}
	
	private void debug(String s) {
		if (debug ) {
			System.err.println(s);
		}
	}
	
	private void out(String s) {
		System.out.println(s);
	}
}
