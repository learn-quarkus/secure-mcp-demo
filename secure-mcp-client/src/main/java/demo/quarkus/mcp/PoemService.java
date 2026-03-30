package demo.quarkus.mcp;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

@RegisterAiService
public interface PoemService {
    @UserMessage("""
            Write a 1 line  poem about a Java programming language.
            Dedicate the poem to the service account, refer to this account by its name.""")
    @McpToolBox("service-account-name")
    String writePoem(String language);
}
