package demo.quarkus.mcp;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/poem")
public class PoemResource {


    @Inject
    PoemService poemService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)    
    public String create() {
        
        return poemService.writePoem("English");
    }
}