package com.amos.ams.agent;

import com.amos.ams.config.AppProperties;
import com.amos.ams.domain.User;
import com.amos.ams.dto.Dtos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowAgentServiceTest {

    @Test
    void usesToolsThenAnswers() {
        LlmClient llm = mock(LlmClient.class);
        WorkflowAgentTools tools = mock(WorkflowAgentTools.class);
        AppProperties props = new AppProperties();
        props.getGrok().setApiKey("xai-test");
        props.getGrok().setMaxSteps(4);

        when(llm.complete(anyList(), anyList())).thenReturn(
                new LlmClient.ChatResult(null, List.of(
                        new LlmClient.ToolCall("call_1", "dashboard", "{}")
                )),
                new LlmClient.ChatResult("3 overdue jobs. I would escalate them if apply is on.", List.of())
        );
        when(tools.execute(eq("dashboard"), eq("{}"), any(), eq(false))).thenReturn("{\"overdue\":3}");

        WorkflowAgentService agent = new WorkflowAgentService(llm, tools, props);
        User actor = new User();
        actor.setUsername("manager");

        Dtos.AgentReply reply = agent.run(new Dtos.AgentAsk("What is overdue?", false), actor);

        assertFalse(reply.applied());
        assertEquals("3 overdue jobs. I would escalate them if apply is on.", reply.answer());
        assertEquals(1, reply.steps().size());
        assertEquals("dashboard", reply.steps().get(0).tool());
        verify(tools).execute("dashboard", "{}", actor, false);
    }
}
