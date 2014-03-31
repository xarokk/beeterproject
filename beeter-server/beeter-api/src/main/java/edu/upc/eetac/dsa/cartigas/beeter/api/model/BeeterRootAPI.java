package edu.upc.eetac.dsa.cartigas.beeter.api.model;
import java.util.List;

import javax.ws.rs.core.Link;
 
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.InjectLinks;
 
import edu.upc.eetac.dsa.cartigas.beeter.api.BeeterRootAPIResource;
import edu.upc.eetac.dsa.cartigas.beeter.api.MediaType;
import edu.upc.eetac.dsa.cartigas.beeter.api.StingResource;
 
public class BeeterRootAPI {
	@InjectLinks({
			@InjectLink(resource = BeeterRootAPIResource.class, style = Style.ABSOLUTE, rel = "self bookmark home", title = "Beeter Root API"),
			@InjectLink(resource = StingResource.class, style = Style.ABSOLUTE, rel = "collection", title = "Latest stings", type = MediaType.BEETER_API_STING_COLLECTION) })
	private List<Link> links;
 
	public List<Link> getLinks() {
		return links;
	}
 
	public void setLinks(List<Link> links) {
		this.links = links;
	}
}